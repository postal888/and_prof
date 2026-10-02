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
 * The profile line as the entitlement this device applied says it. `/me` can name a plan that the
 * billing answer has since replaced, so the counts come from the read while the plan and its limit
 * come from what is actually applied: a free read never walks a confirmed Premium back down to the
 * free limit, and a Premium that was applied has no limit to show.
 */
fun profileOf(appliedPremium: Boolean, account: AccountInfo): AccountInfo = when {
    appliedPremium -> account.copy(isPremium = true, wordLimit = WordLimitPolicy.UNLIMITED)
    account.isPremium -> account.copy(isPremium = false, wordLimit = WordLimitPolicy.FREE_LIMIT)
    else -> account
}

/**
 * One account read at a time. A screen that asks again while one is running is served by the read in
 * flight instead of starting a second one beside it: without this, every new composition of the
 * profile asked again, and one refused session turned into a storm of `/me` requests.
 */
class AccountReadGuard {
    private var reading = false

    /** True when this caller starts the read; false when one is already running. */
    @Synchronized
    fun begin(): Boolean {
        if (reading) return false
        reading = true
        return true
    }

    @Synchronized
    fun end() {
        reading = false
    }
}

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

    /**
     * Whether this device is running on an applied Premium entitlement — the one word limit the
     * profile line may show, because it is the one the composer is held to.
     */
    suspend fun isEntitledPremium(): Boolean = repository.isPremiumUser()

    /**
     * The cloud profile. A read that was refused or never answered throws, and the caller keeps the
     * profile it last had: there is nothing in a non-answer that could replace an entitlement.
     */
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
    suspend fun getSyncMerge(): Boolean = repository.getSyncMerge()

    suspend fun setSyncMerge(merge: Boolean) {
        repository.setSyncMerge(merge)
    }

    suspend fun mirrorSync(authUser: AuthUser?, primary: SyncPrimary, merge: Boolean = false): MirrorSyncResult {
        if (merge) return mergeSync(authUser, primary)
        return when (primary) {
            SyncPrimary.APP -> mirrorPushToCloud(authUser)
            SyncPrimary.SITE -> mirrorPullFromCloud(authUser)
        }
    }

    private suspend fun mirrorPushToCloud(authUser: AuthUser?): MirrorSyncResult {
        val words = repository.buildWebVocabularyWords()
        val pushed = apiClient.pushVocabulary(words)
        if (pushed.refused) {
            // The server kept its larger vocabulary, so the programs and progress that belong to it
            // stay too: overwriting them with this device's sets would orphan the site's word ids.
            val account = refreshAccount(authUser)
            return MirrorSyncResult(
                account = account,
                primary = SyncPrimary.APP,
                wordCount = words.size,
                programCount = 0,
                refusedServerWordCount = pushed.wordCount,
            )
        }
        val programs = repository.exportProgramsForWebsite(words)
        apiClient.pushSyncJson("programs", programs)
        apiClient.pushSyncJson("program_progress", JSONObject())
        val account = refreshAccount(authUser)
        return MirrorSyncResult(
            account = account,
            primary = SyncPrimary.APP,
            wordCount = words.size,
            programCount = programs.length(),
        )
    }

    /**
     * Both sides end up with every word either had; for a word on both, the later edit wins and
     * the chosen source breaks ties. Programs and their progress are left alone: they belong to the
     * website and only the full mirror replaces them.
     */
    private suspend fun mergeSync(authUser: AuthUser?, primary: SyncPrimary): MirrorSyncResult {
        // Earlier app versions uploaded Studio's sample cards; they are not the user's words.
        val server = apiClient.pullVocabulary().filter { it.videoId != STUDIO_DEMO_FOLDER_ID }
        val local = repository.buildWebVocabularyWords()
        val result = mergeVocabulary(local, server, preferLocalOnTie = primary == SyncPrimary.APP)
        repository.applyMergedServerWords(result.applyLocally)
        apiClient.pushVocabulary(result.merged)
        val account = refreshAccount(authUser)
        return MirrorSyncResult(
            account = account,
            primary = primary,
            wordCount = result.merged.size,
            programCount = 0,
            merge = result,
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
            account = account,
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
    /** Set when the server refused to replace its vocabulary of this many words with the push. */
    val refusedServerWordCount: Int? = null,
    /** Set when the sync merged both sides instead of mirroring one of them. */
    val merge: VocabularyMergeResult? = null,
)

/** The folder id the old app gave Studio's sample collection on the website. */
private const val STUDIO_DEMO_FOLDER_ID = "studio-demo"
