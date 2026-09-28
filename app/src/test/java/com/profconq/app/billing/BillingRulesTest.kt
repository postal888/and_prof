package com.profconq.app.billing

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The rules the billing pipeline decides by, tested without Android or Play types: whose answer is
 * entitlement, how many verifies may leave, when the launch gate opens again, and which Play row
 * may actually be bought.
 */
class BillingRulesTest {

    private val monthly = BillingContract.BASE_PLAN_MONTHLY
    private val annual = BillingContract.BASE_PLAN_ANNUAL

    @Test
    fun accountKeyIsADigestAndTheGenerationRisesOnEveryChange() {
        val accounts = BillingAccountCoordinator()
        val first = accounts.onUid("uid-a")
        assertEquals(1L, first.generation)
        assertNotEquals("The uid itself must not be the key", "uid-a", first.key)
        assertEquals(64, first.key!!.length)
        assertEquals("A repeat call must not raise the generation", first, accounts.onUid("uid-a"))

        val second = accounts.onUid("uid-b")
        assertEquals(2L, second.generation)
        assertFalse("A direct switch must drop the previous account", accounts.isCurrent(first))
        assertTrue(accounts.isCurrent(second))

        val signedOut = accounts.onUid(null)
        assertEquals(3L, signedOut.generation)
        assertEquals(null, signedOut.key)
        assertFalse(accounts.isCurrent(second))
    }

    @Test
    fun theLegacyAccountRouteMayInitializeButNotRevokeBillingPremium() {
        val ledger = EntitlementLedger()
        val at = AccountToken("key", 1L)

        // Before any billing answer the legacy route owns the initial state.
        assertEquals(
            EntitlementWrite.Mirror(false, 30),
            ledger.resolve(at, EntitlementSource.LegacyAccount, false, 30),
        )
        assertFalse(ledger.hasBillingSnapshot)

        // A billing answer grants, and is remembered as the authority.
        assertEquals(
            EntitlementWrite.Mirror(true, null),
            ledger.resolve(at, EntitlementSource.Billing, true, null),
        )
        assertTrue(ledger.hasBillingSnapshot)

        // A legacy plan=free after that is a projection, not a revocation.
        assertEquals(EntitlementWrite.Keep, ledger.resolve(at, EntitlementSource.LegacyAccount, false, 30))
        // A legacy premium answer still mirrors through.
        assertEquals(
            EntitlementWrite.Mirror(true, null),
            ledger.resolve(at, EntitlementSource.LegacyAccount, true, null),
        )
        // Billing itself may revoke again: it is the authority in both directions.
        assertEquals(
            EntitlementWrite.Mirror(false, 30),
            ledger.resolve(at, EntitlementSource.Billing, false, 30),
        )
    }

    @Test
    fun anAcceptedPromoCodeGrantsStraightAway() {
        val ledger = EntitlementLedger()
        val at = AccountToken("key", 1L)
        ledger.resolve(at, EntitlementSource.LegacyAccount, false, 30)
        assertEquals(
            EntitlementWrite.Mirror(true, null),
            ledger.resolve(at, EntitlementSource.Promo, true, null),
        )
    }

    @Test
    fun anUnreadablePlanCarriesNoVerdictAndChangesNothing() {
        val ledger = EntitlementLedger()
        val at = AccountToken("key", 1L)
        assertEquals(EntitlementWrite.Keep, ledger.resolve(at, EntitlementSource.Billing, null, null))
        assertFalse(ledger.hasBillingSnapshot)
        assertEquals(EntitlementWrite.Keep, ledger.forget(at))
    }

    @Test
    fun aLateAnswerOfAPreviousAccountIsIgnored() {
        val ledger = EntitlementLedger()
        val old = AccountToken("key-a", 1L)
        val now = AccountToken("key-b", 2L)
        ledger.resolve(old, EntitlementSource.Billing, true, null)
        assertEquals(EntitlementWrite.Reset, ledger.forget(now))
        assertEquals(
            "A late premium answer of the previous account must not be applied",
            EntitlementWrite.Keep,
            ledger.resolve(old, EntitlementSource.Billing, true, null),
        )
    }

    @Test
    fun signOutClearsOnceAndNothingAfterThat() {
        val ledger = EntitlementLedger()
        val at = AccountToken("key", 1L)
        val next = AccountToken(null, 2L)
        ledger.resolve(at, EntitlementSource.Billing, true, null)
        assertEquals(EntitlementWrite.Reset, ledger.forget(next))
        assertEquals("A second sign-out has nothing left to clear", EntitlementWrite.Keep, ledger.forget(next))
    }

    @Test
    fun throttleSendsFourPerWindowAndDelaysTheFifth() {
        val accounts = BillingAccountCoordinator()
        val at = accounts.onUid("uid-a")
        val clock = WindowClock()
        val waits = mutableListOf<Long>()
        val throttle = VerifyThrottle(
            accounts = accounts,
            clock = clock::reading,
            delay = { millis -> waits.add(millis); clock.advance(millis) },
        )
        runBlocking {
            repeat(VerifyThrottle.VerifyPermitsPerWindow) { index ->
                assertTrue("Slot ${index + 1} of the window is free", throttle.acquire(at))
            }
            assertEquals(emptyList<Long>(), waits)
            assertTrue("The fifth waits instead of being dropped", throttle.acquire(at))
            assertEquals(
                "It waits exactly for the oldest call to leave the window",
                listOf(VerifyThrottle.VerifyWindowMs),
                waits,
            )
        }
    }

    @Test
    fun throttleCancelsTheQueueWhenTheAccountChanges() {
        val accounts = BillingAccountCoordinator()
        val at = accounts.onUid("uid-a")
        val hold = CompletableDeferred<Unit>()
        val throttle = VerifyThrottle(
            accounts = accounts,
            clock = { 0L },
            delay = { _ -> hold.await() },
        )
        runBlocking {
            repeat(VerifyThrottle.VerifyPermitsPerWindow) { assertTrue(throttle.acquire(at)) }
            val waiting = async { throttle.acquire(at) }
            kotlinx.coroutines.yield()
            accounts.onUid("uid-b")
            throttle.reset()
            hold.complete(Unit)
            assertFalse("A wait for a gone account ends cancelled", waiting.await())
        }
    }

    @Test
    fun launchGateOpensAgainOnCallbackFailedLaunchAndDestroy() {
        val clock = WindowClock()
        val gate = LaunchGate(clock::reading)
        assertTrue(gate.tryBegin())
        assertFalse("A second sheet may not open while one is up", gate.tryBegin())
        gate.release()
        assertTrue("The purchases callback opens the gate again", gate.tryBegin())
        gate.onActivityDestroyed()
        assertTrue("An destroyed owner opens the gate again", gate.tryBegin())

        // The stale window is only the backstop for a callback that never arrives.
        clock.advance(LaunchGateStaleMs)
        assertTrue("The stale backstop releases a lost callback", gate.tryBegin())
    }

    @Test
    fun onlyTheStandardRecordOfABasePlanIsOffered() {
        val candidates = listOf(
            candidate(monthly, offerId = null, price = "129 ₽"),
            candidate(monthly, offerId = "intro-week", price = "0 ₽"),
            candidate(monthly, offerId = "win-back", price = "99 ₽"),
            candidate(annual, offerId = null, price = "990 ₽"),
        )
        val selected = selectStandardOffers(candidates)
        assertEquals("One row per base plan", listOf(monthly, annual), selected.map { it.basePlanId })
        assertEquals(listOf("129 ₽", "990 ₽"), selected.map { it.price })
    }

    @Test
    fun aPlanWithoutAPriceOrTokenIsNotOfferedAtAll() {
        val selected = selectStandardOffers(
            listOf(
                candidate(monthly, offerId = null, price = ""),
                candidate(annual, offerId = null, price = null, hasPhase = false),
                candidate(monthly, offerId = null, price = "129 ₽", token = null),
            )
        )
        assertEquals("Nothing usable must not become an empty price", emptyList<SelectedOffer>(), selected)
    }

    @Test
    fun unknownPlansAndTrialRowsNeverReachTheUi() {
        val selected = selectStandardOffers(
            listOf(
                candidate("lifetime", offerId = null, price = "490 ₽"),
                candidate(monthly, offerId = "free-trial", price = "0 ₽"),
            )
        )
        assertEquals(emptyList<SelectedOffer>(), selected)
    }

    @Test
    fun alreadyOwnedRoutesToRestoreInsteadOfASecondSheet() {
        assertEquals(LaunchFollowUp.None, launchFollowUpOf(PlayResponseCode.OK))
        assertEquals(
            "An owned subscription is restored, not bought again",
            LaunchFollowUp.RestoreAndVerify,
            launchFollowUpOf(PlayResponseCode.ITEM_ALREADY_OWNED),
        )
        assertEquals(LaunchFollowUp.ShowNotice, launchFollowUpOf(PlayResponseCode.USER_CANCELED))
        assertEquals(LaunchFollowUp.ShowNotice, launchFollowUpOf(PlayResponseCode.SERVICE_UNAVAILABLE))
    }

    @Test
    fun subReasonOnlyReadsTheDocumentedCodes() {
        // getOnPurchasesUpdatedSubResponseCode() is the only sub-code Billing 9.1.0 exposes; any
        // other result answers NO_APPLICABLE, which is why the default below is not invented data.
        assertEquals(BillingSubReason.None, billingSubReasonOf(PlaySubResponseCode.NoApplicable))
        assertEquals(
            BillingSubReason.PaymentDeclinedInsufficientFunds,
            billingSubReasonOf(PlaySubResponseCode.PaymentDeclinedInsufficientFunds),
        )
        assertEquals(BillingSubReason.UserIneligible, billingSubReasonOf(PlaySubResponseCode.UserIneligible))
        assertEquals(BillingSubReason.Unknown, billingSubReasonOf(77))
        assertEquals(
            "A purchase read back by queryPurchasesAsync has no sub code to carry",
            BillingSubReason.None,
            ClientPurchase(
                state = ClientPurchaseState.PURCHASED,
                purchaseToken = "token",
                productId = BillingContract.PREMIUM_PRODUCT_ID,
                suspended = false,
            ).subReason,
        )
    }

    @Test
    fun noticesAreTonedByWhatTheyMean() {
        assertEquals(NoticeSeverity.Success, noticeSeverityOf(BillingNotice.Verified))
        assertEquals(NoticeSeverity.Neutral, noticeSeverityOf(BillingNotice.Canceled))
        assertEquals(NoticeSeverity.Neutral, noticeSeverityOf(BillingNotice.Restoring))
        assertEquals(NoticeSeverity.Info, noticeSeverityOf(BillingNotice.PlanUnavailable))
        assertEquals(NoticeSeverity.Danger, noticeSeverityOf(BillingNotice.OwnedByOther))
        assertEquals(NoticeSeverity.Danger, noticeSeverityOf(BillingNotice.ActivityUnavailable))
        // An owned subscription is presented as a restore, never as a failure.
        assertEquals(BillingNotice.Restoring, billingNoticeOfResponseCode(PlayResponseCode.ITEM_ALREADY_OWNED))
        assertEquals(NoticeSeverity.Neutral, noticeSeverityOf(billingNoticeOfResponseCode(PlayResponseCode.USER_CANCELED)))
    }

    private fun candidate(
        basePlanId: String,
        offerId: String?,
        price: String?,
        hasPhase: Boolean = true,
        token: String? = "offer-token",
    ) = OfferCandidate(
        basePlanId = basePlanId,
        offerId = offerId,
        hasPricingPhase = hasPhase,
        formattedPrice = price,
        offerToken = token,
    )

    private companion object {
        /** LaunchGate keeps its stale window private; the backstop is asserted through the clock. */
        const val LaunchGateStaleMs = 6 * 60_000L
    }
}

private class WindowClock {
    private var now = 0L
    fun reading(): Long = now
    fun advance(millis: Long) {
        now += millis
    }
}
