package com.profconq.app.ui.billing

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.profconq.app.api.BillingApi
import com.profconq.app.api.BillingApiException
import com.profconq.app.billing.AccountToken
import com.profconq.app.billing.BillingAccountCoordinator
import com.profconq.app.billing.BillingConnection
import com.profconq.app.billing.BillingError
import com.profconq.app.billing.BillingNotice
import com.profconq.app.billing.BillingRepository
import com.profconq.app.billing.BillingUiState
import com.profconq.app.billing.BuyGate
import com.profconq.app.billing.BuyLock
import com.profconq.app.billing.LaunchFollowUp
import com.profconq.app.billing.PlansFailure
import com.profconq.app.billing.PlansLoadGuard
import com.profconq.app.billing.PlansOutcome
import com.profconq.app.billing.PlayResponseCode
import com.profconq.app.billing.PremiumSnapshot
import com.profconq.app.billing.PremiumStatus
import com.profconq.app.billing.ProductDetailsQueryResult
import com.profconq.app.billing.PurchaseVerifier
import com.profconq.app.billing.applyPlansOutcome
import com.profconq.app.billing.billingNoticeOfResponseCode
import com.profconq.app.billing.buyLockOf
import com.profconq.app.billing.launchFollowUpOf
import com.profconq.app.billing.runCatalogueLoad
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Profile's Premium section. Entitlement is read from the server snapshot only, and only while it
 * belongs to the account that asked for it; the Play side supplies product names, prices and the
 * purchase flow.
 */
class BillingViewModel(
    private val billing: BillingRepository,
    private val verifier: PurchaseVerifier,
    private val api: BillingApi,
    private val accounts: BillingAccountCoordinator,
    private val signedIn: () -> Boolean,
) : ViewModel() {
    private val _state = MutableStateFlow(BillingUiState())
    val state: StateFlow<BillingUiState> = _state.asStateFlow()

    /**
     * The last allowlist the server confirmed. A failed read keeps it: "the read did not answer"
     * is not the statement "this plan is not on the allowlist", and only an answer replaces it.
     */
    private var serverPlans: Set<String> = emptySet()

    /** One full load at a time, only the newest may write, and a second request becomes one retry. */
    private val plansGuard = PlansLoadGuard()

    /**
     * The entitlement the last server answer supported. A record the server still calls entitled
     * beside an answer of no access waits for one re-check instead of opening a second sheet, and a
     * re-check that failed for a temporary reason keeps the verdict it had before it.
     */
    private val buyGate = BuyGate()

    /** Play is asked by one caller at a time: a purchase re-read must not run beside a plans load. */
    private val playMutex = Mutex()

    init {
        viewModelScope.launch {
            verifier.outcomes.collect { outcome -> applyOutcome(outcome) }
        }
        viewModelScope.launch {
            billing.purchases.collect { purchase ->
                when {
                    purchase.isPending -> _state.update { it.copy(status = PremiumStatus.Pending) }
                    // Verifying is a progress hint only: Play's state never grants access here.
                    purchase.isPurchased && signedIn() ->
                        _state.update { it.copy(status = PremiumStatus.Verifying) }
                }
            }
        }
        viewModelScope.launch {
            billing.connection.collect { connection ->
                if (connection == BillingConnection.Failed) markUnavailable()
            }
        }
        // The one event that turns an empty list back into plans: the Play service came back. The
        // load it starts calls connect() on a ready client, so this cannot loop.
        viewModelScope.launch {
            billing.serviceReconnected.collect { requestPlansLoad() }
        }
        // No load here on purpose. The account-keyed trigger is the single initial one, so two
        // loads never run for one account and compete for the same write.
    }

    /**
     * The account changed, from sign-in through sign-out to a direct switch. The state the previous
     * account confirmed is dropped before anything is read for the new one. Called for every `uid`,
     * including the first frame, which makes it the one deterministic initial trigger.
     */
    fun onAccountChanged(uid: String?) {
        viewModelScope.launch {
            val at = verifier.onAccountChanged(uid)
            val signed = uid != null
            val gate = buyGate.onAccountChanged(signed)
            _state.update {
                it.copy(
                    status = if (signed) PremiumStatus.Loading else PremiumStatus.Free,
                    notice = null,
                    busy = false,
                    expiresAt = null,
                    autoRenews = null,
                    entitlementGate = gate,
                )
            }
            if (signed) {
                applyOutcome(verifier.refreshStatus())
                applyOutcome(verifier.restore())
            }
            requestPlansLoad()
        }
    }

    /**
     * Reloads the whole catalogue: server allowlist, Play connection, product details, offer
     * filter. Retry never stops at `/status` or at a purchases re-read.
     */
    fun retryPlans() {
        requestPlansLoad()
    }

    fun selectPlan(basePlanId: String) {
        _state.update { it.copy(selectedBasePlanId = basePlanId, notice = null) }
    }

    fun clearNotice() {
        _state.update { it.copy(notice = null) }
    }

    /**
     * Opens the Play sheet. The details and the offer come from a read this press just made, so a
     * sheet is never opened with a token the UI still happens to be showing. The same rule that
     * decides whether the button is lit decides whether this press does anything.
     */
    fun purchase(activity: Activity) {
        val current = _state.value
        val lock = buyLockOf(current, signedIn(), buyGate.current())
        if (lock != BuyLock.None) {
            // A press of a live-looking button in a transient state is dropped, the way a second
            // press while the sheet is opening always was; only a real block gets a notice.
            val notice = noticeOfBuyLock(lock, current.plansFailure) ?: return
            _state.update { it.copy(notice = notice) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(busy = true, notice = null) }
            val at = accounts.current
            val basePlanId = current.selectedBasePlanId
            val query = playMutex.withLock { billing.queryPremiumOffers() }
            val launchable = query.launchableFor(basePlanId)
                ?.takeIf { serverPlans.contains(basePlanId) && accounts.isCurrent(at) }
            if (launchable == null) {
                // Nothing fresh and allowlisted for this plan: the sheet stays closed, and the
                // notice says which side of the read failed rather than that a button was missing.
                _state.update { it.copy(busy = false, notice = purchaseBlockerNotice(query)) }
                return@launch
            }
            val code = billing.launchBillingFlow(activity, launchable.details, launchable.offer)
            when {
                code == BillingRepository.BusyLaunchCode -> _state.update { it.copy(busy = false) }
                else -> afterLaunch(launchFollowUpOf(code), code, at)
            }
        }
    }

    fun restore() {
        viewModelScope.launch {
            if (!buyGate.restoreAllowed()) {
                // The re-check waits out its cooldown: neither Play nor the server is asked again,
                // and the press is answered instead of silently doing nothing.
                _state.update {
                    it.copy(notice = BillingNotice.RateLimited, entitlementGate = buyGate.current())
                }
                return@launch
            }
            _state.update { it.copy(busy = true, notice = null) }
            val failed = verifier.restore()
            if (failed == null) applyOutcome(verifier.refreshStatus())
            _state.update { it.copy(busy = false) }
        }
    }

    /** The Play sheet needs an Activity host; without one nothing is launched and the user is told. */
    fun onPurchaseScreenGone() {
        _state.update { it.copy(busy = false, notice = BillingNotice.ActivityUnavailable) }
    }

    private fun requestPlansLoad() {
        val id = plansGuard.begin() ?: return
        viewModelScope.launch {
            try {
                runPlansLoad(id)
            } finally {
                // A request that arrived while this load ran is honoured by exactly one more run.
                if (plansGuard.end()) requestPlansLoad()
            }
        }
    }

    private suspend fun runPlansLoad(id: Long) {
        _state.update { it.copy(plansLoading = true) }
        val at = accounts.current
        val load = runCatalogueLoad(
            lastServerPlans = serverPlans,
            products = {
                runCatching { api.products() }
                    .map { catalogue -> catalogue.premiumPlans.map { plan -> plan.basePlanId }.toSet() }
            },
            errorOf = ::billingErrorOf,
            play = { playMutex.withLock { billing.queryPremiumOffers().snapshot } },
        )
        // Only an answer replaces the allowlist; a load that failed keeps the last one that stood.
        serverPlans = load.serverPlans
        publishPlans(id, at, load.outcome)
    }

    private fun publishPlans(id: Long, at: AccountToken, outcome: PlansOutcome) {
        _state.update {
            applyPlansOutcome(
                state = it,
                outcome = outcome,
                newest = plansGuard.isLatest(id),
                accountCurrent = accounts.isCurrent(at),
            )
        }
    }

    private fun billingErrorOf(failure: Throwable): BillingError =
        (failure as? BillingApiException)?.error ?: BillingError.Unknown

    private fun noticeOfBuyLock(lock: BuyLock, failure: PlansFailure?): BillingNotice? = when (lock) {
        BuyLock.SignedOut -> BillingNotice.SignedOut
        // A press while a record that still reads as owned waits to be re-checked says what is owed,
        // and the Restore button below is the press that pays for it.
        BuyLock.NeedsReverify -> BillingNotice.Restoring
        BuyLock.ReverifyCooldown -> BillingNotice.RateLimited
        BuyLock.NoFreshPlan -> failure?.let(::noticeOfPlansFailure) ?: BillingNotice.BillingUnavailable
        BuyLock.None, BuyLock.Loading, BuyLock.Pressing, BuyLock.AlreadyActive -> null
    }

    private fun noticeOfPlansFailure(failure: PlansFailure): BillingNotice = when (failure) {
        PlansFailure.BackendCatalogue -> BillingNotice.UpstreamUnavailable
        PlansFailure.PlayConnection, PlansFailure.PlayQuery -> BillingNotice.BillingUnavailable
        PlansFailure.Unfetched, PlansFailure.NoEligibleOffers, PlansFailure.FilteredOut ->
            BillingNotice.PlanUnavailable
    }

    private fun purchaseBlockerNotice(query: ProductDetailsQueryResult): BillingNotice = when {
        !query.reachedPlay || query.responseCode != PlayResponseCode.OK -> BillingNotice.BillingUnavailable
        else -> BillingNotice.PlanUnavailable
    }

    private fun afterLaunch(followUp: LaunchFollowUp, code: Int, at: AccountToken) {
        if (!accounts.isCurrent(at)) return
        when (followUp) {
            LaunchFollowUp.None -> _state.update { it.copy(busy = false) }
            LaunchFollowUp.ShowNotice -> _state.update {
                it.copy(busy = false, notice = billingNoticeOfResponseCode(code))
            }
            // The plan is already owned: re-read Play, let the token go through verification and
            // take the entitlement from /status again, instead of reopening the same sheet.
            LaunchFollowUp.RestoreAndVerify -> restoreOwned()
        }
    }

    private fun restoreOwned() {
        viewModelScope.launch {
            if (!buyGate.restoreAllowed()) {
                _state.update {
                    it.copy(
                        busy = false,
                        notice = BillingNotice.RateLimited,
                        entitlementGate = buyGate.current(),
                    )
                }
                return@launch
            }
            _state.update {
                it.copy(busy = false, status = PremiumStatus.Restoring, notice = BillingNotice.Restoring)
            }
            val failed = verifier.restore()
            if (failed == null) applyOutcome(verifier.refreshStatus())
        }
    }

    private fun applyOutcome(outcome: PurchaseVerifier.Outcome?) {
        val current = outcome ?: return
        // A result of an account that is no longer in force says nothing about this one.
        if (!accounts.isCurrent(current.at)) return
        when (current) {
            is PurchaseVerifier.Outcome.Applied -> applySnapshot(current.snapshot)
            is PurchaseVerifier.Outcome.Failed -> {
                // A read that failed is not a verdict about access: it can only put an owed re-check
                // behind a cooldown, and never unlock a purchase.
                buyGate.onReverifyFailure(current.error, signedIn())
                _state.update {
                    it.copy(notice = noticeOfError(current.error), entitlementGate = buyGate.current())
                }
            }
            is PurchaseVerifier.Outcome.Pending -> _state.update { it.copy(status = PremiumStatus.Pending) }
            is PurchaseVerifier.Outcome.SignedOut -> {
                buyGate.onSnapshot(null, signedIn())
                _state.update {
                    it.copy(notice = BillingNotice.SignedOut, entitlementGate = buyGate.current())
                }
            }
        }
    }

    private fun applySnapshot(snapshot: PremiumSnapshot) {
        val gate = buyGate.onSnapshot(snapshot, signedIn())
        _state.update {
            it.copy(
                status = statusOf(snapshot),
                entitlementGate = gate,
                notice = if (snapshot.isPremium && it.status != PremiumStatus.Active) {
                    BillingNotice.Verified
                } else {
                    it.notice
                },
                expiresAt = snapshot.governing?.expiresAt ?: snapshot.expiresAt,
                autoRenews = snapshot.governing?.let { g -> snapshot.entitlements.any { it.id == g.id && it.autoRenews } },
            )
        }
    }

    private fun statusOf(snapshot: PremiumSnapshot): PremiumStatus = when {
        // A stale answer means the server could not re-confirm with Google. Premium stays as is.
        snapshot.isPremium && snapshot.stale -> PremiumStatus.Stale
        snapshot.isPremium -> PremiumStatus.Active
        snapshot.governingStatus == "pending" -> PremiumStatus.Pending
        else -> PremiumStatus.Free
    }

    private fun markUnavailable() {
        _state.update {
            if (it.status == PremiumStatus.Loading) it.copy(status = PremiumStatus.Unavailable) else it
        }
    }

    private fun noticeOfError(error: BillingError): BillingNotice = when (error) {
        BillingError.Unauthorized -> BillingNotice.Unauthorized
        BillingError.OwnedByOtherAccount -> BillingNotice.OwnedByOther
        BillingError.RateLimited -> BillingNotice.RateLimited
        BillingError.UpstreamUnavailable -> BillingNotice.UpstreamUnavailable
        BillingError.ServiceUnavailable, BillingError.ProductNotAllowed -> BillingNotice.ServiceUnavailable
        BillingError.Network -> BillingNotice.Network
        BillingError.InvalidRequest -> BillingNotice.InvalidRequest
        else -> BillingNotice.Generic
    }
}

class BillingViewModelFactory(
    private val billing: BillingRepository,
    private val verifier: PurchaseVerifier,
    private val api: BillingApi,
    private val accounts: BillingAccountCoordinator,
    private val signedIn: () -> Boolean,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(BillingViewModel::class.java)) { "Unknown ViewModel" }
        return BillingViewModel(billing, verifier, api, accounts, signedIn) as T
    }
}
