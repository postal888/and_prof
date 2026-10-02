package com.profconq.app

import android.app.Application
import com.profconq.app.analytics.ProfconqAnalytics
import com.profconq.app.api.DictionarySyncService
import com.profconq.app.api.ProfconqApiClient
import com.profconq.app.api.ProfconqSessionAuth
import com.profconq.app.auth.FirebaseAuthManager
import com.profconq.app.data.local.ProfconqDatabase
import com.profconq.app.data.local.seedIfEmpty
import com.profconq.app.data.repository.ProfconqRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ProfconqApplication : Application() {
    private val appScope = CoroutineScope(SupervisorJob())

    val database: ProfconqDatabase by lazy { ProfconqDatabase.create(this) }

    val repository: ProfconqRepository by lazy {
        ProfconqRepository(
            collectionDao = database.collectionDao(),
            studySetDao = database.studySetDao(),
            reviewStateDao = database.reviewStateDao(),
            dictionaryDao = database.dictionaryDao(),
            appStateDao = database.appStateDao(),
            readerBookDao = database.readerBookDao(),
            dailyActivityDao = database.dailyActivityDao(),
            youtubeWatchHistoryDao = database.youtubeWatchHistoryDao(),
        )
    }

    val authManager: FirebaseAuthManager by lazy { FirebaseAuthManager(this) }

    val profconqSessionAuth: ProfconqSessionAuth by lazy {
        ProfconqSessionAuth(
            context = this,
            authTokenProvider = { forceRefresh -> authManager.getIdToken(forceRefresh) },
        )
    }

    val profconqApiClient: ProfconqApiClient by lazy {
        ProfconqApiClient(
            authTokenProvider = { forceRefresh -> authManager.getIdToken(forceRefresh) },
            sessionAuth = profconqSessionAuth,
        )
    }

    val dictionarySyncService: DictionarySyncService by lazy {
        DictionarySyncService(repository, profconqApiClient, billingAccounts)
    }

    /** Which account the entitlement state belongs to. In memory only, never a Play account id. */
    val billingAccounts: com.profconq.app.billing.BillingAccountCoordinator by lazy {
        com.profconq.app.billing.BillingAccountCoordinator()
    }

    /** Client-side cap on outbound `/verify` calls; the server rate-limits independently. */
    val verifyThrottle: com.profconq.app.billing.VerifyThrottle by lazy {
        com.profconq.app.billing.VerifyThrottle(
            accounts = billingAccounts,
            clock = { System.currentTimeMillis() },
            delay = { millis -> kotlinx.coroutines.delay(millis) },
        )
    }

    /** Billing singletons: one BillingClient and one verify pipeline for the whole process. */
    val profconqBillingApi: com.profconq.app.api.BillingApi by lazy {
        com.profconq.app.api.ProfconqBillingApi(
            authTokenProvider = { forceRefresh -> authManager.getIdToken(forceRefresh) },
            sessionAuth = profconqSessionAuth,
        )
    }

    val billingRepository: com.profconq.app.billing.BillingRepository by lazy {
        com.profconq.app.billing.BillingRepository(this, appScope)
    }

    val purchaseVerifier: com.profconq.app.billing.PurchaseVerifier by lazy {
        com.profconq.app.billing.PurchaseVerifier(
            feed = billingRepository,
            api = profconqBillingApi,
            sink = dictionarySyncService,
            accounts = billingAccounts,
            throttle = verifyThrottle,
            isLoggedIn = { authManager.authUser.value != null },
            scope = appScope,
        )
    }

    val studioStore: com.profconq.app.studio.StudioStore by lazy {
        com.profconq.app.studio.StudioStore(this)
    }

    val studyIntensityStore: com.profconq.app.study.StudyIntensityStore by lazy {
        com.profconq.app.study.StudyIntensityStore(this)
    }

    val vocabTestLampTracker: com.profconq.app.study.VocabTestLampTracker by lazy {
        com.profconq.app.study.VocabTestLampTracker(this, repository)
    }

    val studyTestResultStore: com.profconq.app.study.StudyTestResultStore by lazy {
        com.profconq.app.study.StudyTestResultStore(this, studyIntensityStore) { day ->
            appScope.launch(Dispatchers.IO) { vocabTestLampTracker.onTestDay(day) }
        }
    }

    val studioSyncService: com.profconq.app.studio.StudioSyncService by lazy {
        com.profconq.app.studio.StudioSyncService(
            apiClient = profconqApiClient,
            repository = repository,
            store = studioStore,
        )
    }

    @Volatile
    var uiLanguage: com.profconq.app.ui.i18n.AppLanguage =
        com.profconq.app.ui.i18n.AppLanguage.DEFAULT
        private set

    fun setUiLanguage(language: com.profconq.app.ui.i18n.AppLanguage) {
        uiLanguage = language
        ProfconqAnalytics.setUiLanguage(language.storageCode)
    }

    fun uiStrings(): com.profconq.app.ui.i18n.UiStrings =
        com.profconq.app.ui.i18n.UiStrings.forLanguage(uiLanguage)

    override fun onCreate() {
        super.onCreate()
        instance = this
        // Внутри debugSmokeTest() стоит проверка BuildConfig.DEBUG, поэтому в release вызов пустой.
        ProfconqAnalytics.init(this)
        ProfconqAnalytics.setUiLanguage(uiLanguage.storageCode)
        ProfconqAnalytics.debugSmokeTest()
        com.profconq.app.studio.StudioNowPlayingHub.init(this)
        com.profconq.app.youtube.YouTubeNowPlayingHub.init(this)
        database.seedIfEmpty(appScope)
    }

    companion object {
        @Volatile
        private var instance: ProfconqApplication? = null

        fun uiStrings(): com.profconq.app.ui.i18n.UiStrings =
            instance?.uiStrings()
                ?: com.profconq.app.ui.i18n.UiStrings.forLanguage(
                    com.profconq.app.ui.i18n.AppLanguage.DEFAULT,
                )
    }
}
