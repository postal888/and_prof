package com.profconq.app.billing

import com.profconq.app.api.BillingApi
import com.profconq.app.api.BillingApiException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
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
                first.finishWithin("the held-up first verification to run to its end")
                second.finishWithin("the queued second call to settle once the gate opened")
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
                feed.replayOnRefresh = listOf(purchased())
                verifier.restore()
                assertFalse(
                    "A permanent refusal never becomes retryable",
                    settlesWithin { api.verifyCount == 2 },
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
                val outcome = forA.awaitWithin("the gated status read to answer after the switch")
                    as PurchaseVerifier.Outcome.Applied

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
                queued.finishWithin("the queued verify to let go of its permit after the switch")
                assertEquals("A queued token is never sent behind a switch", 4, api.verifyCount)
                assertFalse(api.verifyTokens.contains("queued-token"))

                // The cancelled token is not remembered as verified, so B's re-query can still send it.
                verifier.submit(purchased(token = "queued-token"))
                awaitUntil { api.verifyCount == VerifyThrottle.VerifyPermitsPerWindow + 1 }
                assertTrue(api.verifyTokens.contains("queued-token"))
            }
        }
    }

    /**
     * P3, guard half: a token the server confirmed moments ago is not sent again because Play handed
     * the same purchase over a second time. There is no re-check press in this scenario.
     */
    @Test
    fun duplicateRedeliveryDoesNotReverifyFreshConfirmedPurchase() {
        runBlocking {
            withTimeout(ScenarioMs) {
                withVerifier {
                    verifier.submit(purchased())
                    awaitUntil { api.verifyCount == 1 }
                    assertEquals("The fresh purchase is verified once", 1, api.verifyCount)

                    // Play redelivers the owned purchase on its own; nobody asked for a re-check.
                    assertTrue("The feed must take the redelivered purchase", feed.tryEmit(purchased()))
                    delay(SettleMs)

                    assertEquals(
                        "A plain redelivery of a freshly confirmed token sends no new verify",
                        1,
                        api.verifyCount,
                    )
                    assertEquals("Nothing re-read Play without a press", 0, feed.refreshCalls)
                }
            }
        }
    }

    /**
     * P3, re-check half: the server's own answer was already stale when the token was confirmed, so
     * an explicit Restore is the one press that has to send that token again, and it sends it once.
     */
    @Test
    fun restoreOfStaleConfirmedPurchaseReverifiesOnce() {
        runBlocking {
            withTimeout(ScenarioMs) {
                withVerifier {
                    api.verifyResult = stalePremium
                    feed.replayOnRefresh = listOf(purchased())

                    verifier.submit(purchased())
                    awaitUntil { api.verifyCount == 1 }
                    assertTrue(
                        "The first server answer already reports the entitlement as stale",
                        sink.applied.single().snapshot.stale,
                    )

                    verifier.restore()
                    val reChecked = settlesWithin { api.verifyCount == 2 }
                    assertEquals(
                        "An explicit Restore of a stale confirmation must send exactly one new verify",
                        2,
                        api.verifyCount,
                    )
                    assertTrue("The second verify arrived within the settle window", reChecked)
                    assertEquals("One press is one re-read of Play", 1, feed.refreshCalls)

                    // After the re-check the token is confirmed again: a redelivery without a press
                    // may not open a third verify.
                    assertTrue("The feed takes the next redelivery", feed.tryEmit(purchased()))
                    val third = settlesWithin { api.verifyCount == 3 }
                    assertFalse("No third verify may appear without a new press", third)
                    assertEquals(2, api.verifyCount)
                }
            }
        }
    }

    @Test
    fun immediateSecondRestoreWithinCooldownDoesNotThirdVerify() {
        runBlocking {
            withTimeout(ScenarioMs) {
                val clock = FakeClock()
                withVerifier(clock = clock) {
                    api.verifyResult = stalePremium
                    feed.replayOnRefresh = listOf(purchased())
                    verifier.submit(purchased())
                    awaitUntil { api.verifyCount == 1 }

                    verifier.restore()
                    awaitUntil { api.verifyCount == 2 }
                    verifier.restore()
                    val immediateThird = settlesWithin { api.verifyCount == 3 }
                    assertFalse("A stale answer cannot be rechecked twice inside its cooldown", immediateThird)
                    assertEquals(2, api.verifyCount)

                    clock.now += PurchaseVerifier.RestoreCooldownMs
                    verifier.restore()
                    awaitUntil { api.verifyCount == 3 }
                    assertEquals("The same stale digest reopens after its cooldown", 3, api.verifyCount)
                    assertEquals("Every explicit Restore still performs discovery", 3, feed.refreshCalls)
                }
            }
        }
    }

    /**
     * P3, single-flight half: a press that arrives while the re-read of Play is still running is
     * coalesced into the one already in flight, so the operation queries Play once and sends one
     * new verify. The gate that holds the re-read is always released before the counts are read.
     */
    @Test
    fun concurrentRestoresCoalesceToOneVerify() {
        runBlocking {
            withTimeout(ScenarioMs) {
                withVerifier {
                    api.verifyResult = stalePremium
                    feed.replayOnRefresh = listOf(purchased())
                    verifier.submit(purchased())
                    awaitUntil { api.verifyCount == 1 }

                    feed.gate = CompletableDeferred()
                    val first = scope.async { verifier.restore() }
                    awaitUntil { feed.refreshCalls == 1 }
                    val second = scope.async { verifier.restore() }
                    delay(SettleMs)
                    assertEquals(
                        "A press during a running re-read does not query Play a second time",
                        1,
                        feed.refreshCalls,
                    )

                    feed.gate?.complete(Unit)
                    first.finishWithin("the first press to finish its re-read")
                    second.finishWithin("the coalesced press to return")
                    settlesWithin { api.verifyCount == 2 }

                    assertEquals("Two overlapping presses are one re-check of the server", 2, api.verifyCount)
                    assertEquals("Two overlapping presses are one re-read of Play", 1, feed.refreshCalls)
                }
            }
        }
    }

    /**
     * P3, isolation half: the press re-checks the digest whose own answer went stale. A second
     * purchase of the same account, confirmed fresh, keeps its confirmation and is not sent again.
     */
    @Test
    fun restoringStalePurchaseDoesNotClearOtherConfirmedPurchase() {
        runBlocking {
            withTimeout(ScenarioMs) {
                withVerifier {
                    feed.replayOnRefresh = listOf(purchased(token = "token-a"), purchased(token = "token-b"))

                    api.verifyResult = premium
                    verifier.submit(purchased(token = "token-b"))
                    awaitUntil { api.verifyCount == 1 }

                    api.verifyResult = stalePremium
                    verifier.submit(purchased(token = "token-a"))
                    awaitUntil { api.verifyCount == 2 }

                    verifier.restore()
                    val reChecked = settlesWithin { api.verifyCount == 3 }
                    assertTrue("The press must re-check the stale digest", reChecked)
                    assertEquals(
                        "Only the digest whose answer went stale is re-checked",
                        listOf("token-b", "token-a", "token-a"),
                        api.verifyTokens,
                    )

                    // The fresh one stays closed afterwards, with a press behind it or without one.
                    feed.tryEmit(purchased(token = "token-b"))
                    val extra = settlesWithin { api.verifyCount == 4 }
                    assertFalse("A redelivery of the fresh digest sends no new verify", extra)
                    assertEquals(3, api.verifyCount)
                }
            }
        }
    }

    /**
     * P3, error half: a refusal reopens only the digest it refused. The answer another token already
     * got stays closed, one redelivery is one attempt, and a success does not open a retry loop.
     */
    @Test
    fun transientFailureOnlyReopensFailedDigest() {
        runBlocking {
            withTimeout(ScenarioMs) {
                val clock = FakeClock()
                withVerifier(clock = clock) {
                    api.verifyResult = premium
                    verifier.submit(purchased(token = "token-b"))
                    awaitUntil { api.verifyCount == 1 }

                    api.verifyFailure = BillingApiException(BillingError.Network, 0, "network")
                    watchOutcomes()
                    verifier.submit(purchased(token = "token-a"))
                    awaitUntil { outcomes.isNotEmpty() }
                    assertEquals(listOf("token-b", "token-a"), api.verifyTokens)

                    api.verifyFailure = null
                    feed.replayOnRefresh = listOf(purchased(token = "token-a"), purchased(token = "token-b"))
                    feed.tryEmit(purchased(token = "token-a"))
                    feed.tryEmit(purchased(token = "token-b"))
                    assertFalse(
                        "Ordinary redelivery cannot retry the refused digest",
                        settlesWithin { api.verifyCount == 3 },
                    )

                    verifier.restore()
                    assertFalse(
                        "An explicit Restore inside the failed digest cooldown cannot retry it",
                        settlesWithin { api.verifyCount == 3 },
                    )
                    clock.now += PurchaseVerifier.RestoreCooldownMs
                    verifier.restore()
                    awaitUntil { api.verifyCount == 3 }

                    assertEquals(
                        "Only A reopens; confirmed B remains closed",
                        listOf("token-b", "token-a", "token-a"),
                        api.verifyTokens,
                    )
                }
            }
        }
    }

    /** P3: a transient refusal releases the digest, so the next Restore can try again. */
    @Test
    fun transientVerifyFailureLeavesTheTokenRetryableByTheNextRestore() {
        runBlocking {
            val clock = FakeClock()
            withVerifier(clock = clock) {
                feed.replayOnRefresh = listOf(purchased())
                api.verifyFailure = BillingApiException(BillingError.Network, 0, "network")
                watchOutcomes()
                verifier.submit(purchased())
                awaitUntil { outcomes.isNotEmpty() }
                assertEquals(1, api.verifyCount)
                clock.now += PurchaseVerifier.RestoreCooldownMs
                verifier.restore()
                awaitUntil { api.verifyCount == 2 }
                assertEquals("A network failure is retryable by an eligible explicit Restore", 2, api.verifyCount)
            }
        }
    }

    @Test
    fun restoreWithEmptyPhaseMapDiscoversOwnedPurchase() {
        runBlocking {
            withTimeout(ScenarioMs) {
                withVerifier {
                    feed.replayOnRefresh = listOf(purchased())
                    verifier.restore()
                    awaitUntil { api.verifyCount == 1 }

                    assertEquals("Restore queries Play even before this process saw a digest", 1, feed.refreshCalls)
                    assertEquals(listOf(TOKEN), api.verifyTokens)
                    feed.tryEmit(purchased())
                    assertFalse(
                        "The discovered purchase becomes a confirmed dedup entry",
                        settlesWithin { api.verifyCount == 2 },
                    )
                }
            }
        }
    }

    @Test
    fun missingClaimedPurchaseDoesNotRemainQueuedForever() {
        runBlocking {
            withTimeout(ScenarioMs) {
                val clock = FakeClock()
                withVerifier(clock = clock) {
                    api.verifyResult = stalePremium
                    verifier.submit(purchased())
                    awaitUntil { api.verifyCount == 1 }

                    feed.replayOnRefresh = emptyList()
                    verifier.restore()
                    assertEquals(1, feed.refreshCalls)
                    assertEquals(1, api.verifyCount)

                    clock.now += PurchaseVerifier.RestoreCooldownMs
                    feed.replayOnRefresh = listOf(purchased())
                    verifier.restore()
                    awaitUntil { api.verifyCount == 2 }
                    assertEquals("A missing claim can be discovered by a later Restore", 2, feed.refreshCalls)
                }
            }
        }
    }

    /**
     * P4: `ITEM_ALREADY_OWNED` starts the same re-check while the first verify of that token is
     * still running. Play hands the purchase over again, and the token is still sent once.
     */
    @Test
    fun itemAlreadyOwnedTriggersSingleRestoreVerify() {
        runBlocking {
            withTimeout(ScenarioMs) {
                withVerifier {
                    feed.replayOnRefresh = listOf(purchased(), purchased(), purchased())
                    api.gate = CompletableDeferred()
                    feed.gate = CompletableDeferred()
                    val initial = scope.async { verifier.submit(purchased()) }
                    var first: Deferred<PurchaseVerifier.Outcome?>? = null
                    var second: Deferred<PurchaseVerifier.Outcome?>? = null
                    try {
                        awaitUntil { api.verifyStarted == 1 }
                        val firstPress = scope.async { verifier.restore() }
                        first = firstPress
                        awaitUntil { feed.refreshCalls == 1 }
                        val secondPress = scope.async { verifier.restore() }
                        second = secondPress
                        delay(SettleMs)
                        assertEquals("Overlapping recovery asks Play once", 1, feed.refreshCalls)

                        feed.gate?.complete(Unit)
                        api.gate?.complete(Unit)
                        initial.finishWithin("the held purchase verification to finish")
                        firstPress.finishWithin("the first restore press to return")
                        secondPress.finishWithin("the coalesced restore press to return")
                        awaitUntil { api.verifyCount == 1 }
                        delay(SettleMs)
                        assertEquals("The owned token is verified once for the whole recovery", 1, api.verifyCount)
                    } finally {
                        feed.gate?.complete(Unit)
                        api.gate?.complete(Unit)
                        listOfNotNull(initial, first, second).forEach { job ->
                            if (!job.isCompleted) job.cancel()
                            runCatching { job.finishWithin("the owned-item test job to stop") }
                        }
                    }
                }
            }
        }
    }

    private companion object {
        const val SettleMs = 40L
    }
}

private const val TOKEN = "rfcKsH7-play-purchase-token"

/** The longest any single wait in a test may take before it is reported as never having happened. */
private const val AwaitMs = 2_000L

/** How long a settling condition is polled before the test keeps the value it can already see. */
private const val SettleWindowMs = 250L

/** Outer bound of a whole scenario: wider than every inner window, far from infinite. */
private const val ScenarioMs = 10_000L

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
    val verifier = PurchaseVerifier(
        feed = feed,
        api = api,
        sink = sink,
        accounts = accounts,
        throttle = throttle,
        isLoggedIn = isLoggedIn,
        scope = scope,
        clock = clock::reading,
    )
    yield()
    try {
        block(Harness(scope, accounts, feed, api, sink, verifier))
    } finally {
        // Cancel before joining, so a collector that only ends because it was cancelled cannot keep
        // the test thread parked forever. A cleanup failure is reported on its own and never replaces
        // the assertion the test already failed on.
        val job = scope.coroutineContext[Job]
        job?.cancel()
        runCatching { job?.join() }.onFailure { failure ->
            System.err.println("Verifier scope cleanup did not finish: ${failure::class.simpleName}")
        }
    }
}

private suspend fun awaitUntil(condition: () -> Boolean) {
    withTimeout(2_000) {
        while (!condition()) delay(5)
    }
}

/**
 * Waits a bounded window for [condition] and answers whether it ever held. A failing expectation can
 * then report the value it actually saw instead of dying as a coroutine timeout.
 */
private suspend fun settlesWithin(condition: () -> Boolean): Boolean =
    withTimeoutOrNull(SettleWindowMs) {
        while (!condition()) delay(5)
        true
    } == true

/** Awaits a deferred inside a bounded window, naming the condition that has to happen to release it. */
private suspend fun <T> Deferred<T>.awaitWithin(what: String): T = try {
    withTimeout(AwaitMs) { await() }
} catch (_: TimeoutCancellationException) {
    throw AssertionError("Timed out waiting for $what")
}

/** Joins a job inside a bounded window, naming the work that still has to finish for it to end. */
private suspend fun Job.finishWithin(what: String) = try {
    withTimeout(AwaitMs) { join() }
} catch (_: TimeoutCancellationException) {
    throw AssertionError("Timed out waiting for $what")
}

private class FakeFeed : PurchaseFeed {
    private val source = MutableSharedFlow<ClientPurchase>(extraBufferCapacity = 64)
    override val purchases: SharedFlow<ClientPurchase> get() = source.asSharedFlow()
    var refreshCalls = 0
    var refreshResult = true

    /** Holds a re-read the way a real `queryPurchasesAsync` callback does: the query is in flight. */
    var gate: CompletableDeferred<Unit>? = null

    /** Owned purchases Play hands over again on a re-read, as it does after a query. */
    var replayOnRefresh: List<ClientPurchase> = emptyList()

    override suspend fun refreshPurchases(): Boolean {
        refreshCalls++
        gate?.await()
        replayOnRefresh.forEach { source.tryEmit(it) }
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
