package com.profconq.app.api

import com.profconq.app.analytics.ProfconqAnalytics
import com.profconq.app.auth.AuthUser
import com.profconq.app.data.repository.ProfconqRepository
import org.json.JSONArray
import org.json.JSONObject

class DictionarySyncService(
    private val repository: ProfconqRepository,
    private val apiClient: ProfconqApiClient,
) {
    suspend fun refreshAccount(authUser: AuthUser?): AccountInfo {
        val account = apiClient.fetchAccount(
            uid = authUser?.uid.orEmpty(),
            email = authUser?.email,
            displayName = authUser?.displayName,
        )
        repository.setPremiumUser(account.isPremium)
        // plan пишется только по ответу сервера: локальный флаг до /api/me считается устаревшим.
        ProfconqAnalytics.setPlan(account.isPremium)
        if (!account.isPremium) {
            repository.setWordLimit(account.wordLimit)
        }
        return account
    }

    suspend fun redeemPromoCode(authUser: AuthUser?, code: String): AccountInfo {
        val redeemed = apiClient.redeemPromoCode(code)
        val account = redeemed.copy(
            uid = authUser?.uid.orEmpty(),
            email = authUser?.email ?: redeemed.email,
            displayName = authUser?.displayName,
        )
        repository.setPremiumUser(account.isPremium)
        ProfconqAnalytics.setPlan(account.isPremium)
        if (!account.isPremium) {
            repository.setWordLimit(account.wordLimit)
        }
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
