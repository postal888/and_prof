package com.profconq.app.media

import org.json.JSONArray

object AudioPathsCodec {
    fun encode(paths: List<String>): String? {
        val cleaned = paths.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        if (cleaned.isEmpty()) return null
        return JSONArray(cleaned).toString()
    }

    fun decode(json: String?): List<String> {
        if (json.isNullOrBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(json)
            List(array.length()) { i -> array.optString(i).trim() }.filter { it.isNotEmpty() }
        }.getOrElse { emptyList() }
    }

    fun merge(legacyPath: String?, json: String?): List<String> {
        val fromJson = decode(json)
        if (fromJson.isNotEmpty()) return fromJson
        return legacyPath?.takeIf { it.isNotBlank() }?.let { listOf(it) } ?: emptyList()
    }
}
