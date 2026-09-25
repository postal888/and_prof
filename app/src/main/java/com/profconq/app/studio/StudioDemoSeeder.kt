package com.profconq.app.studio

import android.content.Context
import com.profconq.app.data.repository.ProfconqRepository
import com.profconq.app.data.repository.WordNormalizer
import org.json.JSONObject

object StudioDemoSeeder {
    private const val ASSET = "studio_demo_words.json"
    private const val FLAG_KEY = "demo_seeded_v1"
    const val DEMO_COLLECTION_ID = "col_studio_demo_site"

    /**
     * Seed demo collection from assets if Studio is empty.
     * Force=true rewrites the demo collection (used after install for samples).
     */
    suspend fun ensureSeeded(
        context: Context,
        repository: ProfconqRepository,
        store: StudioStore,
        force: Boolean = false,
    ): Boolean {
        val prefs = context.applicationContext.getSharedPreferences("profconq_studio", Context.MODE_PRIVATE)
        if (!force) {
            if (store.loadCollections().any { it.id == DEMO_COLLECTION_ID && it.wordIds.isNotEmpty() }) {
                return false
            }
            if (prefs.getBoolean(FLAG_KEY, false) && store.loadCollections().isNotEmpty()) {
                return false
            }
        }

        val raw = runCatching {
            context.assets.open(ASSET).bufferedReader(Charsets.UTF_8).use { it.readText() }
        }.getOrNull() ?: return false

        val root = JSONObject(raw)
        val title = root.optString("title").ifBlank { "Studio · demo" }
        val arr = root.optJSONArray("words") ?: return false
        val localIds = mutableListOf<String>()
        for (i in 0 until arr.length()) {
            val w = arr.optJSONObject(i) ?: continue
            val pt = w.optString("word").trim()
            val ru = w.optString("translation").trim()
            if (pt.isEmpty() || ru.isEmpty()) continue
            val example = w.optString("example").trim().takeIf { it.isNotEmpty() }
            val exampleRu = w.optString("exampleRu").trim().takeIf { it.isNotEmpty() }
            val tag = w.optString("tag").trim().ifBlank { "geral" }
            val id = repository.upsertStudioDemoCard(
                pt = pt,
                ru = ru,
                example = example,
                exampleTranslation = exampleRu,
                tag = tag,
            )
            localIds += id
        }
        if (localIds.isEmpty()) return false

        val col = StudioCollection(
            id = DEMO_COLLECTION_ID,
            title = title,
            wordIds = localIds,
            webWordIds = emptyList(),
        )
        val existing = store.loadCollections().filter { it.id != DEMO_COLLECTION_ID }
        store.saveCollections(listOf(col) + existing)
        store.setActiveCollectionId(DEMO_COLLECTION_ID)
        prefs.edit().putBoolean(FLAG_KEY, true).apply()
        return true
    }
}
