package com.profconq.app.api

import org.json.JSONArray
import org.json.JSONObject

/**
 * Vocabulary entry format used by profconq.com (same as the website SPA).
 */
data class WebVocabWord(
    val id: Int,
    val word: String,
    val translation: String,
    val example: String = "",
    val exampleRu: String = "",
    val tag: String = "geral",
    val infinitivo: String = "",
    val img: String = "",
    val interval: Int = 1,
    val easeFactor: Double = 2.5,
    val nextReview: Long = 0L,
    val reps: Int = 0,
    val learnMark: String = "",
    val videoId: String? = null,
    val videoTitle: String? = null,
    /** Epoch millis of the last content change; 0 when the side that wrote it did not record one. */
    val updatedAt: Long = 0L,
) {
    fun toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("word", word)
        .put("translation", translation)
        .put("example", example)
        .put("exampleRu", exampleRu)
        .put("tag", tag)
        .put("infinitivo", infinitivo)
        .put("img", img)
        .put("interval", interval)
        .put("easeFactor", easeFactor)
        .put("nextReview", nextReview)
        .put("reps", reps)
        .put("learnMark", learnMark)
        .put("videoId", videoId)
        .put("videoTitle", videoTitle)
        .apply { if (updatedAt > 0) put("updatedAt", updatedAt) }

    companion object {
        fun fromJson(json: JSONObject): WebVocabWord? {
            val word = json.optString("word").trim()
            val translation = json.optString("translation").trim()
            if (word.isEmpty() || translation.isEmpty()) return null
            return WebVocabWord(
                id = json.optInt("id", 0),
                word = word,
                translation = translation,
                example = json.optString("example", ""),
                exampleRu = json.optString("exampleRu").ifBlank {
                    json.optString("exampleTranslation").ifBlank {
                        json.optString("example_ru", "")
                    }
                },
                tag = json.optString("tag", "geral"),
                infinitivo = json.optString("infinitivo", ""),
                img = json.optString("img", ""),
                interval = json.optInt("interval", 1),
                easeFactor = json.optDouble("easeFactor", 2.5),
                nextReview = json.optLong("nextReview", 0L),
                reps = json.optInt("reps", 0),
                learnMark = json.optString("learnMark", ""),
                videoId = json.optString("videoId").takeIf { it.isNotBlank() },
                videoTitle = json.optString("videoTitle").takeIf { it.isNotBlank() },
                updatedAt = json.optLong("updatedAt", 0L).takeIf { it > 0 }
                    ?: json.optLong("updated_at", 0L),
            )
        }

        fun parseWordsArray(array: JSONArray?): List<WebVocabWord> {
            if (array == null) return emptyList()
            return buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    fromJson(item)?.let(::add)
                }
            }
        }
    }
}
