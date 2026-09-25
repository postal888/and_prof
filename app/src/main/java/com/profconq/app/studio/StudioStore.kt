package com.profconq.app.studio

import android.content.Context
import org.json.JSONObject

class StudioStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun loadCollections(): List<StudioCollection> =
        parseStudioCollectionsJson(prefs.getString(KEY_COLLECTIONS, null))

    fun saveCollections(collections: List<StudioCollection>) {
        prefs.edit()
            .putString(KEY_COLLECTIONS, studioCollectionsToJson(collections))
            .apply()
    }

    fun activeCollectionId(): String? =
        prefs.getString(KEY_ACTIVE, null)?.takeIf { it.isNotBlank() }

    fun setActiveCollectionId(id: String?) {
        prefs.edit().putString(KEY_ACTIVE, id).apply()
    }

    fun loadPrefs(): StudioPrefs {
        val speed = prefs.getFloat(KEY_SPEED, 1f)
        val pauseAfter = prefs.getFloat(KEY_PAUSE_AFTER, 1f)
        val pauseBetween = prefs.getFloat(KEY_PAUSE_BETWEEN, 1f)
        return StudioPrefs(
            dirPtToRu = prefs.getBoolean(KEY_DIR_PT_RU, true),
            voice = StudioVoice.fromKey(prefs.getString(KEY_VOICE, StudioVoice.Antoni.key)).key,
            speed = speed,
            speedNum = prefs.getFloat(KEY_SPEED_NUM, speed),
            speedWord = prefs.getFloat(KEY_SPEED_WORD, speed),
            speedTr = prefs.getFloat(KEY_SPEED_TR, speed),
            speedEx = prefs.getFloat(KEY_SPEED_EX, speed),
            speedExRu = prefs.getFloat(KEY_SPEED_EX_RU, speed),
            pauseAfterSec = pauseAfter,
            pauseBetweenSec = pauseBetween,
            pauseNumSec = prefs.getFloat(KEY_PAUSE_NUM, pauseBetween),
            pauseWordSec = prefs.getFloat(KEY_PAUSE_WORD, pauseAfter),
            pauseTrSec = prefs.getFloat(KEY_PAUSE_TR, pauseBetween),
            pauseExSec = prefs.getFloat(KEY_PAUSE_EX, pauseBetween),
            pauseExRuSec = prefs.getFloat(KEY_PAUSE_EX_RU, pauseBetween),
            pauseBetweenCardsSec = prefs.getFloat(KEY_PAUSE_CARDS, 1f),
            speakWord = prefs.getBoolean(KEY_SPEAK_WORD, true),
            speakTr = prefs.getBoolean(KEY_SPEAK_TR, true),
            speakEx = prefs.getBoolean(KEY_SPEAK_EX, true),
            speakExRu = prefs.getBoolean(KEY_SPEAK_EX_RU, true),
            speakNum = prefs.getBoolean(KEY_SPEAK_NUM, true),
            loop = prefs.getBoolean(KEY_LOOP, false),
            shuffle = prefs.getBoolean(KEY_SHUFFLE, false),
        )
    }

    fun isSetupCollapsed(): Boolean = prefs.getBoolean(KEY_SETUP_COLLAPSED, true)

    fun setSetupCollapsed(collapsed: Boolean) {
        prefs.edit().putBoolean(KEY_SETUP_COLLAPSED, collapsed).apply()
    }

    fun savePrefs(p: StudioPrefs) {
        prefs.edit()
            .putBoolean(KEY_DIR_PT_RU, p.dirPtToRu)
            .putString(KEY_VOICE, StudioVoice.fromKey(p.voice).key)
            .putFloat(KEY_SPEED, p.speedWord)
            .putFloat(KEY_SPEED_NUM, p.speedNum)
            .putFloat(KEY_SPEED_WORD, p.speedWord)
            .putFloat(KEY_SPEED_TR, p.speedTr)
            .putFloat(KEY_SPEED_EX, p.speedEx)
            .putFloat(KEY_SPEED_EX_RU, p.speedExRu)
            .putFloat(KEY_PAUSE_AFTER, p.pauseWordSec)
            .putFloat(KEY_PAUSE_BETWEEN, p.pauseTrSec)
            .putFloat(KEY_PAUSE_NUM, p.pauseNumSec)
            .putFloat(KEY_PAUSE_WORD, p.pauseWordSec)
            .putFloat(KEY_PAUSE_TR, p.pauseTrSec)
            .putFloat(KEY_PAUSE_EX, p.pauseExSec)
            .putFloat(KEY_PAUSE_EX_RU, p.pauseExRuSec)
            .putFloat(KEY_PAUSE_CARDS, p.pauseBetweenCardsSec)
            .putBoolean(KEY_SPEAK_WORD, p.speakWord)
            .putBoolean(KEY_SPEAK_TR, p.speakTr)
            .putBoolean(KEY_SPEAK_EX, p.speakEx)
            .putBoolean(KEY_SPEAK_EX_RU, p.speakExRu)
            .putBoolean(KEY_SPEAK_NUM, p.speakNum)
            .putBoolean(KEY_LOOP, p.loop)
            .putBoolean(KEY_SHUFFLE, p.shuffle)
            .apply()
    }

    /** normalized PT → website vocab id */
    fun saveWebIdMap(map: Map<String, Int>) {
        val obj = JSONObject()
        map.forEach { (k, v) -> obj.put(k, v) }
        prefs.edit().putString(KEY_WEB_ID_MAP, obj.toString()).apply()
    }

    fun loadWebIdMap(): Map<String, Int> {
        val raw = prefs.getString(KEY_WEB_ID_MAP, null) ?: return emptyMap()
        return runCatching {
            val obj = JSONObject(raw)
            buildMap {
                val keys = obj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    put(k, obj.optInt(k))
                }
            }
        }.getOrDefault(emptyMap())
    }

    companion object {
        private const val PREFS = "profconq_studio"
        private const val KEY_COLLECTIONS = "collections_v1"
        private const val KEY_ACTIVE = "active_collection_v1"
        private const val KEY_WEB_ID_MAP = "web_id_map_v1"
        private const val KEY_DIR_PT_RU = "dir_pt_ru"
        private const val KEY_VOICE = "voice"
        private const val KEY_SPEED = "speed"
        private const val KEY_SPEED_NUM = "speed_num"
        private const val KEY_SPEED_WORD = "speed_word"
        private const val KEY_SPEED_TR = "speed_tr"
        private const val KEY_SPEED_EX = "speed_ex"
        private const val KEY_SPEED_EX_RU = "speed_ex_ru"
        private const val KEY_PAUSE_AFTER = "pause_after"
        private const val KEY_PAUSE_BETWEEN = "pause_between"
        private const val KEY_PAUSE_NUM = "pause_num"
        private const val KEY_PAUSE_WORD = "pause_word"
        private const val KEY_PAUSE_TR = "pause_tr"
        private const val KEY_PAUSE_EX = "pause_ex"
        private const val KEY_PAUSE_EX_RU = "pause_ex_ru"
        private const val KEY_PAUSE_CARDS = "pause_cards"
        private const val KEY_SPEAK_WORD = "speak_word"
        private const val KEY_SPEAK_TR = "speak_tr"
        private const val KEY_SPEAK_EX = "speak_ex"
        private const val KEY_SPEAK_EX_RU = "speak_ex_ru"
        private const val KEY_SPEAK_NUM = "speak_num"
        private const val KEY_LOOP = "loop"
        private const val KEY_SHUFFLE = "shuffle"
        private const val KEY_SETUP_COLLAPSED = "setup_collapsed"
    }
}
