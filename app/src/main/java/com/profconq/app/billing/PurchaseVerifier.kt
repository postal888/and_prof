package com.profconq.app.billing

import com.profconq.app.api.BillingApi
import com.profconq.app.api.BillingApiException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
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
 * An explicit [restore] is the only thing that sends a confirmed token a second time, and then only
 * the one the server itself reported as stale.
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
    private val clock: () -> Long = { System.nanoTime() / 1_000_000L },
    private val restoreCooldownMs: Long = RestoreCooldownMs,
) {
    sealed class Outcome {
        /** The account this outcome belongs to: a stale one may not move the current UI. */
        abstract val at: AccountToken

        data class Applied(val snapshot: PremiumSnapshot, override val at: AccountToken) : Outcome()
        data class Failed(val error: BillingError, override val at: AccountToken) : Outcome()
        data class Pending(override val at: AccountToken) : Outcome()
        data class SignedOut(override val at: AccountToken) : Outcome()
    }

    private sealed interface Phase {
        data class Queued(val restoreNextAt: Long?) : Phase
        data object InFlight : Phase
        data class Confirmed(val stale: Boolean, val nextRestoreAt: Long) : Phase
        data class RetryableFailure(val nextRestoreAt: Long) : Phase
        data class RestoreQueued(val nextRestoreAt: Long) : Phase
        data object Rejected : Phase
    }

    companion object {
        const val RestoreCooldownMs = 60_000L
    }

    private val _outcomes = MutableSharedFlow<Outcome>(extraBufferCapacity = 64)
    val outcomes: SharedFlow<Outcome> = _outcomes.asSharedFlow()

    private val phases = ConcurrentHashMap<String, Phase>()

    /** One re-read of Play at a time; a press that arrives during it is coalesced, not queued. */
    private val recheckRunning = AtomicBoolean(false)

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

    /**
     * Re-read purchases from Play: used on start-up, on restore and after a purchase refusal.
     *
     * The re-check is single-flight, so a press that lands while a re-read is already running is
     * ignored instead of asking Play a second time. Every press still queries Play for discovery,
     * but only stale or retryable digests whose per-token cooldown elapsed may pass dedup.
     */
    suspend fun restore(): Outcome? {
        val at = accounts.current
        if (!isLoggedIn()) {
            val signedOut = Outcome.SignedOut(at)
            _outcomes.tryEmit(signedOut)
            return signedOut
        }
        if (!recheckRunning.compareAndSet(false, true)) return null
        val ok = try {
            val now = clock()
            phases.entries.toList().forEach { (digest, phase) ->
                val eligible = when (phase) {
                    is Phase.Confirmed -> phase.stale && now >= phase.nextRestoreAt
                    is Phase.RetryableFailure -> now >= phase.nextRestoreAt
                    is Phase.RestoreQueued -> now >= phase.nextRestoreAt
                    else -> false
                }
                if (eligible) {
                    phases.replace(digest, phase, Phase.RestoreQueued(now + restoreCooldownMs))
                }
            }
            runCatching { feed.refreshPurchases() }.getOrDefault(false)
        } finally {
            recheckRunning.set(false)
        }
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
            purchase.isPending -> _outcomes.tryEmit(Outcome.Pending(at))
            isUnspecifiedPurchaseState(purchase.state) -> Unit
            !isLoggedIn() -> _outcomes.tryEmit(Outcome.SignedOut(at))
            else -> queue(purchase, productId, at)
        }
    }

    private suspend fun queue(purchase: ClientPurchase, productId: String?, at: AccountToken) {
        val digest = purchase.tokenDigest
        while (true) {
            val current = phases[digest]
            val queued = when (current) {
                null -> Phase.Queued(restoreNextAt = null)
                is Phase.RestoreQueued -> Phase.Queued(restoreNextAt = current.nextRestoreAt)
                else -> return
            }
            val claimed = if (current == null) {
                phases.putIfAbsent(digest, queued) == null
            } else {
                phases.replace(digest, current, queued)
            }
            if (claimed) {
                verify(purchase, productId, at, queued)
                return
            }
        }
    }

    private suspend fun verify(
        purchase: ClientPurchase,
        productId: String?,
        at: AccountToken,
        queued: Phase.Queued,
    ) {
        val digest = purchase.tokenDigest
        if (!throttle.acquire(at)) {
            phases.remove(digest, queued)
            return
        }
        if (!accounts.isCurrent(at) || !phases.replace(digest, queued, Phase.InFlight)) {
            phases.remove(digest, queued)
            return
        }
        try {
            val snapshot = api.verify(productId ?: BillingContract.PREMIUM_PRODUCT_ID, purchase.purchaseToken)
            if (!accounts.isCurrent(at)) return
            sink.applyPremiumSnapshot(at, snapshot)
            phases[digest] = Phase.Confirmed(
                stale = snapshot.stale,
                nextRestoreAt = queued.restoreNextAt ?: clock(),
            )
            _outcomes.tryEmit(Outcome.Applied(snapshot, at))
        } catch (failure: Throwable) {
            if (!accounts.isCurrent(at)) {
                phases.remove(digest)
                return
            }
            val error = errorOf(failure)
            phases[digest] = if (isTransientBillingError(error)) {
                Phase.RetryableFailure(queued.restoreNextAt ?: clock() + restoreCooldownMs)
            } else {
                Phase.Rejected
            }
            _outcomes.tryEmit(Outcome.Failed(error, at))
        }
    }

    private fun errorOf(failure: Throwable): BillingError = when (failure) {
        is BillingApiException -> failure.error
        else -> BillingError.Network
    }
}
