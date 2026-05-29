package com.proficon.app

import android.app.Application
import com.proficon.app.data.local.ProficonDatabase
import com.proficon.app.data.local.seedIfEmpty
import com.proficon.app.data.repository.ProficonRepository
import com.proficon.app.reader.ReaderImportParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ProficonApplication : Application() {
    companion object {
        private const val PREINSTALLED_CAPITAES_ID = "book-capitaes-de-areia"
        private const val PREINSTALLED_CAPITAES_TITLE = "Capitaes de Areia"
        private const val PREINSTALLED_CAPITAES_ASSET = "books/capitaes_de_areia.docx"
    }

    private val appScope = CoroutineScope(SupervisorJob())

    val database: ProficonDatabase by lazy { ProficonDatabase.create(this) }

    val repository: ProficonRepository by lazy {
        ProficonRepository(
            collectionDao = database.collectionDao(),
            dictionaryDao = database.dictionaryDao(),
            appStateDao = database.appStateDao(),
            readerBookDao = database.readerBookDao(),
            dailyActivityDao = database.dailyActivityDao(),
            youtubeWatchHistoryDao = database.youtubeWatchHistoryDao(),
        )
    }

    override fun onCreate() {
        super.onCreate()
        database.seedIfEmpty(appScope)
        appScope.launch(Dispatchers.IO) {
            repository.ensureDemoReaderBook()
            runCatching {
                assets.open(PREINSTALLED_CAPITAES_ASSET).use { stream ->
                    val content = ReaderImportParser.extractDocxText(stream)
                    repository.ensurePreinstalledReaderBook(
                        bookId = PREINSTALLED_CAPITAES_ID,
                        title = PREINSTALLED_CAPITAES_TITLE,
                        content = content,
                        sourceUri = "asset://$PREINSTALLED_CAPITAES_ASSET",
                        forceUpdate = true,
                    )
                }
            }
        }
    }
}
