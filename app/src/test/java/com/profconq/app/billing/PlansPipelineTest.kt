package com.profconq.app.billing

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A plans list can go empty for several different reasons, and only some of them mean "there is no
 * subscription to sell". These tests pin the pipeline down on that distinction: which answer may
 * replace what is on screen, which one only switches buying off, and which load is allowed to write
 * at all. Everything here is plain Kotlin, so no device, Play service or Robolectric is involved.
 */
class PlansPipelineTest {

    private val monthly = BillingContract.BASE_PLAN_MONTHLY
    private val annual = BillingContract.BASE_PLAN_ANNUAL
    private val bothPlans = setOf(monthly, annual)
    private val vendorTitle = "ProfConq Premium"

    /** 1: a catalogue read that failed says nothing about the plans, so the plans stay. */
    @Test
    fun aFailedCatalogueReadKeepsThePlansThatAlreadyLoaded() {
        val before = freshlyLoaded(offer(monthly), offer(annual))
        val outcome = PlansOutcome.BackendCatalogueFailure(
            error = BillingError.Network,
            diagnostics = PlansDiagnostics.forPlay(null, bothPlans),
        )

        val after = applyPlansOutcome(before, outcome, newest = true, accountCurrent = true)

        assertEquals("The rows a successful load confirmed are not erased by a failed one", before.plans, after.plans)
        assertFalse(after.plansFresh)
        assertEquals(PlansFailure.BackendCatalogue, after.plansFailure)
        assertEquals("CATALOGUE_FAIL", codeOf(after))
        assertEquals(BuyLock.NoFreshPlan, buyLockOf(after, signedIn = true))
    }

    /** 2: Play went away mid-session: the rows survive, their freshness does not. */
    @Test
    fun aDisconnectedPlayServiceKeepsTheRowsAndDropsTheirFreshness() {
        val before = freshlyLoaded(offer(monthly), offer(annual))
        val outcome = plansOutcome(
            snapshot(reachedPlay = false, responseCode = PlayResponseCode.SERVICE_DISCONNECTED),
            bothPlans,
        )

        val after = applyPlansOutcome(before, outcome, newest = true, accountCurrent = true)

        assertTrue(outcome is PlansOutcome.PlayConnectionFailure)
        assertEquals(before.plans, after.plans)
        assertFalse("A reconnect must re-confirm the catalogue before anything is bought", after.plansFresh)
        assertEquals(PlansFailure.PlayConnection, after.plansFailure)
        assertEquals("PLAY_CONNECT", codeOf(after))
    }

    /** 3: the answer of a load someone else superseded writes nothing at all. */
    @Test
    fun theAnswerOfAnAbandonedRequestNeverOverwritesANewerOne() {
        val guard = PlansLoadGuard()
        val older = requireNotNull(guard.begin())
        assertEquals(1L, older)
        assertEquals("A second trigger while one runs is queued, not started", null, guard.begin())
        assertTrue("The queued trigger is owed exactly one reload", guard.end())
        val newer = requireNotNull(guard.begin())
        assertEquals(2L, newer)
        assertFalse(guard.isLatest(older))
        assertTrue(guard.isLatest(newer))
        assertFalse("Nothing else is queued", guard.end())

        val fresh = freshlyLoaded(offer(monthly), offer(annual))
        val outage = PlansOutcome.PlayQueryFailure(
            responseCode = PlayResponseCode.SERVICE_UNAVAILABLE,
            diagnostics = PlansDiagnostics.forPlay(
                snapshot(responseCode = PlayResponseCode.SERVICE_UNAVAILABLE),
                bothPlans,
            ),
        )

        assertEquals(
            "An abandoned request may not touch the state",
            fresh,
            applyPlansOutcome(fresh, outage, newest = guard.isLatest(older), accountCurrent = true),
        )
        val written = applyPlansOutcome(fresh, outage, newest = guard.isLatest(newer), accountCurrent = true)
        assertNotEquals("The newest request does write", fresh, written)
        assertFalse(written.plansFresh)
    }

    /** 4: the two triggers the app used to fire for one account now leave a single writer. */
    @Test
    fun twoTriggersForOneAccountNeverWriteCompetingAnswers() = runBlocking {
        val writes = PlansHarness(this)
        writes.trigger { id -> if (id == 1L) outage() else loaded() }
        writes.trigger { id -> if (id == 1L) outage() else loaded() }
        delay(SettleMs)
        assertEquals("Only one load runs at a time", 1, writes.started)

        writes.release()
        writes.awaitLoads(2)

        assertEquals("The queued trigger becomes exactly one follow-up load", 2, writes.started)
        assertEquals(2, writes.published)
        assertTrue("The last load is the answer on screen", writes.state.value.plansFresh)
        assertEquals(listOf(monthly, annual), writes.state.value.plans.map { it.basePlanId })
    }

    /** 5: a reconnect burst while a load runs rebuilds the list once, not three times. */
    @Test
    fun aReconnectArrivingMidLoadReloadsExactlyOnce() = runBlocking {
        val writes = PlansHarness(this)
        writes.trigger { id -> if (id == 1L) loaded() else outage() }
        repeat(3) { writes.trigger { id -> if (id == 1L) loaded() else outage() } }
        delay(SettleMs)
        assertEquals(1, writes.started)

        writes.release()
        writes.awaitLoads(2)
        delay(SettleMs)

        assertEquals("A burst of events collapses into one follow-up", 2, writes.started)
        assertEquals(2, writes.published)
        assertFalse("The pipeline is ready for the next request", writes.guard.isLoading())
    }

    /** 6: Retry walks the whole chain - the server first, then Play - instead of one cheap read. */
    @Test
    fun aFullLoadAsksTheServerAndThenPlay() = runBlocking {
        val calls = mutableListOf<String>()
        val load = runCatalogueLoad(
            lastServerPlans = setOf(monthly),
            products = { calls.add("products"); Result.success(bothPlans) },
            errorOf = { BillingError.Unknown },
            play = { calls.add("play"); snapshot(accepted = listOf(offer(monthly), offer(annual))) },
        )

        assertEquals(listOf("products", "play"), calls)
        assertEquals(bothPlans, load.serverPlans)
        assertTrue(load.outcome is PlansOutcome.SuccessWithPlans)
    }

    /** 6 + 9: a failed allowlist read stops the load before Play is even asked. */
    @Test
    fun aFailedCatalogueReadNeverAsksPlayAndKeepsTheLastAllowlist() = runBlocking {
        val calls = mutableListOf<String>()
        val load = runCatalogueLoad(
            lastServerPlans = setOf(monthly),
            products = { calls.add("products"); Result.failure(IllegalStateException("no route")) },
            errorOf = { BillingError.Network },
            play = { calls.add("play"); snapshot() },
        )

        assertEquals(listOf("products"), calls)
        assertEquals("The allowlist that answered stays in force", setOf(monthly), load.serverPlans)
        assertEquals(PlansFailure.BackendCatalogue, load.outcome.failure)
    }

    /** 7: an OK answer with only unfetched products still carries each product's status code. */
    @Test
    fun anOkAnswerOfOnlyUnfetchedProductsKeepsEveryStatusCode() {
        val outcome = plansOutcome(
            snapshot(
                productsReturned = 0,
                unfetched = listOf(unfetched(UnfetchedStatusCode.ProductNotFound)),
            ),
            bothPlans,
        )

        val onlyUnfetched = outcome as PlansOutcome.UnfetchedOnly
        assertEquals(listOf(UnfetchedStatusCode.ProductNotFound), onlyUnfetched.statusCodes)
        assertEquals(PlansFailure.Unfetched, onlyUnfetched.failure)
        assertEquals(
            "UNFETCHED_${UnfetchedStatusCode.ProductNotFound}",
            onlyUnfetched.diagnostics.diagnosticCode(onlyUnfetched.failure),
        )
    }

    /** 8: "no such product" and "not for this account" are two different Play answers. */
    @Test
    fun anUnknownProductAndAnIneligibleAccountStayDistinguishable() {
        val notFound = plansOutcome(snapshot(unfetched = listOf(unfetched(UnfetchedStatusCode.ProductNotFound))), bothPlans)
        val ineligible = plansOutcome(snapshot(unfetched = listOf(unfetched(UnfetchedStatusCode.NoEligibleOffer))), bothPlans)

        assertEquals(PlansFailure.Unfetched, notFound.failure)
        assertEquals(PlansFailure.Unfetched, ineligible.failure)
        assertNotEquals(
            "The two statuses must not collapse into one code",
            notFound.diagnostics?.diagnosticCode(notFound.failure),
            ineligible.diagnostics?.diagnosticCode(ineligible.failure),
        )
        assertEquals("UNFETCHED_3", notFound.diagnostics?.diagnosticCode(notFound.failure))
        assertEquals("UNFETCHED_4", ineligible.diagnostics?.diagnosticCode(ineligible.failure))
        // Billing 9.1.0 has no PRODUCT_UNENTITLED constant: NO_ELIGIBLE_OFFER is its stand-in.
        assertNotEquals(UnfetchedStatusCode.ProductNotFound, UnfetchedStatusCode.NoEligibleOffer)
    }

    /** 9: an answer of "nothing for you" is a success, not a disguised network failure. */
    @Test
    fun aCompleteButEmptyAnswerIsNotReportedAsAFailedRead() = runBlocking {
        val accepted = listOf(offer(monthly), offer(annual))
        val play = snapshot(productsReturned = 1, accepted = accepted)

        // Play offered both plans, the server lists none of them: a real answer, and an empty list.
        val load = runCatalogueLoad(
            lastServerPlans = bothPlans,
            products = { Result.success(emptySet()) },
            errorOf = { BillingError.Unknown },
            play = { play },
        )
        assertEquals("A correct but empty answer replaces the old allowlist", emptySet<String>(), load.serverPlans)
        assertTrue(load.outcome is PlansOutcome.FilteredOut)
        assertTrue(load.outcome.isAuthoritative())

        val before = freshlyLoaded(offer(monthly), offer(annual))
        assertEquals(
            "An authoritative empty answer clears the rows",
            emptyList<PlanView>(),
            applyPlansOutcome(before, load.outcome, newest = true, accountCurrent = true).plans,
        )

        val outage = plansOutcome(snapshot(reachedPlay = false, responseCode = PlayResponseCode.ERROR), bothPlans)
        assertFalse("An outage is not an answer about the catalogue", outage.isAuthoritative())
        assertEquals(
            before.plans,
            applyPlansOutcome(before, outage, newest = true, accountCurrent = true).plans,
        )
    }

    /** 10: a trial or promo record never becomes a plan row, however Play prices it. */
    @Test
    fun aPromotionalRecordNeverBecomesAPlan() {
        val candidates = listOf(
            candidate(monthly, offerId = "free-trial", price = "0 ₽"),
            candidate(annual, offerId = "win-back", price = "490 ₽"),
        )
        val accepted = selectStandardOffers(candidates).map { AcceptedOffer(it.basePlanId, vendorTitle, it.price) }

        assertEquals(emptyList<AcceptedOffer>(), accepted)
        val outcome = plansOutcome(
            snapshot(productsReturned = 1, offersSeen = candidates.size, basePlanIdsSeen = listOf(monthly, annual)),
            bothPlans,
        )
        assertTrue("Offers were seen, none of them standard", outcome is PlansOutcome.FilteredOut)
        assertEquals("FILTERED_2", outcome.diagnostics?.diagnosticCode(outcome.failure))
    }

    /** 11: the two contracted plans pass, and a plan the server does not list is dropped. */
    @Test
    fun bothContractedPlansPassAndAnUnlistedOneDoesNot() {
        val accepted = selectStandardOffers(
            listOf(
                candidate(monthly, offerId = null, price = "129 ₽"),
                candidate(annual, offerId = null, price = "990 ₽"),
            )
        ).map { AcceptedOffer(it.basePlanId, vendorTitle, it.price) }
        val play = snapshot(productsReturned = 1, accepted = accepted)

        val listed = plansOutcome(play, bothPlans) as PlansOutcome.SuccessWithPlans
        assertEquals(listOf(monthly, annual), listed.rows.map { it.basePlanId })
        assertEquals(listOf("129 ₽", "990 ₽"), listed.rows.map { it.price })
        assertEquals("PLANS_OK_2", listed.diagnostics.diagnosticCode(null))

        val narrower = plansOutcome(play, setOf(monthly)) as PlansOutcome.SuccessWithPlans
        assertEquals("Only what the server lists may be sold", listOf(monthly), narrower.rows.map { it.basePlanId })
    }

    /** 12: an outage takes buying away, not the plans. */
    @Test
    fun anOutageSwitchesBuyingOffWithoutTakingThePlansAway() {
        val before = freshlyLoaded(offer(monthly), offer(annual))
        assertEquals(BuyLock.None, buyLockOf(before, signedIn = true))

        val after = applyPlansOutcome(
            before,
            PlansOutcome.PlayQueryFailure(
                responseCode = PlayResponseCode.NETWORK_ERROR,
                diagnostics = PlansDiagnostics.forPlay(
                    snapshot(responseCode = PlayResponseCode.NETWORK_ERROR),
                    bothPlans,
                ),
            ),
            newest = true,
            accountCurrent = true,
        )

        assertEquals(before.plans, after.plans)
        assertEquals(BuyLock.NoFreshPlan, buyLockOf(after, signedIn = true))
        assertEquals("PLAY_QUERY_${PlayResponseCode.NETWORK_ERROR}", codeOf(after))
    }

    /** 13: only a load both the server and Play confirmed can open a sheet. */
    @Test
    fun onlyAFullyConfirmedLoadLightsTheBuyButton() {
        val rows = listOf(PlanView(monthly, vendorTitle, "129 ₽"))
        val base = BillingUiState(status = PremiumStatus.Free, plans = rows, selectedBasePlanId = monthly)

        assertEquals(BuyLock.NoFreshPlan, buyLockOf(base.copy(plansFresh = false), signedIn = true))
        assertEquals(BuyLock.None, buyLockOf(base.copy(plansFresh = true), signedIn = true))
        assertEquals(
            BuyLock.NoFreshPlan,
            buyLockOf(base.copy(plansFresh = true, selectedBasePlanId = annual), signedIn = true),
        )
        assertEquals(BuyLock.SignedOut, buyLockOf(base.copy(plansFresh = true), signedIn = false))
        assertEquals(BuyLock.Loading, buyLockOf(base.copy(plansFresh = true, plansLoading = true), signedIn = true))
        assertEquals(BuyLock.Pressing, buyLockOf(base.copy(plansFresh = true, busy = true), signedIn = true))
        assertEquals(
            BuyLock.AlreadyActive,
            buyLockOf(base.copy(plansFresh = true, status = PremiumStatus.Active), signedIn = true),
        )
    }

    // The entitlement states the buy button reads, kept apart from the plan list: an owned plan is
    // not for sale, a record the server still calls entitled waits for one re-check, and only a
    // trustworthy answer of no access opens buying again.
    /** Confirmed access locks the plan it was granted on. */
    @Test
    fun activeEntitlementLocksCurrentPlan() {
        val gate = entitlementGateOf(premiumAnswer, signedIn = true)
        assertEquals(EntitlementGate.Active, gate)
        assertEquals(BuyLock.AlreadyActive, buyLockOf(freshRows(selected = monthly), signedIn = true, gate))
        // `stale` says Google could not be re-confirmed, which never removes granted access.
        val stale = entitlementGateOf(snapshotOf(plan = "premium", stale = true), signedIn = true)
        assertEquals(BuyLock.AlreadyActive, buyLockOf(freshRows(selected = monthly), signedIn = true, stale))
    }

    /**
     * Until the replacement flow exists there is no second sheet to open for the same product, so
     * an owned monthly plan holds the annual row closed too rather than offering it as an upgrade.
     */
    @Test
    fun activeEntitlementLocksOtherPlanUntilReplacementFlow() {
        val gate = entitlementGateOf(premiumAnswer, signedIn = true)
        assertEquals(BuyLock.AlreadyActive, buyLockOf(freshRows(selected = annual), signedIn = true, gate))
    }

    /**
     * The incident state: the server projects no access while a record it listed still reads as
     * entitled. That is a re-check to be paid, not a plan to buy and not a confirmed Active either.
     */
    @Test
    fun uncertainLocalEntitlementRequiresReverify() {
        val gate = entitlementGateOf(uncertainAnswer, signedIn = true)
        assertEquals(EntitlementGate.NeedsReverify, gate)
        assertNotEquals("An owed re-check must not be shown as confirmed access", EntitlementGate.Active, gate)
        val lock = buyLockOf(freshRows(selected = monthly), signedIn = true, gate)
        assertNotEquals(BuyLock.AlreadyActive, lock)
        assertEquals(BuyLock.NeedsReverify, lock)
    }

    /** A server answer of no access, with nothing it reported still granting any, unlocks buying. */
    @Test
    fun authoritativeExpiryUnlocksPurchase() {
        for (status in listOf("expired", "revoked", "refunded", "replaced")) {
            val gate = entitlementGateOf(snapshotOf(plan = "free", statuses = listOf(status)), signedIn = true)
            assertEquals("$status is terminal", EntitlementGate.FreeAuthoritative, gate)
            assertEquals(BuyLock.None, buyLockOf(freshRows(selected = monthly), signedIn = true, gate))
        }
        // A record that grants nothing on its own, like a paused or pending purchase, is a
        // consistent free answer: Play decides, and the user is never left waiting for nothing.
        for (status in listOf("pending", "on_hold", "paused", "unknown")) {
            assertEquals(
                "$status grants no access, so free is the whole answer",
                EntitlementGate.FreeAuthoritative,
                entitlementGateOf(snapshotOf(plan = "free", statuses = listOf(status)), signedIn = true),
            )
        }
    }

    /** A temporary failure of the re-check never reads as "you are free after all". */
    @Test
    fun transientFailureDoesNotUnlockPurchaseAsFree() {
        val buyGate = BuyGate(clock = { 0L })
        assertEquals(EntitlementGate.NeedsReverify, buyGate.onSnapshot(uncertainAnswer, signedIn = true))
        for (error in listOf(
            BillingError.Network,
            BillingError.RateLimited,
            BillingError.UpstreamUnavailable,
            BillingError.ServiceUnavailable,
            BillingError.Unauthorized,
        )) {
            val gate = buyGate.onReverifyFailure(error, signedIn = true)
            assertNotEquals("$error must not be read as free", EntitlementGate.FreeAuthoritative, gate)
            assertEquals("$error waits for a re-check", EntitlementGate.ReverifyFailed, gate)
            val lock = buyLockOf(freshRows(selected = monthly), signedIn = true, buyGate.current())
            assertEquals(BuyLock.ReverifyCooldown, lock)
        }
    }

    /** The retry after a failed re-check is gated by a cooldown, not by a new free verdict. */
    @Test
    fun retryAfterTransientFailureRespectsCooldown() {
        var now = 0L
        val buyGate = BuyGate(clock = { now })
        buyGate.onSnapshot(uncertainAnswer, signedIn = true)
        buyGate.onReverifyFailure(BillingError.Network, signedIn = true)
        assertFalse("A press inside the cooldown asks nothing of Play or the server", buyGate.restoreAllowed())

        now = BuyGate.CooldownMs - 1
        assertFalse(buyGate.restoreAllowed())

        now = BuyGate.CooldownMs
        assertTrue("After the cooldown the same re-check may run again", buyGate.restoreAllowed())
        assertEquals(
            "A passed cooldown is a re-check still owed, not access that ended",
            EntitlementGate.NeedsReverify,
            buyGate.current(),
        )
    }

    /** The re-check settles in one answer either way, and settling it ends the wait. */
    @Test
    fun restoreSuccessReplacesStaleExpiry() {
        val now = 0L
        val buyGate = BuyGate(clock = { now })
        buyGate.onSnapshot(uncertainAnswer, signedIn = true)
        buyGate.onReverifyFailure(BillingError.RateLimited, signedIn = true)

        // Google answered while the cooldown still ran: the record is terminal now, so buying opens
        // again and a re-check that is no longer owed is not waited for either.
        assertEquals(
            EntitlementGate.FreeAuthoritative,
            buyGate.onSnapshot(snapshotOf(plan = "free", statuses = listOf("expired")), signedIn = true),
        )
        assertTrue("A settled answer is not held behind a cooldown", buyGate.restoreAllowed())
        assertEquals(
            BuyLock.None,
            buyLockOf(freshRows(selected = monthly), signedIn = true, buyGate.current()),
        )

        // The same press on an account that really does own it: the gate follows the answer.
        assertEquals(EntitlementGate.Active, buyGate.onSnapshot(premiumAnswer, signedIn = true))
        assertEquals(BuyLock.AlreadyActive, buyLockOf(freshRows(selected = monthly), signedIn = true, buyGate.current()))
    }

    /**
     * NeedsReverify always has a way out and never becomes a permanent lock: an explicit re-check is
     * available, a cooldown only delays it, an answer settles it, and a local record without any
     * answer cannot close buying at all.
     */
    @Test
    fun noInfiniteNeedsReverify() {
        var now = 0L
        val buyGate = BuyGate(clock = { now })
        assertEquals(EntitlementGate.NeedsReverify, buyGate.onSnapshot(uncertainAnswer, signedIn = true))
        assertTrue("Restore is the way out, and it is open from the first moment", buyGate.restoreAllowed())

        buyGate.onReverifyFailure(BillingError.Network, signedIn = true)
        assertFalse(buyGate.restoreAllowed())
        now = BuyGate.CooldownMs
        assertEquals(EntitlementGate.NeedsReverify, buyGate.current())
        assertTrue(buyGate.restoreAllowed())

        // An answer of no access ends it for good.
        assertEquals(
            EntitlementGate.FreeAuthoritative,
            buyGate.onSnapshot(snapshotOf(plan = "free", statuses = listOf("expired")), signedIn = true),
        )
        assertEquals(BuyLock.None, buyLockOf(freshRows(selected = monthly), signedIn = true, buyGate.current()))

        // Nothing was ever answered: the local record alone is not a lock.
        assertEquals(EntitlementGate.Unknown, entitlementGateOf(null, signedIn = true))
        assertEquals(BuyLock.None, buyLockOf(freshRows(selected = monthly), signedIn = true, EntitlementGate.Unknown))
    }

    /** A row whose base plan this client does not know is not a plan to launch, gate or no gate. */
    @Test
    fun unknownBasePlanRemainsLocked() {
        val stranger = "lifetime_deal"
        assertNull(BillingContract.planRank(stranger))
        val state = freshRows(selected = stranger)
        assertEquals(BuyLock.NoFreshPlan, buyLockOf(state, signedIn = true, EntitlementGate.FreeAuthoritative))
        assertEquals(BuyLock.NeedsReverify, buyLockOf(state, signedIn = true, EntitlementGate.NeedsReverify))
        assertEquals(BuyLock.AlreadyActive, buyLockOf(state, signedIn = true, EntitlementGate.Active))
    }

    /** A `/status` answer of the shape the server sends: a projection plus the rows behind it. */
    private fun snapshotOf(plan: String, statuses: List<String> = emptyList(), stale: Boolean = false): PremiumSnapshot {
        val rows = statuses.mapIndexed { index, status ->
            """{"id":"ent-$index","source":"google_play","productId":"profconq_premium",""" +
                """"basePlanId":"$monthly","status":"$status","startedAt":null,"expiresAt":null,""" +
                """"autoRenews":true,"lastVerifiedAt":null}"""
        }.joinToString(",")
        return PremiumSnapshot.parse(
            """{"plan":"$plan","wordLimit":${if (plan == "premium") "null" else "10"},""" +
                """"entitlements":[$rows],"governing":null,"stale":$stale,"checkedAt":1}"""
        )
    }

    /** Confirmed access, with the row that grants it still listed. */
    private val premiumAnswer: PremiumSnapshot get() = snapshotOf("premium", listOf("active"))

    /** The disagreement the buy button has to wait on: no access projected, a live record listed. */
    private val uncertainAnswer: PremiumSnapshot get() = snapshotOf("free", listOf("active"))

    /** A list both the server and Play confirmed, ready to buy from if the entitlement allows it. */
    private fun freshRows(selected: String): BillingUiState = BillingUiState(
        status = PremiumStatus.Free,
        plans = listOf(PlanView(monthly, vendorTitle, "129 ₽"), PlanView(annual, vendorTitle, "990 ₽")),
        selectedBasePlanId = selected,
        plansFresh = true,
    )

    /** 15: nothing that could be replayed travels with the diagnostics. */
    @Test
    fun thePlansDiagnosticsCarryNoTokenAndNoPlayText() {
        val secret = "super-secret-offer-token"
        val play = snapshot(
            productsReturned = 1,
            unfetched = listOf(UnfetchedProductInfo(secret, secret, UnfetchedStatusCode.ProductNotFound)),
            accepted = listOf(offer(monthly, secret)),
        )
        val diagnostics = PlansDiagnostics.forPlay(play, bothPlans)

        for (type in listOf(
            PlansDiagnostics::class.java,
            PlayQuerySnapshot::class.java,
            AcceptedOffer::class.java,
            UnfetchedProductInfo::class.java,
            PlanView::class.java,
            BillingUiState::class.java,
        )) {
            for (field in type.declaredFields) {
                assertFalse(
                    "${type.simpleName}.${field.name} looks like a token holder",
                    field.name.contains("token", ignoreCase = true),
                )
                assertFalse(
                    "${type.simpleName}.${field.name} looks like Play free text",
                    field.name.contains("debug", ignoreCase = true),
                )
            }
        }

        // The compact code a tester reads is counts and documented codes only.
        val line = "${diagnostics.diagnosticCode(PlansFailure.Unfetched)} ${diagnostics.basePlanIdsSeen}"
        assertFalse("A token reached the diagnostics text: $line", line.contains(secret))
        val priced = plansOutcome(snapshot(accepted = listOf(offer(monthly, "129 ₽"))), bothPlans)
        val rendered = priced.diagnostics.toString()
        assertFalse("A price reached the diagnostics text: $rendered", rendered.contains("129"))
    }

    /** 16: a load that was abandoned publishes nothing, and does not wedge the pipeline. */
    @Test
    fun anAbandonedLoadWritesNothingToTheStateFlow() = runBlocking {
        val state = MutableStateFlow(BillingUiState(plansLoading = true))
        val guard = PlansLoadGuard()
        val id = requireNotNull(guard.begin())
        val entered = CompletableDeferred<Unit>()
        val never = CompletableDeferred<Set<String>>()

        val job = launch {
            try {
                val load = runCatalogueLoad(
                    lastServerPlans = emptySet(),
                    products = { entered.complete(Unit); runCatching { never.await() } },
                    errorOf = { BillingError.Unknown },
                    play = { snapshot(accepted = listOf(offer(monthly))) },
                )
                state.update {
                    applyPlansOutcome(it, load.outcome, newest = guard.isLatest(id), accountCurrent = true)
                }
            } finally {
                guard.end()
            }
        }
        entered.await()
        job.cancel()
        job.join()

        assertEquals("A cancelled load leaves no trace", BillingUiState(plansLoading = true), state.value)
        assertFalse("The guard is free again for the next load", guard.isLoading())
        assertEquals("A Retry after a cancellation starts a real load", 2L, guard.begin())
    }

    private fun offer(basePlanId: String, price: String = "129 ₽") = AcceptedOffer(basePlanId, vendorTitle, price)

    private fun unfetched(statusCode: Int) =
        UnfetchedProductInfo(BillingContract.PREMIUM_PRODUCT_ID, "subs", statusCode)

    private fun snapshot(
        responseCode: Int = PlayResponseCode.OK,
        reachedPlay: Boolean = true,
        productsReturned: Int = 0,
        unfetched: List<UnfetchedProductInfo> = emptyList(),
        offersSeen: Int = 0,
        basePlanIdsSeen: List<String> = emptyList(),
        accepted: List<AcceptedOffer> = emptyList(),
    ) = PlayQuerySnapshot(
        responseCode = responseCode,
        subResponseCode = null,
        reachedPlay = reachedPlay,
        productsReturned = if (accepted.isEmpty()) productsReturned else productsReturned.coerceAtLeast(1),
        unfetched = unfetched,
        offersSeen = if (offersSeen == 0) accepted.size else offersSeen,
        basePlanIdsSeen = if (basePlanIdsSeen.isEmpty()) accepted.map { it.basePlanId } else basePlanIdsSeen,
        acceptedOffers = accepted,
    )

    /** The state after one load in which both the server and Play answered. */
    private fun freshlyLoaded(vararg accepted: AcceptedOffer): BillingUiState {
        val outcome = plansOutcome(
            snapshot(productsReturned = 1, accepted = accepted.toList()),
            bothPlans,
        )
        return applyPlansOutcome(
            BillingUiState(status = PremiumStatus.Free),
            outcome,
            newest = true,
            accountCurrent = true,
        )
    }

    private fun outage(): PlansOutcome = PlansOutcome.PlayConnectionFailure(
        responseCode = PlayResponseCode.SERVICE_DISCONNECTED,
        diagnostics = PlansDiagnostics.forPlay(
            snapshot(reachedPlay = false, responseCode = PlayResponseCode.SERVICE_DISCONNECTED),
            bothPlans,
        ),
    )

    private fun loaded(): PlansOutcome = plansOutcome(
        snapshot(accepted = listOf(offer(monthly), offer(annual))),
        bothPlans,
    )

    private fun codeOf(state: BillingUiState): String? =
        state.plansDiagnostics?.diagnosticCode(state.plansFailure)

    private fun candidate(
        basePlanId: String,
        offerId: String?,
        price: String,
    ) = OfferCandidate(
        basePlanId = basePlanId,
        offerId = offerId,
        hasPricingPhase = true,
        formattedPrice = price,
        offerToken = "offer-token",
    )

    private companion object {
        /** Letting the single-threaded event loop run, so "nothing else started" is observable. */
        const val SettleMs = 30L
    }
}

/**
 * The load sequence of the ViewModel, with Play and the server replaced by gates the test controls:
 * one request either starts the only load allowed or is remembered as a single follow-up. The
 * outcome of a load comes from the caller, so each test decides which answer its requestId gets.
 */
private class PlansHarness(private val scope: CoroutineScope) {
    val guard = PlansLoadGuard()
    val state = MutableStateFlow(BillingUiState())
    var started = 0
    var published = 0
    private val gate = CompletableDeferred<Unit>()

    fun trigger(outcomeOf: (Long) -> PlansOutcome) {
        scope.launch {
            val id = guard.begin() ?: return@launch
            started++
            gate.await()
            published++
            state.update {
                applyPlansOutcome(it, outcomeOf(id), newest = guard.isLatest(id), accountCurrent = true)
            }
            if (guard.end()) trigger(outcomeOf)
        }
    }

    fun release() {
        gate.complete(Unit)
    }

    suspend fun awaitLoads(count: Int) {
        while (published < count) delay(5L)
    }
}
