package com.profconq.app.media

import org.json.JSONObject

object AudioLabelsCodec {
    fun encode(labels: Map<String, String>): String? {
        val cleaned = labels
            .mapKeys { it.key.trim() }
            .mapValues { it.value.trim() }
            .filter { (path, label) -> path.isNotEmpty() && label.isNotEmpty() }
        if (cleaned.isEmpty()) return null
        return JSONObject(cleaned as Map<*, *>).toString()
    }

    fun decode(json: String?): Map<String, String> {
        if (json.isNullOrBlank()) return emptyMap()
        return runCatching {
            val obj = JSONObject(json)
            buildMap {
                obj.keys().forEach { key ->
                    val trimmedKey = key.trim()
                    val value = obj.optString(key).trim()
                    if (trimmedKey.isNotEmpty() && value.isNotEmpty()) {
                        put(trimmedKey, value)
                    }
                }
            }
        }.getOrElse { emptyMap() }
    }

    fun migrate(labels: Map<String, String>, replacements: Map<String, String>): Map<String, String> {
        if (replacements.isEmpty()) return labels
        val result = labels.filterKeys { it !in replacements.keys }.toMutableMap()
        replacements.forEach { (oldPath, newPath) ->
            labels[oldPath]?.let { result[newPath] = it }
        }
        return result
    }

    fun retainOnly(labels: Map<String, String>, paths: Collection<String>): Map<String, String> {
        val keep = paths.toSet()
        return labels.filterKeys { it in keep }
    }
}
