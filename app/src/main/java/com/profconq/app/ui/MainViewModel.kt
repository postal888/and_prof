package com.profconq.app.ui

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.android.gms.common.api.ApiException
import com.profconq.app.api.AccountInfo
import com.profconq.app.api.AccountReadGuard
import com.profconq.app.api.DictionarySyncService
import com.profconq.app.api.MirrorSyncResult
import com.profconq.app.api.ProfconqAdminSession
import com.profconq.app.api.ProfconqApiException
import com.profconq.app.api.ProfconqSessionAuth
import com.profconq.app.api.SyncPrimary
import com.profconq.app.api.profileOf
import com.profconq.app.auth.AuthUser
import com.profconq.app.auth.FirebaseAuthManager
import com.profconq.app.billing.PremiumStatus
import com.profconq.app.data.DataOwnership
import com.profconq.app.data.WordLimitPolicy
import com.profconq.app.data.accountDataOwnerOf
import com.profconq.app.data.dataOwnershipOf
import com.profconq.app.data.model.AppSettings
import com.profconq.app.data.model.AppThemeMode
import com.profconq.app.data.model.Collection
import com.profconq.app.data.model.DictionaryEntry
import com.profconq.app.data.model.ProgressSnapshot
import com.profconq.app.data.model.StudySet
import com.profconq.app.data.model.TodayPlan
import com.profconq.app.data.model.DailyStudyActivity
import com.profconq.app.data.WordLimitReachedException
import com.profconq.app.data.model.WordCard
import com.profconq.app.data.repository.ProfconqRepository
import com.profconq.app.ui.i18n.AppLanguage
import com.profconq.app.ui.i18n.UiStrings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface WordLimitProjection {
    data object Unknown : WordLimitProjection
    data object Unlimited : WordLimitProjection
    data class Limited(val value: Int) : WordLimitProjection
}

internal class WordLimitProjectionState {
    private val _value = MutableStateFlow<WordLimitProjection>(WordLimitProjection.Unknown)
    val value: StateFlow<WordLimitProjection> = _value.asStateFlow()
    private var persisted: WordLimitProjection = WordLimitProjection.Unknown
    private var account: WordLimitProjection = WordLimitProjection.Unknown
    private var billing: WordLimitProjection = WordLimitProjection.Unknown

    fun restorePersistedPremium(isPremium: Boolean) {
        persisted = if (isPremium) WordLimitProjection.Unlimited else WordLimitProjection.Unknown
        publishResolved()
    }

    fun publishBillingStatus(status: PremiumStatus) {
        billing = when (status) {
            PremiumStatus.Active,
            PremiumStatus.Stale,
            -> WordLimitProjection.Unlimited

            PremiumStatus.Free -> WordLimitProjection.Limited(WordLimitPolicy.FREE_LIMIT)
            PremiumStatus.Loading -> WordLimitProjection.Unknown
            else -> billing
        }
        publishResolved()
    }

    fun publish(result: Result<AccountInfo>) {
        val profile = result.getOrNull() ?: return
        account = if (profile.isPremium) {
            WordLimitProjection.Unlimited
        } else {
            WordLimitProjection.Limited(profile.wordLimit)
        }
        publishResolved()
    }

    fun clear() {
        persisted = WordLimitProjection.Unknown
        account = WordLimitProjection.Unknown
        billing = WordLimitProjection.Unknown
        publishResolved()
    }

    // The account route may grant Premium past a free billing answer (see EntitlementLedger), and
    // the composer then has no cap, so the profile cannot show the free limit for it either.
    private fun publishResolved() {
        _value.value = when {
            account == WordLimitProjection.Unlimited -> account
            billing != WordLimitProjection.Unknown -> billing
            account != WordLimitProjection.Unknown -> account
            else -> persisted
        }
    }
}

class MainViewModel(
    private val repository: ProfconqRepository,
    private val authManager: FirebaseAuthManager,
    private val dictionarySyncService: DictionarySyncService,
    private val adminSession: ProfconqAdminSession,
    private val sessionAuth: ProfconqSessionAuth,
    private val onSignOutCleanup: () -> Unit = {},
    private val onAccountDataWiped: () -> Unit = {},
) : ViewModel() {
    val authUser: StateFlow<AuthUser?> = authManager.authUser
    private val _authBusy = MutableStateFlow(false)
    val authBusy: StateFlow<Boolean> = _authBusy.asStateFlow()
    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()
    private val _cloudAccount = MutableStateFlow<AccountInfo?>(null)
    val cloudAccount: StateFlow<AccountInfo?> = _cloudAccount.asStateFlow()
    private val wordLimitProjectionState = WordLimitProjectionState()
    val wordLimitProjection: StateFlow<WordLimitProjection> = wordLimitProjectionState.value
    private val _syncBusy = MutableStateFlow(false)
    val syncBusy: StateFlow<Boolean> = _syncBusy.asStateFlow()
    private val _syncMessage = MutableStateFlow<String?>(null)
    val syncMessage: StateFlow<String?> = _syncMessage.asStateFlow()
    private val _syncPrimary = MutableStateFlow(SyncPrimary.APP)
    val syncPrimary: StateFlow<SyncPrimary> = _syncPrimary.asStateFlow()

    private val _syncMerge = MutableStateFlow(false)
    val syncMerge: StateFlow<Boolean> = _syncMerge.asStateFlow()
    private val _wordLimitMessage = MutableStateFlow<String?>(null)
    val wordLimitMessage: StateFlow<String?> = _wordLimitMessage.asStateFlow()
    private val _promoBusy = MutableStateFlow(false)
    val promoBusy: StateFlow<Boolean> = _promoBusy.asStateFlow()
    private val _promoMessage = MutableStateFlow<String?>(null)
    val promoMessage: StateFlow<String?> = _promoMessage.asStateFlow()
    private val _adminUsername = MutableStateFlow<String?>(null)
    val adminUsername: StateFlow<String?> = _adminUsername.asStateFlow()
    private val _adminBusy = MutableStateFlow(false)
    val adminBusy: StateFlow<Boolean> = _adminBusy.asStateFlow()
    private val _adminError = MutableStateFlow<String?>(null)
    val adminError: StateFlow<String?> = _adminError.asStateFlow()

    /** The signed-in account is not the one the device's learning data belongs to. */
    private val _accountSwitchPending = MutableStateFlow(false)
    val accountSwitchPending: StateFlow<Boolean> = _accountSwitchPending.asStateFlow()
    private val _accountSwitchBusy = MutableStateFlow(false)
    val accountSwitchBusy: StateFlow<Boolean> = _accountSwitchBusy.asStateFlow()

    init {
        viewModelScope.launch {
            wordLimitProjectionState.restorePersistedPremium(
                dictionarySyncService.isEntitledPremium()
            )
            _syncPrimary.value = dictionarySyncService.getSyncPrimary()
            _syncMerge.value = dictionarySyncService.getSyncMerge()
            _adminUsername.value = adminSession.currentUsername()
            restoreSiteSession()
        }
        authManager.authUser
            .onEach { user ->
                if (user != null) {
                    claimLocalData(user)
                    refreshCloudAccount()
                } else {
                    _cloudAccount.value = null
                    _accountSwitchPending.value = false
                }
            }
            .launchIn(viewModelScope)
    }

    private suspend fun claimLocalData(user: AuthUser) {
        val owner = accountDataOwnerOf(user)
        when (dataOwnershipOf(repository.getDataOwner(), owner)) {
            DataOwnership.Adopt -> repository.setDataOwner(owner)
            DataOwnership.Same -> _accountSwitchPending.value = false
            DataOwnership.Foreign -> _accountSwitchPending.value = true
        }
    }

    /** Replaces another account's learning data on this device with the signed-in account's own. */
    fun confirmAccountSwitch() {
        val user = authManager.authUser.value ?: return
        if (_accountSwitchBusy.value) return
        viewModelScope.launch {
            _accountSwitchBusy.value = true
            repository.wipeAccountData()
            onAccountDataWiped()
            repository.setDataOwner(accountDataOwnerOf(user))
            _accountSwitchPending.value = false
            _syncBusy.value = true
            _syncMessage.value = null
            runCatching { dictionarySyncService.mirrorSync(user, SyncPrimary.SITE) }
                .onSuccess { result ->
                    publishCloudAccount(result.account)
                    _syncMessage.value = formatMirrorSyncMessage(result)
                }
                .onFailure { error -> _syncMessage.value = mapSyncError(error) }
            _syncBusy.value = false
            _accountSwitchBusy.value = false
        }
    }

    /** Leaves the device's data untouched by signing the other account back out. */
    fun declineAccountSwitch() {
        _accountSwitchPending.value = false
        signOut()
    }

    fun publishBillingStatus(status: PremiumStatus) {
        wordLimitProjectionState.publishBillingStatus(status)
        // The profile line's plan and word count come from /me, which a purchase also changes.
        if (status == PremiumStatus.Active) refreshCloudAccount()
    }

    val collections: StateFlow<List<Collection>> =
        repository.collections.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val studySets: StateFlow<List<StudySet>> =
        repository.studySets.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val dictionaryCollections: StateFlow<List<Collection>> =
        repository.dictionaryCollections.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val dictionary: StateFlow<List<DictionaryEntry>> =
        repository.dictionary.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val todayPlan: StateFlow<TodayPlan> =
        repository.todayPlan.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            TodayPlan(dueCount = 0, newCount = 0, streak = 0),
        )

    val dailyActivities: StateFlow<List<DailyStudyActivity>> =
        repository.dailyActivities.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            emptyList(),
        )

    val progress: StateFlow<ProgressSnapshot> =
        repository.progressSnapshot.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            ProgressSnapshot(),
        )

    val settings: StateFlow<AppSettings> =
        repository.settings.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            AppSettings(),
        )

    fun setUseChatGptTranslation(enabled: Boolean) {
        viewModelScope.launch { repository.setUseChatGptTranslation(enabled) }
    }

    fun setPhraseCopyEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setPhraseCopyEnabled(enabled) }
    }

    fun setYoutubeBackgroundPlayback(enabled: Boolean) {
        viewModelScope.launch { repository.setYoutubeBackgroundPlayback(enabled) }
    }

    fun setWordContextExampleEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setWordContextExampleEnabled(enabled) }
    }

    fun setSubtitleFontSizeLevel(level: Int) {
        viewModelScope.launch { repository.setSubtitleFontSizeLevel(level) }
    }

    fun setUiLanguage(languageCode: Int) {
        viewModelScope.launch { repository.setUiLanguage(languageCode) }
    }

    fun setTranslationSourceLanguage(languageCode: Int) {
        viewModelScope.launch { repository.setTranslationSourceLanguage(languageCode) }
    }

    fun setTranslationTargetLanguage(languageCode: Int) {
        viewModelScope.launch { repository.setTranslationTargetLanguage(languageCode) }
    }

    fun setThemeMode(mode: AppThemeMode) {
        viewModelScope.launch { repository.setThemeMode(mode) }
    }

    fun markKnown(cardId: String) {
        viewModelScope.launch { repository.markCardKnown(cardId) }
    }

    fun markRepeat(cardId: String) {
        viewModelScope.launch { repository.markCardRepeat(cardId) }
    }

    fun deleteCollection(collectionId: String) {
        viewModelScope.launch { repository.deleteCollection(collectionId) }
    }

    fun deleteCard(cardId: String) {
        viewModelScope.launch { repository.deleteCard(cardId) }
    }

    fun deleteStudySet(setId: String) {
        viewModelScope.launch { repository.deleteStudySet(setId) }
    }

    fun renameStudySet(setId: String, name: String) {
        viewModelScope.launch { repository.renameStudySet(setId, name) }
    }

    fun updateCollectionTitle(collectionId: String, title: String) {
        viewModelScope.launch { repository.updateCollectionTitle(collectionId, title) }
    }

    fun updateCollectionDescription(collectionId: String, description: String?) {
        viewModelScope.launch { repository.updateCollectionDescription(collectionId, description) }
    }

    fun createManualCollection(title: String, description: String? = null, onCreated: (String) -> Unit = {}) {
        viewModelScope.launch {
            val id = repository.createManualCollection(title, description)
            onCreated(id)
        }
    }

    fun addCardToCollection(collectionId: String, pt: String, ru: String, example: String? = null) {
        viewModelScope.launch {
            runCatching { repository.addCardToCollection(collectionId, pt, ru, example) }
                .onFailure { reportWordLimit(it) }
        }
    }

    fun updateCollectionFolderAudio(collectionId: String, path: String?) {
        viewModelScope.launch { repository.updateCollectionFolderAudio(collectionId, path) }
    }

    fun appendCollectionFolderAudio(collectionId: String, path: String) {
        viewModelScope.launch { repository.appendCollectionFolderAudio(collectionId, path) }
    }

    fun setCollectionFolderAudios(
        collectionId: String,
        paths: List<String>,
        pathReplacements: Map<String, String> = emptyMap(),
    ) {
        viewModelScope.launch { repository.setCollectionFolderAudios(collectionId, paths, pathReplacements) }
    }

    fun setCollectionFolderAudioLabel(collectionId: String, path: String, label: String) {
        viewModelScope.launch { repository.setCollectionFolderAudioLabel(collectionId, path, label) }
    }

    fun updateCard(card: WordCard) {
        viewModelScope.launch { repository.updateCard(card) }
    }

    fun setCardLearnMark(cardId: String, mark: String) {
        viewModelScope.launch {
            repository.setCardLearnMark(cardId, mark)
        }
    }

    fun addDictionaryEntry(pt: String, ru: String, example: String? = null) {
        viewModelScope.launch {
            runCatching { repository.addDictionaryEntry(pt, ru, example) }
                .onFailure { reportWordLimit(it) }
        }
    }

    fun resetProgressMetrics() {
        viewModelScope.launch { repository.resetProgressMetrics() }
    }

    fun addWordsToStudySet(setId: String, wordIds: Set<String>) {
        viewModelScope.launch {
            repository.addWordsToStudySet(setId, wordIds.toList())
        }
    }

    fun createGoogleSignInIntent(): Intent? {
        return runCatching { authManager.createGoogleSignInIntent() }
            .onFailure { _authError.value = it.message ?: uiStrings().googleSignInDeveloper }
            .getOrNull()
    }

    fun signInWithEmail(email: String, password: String) {
        viewModelScope.launch {
            _authBusy.value = true
            _authError.value = null
            runCatching { sessionAuth.login(email, password) }
                .onSuccess { user ->
                    authManager.setSiteUser(user)
                    refreshCloudAccount()
                }
                .onFailure { error ->
                    _authError.value = when (error) {
                        is ProfconqApiException.InvalidCredentials -> uiStrings().profileAdminInvalidCredentials
                        is ProfconqApiException.EmailNotVerified -> error.message
                        else -> error.message ?: uiStrings().googleSignInFailed(-1)
                    }
                }
            _authBusy.value = false
        }
    }

    private suspend fun restoreSiteSession() {
        val sessionUser = runCatching { sessionAuth.fetchSessionUser() }.getOrNull()
        if (sessionUser != null) {
            authManager.setSiteUser(sessionUser)
        } else if (authManager.getIdToken() == null && authManager.authUser.value != null) {
            authManager.setSiteUser(null)
        }
    }

    fun handleGoogleSignInResult(data: Intent?) {
        viewModelScope.launch {
            _authBusy.value = true
            _authError.value = null
            runCatching { authManager.signInWithGoogleResult(data) }
                .onSuccess { refreshCloudAccount() }
                .onFailure { error ->
                    _authError.value = when (error) {
                        is ApiException -> mapGoogleSignInError(error.statusCode)
                        else -> error.message ?: uiStrings().googleSignInFailed(-1)
                    }
                }
            _authBusy.value = false
        }
    }

    fun signOut() {
        viewModelScope.launch {
            _authBusy.value = true
            _authError.value = null
            val remoteFailure = runCatching {
                sessionAuth.logout()
                authManager.signOut()
            }.exceptionOrNull()
            // Local teardown runs whatever the network said, and in its own steps: an account the
            // site refused to release must never stay behind on the device.
            authManager.setSiteUser(null)
            _cloudAccount.value = null
            wordLimitProjectionState.clear()
            _syncMessage.value = null
            _wordLimitMessage.value = null
            _promoMessage.value = null
            runCatching {
                // The entitlement mirror belongs to the sync coordinator, not to this screen.
                dictionarySyncService.onSignedOut()
                onSignOutCleanup()
            }
            if (remoteFailure != null) _authError.value = uiStrings().signOutFailed
            _authBusy.value = false
        }
    }

    fun clearAuthError() {
        _authError.value = null
    }

    fun clearSyncMessage() {
        _syncMessage.value = null
    }

    fun clearWordLimitMessage() {
        _wordLimitMessage.value = null
    }

    fun clearPromoMessage() {
        _promoMessage.value = null
    }

    fun clearAdminError() {
        _adminError.value = null
    }

    fun signInAdmin(username: String, password: String) {
        viewModelScope.launch {
            _adminBusy.value = true
            _adminError.value = null
            adminSession.login(username, password)
                .onSuccess { name -> _adminUsername.value = name }
                .onFailure {
                    _adminError.value = when (it.message) {
                        "invalid_credentials" -> uiStrings().profileAdminInvalidCredentials
                        else -> uiStrings().profileAdminErrorGeneric
                    }
                }
            _adminBusy.value = false
        }
    }

    fun signOutAdmin() {
        viewModelScope.launch {
            _adminBusy.value = true
            adminSession.logout()
            _adminUsername.value = null
            _adminBusy.value = false
        }
    }

    fun redeemPromoCode(code: String) {
        if (authManager.authUser.value == null) {
            _promoMessage.value = uiStrings().promoSignInFirst
            return
        }
        val trimmed = code.trim()
        if (trimmed.isEmpty()) {
            _promoMessage.value = uiStrings().promoEnterCode
            return
        }
        viewModelScope.launch {
            _promoBusy.value = true
            _promoMessage.value = null
            runCatching { dictionarySyncService.redeemPromoCode(authUser.value, trimmed) }
                .onSuccess { account ->
                    publishCloudAccount(account)
                    _wordLimitMessage.value = null
                    _promoMessage.value = uiStrings().promoSuccess(account.wordLimit)
                }
                .onFailure { error ->
                    _promoMessage.value = mapPromoError(error)
                }
            _promoBusy.value = false
        }
    }

    private fun mapPromoError(error: Throwable): String {
        val s = uiStrings()
        return when (error) {
            is ProfconqApiException.Unauthorized -> error.message ?: s.syncSessionExpired
            is ProfconqApiException.PromoInvalid -> s.promoInvalid
            is ProfconqApiException.PromoAlreadyRedeemed -> s.promoAlreadyRedeemed
            else -> error.message ?: s.promoErrorGeneric
        }
    }

    private fun uiStrings(): UiStrings =
        UiStrings.forLanguage(AppLanguage.fromStorage(settings.value.uiLanguage))

    private fun reportWordLimit(error: Throwable) {
        if (error is WordLimitReachedException) {
            _wordLimitMessage.value = uiStrings().wordLimitMessage(error.count, error.limit)
        }
    }

    private fun formatMirrorSyncMessage(result: MirrorSyncResult): String {
        val s = uiStrings()
        result.refusedServerWordCount?.let { serverCount ->
            return s.syncMirrorRefusedShrink(result.wordCount, serverCount)
        }
        result.merge?.let { merge ->
            return s.syncMergeSuccess(
                added = merge.addedToApp,
                updated = merge.updatedInApp,
                sent = merge.sentToSite,
                total = result.wordCount,
            )
        }
        return when (result.primary) {
            SyncPrimary.APP -> s.syncMirrorSuccessApp(result.wordCount, result.programCount)
            SyncPrimary.SITE -> s.syncMirrorSuccessSite(result.wordCount, result.programCount)
        }
    }

    /** One account read at a time, so a re-composed screen cannot stack reads beside it. */
    private val cloudAccountRead = AccountReadGuard()

    /** Publishes the profile line, which agrees with the entitlement this device applied. */
    private suspend fun publishCloudAccount(account: AccountInfo) {
        val profile = profileOf(dictionarySyncService.isEntitledPremium(), account)
        _cloudAccount.value = profile
        wordLimitProjectionState.publish(Result.success(profile))
    }

    private fun refreshCloudAccount() {
        if (authManager.authUser.value == null) return
        if (!cloudAccountRead.begin()) return
        viewModelScope.launch {
            val result = runCatching { dictionarySyncService.refreshAccount(authUser.value) }
            cloudAccountRead.end()
            // A refused or unreachable read said nothing about this account, so it publishes
            // nothing: writing a fabricated free profile here recomposed the screen, which read the
            // account again, and one expired session became a storm of refused requests.
            val account = result.getOrNull() ?: run {
                wordLimitProjectionState.publish(result)
                return@launch
            }
            publishCloudAccount(account)
        }
    }

    fun setSyncPrimary(primary: SyncPrimary) {
        _syncPrimary.value = primary
        viewModelScope.launch { dictionarySyncService.setSyncPrimary(primary) }
    }

    fun setSyncMerge(merge: Boolean) {
        _syncMerge.value = merge
        viewModelScope.launch { dictionarySyncService.setSyncMerge(merge) }
    }

    fun runCloudMirrorSync() {
        if (authManager.authUser.value == null) {
            _syncMessage.value = uiStrings().syncSignInFirst
            return
        }
        if (_accountSwitchPending.value) return
        val primary = _syncPrimary.value
        val merge = _syncMerge.value
        viewModelScope.launch {
            _syncBusy.value = true
            _syncMessage.value = null
            runCatching { dictionarySyncService.mirrorSync(authUser.value, primary, merge) }
                .onSuccess { result ->
                    publishCloudAccount(result.account)
                    _wordLimitMessage.value = null
                    _syncMessage.value = formatMirrorSyncMessage(result)
                }
                .onFailure { error ->
                    _syncMessage.value = mapSyncError(error)
                }
            _syncBusy.value = false
        }
    }

    private fun mapSyncError(error: Throwable): String {
        val s = uiStrings()
        return when (error) {
            is ProfconqApiException.Unauthorized -> error.message ?: s.syncSessionExpired
            is ProfconqApiException.WordLimit -> s.syncWordLimit(error.count, error.limit)
            else -> error.message ?: s.syncErrorGeneric
        }
    }

    private fun mapGoogleSignInError(statusCode: Int): String {
        val s = uiStrings()
        return when (statusCode) {
            7 -> s.googleSignInNetwork
            8 -> s.googleSignInInternal
            10 -> s.googleSignInDeveloperWithSha(authManager.currentSigningSha1())
            12500 -> s.googleSignInOAuth
            12501 -> s.googleSignInCancelled
            12502 -> s.googleSignInInProgress
            else -> s.googleSignInFailed(statusCode)
        }
    }
}

class MainViewModelFactory(
    private val repository: ProfconqRepository,
    private val authManager: FirebaseAuthManager,
    private val dictionarySyncService: DictionarySyncService,
    private val adminSession: ProfconqAdminSession,
    private val sessionAuth: ProfconqSessionAuth,
    private val onSignOutCleanup: () -> Unit = {},
    private val onAccountDataWiped: () -> Unit = {},
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            return MainViewModel(
                repository,
                authManager,
                dictionarySyncService,
                adminSession,
                sessionAuth,
                onSignOutCleanup,
                onAccountDataWiped,
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
