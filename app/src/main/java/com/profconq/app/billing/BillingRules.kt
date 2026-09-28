package com.profconq.app.billing

/**
 * Rules the billing pipeline decides by, kept free of Android and Play types so they can be
 * tested directly: account generation, the verify throttle, the launch gate, offer selection.
 */

/**
 * In-memory identity of the signed-in account. It is a digest, never the uid, and it is never
 * logged, persisted or sent anywhere - in particular it is not a `setObfuscatedAccountId`.
 */
fun accountKeyOf(uid: String): String = tokenDigest(uid)

/** The account an asynchronous request started for. A result may only be applied while it holds. */
data class AccountToken(val key: String?, val generation: Long)

/**
 * Tracks which account the entitlement state belongs to. Every account change - including a
 * direct A to B swap - raises the generation, so a late answer for the previous account can be
 * recognised and dropped instead of overwriting the current one.
 */
class BillingAccountCoordinator {
    @Volatile
    var current: AccountToken = AccountToken(key = null, generation = 0L)
        private set

    /** Switches to [uid] (kept only as its digest) and returns the identity now in force. */
    @Synchronized
    fun onUid(uid: String?): AccountToken {
        val key = uid?.let { accountKeyOf(it) }
        val now = current
        if (key == now.key) return now
        current = AccountToken(key = key, generation = now.generation + 1L)
        return current
    }

    fun isCurrent(token: AccountToken): Boolean {
        val now = current
        return token.generation == now.generation && token.key == now.key
    }
}

/** Where an entitlement answer came from; the source decides what it may change locally. */
enum class EntitlementSource {
    /** `GET /api/billing/status` and `POST /api/billing/google/verify`: grants and revokes. */
    Billing,

    /** A promo code the server accepted: may grant straight away. */
    Promo,

    /** The legacy `POST /api/auth/me` projection: initializes state, never revokes billing Premium. */
    LegacyAccount,
}

/** What the coordinator tells the single local writer to do. */
sealed class EntitlementWrite {
    data class Mirror(val isPremium: Boolean, val wordLimit: Int?) : EntitlementWrite()

    /** The account is gone: the local mirror of the one that left is dropped. */
    object Reset : EntitlementWrite()

    /** This answer does not touch local state. */
    object Keep : EntitlementWrite()
}

/**
 * Whose answer currently decides the local entitlement, per account generation. Billing is the
 * authority once it has spoken: the legacy account route may still grant, but a legacy
 * `plan=free` can no longer revoke a Premium the billing snapshot confirmed. A failed read never
 * reaches this ledger at all - only an answer does.
 */
class EntitlementLedger {
    private var generation: Long? = null
    private var billingConfirmed = false
    private var premium = false

    /** True once something has been written for an account, so a reset has anything to clear. */
    private var mirrored = false

    val hasBillingSnapshot: Boolean get() = billingConfirmed

    /**
     * Resolves [source]'s answer for account [at]. A `null` verdict is an unreadable plan, which is
     * not a "free" statement, so it is dropped rather than applied.
     */
    @Synchronized
    fun resolve(
        at: AccountToken,
        source: EntitlementSource,
        premium: Boolean?,
        wordLimit: Int?,
    ): EntitlementWrite {
        val owned = generation
        if (owned != null && owned != at.generation) return EntitlementWrite.Keep
        val verdict = premium ?: return EntitlementWrite.Keep
        if (source == EntitlementSource.LegacyAccount && billingConfirmed && !verdict && this.premium) {
            return EntitlementWrite.Keep
        }
        if (source == EntitlementSource.Billing) billingConfirmed = true
        this.premium = verdict
        generation = at.generation
        mirrored = true
        return EntitlementWrite.Mirror(verdict, wordLimit)
    }

    /** Sign-out or a direct account switch: nothing the previous account confirmed survives. */
    @Synchronized
    fun forget(at: AccountToken): EntitlementWrite {
        if (!mirrored) return EntitlementWrite.Keep
        mirrored = false
        billingConfirmed = false
        premium = false
        generation = at.generation
        return EntitlementWrite.Reset
    }
}

/** Local window for outbound `/verify` calls; the server enforces its own limit independently. */
class VerifyThrottle(
    private val accounts: BillingAccountCoordinator,
    private val clock: () -> Long,
    private val delay: suspend (Long) -> Unit,
    private val maxPermits: Int = VerifyPermitsPerWindow,
    private val windowMs: Long = VerifyWindowMs,
) {
    private val startedAt = ArrayDeque<Long>()
    private var windowGeneration = -1L

    /**
     * Waits for a slot instead of dropping the purchase. Returns false when the account changed
     * or the user signed out while waiting, which cancels the queue: the token is then not
     * marked verified and a later re-query picks it up again.
     */
    suspend fun acquire(token: AccountToken): Boolean {
        while (true) {
            if (!accounts.isCurrent(token)) return false
            val now = clock()
            if (windowGeneration != token.generation) {
                startedAt.clear()
                windowGeneration = token.generation
            }
            while (startedAt.isNotEmpty() && now - startedAt.first() >= windowMs) startedAt.removeFirst()
            if (startedAt.size < maxPermits) {
                startedAt.addLast(now)
                return true
            }
            delay((windowMs - (now - startedAt.first())).coerceAtLeast(1L))
        }
    }

    /** Drops the window on sign-out or account switch; nothing is carried to the next account. */
    fun reset() {
        startedAt.clear()
        windowGeneration = -1L
    }

    companion object {
        const val VerifyPermitsPerWindow = 4
        const val VerifyWindowMs = 60_000L
    }
}

/**
 * One billing flow at a time. The gate is released by the terminal Play callbacks, by a launch
 * that did not succeed, and by the destruction of the Activity that opened the sheet; the stale
 * window is only a backstop for a callback that never arrives.
 */
class LaunchGate(
    private val clock: () -> Long,
    private val staleMs: Long = StaleMs,
) {
    @Volatile
    var startedAt = NotStarted
        private set

    @Volatile
    private var inFlight = false

    fun tryBegin(): Boolean {
        val now = clock()
        synchronized(this) {
            if (inFlight && startedAt != NotStarted && now - startedAt > staleMs) inFlight = false
            if (inFlight) return false
            inFlight = true
            startedAt = now
            return true
        }
    }

    /** A callback arrived, or the sheet did not open: either way the flow is over. */
    fun release() {
        synchronized(this) {
            inFlight = false
            startedAt = NotStarted
        }
    }

    /** The Activity that owns the sheet is gone, so no callback can be expected anymore. */
    fun onActivityDestroyed() = release()

    fun isBusy(): Boolean = synchronized(this) { inFlight }

    private companion object {
        /** -1 rather than 0: a clock that starts at zero must still read as "no flow". */
        const val NotStarted = -1L
        const val StaleMs = 5 * 60_000L
    }
}

/** A `ProductDetails.SubscriptionOfferDetails` row reduced to the fields selection looks at. */
data class OfferCandidate(
    val basePlanId: String?,
    val offerId: String?,
    val hasPricingPhase: Boolean,
    val formattedPrice: String?,
    val offerToken: String?,
)

/** One purchasable base plan, chosen deterministically. */
data class SelectedOffer(
    val basePlanId: String,
    val price: String,
    val offerToken: String,
) {
    /** offerToken belongs to the Play purchase flow; it stays out of any rendered text. */
    override fun toString(): String = "SelectedOffer($basePlanId, $price)"
}

/**
 * Only the standard record of a base plan may be bought: that is the entry whose `offerId` is
 * null, which excludes trials and promotional offers rather than picking one of them at random.
 * A row without a pricing phase, without a price or without an offer token is not offerable at
 * all, so an unusable plan simply disappears instead of showing an empty price.
 */
fun selectStandardOffers(candidates: List<OfferCandidate>): List<SelectedOffer> = candidates
    .filter { it.offerId == null }
    .mapNotNull { offer ->
        val basePlanId = offer.basePlanId?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
        if (!offer.hasPricingPhase) return@mapNotNull null
        val price = offer.formattedPrice?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
        val token = offer.offerToken?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
        SelectedOffer(basePlanId, price, token)
    }
    .filter { BillingContract.knownBasePlans.contains(it.basePlanId) }
    .distinctBy { it.basePlanId }
    .sortedBy { BillingContract.planRank(it.basePlanId) ?: Int.MAX_VALUE }

/** `BillingResult.getOnPurchasesUpdatedSubResponseCode()`, as an internal enum. */
enum class BillingSubReason {
    None,
    PaymentDeclinedInsufficientFunds,
    UserIneligible,
    Unknown,
}

/**
 * Only the purchases-updated callback carries a sub code in Billing 9.1.0; every other result
 * answers [PlaySubResponseCode.NoApplicable], and no other callback has one to read.
 */
fun billingSubReasonOf(code: Int): BillingSubReason = when (code) {
    PlaySubResponseCode.NoApplicable -> BillingSubReason.None
    PlaySubResponseCode.PaymentDeclinedInsufficientFunds -> BillingSubReason.PaymentDeclinedInsufficientFunds
    PlaySubResponseCode.UserIneligible -> BillingSubReason.UserIneligible
    else -> BillingSubReason.Unknown
}

object PlaySubResponseCode {
    const val NoApplicable = 0
    const val PaymentDeclinedInsufficientFunds = 1
    const val UserIneligible = 2
}

/** What the UI does after a `launchBillingFlow` answer that is not OK. */
enum class LaunchFollowUp {
    /** The sheet opened; the terminal callback owns whatever comes next. */
    None,

    /**
     * `ITEM_ALREADY_OWNED`: re-read Play's purchases, let the owned token run through the normal
     * verify pipeline and refresh `/status`, instead of opening the same sheet again.
     */
    RestoreAndVerify,

    /** Report the reason and leave the flow closed. */
    ShowNotice,
}

fun launchFollowUpOf(code: Int): LaunchFollowUp = when (code) {
    PlayResponseCode.OK -> LaunchFollowUp.None
    PlayResponseCode.ITEM_ALREADY_OWNED -> LaunchFollowUp.RestoreAndVerify
    PlayResponseCode.USER_CANCELED -> LaunchFollowUp.ShowNotice
    else -> LaunchFollowUp.ShowNotice
}

/** Notice tone: only real failures are shown as errors. */
enum class NoticeSeverity {
    Info,
    Neutral,
    Success,
    Danger,
}

fun noticeSeverityOf(notice: BillingNotice): NoticeSeverity = when (notice) {
    BillingNotice.Verified -> NoticeSeverity.Success
    BillingNotice.Canceled,
    BillingNotice.NothingToRestore,
    BillingNotice.Restoring,
    -> NoticeSeverity.Neutral
    BillingNotice.PendingVerification,
    BillingNotice.SignedOut,
    BillingNotice.PlanUnavailable,
    -> NoticeSeverity.Info
    BillingNotice.BillingUnavailable,
    BillingNotice.InvalidRequest,
    BillingNotice.Network,
    BillingNotice.Unauthorized,
    BillingNotice.OwnedByOther,
    BillingNotice.RateLimited,
    BillingNotice.UpstreamUnavailable,
    BillingNotice.ServiceUnavailable,
    BillingNotice.ActivityUnavailable,
    BillingNotice.Generic,
    -> NoticeSeverity.Danger
}
