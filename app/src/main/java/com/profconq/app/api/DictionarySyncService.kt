package com.profconq.app.api

import com.profconq.app.analytics.ProfconqAnalytics
import com.profconq.app.auth.AuthUser
import com.profconq.app.billing.AccountToken
import com.profconq.app.billing.BillingAccountCoordinator
import com.profconq.app.billing.EntitlementLedger
import com.profconq.app.billing.EntitlementSource
import com.profconq.app.billing.EntitlementWrite
import com.profconq.app.billing.PremiumSink
import com.profconq.app.billing.PremiumSnapshot
import com.profconq.app.billing.ServerPlan
import com.profconq.app.data.WordLimitPolicy
import com.profconq.app.data.repository.ProfconqRepository
import org.json.JSONArray
import org.json.JSONObject

/**
 * The single coordinator of the local entitlement: IS_PREMIUM, WORD_LIMIT and the Analytics plan
 * are written here and nowhere else, and only from an answer that still belongs to the account it
 * was read for. Billing is the authority; the legacy account route may initialize state and a
 * accepted promo code may grant Premium, but neither can revoke a confirmed billing snapshot.
 */
class DictionarySyncService(
    private val repository: ProfconqRepository,
    private val apiClient: ProfconqApiClient,
    private val accounts: BillingAccountCoordinator,
) : PremiumSink {
    private val entitlements = EntitlementLedger()

    override suspend fun applyPremiumSnapshot(at: AccountToken, snapshot: PremiumSnapshot) {
        // An unreadable plan is not a "free" verdict, so it carries no decision at all.
        val premium = if (snapshot.plan == ServerPlan.Unknown) null else snapshot.isPremium
        write(entitlements.resolve(at, EntitlementSource.Billing, premium, snapshot.wordLimit))
    }

    /** Sign-out or an account switch: the leaving account's mirror is dropped. */
    override suspend fun onAccountChanged(at: AccountToken) {
        write(entitlements.forget(at))
    }

    /** Auth-side sign-out: same single path, so the coordinator stays the only writer. */
    suspend fun onSignedOut() {
        write(entitlements.forget(accounts.onUid(null)))
    }

    private suspend fun write(decision: EntitlementWrite) {
        when (decision) {
            EntitlementWrite.Keep -> Unit
            EntitlementWrite.Reset -> {
                repository.resetAccountLimits()
                ProfconqAnalytics.setPlan(false)
            }
            is EntitlementWrite.Mirror -> {
                repository.setPremiumUser(decision.isPremium)
                ProfconqAnalytics.setPlan(decision.isPremium)
                // A Premium account has no word limit of its own; the stored one stays untouched.
                if (!decision.isPremium) {
                    repository.setWordLimit(decision.wordLimit ?: WordLimitPolicy.FREE_LIMIT)
                }
            }
        }
    }

    suspend fun refreshAccount(authUser: AuthUser?): AccountInfo {
        val account = apiClient.fetchAccount(
            uid = authUser?.uid.orEmpty(),
            email = authUser?.email,
            displayName = authUser?.displayName,
        )
        write(
            entitlements.resolve(
                accounts.current,
                EntitlementSource.LegacyAccount,
                account.isPremium,
                account.wordLimit,
            )
        )
        return account
    }

    suspend fun redeemPromoCode(authUser: AuthUser?, code: String): AccountInfo {
        val redeemed = apiClient.redeemPromoCode(code)
        val account = redeemed.copy(
            uid = authUser?.uid.orEmpty(),
            email = authUser?.email ?: redeemed.email,
            displayName = authUser?.displayName,
        )
        write(entitlements.resolve(accounts.current, EntitlementSource.Promo, account.isPremium, account.wordLimit))
        return account
    }

    suspend fun getSyncPrimary(): SyncPrimary = repository.getSyncPrimary()

    suspend fun setSyncPrimary(primary: SyncPrimary) {
        repository.setSyncPrimary(primary)
    }

    /**
     * Run mirror sync for the selected primary source.
     * APP → cloud (+form for site); SITE → device.
     */
    suspend fun mirrorSync(authUser: AuthUser?, primary: SyncPrimary): MirrorSyncResult {
        return when (primary) {
            SyncPrimary.APP -> mirrorPushToCloud(authUser)
            SyncPrimary.SITE -> mirrorPullFromCloud(authUser)
        }
    }

    private suspend fun mirrorPushToCloud(authUser: AuthUser?): MirrorSyncResult {
        val words = repository.buildWebVocabularyWords()
        apiClient.pushVocabulary(words)
        val programs = repository.exportProgramsForWebsite(words)
        apiClient.pushSyncJson("programs", programs)
        apiClient.pushSyncJson("program_progress", JSONObject())
        val account = refreshAccount(authUser)
        return MirrorSyncResult(
            account = account.copy(wordCount = words.size),
            primary = SyncPrimary.APP,
            wordCount = words.size,
            programCount = programs.length(),
        )
    }

    private suspend fun mirrorPullFromCloud(authUser: AuthUser?): MirrorSyncResult {
        val words = apiClient.pullVocabulary()
        repository.mirrorReplaceLocalFromWeb(words)
        val programs = apiClient.pullSyncJson("programs")
        var programCount = 0
        if (programs is JSONArray) {
            programCount = programs.length()
            repository.mirrorImportProgramsFromWebsite(programs)
        }
        apiClient.pullSyncJson("program_progress")
        runCatching {
            // Studio collections live inside settings blob on the website.
            // Wired via StudioSyncService when user opens Studio; keep vocab sync lean here.
        }
        val account = refreshAccount(authUser)
        return MirrorSyncResult(
            account = account.copy(wordCount = words.size),
            primary = SyncPrimary.SITE,
            wordCount = words.size,
            programCount = programCount,
        )
    }
}

data class MirrorSyncResult(
    val account: AccountInfo,
    val primary: SyncPrimary,
    val wordCount: Int,
    val programCount: Int,
)
