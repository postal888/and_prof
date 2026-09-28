package com.profconq.app.billing

import java.util.concurrent.CancellationException

/**
 * The plans pipeline: what one full catalogue load means, when that answer may replace what is on
 * screen, and how two loads for one account are kept from racing. Plain Kotlin on purpose, so all
 * of it is unit-testable without Play, Android or a device.
 */

/** `UnfetchedProduct.StatusCode` as Billing 9.1.0 names it. */
object UnfetchedStatusCode {
    const val Unknown = 0
    const val InvalidProductIdFormat = 2
    const val ProductNotFound = 3

    /** The product exists, but this account cannot be offered it: track, eligibility or country. */
    const val NoEligibleOffer = 4
}

/** One Play product that could not be fetched, reduced to the three documented fields. */
data class UnfetchedProductInfo(
    val productId: String?,
    val productType: String?,
    val statusCode: Int,
)

/** One offerable base plan, with the name and price Play reported for it and no offer token. */
data class AcceptedOffer(
    val basePlanId: String,
    val title: String,
    val price: String,
)

/**
 * Play's answer with every Play and Android type already removed. [reachedPlay] is what keeps a
 * service that never answered apart from a service that answered "nothing for you".
 */
data class PlayQuerySnapshot(
    val responseCode: Int,
    /** Only the purchases-updated callback carries a sub code in Billing 9.1.0, so a query has none. */
    val subResponseCode: Int?,
    val reachedPlay: Boolean,
    val productsReturned: Int,
    val unfetched: List<UnfetchedProductInfo>,
    val offersSeen: Int,
    val basePlanIdsSeen: List<String>,
    val acceptedOffers: List<AcceptedOffer>,
) {
    val unfetchedStatusCodes: List<Int> get() = unfetched.map { it.statusCode }
}

/** Why the plan list cannot be bought right now, as a typed reason with no secret in it. */
enum class PlansFailure {
    /** `GET /api/billing/products` did not answer. */
    BackendCatalogue,

    /** The Play billing service was not reachable. */
    PlayConnection,

    /** Play answered a code that is not OK. */
    PlayQuery,

    /** Play named the product but would not return it. */
    Unfetched,

    /** Play and the server both answered: there is nothing offerable for now. */
    NoEligibleOffers,

    /** Offers came back, but none survived the standard-record and allowlist filters. */
    FilteredOut,
}

/**
 * What the last load saw: counts, documented codes and base-plan ids. Never a purchase token, an
 * offer token, a price, a product title, account data or `BillingResult.debugMessage`.
 */
data class PlansDiagnostics(
    /** Null means Play was not asked at all, because the load stopped before that step. */
    val responseCode: Int?,
    val subResponseCode: Int?,
    val productsReturned: Int,
    val unfetchedStatusCodes: List<Int>,
    val offersSeen: Int,
    val basePlanIdsSeen: List<String>,
    val acceptedOffers: Int,
    val serverPlanIds: List<String>,
) {
    companion object {
        fun forPlay(play: PlayQuerySnapshot?, serverPlanIds: Set<String>): PlansDiagnostics =
            PlansDiagnostics(
                responseCode = play?.responseCode,
                subResponseCode = play?.subResponseCode,
                productsReturned = play?.productsReturned ?: 0,
                unfetchedStatusCodes = play?.unfetchedStatusCodes ?: emptyList(),
                offersSeen = play?.offersSeen ?: 0,
                basePlanIdsSeen = play?.basePlanIdsSeen ?: emptyList(),
                acceptedOffers = play?.acceptedOffers?.size ?: 0,
                serverPlanIds = serverPlanIds.sorted(),
            )
    }

    /**
     * Compact code for internal testing, e.g. `PLAY_QUERY_2` or `UNFETCHED_3`. No personal data
     * in it, so it is the one thing that may sit next to a plain-language reason.
     */
    fun diagnosticCode(failure: PlansFailure?): String = when (failure) {
        null -> "PLANS_OK_$acceptedOffers"
        PlansFailure.BackendCatalogue -> "CATALOGUE_FAIL"
        PlansFailure.PlayConnection -> "PLAY_CONNECT"
        PlansFailure.PlayQuery -> "PLAY_QUERY_${responseCode ?: PlayResponseCode.ERROR}"
        PlansFailure.Unfetched ->
            "UNFETCHED_${unfetchedStatusCodes.firstOrNull() ?: UnfetchedStatusCode.Unknown}"
        PlansFailure.NoEligibleOffers -> "NO_OFFER"
        PlansFailure.FilteredOut -> "FILTERED_${basePlanIdsSeen.size}"
    }
}

/** The result of one full load, with the stage that produced it still distinguishable. */
sealed class PlansOutcome {
    open val failure: PlansFailure? = null
    open val diagnostics: PlansDiagnostics? = null

    /** Both reads succeeded and at least one allowlisted, priced standard offer can be bought. */
    data class SuccessWithPlans(
        val rows: List<PlanView>,
        override val diagnostics: PlansDiagnostics,
    ) : PlansOutcome()

    /** Play and the server answered, and there is genuinely nothing offerable right now. */
    data class SuccessNoEligibleOffers(override val diagnostics: PlansDiagnostics) : PlansOutcome() {
        override val failure: PlansFailure = PlansFailure.NoEligibleOffers
    }

    /** The allowlist read failed, so nothing may be bought until it answers again. */
    data class BackendCatalogueFailure(
        val error: BillingError,
        override val diagnostics: PlansDiagnostics,
    ) : PlansOutcome() {
        override val failure: PlansFailure = PlansFailure.BackendCatalogue
    }

    /** The Play service did not answer; `responseCode` is the documented Billing code. */
    data class PlayConnectionFailure(
        val responseCode: Int,
        override val diagnostics: PlansDiagnostics,
    ) : PlansOutcome() {
        override val failure: PlansFailure = PlansFailure.PlayConnection
    }

    /** Play answered a code that is not OK. */
    data class PlayQueryFailure(
        val responseCode: Int,
        override val diagnostics: PlansDiagnostics,
    ) : PlansOutcome() {
        override val failure: PlansFailure = PlansFailure.PlayQuery
    }

    /** Play returned the product only as unfetched; the status codes say why. */
    data class UnfetchedOnly(
        val statusCodes: List<Int>,
        override val diagnostics: PlansDiagnostics,
    ) : PlansOutcome() {
        override val failure: PlansFailure = PlansFailure.Unfetched
    }

    /** Offers came back but none was a standard, priced record on the allowlist. */
    data class FilteredOut(
        val basePlanIdsSeen: List<String>,
        override val diagnostics: PlansDiagnostics,
    ) : PlansOutcome() {
        override val failure: PlansFailure = PlansFailure.FilteredOut
    }

    /** A newer load or another account took over, so this answer writes nothing at all. */
    data class StaleRequestIgnored(val requestId: Long) : PlansOutcome()
}

/**
 * The one place that decides what Play's answer plus the server allowlist mean. Rows are built
 * here, from the accepted offers the allowlist confirmed, so no caller can show a plan the server
 * has not listed.
 */
fun plansOutcome(play: PlayQuerySnapshot, serverPlanIds: Set<String>): PlansOutcome {
    val diagnostics = PlansDiagnostics.forPlay(play, serverPlanIds)
    val rows = play.acceptedOffers
        .filter { serverPlanIds.contains(it.basePlanId) }
        .map { PlanView(it.basePlanId, it.title, it.price) }
    return when {
        !play.reachedPlay -> PlansOutcome.PlayConnectionFailure(play.responseCode, diagnostics)
        play.responseCode != PlayResponseCode.OK -> PlansOutcome.PlayQueryFailure(play.responseCode, diagnostics)
        rows.isNotEmpty() -> PlansOutcome.SuccessWithPlans(rows, diagnostics)
        play.productsReturned == 0 && play.unfetched.isNotEmpty() ->
            PlansOutcome.UnfetchedOnly(play.unfetchedStatusCodes, diagnostics)
        play.productsReturned == 0 -> PlansOutcome.SuccessNoEligibleOffers(diagnostics)
        else -> PlansOutcome.FilteredOut(play.basePlanIdsSeen, diagnostics)
    }
}

/** An outcome from a real answer, as opposed to one that simply did not arrive. */
fun PlansOutcome.isAuthoritative(): Boolean = when (this) {
    is PlansOutcome.SuccessWithPlans,
    is PlansOutcome.SuccessNoEligibleOffers,
    is PlansOutcome.UnfetchedOnly,
    is PlansOutcome.FilteredOut,
    -> true

    else -> false
}

/**
 * The merge rule that stops a temporary outage from erasing the plans a successful load showed.
 * Only the newest request for the account still in force may write anything.
 */
fun applyPlansOutcome(
    state: BillingUiState,
    outcome: PlansOutcome,
    newest: Boolean,
    accountCurrent: Boolean,
): BillingUiState {
    if (!newest || !accountCurrent) return state.copy(plansLoading = false)
    if (outcome is PlansOutcome.SuccessWithPlans) {
        return state.copy(
            plans = outcome.rows,
            plansLoading = false,
            plansFresh = true,
            plansFailure = null,
            plansDiagnostics = outcome.diagnostics,
            selectedBasePlanId = outcome.rows
                .firstOrNull { it.basePlanId == state.selectedBasePlanId }
                ?.basePlanId
                ?: outcome.rows.first().basePlanId,
        )
    }
    // A read that never got an answer keeps the rows on screen: they are the last thing the server
    // and Play confirmed, and the buy button is switched off instead of the plans disappearing.
    val keepRows = !outcome.isAuthoritative() && state.plans.isNotEmpty()
    return state.copy(
        plans = if (keepRows) state.plans else emptyList(),
        plansLoading = false,
        plansFresh = false,
        plansFailure = outcome.failure,
        plansDiagnostics = outcome.diagnostics ?: state.plansDiagnostics,
        selectedBasePlanId = if (keepRows) state.selectedBasePlanId else BillingContract.BASE_PLAN_MONTHLY,
    )
}

/** One full load: the allowlist now in force and the outcome to publish for it. */
class PlansLoad(val serverPlans: Set<String>, val outcome: PlansOutcome)

/**
 * The order of one load, kept away from Play and Android types so it can be tested: server
 * allowlist first, Play catalogue second. A failed allowlist read keeps the last one that answered
 * instead of replacing it with an empty set, and Play is not asked at all then.
 */
suspend fun runCatalogueLoad(
    lastServerPlans: Set<String>,
    products: suspend () -> Result<Set<String>>,
    errorOf: (Throwable) -> BillingError,
    play: suspend () -> PlayQuerySnapshot,
): PlansLoad {
    val catalogue = products()
    val failure = catalogue.exceptionOrNull()
    if (failure != null) {
        // A caller that was cancelled has no answer to publish, and its exception must not be read
        // as "the server did not answer".
        if (failure is CancellationException) throw failure
        val diagnostics = PlansDiagnostics.forPlay(null, lastServerPlans)
        return PlansLoad(lastServerPlans, PlansOutcome.BackendCatalogueFailure(errorOf(failure), diagnostics))
    }
    val serverPlans = catalogue.getOrThrow()
    return PlansLoad(serverPlans, plansOutcome(play(), serverPlans))
}

/** Why buying is switched off, as the single answer the buy button and its lock reason share. */
enum class BuyLock {
    None,

    /** Nothing can be bought without an account. */
    SignedOut,

    /** A load is running; the answer of the one before it is not current. */
    Loading,

    /** A purchase press is in flight. */
    Pressing,

    /** This account already holds the subscription: Restore, not a second sheet. */
    AlreadyActive,

    /** The last full load did not leave a fresh, allowlisted plan to launch. */
    NoFreshPlan,
}

/**
 * The only rule for the buy button. Rows a temporary outage left standing stay on screen, but no
 * sheet opens from them until a load has been confirmed by both the server and Play.
 */
fun buyLockOf(state: BillingUiState, signedIn: Boolean): BuyLock = when {
    !signedIn -> BuyLock.SignedOut
    state.plansLoading -> BuyLock.Loading
    state.busy -> BuyLock.Pressing
    state.status == PremiumStatus.Active -> BuyLock.AlreadyActive
    !state.plansFresh || state.plans.none { it.basePlanId == state.selectedBasePlanId } -> BuyLock.NoFreshPlan
    else -> BuyLock.None
}

/**
 * One full load at a time. A second request while a load runs does not start a competing write;
 * it is remembered as exactly one follow-up. Every write is checked against the newest request id,
 * so the answer of an abandoned load can never replace a newer one.
 */
class PlansLoadGuard {
    private var latestId = 0L
    private var inFlight = false
    private var renewal = false

    /** The id of the load to run, or null when one already runs and this request is a follow-up. */
    @Synchronized
    fun begin(): Long? {
        if (inFlight) {
            renewal = true
            return null
        }
        inFlight = true
        latestId += 1
        return latestId
    }

    /** True when another load was asked for while this one ran: run exactly one more. */
    @Synchronized
    fun end(): Boolean {
        inFlight = false
        val again = renewal
        renewal = false
        return again
    }

    @Synchronized
    fun isLatest(id: Long): Boolean = id == latestId

    @Synchronized
    fun isLoading(): Boolean = inFlight
}
