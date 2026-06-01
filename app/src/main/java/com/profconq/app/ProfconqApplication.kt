package com.profconq.app

import android.app.Application
import com.profconq.app.api.DictionarySyncService
import com.profconq.app.api.ProfconqApiClient
import com.profconq.app.api.ProfconqSessionAuth
import com.profconq.app.auth.FirebaseAuthManager
import com.profconq.app.data.local.ProfconqDatabase
import com.profconq.app.data.local.seedIfEmpty
import com.profconq.app.data.repository.ProfconqRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

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
        DictionarySyncService(repository, profconqApiClient)
    }

    override fun onCreate() {
        super.onCreate()
        database.seedIfEmpty(appScope)
    }
}
