package com.profconq.app.ui.billing

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.android.billingclient.api.ProductDetails
import com.profconq.app.api.BillingApi
import com.profconq.app.billing.AccountToken
import com.profconq.app.billing.BillingAccountCoordinator
import com.profconq.app.billing.BillingConnection
import com.profconq.app.billing.BillingContract
import com.profconq.app.billing.BillingError
import com.profconq.app.billing.BillingNotice
import com.profconq.app.billing.BillingRepository
import com.profconq.app.billing.BillingUiState
import com.profconq.app.billing.FreshDetails
import com.profconq.app.billing.LaunchFollowUp
import com.profconq.app.billing.PlanView
import com.profconq.app.billing.PremiumSnapshot
import com.profconq.app.billing.PremiumStatus
import com.profconq.app.billing.PurchaseVerifier
import com.profconq.app.billing.billingNoticeOfResponseCode
import com.profconq.app.billing.launchFollowUpOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
     * Play's answer from the refresh that ran immediately before it was needed. A failed or empty
     * refresh clears it, so a stale `offerToken` is never replayed and prices cannot go stale
     * underneath a purchase.
     */
    private val details = FreshDetails<ProductDetails>()

    /** Server allowlist from `GET /api/billing/products`; empty means nothing may be bought. */
    private var serverPlans: Set<String> = emptySet()

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
        refresh()
    }

    fun refresh() {
        viewModelScope.launch { load() }
    }

    /**
     * The account changed, from sign-in through sign-out to a direct switch. The state the previous
     * account confirmed is dropped before anything is read for the new one, so a late answer of the
     * old account has nothing to overwrite. Called for every `uid`, including the first frame.
     */
    fun onAccountChanged(uid: String?) {
        viewModelScope.launch {
            val at = verifier.onAccountChanged(uid)
            val signed = uid != null
            _state.update {
                it.copy(
                    status = if (signed) PremiumStatus.Loading else PremiumStatus.Free,
                    notice = null,
                    busy = false,
                    expiresAt = null,
                    autoRenews = null,
                )
            }
            if (signed) {
                applyOutcome(verifier.refreshStatus())
                applyOutcome(verifier.restore())
            }
            loadPlans(at)
        }
    }

    fun selectPlan(basePlanId: String) {
        _state.update { it.copy(selectedBasePlanId = basePlanId, notice = null) }
    }

    fun clearNotice() {
        _state.update { it.copy(notice = null) }
    }

    /**
     * Opens the Play sheet for the selected plan. Prices are re-read first, and the sheet only ever
     * opens for a standard, priced offer the server allowlist confirmed.
     */
    fun purchase(activity: Activity) {
        val current = _state.value
        if (current.busy) return
        val basePlanId = current.selectedBasePlanId
        if (!signedIn()) {
            _state.update { it.copy(notice = BillingNotice.SignedOut) }
            return
        }
        if (!serverPlans.contains(basePlanId)) {
            _state.update { it.copy(notice = BillingNotice.BillingUnavailable) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(busy = true, notice = null) }
            val at = accounts.current
            val fresh = runCatching { billing.fetchPremiumDetails() }.getOrNull()
            details.complete(fresh)
            val product = details.all().firstOrNull { candidate ->
                billing.offersFor(candidate).any { it.basePlanId == basePlanId }
            }
            val offer = product?.let { billing.offersFor(it).firstOrNull { o -> o.basePlanId == basePlanId } }
            if (product == null || offer == null) {
                // No standard, priced record for this plan right now: nothing is launched for it.
                _state.update { it.copy(busy = false, notice = BillingNotice.PlanUnavailable) }
                return@launch
            }
            val code = billing.launchBillingFlow(activity, product, offer)
            when {
                code == BillingRepository.BusyLaunchCode -> _state.update { it.copy(busy = false) }
                else -> afterLaunch(launchFollowUpOf(code), code, at)
            }
        }
    }

    fun restore() {
        viewModelScope.launch {
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

    private suspend fun load() {
        billing.connect()
        val at = verifier.currentAccount()
        applyOutcome(verifier.refreshStatus())
        loadPlans(at)
    }

    /** Play's rows come from a refresh that starts here: whatever it does not return is gone. */
    private suspend fun loadPlans(at: AccountToken = accounts.current) {
        serverPlans = runCatching { api.products() }
            .getOrNull()
            ?.premiumPlans
            ?.map { it.basePlanId }
            ?.toSet()
            .orEmpty()
        val fetched = runCatching { billing.fetchPremiumDetails() }.getOrNull()
        details.complete(fetched)
        val views = details.all()
            .flatMap { billing.offersFor(it) }
            .filter { serverPlans.contains(it.basePlanId) }
            .map { PlanView(it.basePlanId, it.title, it.price, it.offerToken) }
        if (!accounts.isCurrent(at)) return
        _state.update {
            it.copy(
                plans = views,
                selectedBasePlanId = if (views.any { view -> view.basePlanId == it.selectedBasePlanId }) {
                    it.selectedBasePlanId
                } else {
                    views.firstOrNull()?.basePlanId ?: BillingContract.BASE_PLAN_MONTHLY
                },
            )
        }
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
            is PurchaseVerifier.Outcome.Failed -> _state.update { it.copy(notice = noticeOfError(current.error)) }
            is PurchaseVerifier.Outcome.Pending -> _state.update { it.copy(status = PremiumStatus.Pending) }
            is PurchaseVerifier.Outcome.SignedOut -> _state.update {
                it.copy(notice = BillingNotice.SignedOut)
            }
        }
    }

    private fun applySnapshot(snapshot: PremiumSnapshot) {
        _state.update {
            it.copy(
                status = statusOf(snapshot),
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
