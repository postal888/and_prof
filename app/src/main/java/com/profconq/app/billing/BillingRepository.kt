package com.profconq.app.billing

import android.app.Activity
import android.content.Context
import android.os.Looper
import android.os.SystemClock
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

enum class BillingConnection {
    Disconnected,
    Connecting,
    Connected,
    Failed,
}

/** One purchasable base plan, read entirely out of `ProductDetails`. */
data class PlanOffer(
    val basePlanId: String,
    val title: String,
    val price: String,
    val offerToken: String,
) {
    override fun toString(): String = "PlanOffer($basePlanId, $title, $price)"
}

/** One offer that may be launched, with the `ProductDetails` Play answered for it. */
class LaunchableOffer(val details: ProductDetails, val offer: PlanOffer) {
    /** The token is Play data for the sheet only; it never appears in a rendered string. */
    override fun toString(): String = "LaunchableOffer(${offer.basePlanId})"
}

/**
 * Everything one `queryProductDetailsAsync` call said: the products, the products Play refused to
 * return with the reason for each, and the offers a sheet may be opened for. `debugMessage` is
 * Play's free text and is deliberately absent. The only tokens here live in [launchable], which is
 * handed straight to `launchBillingFlow` and never reaches the UI state, a log or an exception.
 */
class ProductDetailsQueryResult(
    val responseCode: Int,
    /** Only the purchases-updated callback carries a sub code in Billing 9.1.0, so a query has none. */
    val subResponseCode: Int?,
    val reachedPlay: Boolean,
    val productDetails: List<ProductDetails>,
    val unfetched: List<UnfetchedProductInfo>,
    val offersSeen: Int,
    val basePlanIdsSeen: List<String>,
    val launchable: List<LaunchableOffer>,
) {
    val productsReturned: Int get() = productDetails.size
    val acceptedOffers: List<PlanOffer> get() = launchable.map { it.offer }

    /** The same answer without any Play type, for the pure outcome decision and the diagnostics. */
    val snapshot: PlayQuerySnapshot
        get() = PlayQuerySnapshot(
            responseCode = responseCode,
            subResponseCode = subResponseCode,
            reachedPlay = reachedPlay,
            productsReturned = productsReturned,
            unfetched = unfetched,
            offersSeen = offersSeen,
            basePlanIdsSeen = basePlanIdsSeen,
            acceptedOffers = acceptedOffers.map { AcceptedOffer(it.basePlanId, it.title, it.price) },
        )

    fun launchableFor(basePlanId: String): LaunchableOffer? =
        launchable.firstOrNull { it.offer.basePlanId == basePlanId }

    override fun toString(): String =
        "ProductDetailsQueryResult($responseCode, products=$productsReturned, " +
            "unfetched=${unfetched.size}, offers=$offersSeen, accepted=${launchable.size})"

    companion object {
        /** The Play service never answered: no counts are claimed for a call that did not run. */
        fun notReached(responseCode: Int) = ProductDetailsQueryResult(
            responseCode = responseCode,
            subResponseCode = null,
            reachedPlay = false,
            productDetails = emptyList(),
            unfetched = emptyList(),
            offersSeen = 0,
            basePlanIdsSeen = emptyList(),
            launchable = emptyList(),
        )
    }
}

/** The part of the Play side the verification pipeline needs, kept small enough to fake in tests. */
interface PurchaseFeed {
    val purchases: SharedFlow<ClientPurchase>

    suspend fun refreshPurchases(): Boolean
}

/**
 * The one process-wide BillingClient. Built with auto service reconnection, so
 * [onBillingServiceDisconnected] only mirrors the state and never hand-rolls a backoff restart.
 * Nothing here decides entitlement: purchases are forwarded for server verification, and Premium
 * is read back from the server snapshot only.
 */
class BillingRepository(
    context: Context,
    private val scope: CoroutineScope,
) : BillingClientStateListener, PurchasesUpdatedListener, PurchaseFeed {
    private val appContext = context.applicationContext

    private val _connection = MutableStateFlow(BillingConnection.Disconnected)
    val connection: StateFlow<BillingConnection> = _connection.asStateFlow()

    /**
     * Fired when the Play service became usable again after having been lost. This is the event a
     * plan list is rebuilt on: a reconnect used to re-read purchases only, so a catalogue that had
     * come back empty stayed empty for the whole process.
     */
    private val _serviceReconnected = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val serviceReconnected: SharedFlow<Unit> = _serviceReconnected.asSharedFlow()

    /** The documented code of the last setup attempt, kept so a failure can name its own reason. */
    @Volatile
    private var lastSetupCode = PlayResponseCode.SERVICE_DISCONNECTED

    /** Every purchase Play tells us about, from any source, without a local gate. */
    private val _purchases = MutableSharedFlow<ClientPurchase>(extraBufferCapacity = 64)
    override val purchases: SharedFlow<ClientPurchase> = _purchases.asSharedFlow()

    /** One Play sheet at a time; released by its callback, the launch answer, or the owner's end. */
    private val gate = LaunchGate(clock = { SystemClock.elapsedRealtime() })

    private val client: BillingClient = BillingClient.newBuilder(appContext)
        .setListener(this)
        .enablePendingPurchases(
            // monthly/annual are auto-renewing subscriptions; enablePrepaidPlans() stays unset.
            PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
        )
        .enableAutoServiceReconnection()
        .build()

    override fun onBillingSetupFinished(result: BillingResult) {
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            lastSetupCode = result.responseCode
            _connection.value = BillingConnection.Failed
            return
        }
        // The first successful setup is answered by the caller that waited for it, so only a
        // connection that had been lost counts as a reconnect. Nothing is loaded from here
        // directly: that would make the load's own connect() re-enter this callback.
        val wasLost = _connection.value == BillingConnection.Disconnected ||
            _connection.value == BillingConnection.Failed
        lastSetupCode = BillingClient.BillingResponseCode.OK
        _connection.value = BillingConnection.Connected
        // Own the re-query here rather than in the UI: a purchase made outside this app (or a
        // suspended renewal) must reach the server even when Profile was never opened.
        scope.launch { refreshPurchases() }
        if (wasLost) _serviceReconnected.tryEmit(Unit)
    }

    override fun onBillingServiceDisconnected() {
        // State only: enableAutoServiceReconnection() owns the retry clock.
        _connection.value = BillingConnection.Disconnected
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: List<Purchase>?) {
        // Deliberately no "did we launch the flow" check: a purchase can arrive unprompted, and
        // every one of them has to be verified by the server.
        gate.release()
        // This is the one callback in Billing 9.1.0 that carries a sub code. Only the documented
        // constant is read out of it; `debugMessage` is Play's free text and never leaves here.
        val subResponseCode = result.onPurchasesUpdatedSubResponseCode
        purchases?.forEach { purchase -> forward(purchase, subResponseCode) }
    }

    private fun forward(purchase: Purchase, subResponseCode: Int = PlaySubResponseCode.NoApplicable) {
        _purchases.tryEmit(
            ClientPurchase(
                state = purchase.purchaseState,
                purchaseToken = purchase.purchaseToken,
                productId = purchase.products.firstOrNull(),
                suspended = purchase.isSuspended,
                subResponseCode = subResponseCode,
            )
        )
    }

    fun connect() {
        if (client.isReady) {
            _connection.value = BillingConnection.Connected
            return
        }
        // A setup already in flight is left to the library: with auto service reconnection on,
        // starting a second connection here would be a hand-rolled retry clock next to Play's own.
        if (client.connectionState == BillingClient.ConnectionState.CONNECTING) return
        _connection.value = BillingConnection.Connecting
        client.startConnection(this)
    }

    /**
     * Null once Play answers ready, otherwise the documented code for why it did not. The `Failed`
     * value of an earlier attempt says nothing about the connection being set up now, so only a
     * real `Connected` settles this wait.
     */
    private suspend fun awaitReady(): Int? {
        if (client.isReady) return null
        connect()
        val settled = withTimeoutOrNull(ConnectTimeoutMs) {
            _connection.first { it == BillingConnection.Connected }
        }
        return if (settled == BillingConnection.Connected) null else lastSetupCode
    }

    /**
     * Read owned subscriptions back from Play. Suspended ones are included so the server gets to
     * judge them; the client never reads access out of a purchase on its own.
     */
    override suspend fun refreshPurchases(): Boolean = withContext(Dispatchers.Main.immediate) {
        if (awaitReady() != null) return@withContext false
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .includeSuspendedSubscriptions(true)
            .build()
        val response = suspendCancellableCoroutine { cont ->
            client.queryPurchasesAsync(params) { result, list ->
                // A cancelled caller must not be revived by a late Play callback.
                if (cont.isActive) cont.resume(result to list)
            }
        }
        if (response.first.responseCode != BillingClient.BillingResponseCode.OK) return@withContext false
        response.second.forEach { forward(it) }
        true
    }

    /**
     * Play's catalogue for the premium product: the products it returned, the products it would
     * not return with the reason for each, and the offers a sheet may be opened for. Nothing is
     * cached here, so a plan list is only ever as fresh as the load that produced it.
     */
    suspend fun queryPremiumOffers(): ProductDetailsQueryResult =
        withContext(Dispatchers.Main.immediate) {
            val notReady = awaitReady()
            if (notReady != null) return@withContext ProductDetailsQueryResult.notReached(notReady)
            val params = QueryProductDetailsParams.newBuilder()
                .setProductList(
                    listOf(
                        QueryProductDetailsParams.Product.newBuilder()
                            .setProductId(BillingContract.PREMIUM_PRODUCT_ID)
                            .setProductType(BillingClient.ProductType.SUBS)
                            .build()
                    )
                )
                .build()
            val response = suspendCancellableCoroutine { cont ->
                client.queryProductDetailsAsync(params) { result, queryResult ->
                    if (cont.isActive) cont.resume(result to queryResult)
                }
            }
            val products = response.second.productDetailsList.orEmpty()
                .filter { it.productId == BillingContract.PREMIUM_PRODUCT_ID }
            val unfetched = response.second.unfetchedProductList.orEmpty().map { product ->
                UnfetchedProductInfo(
                    productId = product.productId,
                    productType = product.productType,
                    statusCode = product.statusCode,
                )
            }
            val rows = products.flatMap { offerRowsFor(it) }
            val launchable = products.flatMap { details ->
                offersFor(details).map { offer -> LaunchableOffer(details, offer) }
            }
            ProductDetailsQueryResult(
                responseCode = response.first.responseCode,
                // Deliberately null: `getOnPurchasesUpdatedSubResponseCode` belongs to the
                // purchases-updated callback, and a product query has no sub code of its own.
                subResponseCode = null,
                reachedPlay = true,
                productDetails = products,
                unfetched = unfetched,
                offersSeen = rows.size,
                basePlanIdsSeen = rows.mapNotNull { it.basePlanId }.distinct(),
                launchable = launchable,
            )
        }

    /** Every offer row Play reported for one product, before any of the selection rules run. */
    private fun offerRowsFor(details: ProductDetails): List<OfferCandidate> =
        details.subscriptionOfferDetails.orEmpty().map { offer ->
            val phases = offer.pricingPhases?.pricingPhaseList.orEmpty()
            OfferCandidate(
                basePlanId = offer.basePlanId,
                offerId = offer.offerId,
                hasPricingPhase = phases.isNotEmpty(),
                formattedPrice = phases.firstOrNull()?.formattedPrice,
                offerToken = offer.offerToken,
            )
        }

    /**
     * Base plans offered for purchase, in UI order: at most one row per `basePlanId`, always the
     * standard record (no trial or promotional offer), never a row without a price or an offer
     * token. The server catalogue is the allowlist that decides which of these may be launched.
     */
    fun offersFor(details: ProductDetails): List<PlanOffer> {
        val title = details.name.nonBlank() ?: details.title.nonBlank()
        return selectStandardOffers(offerRowsFor(details)).map { offer ->
            PlanOffer(
                basePlanId = offer.basePlanId,
                title = title ?: offer.basePlanId,
                price = offer.price,
                offerToken = offer.offerToken,
            )
        }
    }

    /**
     * Opens the Play sheet. Returns the response code, or [BusyLaunchCode] when a sheet is already
     * up: the guard only protects against a double launch, never against handling a purchase.
     *
     * The gate is released by the launch answer, by the purchases callback, and - since a sheet the
     * user leaves behind has no callback at all - by the destruction of the Activity that owns it.
     * Only that Activity's `Lifecycle` is captured, never the Activity itself.
     */
    fun launchBillingFlow(activity: Activity, details: ProductDetails, offer: PlanOffer): Int {
        check(Looper.myLooper() == Looper.getMainLooper()) { "launchBillingFlow needs the main thread" }
        if (!gate.tryBegin()) return BusyLaunchCode
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(details)
                        .setOfferToken(offer.offerToken)
                        // setObfuscatedAccountId/ProfileId are intentionally not set: the server
                        // has no opaque account id to hand back yet, and email/ANDROID_ID would be
                        // personal data. The in-memory account digest is not one either: it is
                        // never sent. TODO(billing): use the server account id once /me returns it.
                        .build()
                )
            )
            .build()
        val result = client.launchBillingFlow(activity, params)
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            gate.release()
            return result.responseCode
        }
        (activity as? LifecycleOwner)?.let { owner -> watchFlowOwner(owner.lifecycle) }
        return result.responseCode
    }

    private fun watchFlowOwner(lifecycle: Lifecycle) {
        if (lifecycle.currentState == Lifecycle.State.DESTROYED) {
            gate.onActivityDestroyed()
            return
        }
        val observer = object : LifecycleEventObserver {
            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                // ON_DESTROY only: leaving the Premium section is not the end of a Play flow.
                if (event != Lifecycle.Event.ON_DESTROY) return
                source.lifecycle.removeObserver(this)
                gate.onActivityDestroyed()
            }
        }
        lifecycle.addObserver(observer)
    }

    private fun String?.nonBlank(): String? = this?.takeIf { it.isNotBlank() }

    companion object {
        private const val ConnectTimeoutMs = 10_000L

        /** Not a Play code: a local answer for "the sheet is already open". */
        const val BusyLaunchCode = -1000
    }
}

