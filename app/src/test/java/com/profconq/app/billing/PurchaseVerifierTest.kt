package com.profconq.app.billing

import com.profconq.app.api.BillingApi
import com.profconq.app.api.BillingApiException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import kotlin.coroutines.coroutineContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The verification pipeline: only a server answer changes entitlement, a purchase token never
 * leaves the call that sends it, one token is verified once, and every result belongs to the
 * account that asked for it. No Play or Android class is loaded here, which is why a purchase
 * reaches the pipeline as plain [ClientPurchase] data.
 */
class PurchaseVerifierTest {

    @Test
    fun unsolicitedPurchasedPurchaseIsVerifiedOnceAndPremiumComesFromTheAnswer() {
        runBlocking {
            withVerifier {
                assertTrue(feed.tryEmit(purchased()))
                awaitUntil { api.verifyCount == 1 }
                assertEquals(listOf(TOKEN), api.verifyTokens)
                assertEquals(listOf(BillingContract.PREMIUM_PRODUCT_ID), api.verifyProductIds)
                assertEquals(1, sink.applied.size)
                assertTrue(sink.applied.single().snapshot.isPremium)
            }
        }
    }

    @Test
    fun pendingPurchaseIsNeitherVerifiedNorGranted() {
        runBlocking {
            withVerifier {
                watchOutcomes()
                verifier.submit(purchase(state = ClientPurchaseState.PENDING))
                awaitUntil { outcomes.isNotEmpty() }
                assertEquals(0, api.verifyCount)
                assertTrue("PENDING must not reach the entitlement sink", sink.applied.isEmpty())
                assertTrue(outcomes.single() is PurchaseVerifier.Outcome.Pending)
            }
        }
    }

    @Test
    fun unspecifiedStateAndForeignProductChangeNothingLocally() {
        runBlocking {
            withVerifier {
                verifier.submit(purchase(state = ClientPurchaseState.UNSPECIFIED))
                verifier.submit(purchase(productId = "someone_elses_product"))
                delay(SettleMs)
                assertEquals(0, api.verifyCount)
                assertTrue(sink.applied.isEmpty())
                assertEquals(0, sink.accountChanges)
            }
        }
    }

    @Test
    fun purchasedWithoutSignedInUserDropsTheTokenInsteadOfHoldingIt() {
        runBlocking {
            var signedIn = false
            withVerifier(isLoggedIn = { signedIn }) {
                watchOutcomes()
                verifier.submit(purchased())
                awaitUntil { outcomes.isNotEmpty() }
                assertEquals(0, api.verifyCount)
                assertTrue(sink.applied.isEmpty())
                assertTrue(outcomes.single() is PurchaseVerifier.Outcome.SignedOut)
                signedIn = true
            }
        }
    }

    @Test
    fun oneTokenIsNeverVerifiedTwiceEvenWhenAnswersAreStillPending() {
        runBlocking {
            withVerifier {
                api.gate = CompletableDeferred()
                val first = scope.launch { verifier.submit(purchased()) }
                awaitUntil { api.verifyStarted == 1 }
                val second = scope.launch { verifier.submit(purchased()) }
                delay(SettleMs)
                assertEquals("A second call must not start while one is in flight", 1, api.verifyStarted)
                assertEquals(0, api.verifyCount)
                api.gate?.complete(Unit)
                first.join()
                second.join()
                assertEquals(1, api.verifyCount)

                // Confirmed tokens are remembered, so a re-delivered purchase is not re-verified.
                verifier.submit(purchased())
                // A different token is a different purchase and still gets its one call.
                verifier.submit(purchased(token = "another-purchase-token"))
                awaitUntil { api.verifyCount == 2 }
                assertEquals(2, api.verifyCount)
            }
        }
    }

    @Test
    fun failedStatusReadLeavesTheConfirmedPremiumStanding() {
        runBlocking {
            withVerifier {
                sink.applied.add(FakeSink.Apply(premium))
                api.statusFailure = BillingApiException(
                    BillingError.UpstreamUnavailable,
                    502,
                    "google_unavailable",
                )
                val outcome = verifier.refreshStatus()
                assertEquals(BillingError.UpstreamUnavailable, (outcome as PurchaseVerifier.Outcome.Failed).error)
                assertEquals("A 502 must not clear the entitlement", 0, sink.accountChanges)
                assertEquals(listOf(premium), sink.applied.map { it.snapshot })
                assertTrue(sink.applied.last().snapshot.isPremium)
            }
        }
    }

    @Test
    fun stalePremiumSnapshotStillReadsAsPremium() {
        runBlocking {
            withVerifier {
                api.statusResult = stalePremium
                val outcome = verifier.refreshStatus()
                val applied = outcome as PurchaseVerifier.Outcome.Applied
                assertTrue(applied.snapshot.isPremium)
                assertTrue(applied.snapshot.stale)
                assertEquals(0, sink.accountChanges)
                assertTrue(sink.applied.single().snapshot.isPremium)
            }
        }
    }

    @Test
    fun verifyRejectsNeverReEchoTheToken() {
        runBlocking {
            withVerifier {
                api.verifyFailure = BillingApiException(
                    BillingError.OwnedByOtherAccount,
                    409,
                    "token_owned_by_other_account",
                )
                watchOutcomes()
                verifier.submit(purchased())
                awaitUntil { outcomes.isNotEmpty() }
                val failure = api.verifyFailure!!
                val rendered = listOf(
                    failure.message.orEmpty(),
                    failure.toString(),
                    outcomes.single().toString(),
                    sink.toString(),
                    purchase().toString(),
                ).joinToString("\n")
                assertFalse("A purchase token leaked into user-visible text", rendered.contains(TOKEN))
                assertEquals(0, sink.applied.size)
                assertEquals(0, sink.accountChanges)
                assertEquals(
                    BillingError.OwnedByOtherAccount,
                    (outcomes.single() as PurchaseVerifier.Outcome.Failed).error,
                )
            }
        }
    }

    /** H1/M5: an answer of the account that already left may not write for the one in force. */
    @Test
    fun lateAnswerOfThePreviousAccountIsDroppedInsteadOfOverwritingTheCurrentOne() {
        runBlocking {
            val hold = CompletableDeferred<Unit>()
            withVerifier {
                api.statusGate = hold
                api.statusResult = premium
                val forA = scope.async { verifier.refreshStatus() }
                awaitUntil { api.statusStarted == 1 }

                val forB = verifier.onAccountChanged("uid-b")
                hold.complete(Unit)
                val outcome = forA.await() as PurchaseVerifier.Outcome.Applied

                assertEquals(1L, outcome.at.generation)
                assertEquals(2L, forB.generation)
                assertTrue("A must not be the current account anymore", !accounts.isCurrent(outcome.at))
                assertTrue("A late premium answer must not be applied", sink.applied.isEmpty())
                assertEquals("The switch itself clears", 1, sink.accountChanges)
            }
        }
    }

    /** Sign-out always clears the local mirror of the account that left. */
    @Test
    fun signedOutAccountLosesItsLocalMirror() {
        runBlocking {
            withVerifier {
                val before = accounts.current
                verifier.submit(purchased())
                awaitUntil { sink.applied.size == 1 }
                verifier.onAccountChanged(null)
                assertEquals(1, sink.accountChanges)
                assertFalse("The leaving account is no longer current", accounts.isCurrent(before))
                assertEquals(null, accounts.current.key)
            }
        }
    }

    /** L5: at most four outbound verifies per window; the fifth waits instead of being lost. */
    @Test
    fun fifthVerifyWaitsForTheNextWindowInsteadOfBeingDropped() {
        runBlocking {
            val clock = FakeClock()
            withVerifier(clock = clock, advanceOnWait = true) {
                repeat(VerifyThrottle.VerifyPermitsPerWindow) { index ->
                    verifier.submit(purchased(token = "token-$index"))
                }
                awaitUntil { api.verifyCount == VerifyThrottle.VerifyPermitsPerWindow }
                verifier.submit(purchased(token = "token-out-of-window"))
                awaitUntil { api.verifyCount == VerifyThrottle.VerifyPermitsPerWindow + 1 }

                assertEquals("Four calls go out at once", listOf(0L, 0L, 0L, 0L), api.verifyAt.take(4))
                assertEquals(
                    "The fifth waits for the window to move on instead of being dropped",
                    VerifyThrottle.VerifyWindowMs,
                    api.verifyAt.last(),
                )
            }
        }
    }

    /** L5: a switch cancels the queue, and the cancelled token is not marked verified. */
    @Test
    fun accountSwitchCancelsAQueuedVerify() {
        runBlocking {
            val hold = CompletableDeferred<Unit>()
            var waits = 0
            withVerifier(clock = FakeClock(), waitFor = { millis -> waits++; hold.await() }) {
                repeat(VerifyThrottle.VerifyPermitsPerWindow) { index ->
                    verifier.submit(purchased(token = "token-$index"))
                }
                awaitUntil { api.verifyCount == VerifyThrottle.VerifyPermitsPerWindow }
                val queued = scope.launch { verifier.submit(purchased(token = "queued-token")) }
                awaitUntil { waits == 1 }
                verifier.onAccountChanged("uid-b")
                hold.complete(Unit)
                queued.join()
                assertEquals("A queued token is never sent behind a switch", 4, api.verifyCount)
                assertFalse(api.verifyTokens.contains("queued-token"))

                // The cancelled token is not remembered as verified, so B's re-query can still send it.
                verifier.submit(purchased(token = "queued-token"))
                awaitUntil { api.verifyCount == VerifyThrottle.VerifyPermitsPerWindow + 1 }
                assertTrue(api.verifyTokens.contains("queued-token"))
            }
        }
    }

    private companion object {
        const val SettleMs = 40L
    }
}

private const val TOKEN = "rfcKsH7-play-purchase-token"

private val premium: PremiumSnapshot = PremiumSnapshot.parse(
    """{"plan":"premium","wordLimit":null,"entitlements":[],"governing":null,"stale":false,"checkedAt":1}"""
)

private val stalePremium: PremiumSnapshot = PremiumSnapshot.parse(
    """{"plan":"premium","wordLimit":null,"entitlements":[],"governing":null,"stale":true,"checkedAt":2}"""
)

private fun purchase(
    state: Int = ClientPurchaseState.PURCHASED,
    productId: String? = BillingContract.PREMIUM_PRODUCT_ID,
    token: String = TOKEN,
    suspended: Boolean = false,
    subResponseCode: Int = PlaySubResponseCode.NoApplicable,
) = ClientPurchase(
    state = state,
    purchaseToken = token,
    productId = productId,
    suspended = suspended,
    subResponseCode = subResponseCode,
)

private fun purchased(token: String = TOKEN) = purchase(token = token)

private class FakeClock {
    var now = 0L
    fun reading(): Long = now
}

/** Runs the verifier on the test thread, so every step below is deterministic. */
private class Harness(
    val scope: CoroutineScope,
    val accounts: BillingAccountCoordinator,
    val feed: FakeFeed,
    val api: FakeApi,
    val sink: FakeSink,
    val verifier: PurchaseVerifier,
) {
    val outcomes = mutableListOf<PurchaseVerifier.Outcome>()

    suspend fun watchOutcomes() {
        scope.launch { verifier.outcomes.collect { outcomes.add(it) } }
        yield()
    }
}

private suspend fun withVerifier(
    isLoggedIn: () -> Boolean = { true },
    uid: String? = "uid-a",
    clock: FakeClock = FakeClock(),
    advanceOnWait: Boolean = false,
    waitFor: suspend (Long) -> Unit = { },
    block: suspend Harness.() -> Unit,
) {
    val scope = CoroutineScope(coroutineContext + SupervisorJob())
    val feed = FakeFeed()
    val api = FakeApi()
    api.clock = clock::reading
    val sink = FakeSink()
    val accounts = BillingAccountCoordinator()
    if (uid != null) accounts.onUid(uid)
    val wait: suspend (Long) -> Unit = { millis ->
        if (advanceOnWait) clock.now += millis else waitFor(millis)
        yield()
    }
    val throttle = VerifyThrottle(
        accounts = accounts,
        clock = clock::reading,
        delay = wait,
    )
    val verifier = PurchaseVerifier(feed, api, sink, accounts, throttle, isLoggedIn, scope)
    yield()
    try {
        block(Harness(scope, accounts, feed, api, sink, verifier))
    } finally {
        scope.cancel()
    }
}

private suspend fun awaitUntil(condition: () -> Boolean) {
    withTimeout(2_000) {
        while (!condition()) delay(5)
    }
}

private class FakeFeed : PurchaseFeed {
    private val source = MutableSharedFlow<ClientPurchase>(extraBufferCapacity = 64)
    override val purchases: SharedFlow<ClientPurchase> get() = source.asSharedFlow()
    var refreshCalls = 0
    var refreshResult = true

    override suspend fun refreshPurchases(): Boolean {
        refreshCalls++
        return refreshResult
    }

    fun tryEmit(purchase: ClientPurchase): Boolean = source.tryEmit(purchase)
}

private class FakeApi : BillingApi {
    var clock: (() -> Long)? = null
    var statusResult: PremiumSnapshot? = null
    var statusFailure: Throwable? = null
    var statusGate: CompletableDeferred<Unit>? = null
    var statusStarted = 0
    var verifyResult: PremiumSnapshot = premium
    var verifyFailure: Throwable? = null
    var gate: CompletableDeferred<Unit>? = null
    var verifyStarted = 0
    val verifyTokens = mutableListOf<String>()
    val verifyProductIds = mutableListOf<String>()
    val verifyAt = mutableListOf<Long>()

    val verifyCount: Int get() = verifyTokens.size

    override suspend fun products(): BillingCatalogue =
        BillingCatalogue.parse("""{"products":[],"packageName":null}""")

    override suspend fun status(): PremiumSnapshot {
        statusStarted++
        statusGate?.await()
        statusFailure?.let { throw it }
        return statusResult ?: error("no status fixture")
    }

    override suspend fun verify(productId: String, purchaseToken: String): PremiumSnapshot {
        verifyStarted++
        gate?.await()
        verifyAt.add(clock?.invoke() ?: 0L)
        verifyProductIds.add(productId)
        verifyTokens.add(purchaseToken)
        verifyFailure?.let { throw it }
        return verifyResult
    }
}

private class FakeSink : PremiumSink {
    data class Apply(val snapshot: PremiumSnapshot)

    val applied = mutableListOf<Apply>()
    var accountChanges = 0

    override suspend fun applyPremiumSnapshot(at: AccountToken, snapshot: PremiumSnapshot) {
        applied.add(Apply(snapshot))
    }

    override suspend fun onAccountChanged(at: AccountToken) {
        accountChanges++
    }
}
