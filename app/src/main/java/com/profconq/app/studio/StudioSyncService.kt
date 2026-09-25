package com.profconq.app.studio

import com.profconq.app.api.ProfconqApiClient
import com.profconq.app.data.model.WordCard
import com.profconq.app.data.repository.ProfconqRepository
import com.profconq.app.data.repository.WordNormalizer
import org.json.JSONArray
import org.json.JSONObject

class StudioSyncService(
    private val apiClient: ProfconqApiClient,
    private val repository: ProfconqRepository,
    private val store: StudioStore,
) {
    /**
     * Pull Studio collections from `/api/sync/settings` and resolve web word ids
     * against local vocabulary cards.
     */
    suspend fun pullFromCloud(): List<StudioCollection> {
        val settings = apiClient.pullSyncJson("settings")
        val settingsObj = settings as? JSONObject ?: JSONObject()
        val collectionsRaw = settingsObj.opt("proconq_vt_collections_v1")
        val collectionsJson = when (collectionsRaw) {
            is String -> collectionsRaw
            is JSONArray -> collectionsRaw.toString()
            is JSONObject -> collectionsRaw.toString()
            null -> "[]"
            else -> collectionsRaw.toString()
        }
        val active = settingsObj.optString("proconq_vt_activeCollection_v1").takeIf { it.isNotBlank() }

        // Refresh web-id map from live vocabulary (stable website ids).
        val webWords = runCatching { apiClient.pullVocabulary() }.getOrElse {
            repository.buildWebVocabularyWords()
        }
        val webIdToPt = webWords.associate { it.id to WordNormalizer.normalize(it.word) }
        val ptToWebId = webWords.associate { WordNormalizer.normalize(it.word) to it.id }
        store.saveWebIdMap(ptToWebId)

        val parsed = parseStudioCollectionsJson(collectionsJson)
        val resolved = parsed.map { col ->
            val localIds = resolveLocalIds(col, webIdToPt)
            col.copy(wordIds = localIds)
        }
        store.saveCollections(resolved)
        if (active != null) store.setActiveCollectionId(active)
        else if (resolved.isNotEmpty()) store.setActiveCollectionId(resolved.first().id)

        // Prefs from settings (optional)
        applyPrefsFromSettings(settingsObj)
        return resolved
    }

    suspend fun pushToCloud(collections: List<StudioCollection> = store.loadCollections()) {
        val withWebIds = collections.map { enrichWebIds(it) }
        store.saveCollections(withWebIds)

        val existing = runCatching { apiClient.pullSyncJson("settings") }.getOrNull()
        val settingsObj = (existing as? JSONObject)?.let { JSONObject(it.toString()) } ?: JSONObject()
        settingsObj.put("proconq_vt_collections_v1", studioCollectionsToJson(withWebIds))
        store.activeCollectionId()?.let {
            settingsObj.put("proconq_vt_activeCollection_v1", it)
        }
        val prefs = store.loadPrefs()
        settingsObj.put("proconq_vt_dir", if (prefs.dirPtToRu) "pt-ru" else "ru-pt")
        settingsObj.put("proconq_vt_voice", StudioVoice.fromKey(prefs.voice).key)
        settingsObj.put("proconq_vt_loop", if (prefs.loop) "1" else "0")
        settingsObj.put("proconq_vt_shuffle", if (prefs.shuffle) "1" else "0")
        settingsObj.put("proconq_vt_pauseAfter", prefs.pauseAfterSec.toString())
        settingsObj.put("proconq_vt_pauseBetween", prefs.pauseBetweenSec.toString())
        settingsObj.put("proconq_vt_pauseBetweenCards", prefs.pauseBetweenCardsSec.toString())
        apiClient.pushSyncJson("settings", settingsObj)
    }

    suspend fun resolveCards(collection: StudioCollection): List<WordCard> {
        val ids = if (collection.wordIds.isNotEmpty()) {
            collection.wordIds
        } else {
            val webIdToPt = store.loadWebIdMap().entries.associate { it.value to it.key }
                .ifEmpty {
                    repository.buildWebVocabularyWords()
                        .associate { it.id to WordNormalizer.normalize(it.word) }
                }
            resolveLocalIds(collection, webIdToPt)
        }
        return repository.getCardsByIds(ids)
    }

    fun createLocalCollection(title: String, wordIds: List<String> = emptyList()): StudioCollection {
        val col = StudioCollection(
            id = "col_${System.currentTimeMillis().toString(36)}",
            title = title.ifBlank { "Studio" },
            wordIds = wordIds.distinct(),
            webWordIds = emptyList(),
        )
        val all = store.loadCollections().toMutableList()
        all += col
        store.saveCollections(all)
        store.setActiveCollectionId(col.id)
        return col
    }

    fun renameCollection(id: String, title: String): StudioCollection? {
        val next = store.loadCollections().map {
            if (it.id == id) it.copy(title = title.ifBlank { it.title }) else it
        }
        store.saveCollections(next)
        return next.find { it.id == id }
    }

    fun deleteCollection(id: String): List<StudioCollection> {
        val next = store.loadCollections().filter { it.id != id }
        store.saveCollections(next)
        if (store.activeCollectionId() == id) {
            store.setActiveCollectionId(next.firstOrNull()?.id)
        }
        return next
    }

    fun duplicateCollection(id: String): StudioCollection? {
        val src = store.loadCollections().find { it.id == id } ?: return null
        return createLocalCollection("${src.title} · 2", src.wordIds).copy(webWordIds = src.webWordIds).also { copy ->
            val all = store.loadCollections().map { if (it.id == copy.id) copy else it }
            store.saveCollections(all)
        }
    }

    fun updateCollectionWords(id: String, wordIds: List<String>): StudioCollection? {
        val next = store.loadCollections().map {
            if (it.id == id) it.copy(wordIds = wordIds.distinct(), webWordIds = emptyList()) else it
        }
        store.saveCollections(next)
        return next.find { it.id == id }
    }

    fun setCollectionRender(id: String, render: StudioRenderMeta?): StudioCollection? {
        val next = store.loadCollections().map {
            if (it.id == id) it.copy(render = render) else it
        }
        store.saveCollections(next)
        return next.find { it.id == id }
    }

    private suspend fun enrichWebIds(col: StudioCollection): StudioCollection {
        if (col.webWordIds.isNotEmpty() && col.webWordIds.size == col.wordIds.size) return col
        val map = store.loadWebIdMap().ifEmpty {
            repository.buildWebVocabularyWords()
                .associate { WordNormalizer.normalize(it.word) to it.id }
                .also { store.saveWebIdMap(it) }
        }
        val cards = repository.getCardsByIds(col.wordIds)
        val webIds = cards.mapNotNull { map[WordNormalizer.normalize(it.pt)] }
        return col.copy(webWordIds = webIds)
    }

    private suspend fun resolveLocalIds(
        col: StudioCollection,
        webIdToPt: Map<Int, String>,
    ): List<String> {
        if (col.wordIds.isNotEmpty() && col.webWordIds.isEmpty()) return col.wordIds
        val out = mutableListOf<String>()
        val seen = mutableSetOf<String>()
        for (webId in col.webWordIds) {
            val pt = webIdToPt[webId] ?: continue
            val localId = repository.findCardIdByNormalizedPtForStudio(pt) ?: continue
            if (seen.add(localId)) out += localId
        }
        if (out.isEmpty() && col.wordIds.isNotEmpty()) return col.wordIds
        return out
    }

    private fun applyPrefsFromSettings(settings: JSONObject) {
        val current = store.loadPrefs()
        val dir = settings.optString("proconq_vt_dir")
        val updated = current.copy(
            dirPtToRu = when (dir) {
                "ru-pt" -> false
                "pt-ru" -> true
                else -> current.dirPtToRu
            },
            loop = settings.optString("proconq_vt_loop").let {
                if (it.isBlank()) current.loop else it == "1" || it.equals("true", true)
            },
            shuffle = settings.optString("proconq_vt_shuffle").let {
                if (it.isBlank()) current.shuffle else it == "1" || it.equals("true", true)
            },
            pauseAfterSec = settings.optString("proconq_vt_pauseAfter").toFloatOrNull()
                ?: current.pauseAfterSec,
            pauseBetweenSec = settings.optString("proconq_vt_pauseBetween").toFloatOrNull()
                ?: current.pauseBetweenSec,
            pauseWordSec = settings.optString("proconq_vt_pauseAfter").toFloatOrNull()
                ?: current.pauseWordSec,
            pauseTrSec = settings.optString("proconq_vt_pauseBetween").toFloatOrNull()
                ?: current.pauseTrSec,
            pauseNumSec = current.pauseNumSec,
            pauseExSec = current.pauseExSec,
            pauseExRuSec = current.pauseExRuSec,
            pauseBetweenCardsSec = settings.optString("proconq_vt_pauseBetweenCards").toFloatOrNull()
                ?: current.pauseBetweenCardsSec,
            speed = settings.optString("proconq_vt_speed").toFloatOrNull() ?: current.speed,
            voice = settings.optString("proconq_vt_voice").let { raw ->
                if (raw.isBlank()) current.voice else StudioVoice.fromKey(raw).key
            },
            speakNum = speakFlag(settings, "num", current.speakNum),
            speakWord = speakFlag(settings, "word", current.speakWord),
            speakTr = speakFlag(settings, "tr", current.speakTr),
            speakEx = speakFlag(settings, "ex", current.speakEx),
            speakExRu = speakFlag(settings, "exRu", current.speakExRu),
        )
        store.savePrefs(updated)
    }

    private fun speakFlag(settings: JSONObject, key: String, fallback: Boolean): Boolean {
        val raw = settings.opt("proconq_vt_speakOpts") ?: return fallback
        val obj = when (raw) {
            is JSONObject -> raw
            is String -> runCatching { JSONObject(raw) }.getOrNull()
            else -> null
        } ?: return fallback
        return if (obj.has(key)) obj.optBoolean(key, fallback) else fallback
    }
}
