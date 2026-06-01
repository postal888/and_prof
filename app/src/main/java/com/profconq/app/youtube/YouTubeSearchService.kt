package com.profconq.app.youtube

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class YouTubeSearchService(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build(),
) {
    suspend fun searchVideos(query: String, limit: Int = 40): List<YouTubeVideoResult> =
        withContext(Dispatchers.IO) {
            val trimmed = query.trim()
            if (trimmed.isEmpty()) return@withContext emptyList()

            val body = JSONObject()
                .put(
                    "context",
                    JSONObject().put(
                        "client",
                        JSONObject()
                            .put("clientName", "ANDROID")
                            .put("clientVersion", "20.10.38")
                            .put("hl", "pt")
                            .put("gl", "BR"),
                    ),
                )
                .put("query", trimmed)
                .toString()

            val request = Request.Builder()
                .url("https://www.youtube.com/youtubei/v1/search?prettyPrint=false")
                .post(body.toRequestBody("application/json".toMediaType()))
                .header("User-Agent", ANDROID_UA)
                .header("Content-Type", "application/json")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                throw IllegalStateException("YouTube search failed: HTTP ${response.code}")
            }

            val json = JSONObject(response.body?.string().orEmpty())
            parseResults(json).take(limit).ifEmpty {
                throw IllegalStateException("Ничего не найдено. Попробуйте другой запрос.")
            }
        }

    private fun parseResults(root: JSONObject): List<YouTubeVideoResult> {
        val found = mutableListOf<YouTubeVideoResult>()
        walkJson(root, found)
        return found.distinctBy { it.videoId }
    }

    private fun walkJson(node: Any?, out: MutableList<YouTubeVideoResult>) {
        when (node) {
            is JSONObject -> {
                parseVideoRenderer(node)?.let { out += it }
                node.keys().forEach { key -> walkJson(node.opt(key), out) }
            }
            is JSONArray -> {
                for (i in 0 until node.length()) {
                    walkJson(node.opt(i), out)
                }
            }
        }
    }

    private fun parseVideoRenderer(obj: JSONObject): YouTubeVideoResult? {
        val videoId = obj.optString("videoId")
        if (videoId.length != 11) return null
        if (!obj.has("title")) return null

        val title = readTextNode(obj.optJSONObject("title")) ?: return null
        val channel = readTextNode(obj.optJSONObject("ownerText"))
            ?: readTextNode(obj.optJSONObject("longBylineText"))
            ?: ""

        val overlayStatus = findTimeStatusRenderer(obj)
        val duration = obj.optJSONObject("lengthText")?.optString("simpleText")
            ?: overlayStatus?.optJSONObject("text")?.optString("simpleText")

        val isShort = detectShort(obj, overlayStatus, duration)

        val thumbnailUrl = obj.optJSONObject("thumbnail")
            ?.optJSONArray("thumbnails")
            ?.let { thumbs ->
                (0 until thumbs.length())
                    .mapNotNull { i -> thumbs.optJSONObject(i)?.optString("url") }
                    .lastOrNull { it.isNotBlank() }
            }

        return YouTubeVideoResult(
            videoId = videoId,
            title = title,
            channel = channel,
            duration = duration,
            thumbnailUrl = thumbnailUrl,
            isShort = isShort,
        )
    }

    private fun findTimeStatusRenderer(obj: JSONObject): JSONObject? {
        obj.optJSONObject("thumbnailOverlayTimeStatusRenderer")?.let { return it }
        val overlays = obj.optJSONArray("thumbnailOverlays") ?: return null
        for (i in 0 until overlays.length()) {
            overlays.optJSONObject(i)
                ?.optJSONObject("thumbnailOverlayTimeStatusRenderer")
                ?.let { return it }
        }
        return null
    }

    private fun detectShort(
        obj: JSONObject,
        overlayStatus: JSONObject?,
        duration: String?,
    ): Boolean {
        if (overlayStatus?.optString("style").equals("SHORTS", ignoreCase = true)) return true
        if (duration.equals("SHORTS", ignoreCase = true)) return true
        if (obj.has("reelWatchEndpoint")) return true

        val nav = obj.optJSONObject("navigationEndpoint")
        if (nav?.has("reelWatchEndpoint") == true) return true
        if (nav?.optJSONObject("commandMetadata")
                ?.optJSONObject("webCommandMetadata")
                ?.optString("url")
                ?.startsWith("/shorts/") == true
        ) {
            return true
        }

        val durationSec = parseDurationSec(duration)
        return durationSec != null && durationSec <= 60
    }

    private fun parseDurationSec(duration: String?): Int? {
        val value = duration?.trim().orEmpty()
        if (value.isEmpty() || value.equals("SHORTS", ignoreCase = true)) return null

        val parts = value.split(":").mapNotNull { it.toIntOrNull() }
        return when (parts.size) {
            1 -> parts[0]
            2 -> parts[0] * 60 + parts[1]
            3 -> parts[0] * 3600 + parts[1] * 60 + parts[2]
            else -> null
        }
    }

    private fun readTextNode(node: JSONObject?): String? {
        if (node == null) return null
        node.optString("simpleText").takeIf { it.isNotBlank() }?.let { return it }
        val runs = node.optJSONArray("runs") ?: return null
        return buildString {
            for (i in 0 until runs.length()) {
                append(runs.optJSONObject(i)?.optString("text").orEmpty())
            }
        }.trim().ifBlank { null }
    }

    companion object {
        private const val ANDROID_UA =
            "com.google.android.youtube/20.10.38 (Linux; U; Android 14)"
    }
}
