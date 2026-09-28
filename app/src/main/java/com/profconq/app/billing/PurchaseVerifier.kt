package com.profconq.app.billing

import com.profconq.app.api.BillingApi
import com.profconq.app.api.BillingApiException
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

/**
 * The only writer of the local entitlement mirror. Every answer it takes carries the account it
 * was read for, so a late reply of a previous account cannot reach the current one.
 */
interface PremiumSink {
    suspend fun applyPremiumSnapshot(at: AccountToken, snapshot: PremiumSnapshot)

    /** Sign-out or an account switch: whatever the leaving account confirmed is dropped. */
    suspend fun onAccountChanged(at: AccountToken)
}

/**
 * The single verify pipeline: every `PURCHASED` purchase, however it surfaced, goes through
 * `POST /api/billing/google/verify` at most once per token and per account, and only the server
 * answer changes entitlement. The client never reads access out of a Play purchase by itself.
 *
 * Purchase tokens live only inside [submit] for the duration of the request: they are never
 * persisted, never keyed by value (digests are), and never part of an outcome or message.
 */
class PurchaseVerifier(
    private val feed: PurchaseFeed,
    private val api: BillingApi,
    private val sink: PremiumSink,
    private val accounts: BillingAccountCoordinator,
    private val throttle: VerifyThrottle,
    private val isLoggedIn: () -> Boolean,
    private val scope: CoroutineScope,
) {
    sealed class Outcome {
        /** The account this outcome belongs to: a stale one may not move the current UI. */
        abstract val at: AccountToken

        data class Applied(val snapshot: PremiumSnapshot, override val at: AccountToken) : Outcome()
        data class Failed(val error: BillingError, override val at: AccountToken) : Outcome()
        data class Pending(override val at: AccountToken) : Outcome()
        data class SignedOut(override val at: AccountToken) : Outcome()
    }

    /**
     * Where a token digest stands. One digest is in exactly one phase, so a queued token is never
     * also in flight, and a confirmed one is never verified again.
     */
    private enum class Phase { Queued, InFlight, Confirmed }

    private val _outcomes = MutableSharedFlow<Outcome>(extraBufferCapacity = 64)
    val outcomes: SharedFlow<Outcome> = _outcomes.asSharedFlow()

    private val phases = ConcurrentHashMap<String, Phase>()

    init {
        scope.launch {
            feed.purchases.collect { purchase -> submit(purchase) }
        }
    }

    /**
     * Binds the pipeline to [uid] (kept only as its in-memory digest): raises the generation on any
     * real account change, drops the throttle window and the verified digests of the account that
     * left, and lets the sink clear the local mirror. A repeat call for the same account is inert.
     */
    suspend fun onAccountChanged(uid: String?): AccountToken {
        val at = accounts.onUid(uid)
        throttle.reset()
        phases.clear()
        sink.onAccountChanged(at)
        return at
    }

    /** The identity an asynchronous result has to be tagged with before it starts. */
    fun currentAccount(): AccountToken = accounts.current

    /** Read the entitlement from the server. A failed read leaves the last snapshot standing. */
    suspend fun refreshStatus(): Outcome {
        val at = accounts.current
        return runCatching { api.status() }
            .fold(
                onSuccess = { snapshot ->
                    if (accounts.isCurrent(at)) sink.applyPremiumSnapshot(at, snapshot)
                    Outcome.Applied(snapshot, at)
                },
                onFailure = { failure -> Outcome.Failed(errorOf(failure), at) },
            )
            .also { _outcomes.tryEmit(it) }
    }

    /** Re-read purchases from Play: used on start-up, on restore and after a purchase refusal. */
    suspend fun restore(): Outcome? {
        val at = accounts.current
        if (!isLoggedIn()) {
            val signedOut = Outcome.SignedOut(at)
            _outcomes.tryEmit(signedOut)
            return signedOut
        }
        val ok = runCatching { feed.refreshPurchases() }.getOrDefault(false)
        if (ok) return null
        val failed = Outcome.Failed(BillingError.ServiceUnavailable, at)
        _outcomes.tryEmit(failed)
        return failed
    }

    suspend fun submit(purchase: ClientPurchase) {
        val productId = purchase.productId
        if (productId != null && productId !in BillingContract.knownProductIds) return
        val at = accounts.current
        when {
            // PENDING is a Play state, not access: no verification, no Premium.
            purchase.isPending -> _outcomes.tryEmit(Outcome.Pending(at))
            // UNSPECIFIED_STATE says nothing about ownership; the server decides on its own read.
            isUnspecifiedPurchaseState(purchase.state) -> Unit
            // Without an authorized user there is nobody to attach the purchase to, so the token
            // is dropped rather than held back for a later attempt.
            !isLoggedIn() -> _outcomes.tryEmit(Outcome.SignedOut(at))
            else -> verify(purchase, productId, at)
        }
    }

    private suspend fun verify(purchase: ClientPurchase, productId: String?, at: AccountToken) {
        val digest = purchase.tokenDigest
        if (phases.putIfAbsent(digest, Phase.Queued) != null) return
        // The throttle waits for a slot instead of dropping the purchase: the 5th token of a window
        // is neither lost nor marked verified. Losing the account cancels the wait.
        if (!throttle.acquire(at)) {
            phases.remove(digest)
            return
        }
        if (!accounts.isCurrent(at)) {
            phases.remove(digest)
            return
        }
        phases[digest] = Phase.InFlight
        try {
            val snapshot = api.verify(productId ?: BillingContract.PREMIUM_PRODUCT_ID, purchase.purchaseToken)
            if (!accounts.isCurrent(at)) return
            sink.applyPremiumSnapshot(at, snapshot)
            phases[digest] = Phase.Confirmed
            _outcomes.tryEmit(Outcome.Applied(snapshot, at))
        } catch (failure: Throwable) {
            // A refused token may be retried by the next purchase re-query, so it is not remembered.
            phases.remove(digest)
            if (accounts.isCurrent(at)) _outcomes.tryEmit(Outcome.Failed(errorOf(failure), at))
        }
    }

    private fun errorOf(failure: Throwable): BillingError = when (failure) {
        is BillingApiException -> failure.error
        else -> BillingError.Network
    }
}
