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
import java.util.regex.Pattern

class YouTubeTranscriptFetcher(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .build(),
) {
    suspend fun fetchTranscript(videoId: String, preferredLang: String = "pt"): TranscriptResult =
        withContext(Dispatchers.IO) {
            val langCandidates = listOf(preferredLang, "pt", "pt-BR", "en").distinct()
            var lastError = "Субтитры недоступны."

            for (lang in langCandidates) {
                try {
                    val segments = fetchSegments(videoId, lang)
                    if (segments.isNotEmpty()) {
                        return@withContext TranscriptResult(
                            lines = mergeSegments(segments),
                            language = lang,
                        )
                    }
                } catch (e: Exception) {
                    lastError = e.message ?: lastError
                }
            }
            throw IllegalStateException(lastError)
        }

    private fun fetchSegments(videoId: String, lang: String): List<TranscriptSegment> {
        val tracks = fetchCaptionTracksFromWatchPage(videoId)
            .ifEmpty { fetchCaptionTracksViaInnertube(videoId) }
        val track = pickCaptionTrack(tracks, lang)
            ?: throw IllegalStateException("Нет дорожки субтитров для языка $lang.")
        val videoDurationSec = fetchVideoDurationSec(videoId)
        return downloadCaptionSegments(track.baseUrl, lang, videoDurationSec)
    }

    private fun fetchCaptionTracksViaInnertube(videoId: String): List<CaptionTrack> {
        val apiKey = fetchInnertubeApiKey(videoId)
        val url = if (apiKey.isNullOrBlank()) {
            "https://www.youtube.com/youtubei/v1/player?prettyPrint=false"
        } else {
            "https://www.youtube.com/youtubei/v1/player?key=$apiKey&prettyPrint=false"
        }
        val body = JSONObject()
            .put(
                "context",
                JSONObject().put(
                    "client",
                    JSONObject()
                        .put("clientName", "ANDROID")
                        .put("clientVersion", "20.10.38"),
                ),
            )
            .put("videoId", videoId)
            .toString()

        val request = Request.Builder()
            .url(url)
            .post(body.toRequestBody("application/json".toMediaType()))
            .header("User-Agent", INNERTUBE_ANDROID_UA)
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) return emptyList()
        val json = JSONObject(response.body?.string().orEmpty())
        return parseCaptionTracksJson(json)
    }

    private fun fetchInnertubeApiKey(videoId: String): String? {
        return try {
            val request = Request.Builder()
                .url("https://www.youtube.com/watch?v=$videoId")
                .header("User-Agent", MOBILE_UA)
                .header("Accept-Language", "pt-BR,pt;q=0.9,en;q=0.8")
                .build()
            val html = client.newCall(request).execute().body?.string().orEmpty()
            Regex(""""INNERTUBE_API_KEY"\s*:\s*"([a-zA-Z0-9_-]+)"""")
                .find(html)
                ?.groupValues
                ?.getOrNull(1)
        } catch (_: Exception) {
            null
        }
    }

    private fun fetchCaptionTracksFromWatchPage(videoId: String): List<CaptionTrack> {
        val request = Request.Builder()
            .url("https://www.youtube.com/watch?v=$videoId&hl=pt")
            .header("User-Agent", MOBILE_UA)
            .header("Accept-Language", "pt-BR,pt;q=0.9,en;q=0.8")
            .build()
        val html = client.newCall(request).execute().body?.string().orEmpty()

        val regexMatch = Regex("\"captionTracks\":(\\[[\\s\\S]*?\\])").find(html)
        if (regexMatch != null) {
            try {
                val tracks = parseCaptionTracksArray(JSONArray(regexMatch.groupValues[1]))
                if (tracks.isNotEmpty()) return tracks
            } catch (_: Exception) {
                /* fall through */
            }
        }

        for (marker in listOf("ytInitialPlayerResponse =", "var ytInitialPlayerResponse =")) {
            val jsonRaw = extractJsonObjectAfter(html, marker) ?: continue
            try {
                val tracks = parseCaptionTracksJson(JSONObject(jsonRaw))
                if (tracks.isNotEmpty()) return tracks
            } catch (_: Exception) {
                /* try next marker */
            }
        }
        return emptyList()
    }

    private fun parseCaptionTracksJson(json: JSONObject): List<CaptionTrack> {
        val tracks = json.optJSONObject("captions")
            ?.optJSONObject("playerCaptionsTracklistRenderer")
            ?.optJSONArray("captionTracks")
            ?: return emptyList()
        return parseCaptionTracksArray(tracks)
    }

    private fun parseCaptionTracksArray(array: JSONArray): List<CaptionTrack> {
        val out = mutableListOf<CaptionTrack>()
        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            val baseUrl = item.optString("baseUrl")
            if (baseUrl.isBlank()) continue
            out += CaptionTrack(
                languageCode = item.optString("languageCode"),
                kind = item.optString("kind"),
                baseUrl = baseUrl,
            )
        }
        return out
    }

    private fun pickCaptionTrack(tracks: List<CaptionTrack>, preferredLang: String): CaptionTrack? {
        if (tracks.isEmpty()) return null
        val order = listOf(preferredLang, "pt-BR", "pt", "en")
        for (lang in order) {
            tracks.firstOrNull {
                it.languageCode.equals(lang, ignoreCase = true) && it.baseUrl.isNotBlank()
            }?.let { return it }
        }
        return tracks.firstOrNull { it.kind != "asr" && it.baseUrl.isNotBlank() }
            ?: tracks.firstOrNull { it.baseUrl.isNotBlank() }
    }

    private fun fetchVideoDurationSec(videoId: String): Float? {
        return try {
            val apiKey = fetchInnertubeApiKey(videoId)
            val url = if (apiKey.isNullOrBlank()) {
                "https://www.youtube.com/youtubei/v1/player?prettyPrint=false"
            } else {
                "https://www.youtube.com/youtubei/v1/player?key=$apiKey&prettyPrint=false"
            }
            val body = JSONObject()
                .put(
                    "context",
                    JSONObject().put(
                        "client",
                        JSONObject()
                            .put("clientName", "ANDROID")
                            .put("clientVersion", "20.10.38"),
                    ),
                )
                .put("videoId", videoId)
                .toString()
            val request = Request.Builder()
                .url(url)
                .post(body.toRequestBody("application/json".toMediaType()))
                .header("User-Agent", INNERTUBE_ANDROID_UA)
                .build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return null
            val json = JSONObject(response.body?.string().orEmpty())
            json
                .optJSONObject("videoDetails")
                ?.optString("lengthSeconds")
                ?.toFloatOrNull()
        } catch (_: Exception) {
            null
        }
    }

    private fun downloadCaptionSegments(
        baseUrl: String,
        preferredLang: String,
        videoDurationSec: Float?,
    ): List<TranscriptSegment> {
        var captionUrl = baseUrl
        if (!captionUrl.startsWith("http")) {
            captionUrl = "https://www.youtube.com${if (captionUrl.startsWith("/")) "" else "/"}$captionUrl"
        }
        val sanitizedUrl = sanitizeCaptionBaseUrl(captionUrl)
        val candidates = listOf(
            sanitizedUrl,
            sanitizedUrl.withFmt("vtt"),
            sanitizedUrl.withFmt("json3"),
            sanitizedUrl.withFmt("srv1"),
            sanitizedUrl.withFmt("srv3"),
        ).distinct()

        var bestSegments = emptyList<TranscriptSegment>()
        var lastError = "Пустые субтитры."

        for (url in candidates) {
            try {
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", CAPTION_UA)
                    .header("Accept-Language", preferredLang)
                    .build()
                val text = client.newCall(request).execute().body?.string().orEmpty()
                val segments = parseCaptionPayload(text)
                if (segments.isEmpty()) continue
                if (isBetterTranscript(segments, bestSegments, videoDurationSec)) {
                    bestSegments = segments
                }
            } catch (e: Exception) {
                lastError = e.message ?: lastError
            }
        }

        if (bestSegments.isNotEmpty()) return bestSegments
        throw IllegalStateException(lastError)
    }

    private fun sanitizeCaptionBaseUrl(baseUrl: String): String {
        return baseUrl
            .replace(Regex("[?&]fmt=[^&]+"), "")
            .replace("?&", "?")
            .trimEnd('?', '&')
    }

    private fun isBetterTranscript(
        candidate: List<TranscriptSegment>,
        currentBest: List<TranscriptSegment>,
        videoDurationSec: Float?,
    ): Boolean {
        if (currentBest.isEmpty()) return true

        val candidateComplete = isTranscriptLikelyComplete(candidate, videoDurationSec)
        val bestComplete = isTranscriptLikelyComplete(currentBest, videoDurationSec)
        if (candidateComplete && !bestComplete) return true
        if (!candidateComplete && bestComplete) return false

        val candidateEnd = segmentsEndSec(candidate)
        val bestEnd = segmentsEndSec(currentBest)
        if (candidateEnd > bestEnd + 1f) return true
        if (kotlin.math.abs(candidateEnd - bestEnd) <= 1f && candidate.size > currentBest.size) return true
        return false
    }

    private fun isTranscriptLikelyComplete(
        segments: List<TranscriptSegment>,
        videoDurationSec: Float?,
    ): Boolean {
        if (videoDurationSec == null || videoDurationSec < 120f) return true
        val transcriptEnd = segmentsEndSec(segments)
        return transcriptEnd >= videoDurationSec * 0.8f
    }

    private fun segmentsEndSec(segments: List<TranscriptSegment>): Float {
        return segments.maxOfOrNull { it.offsetSec + it.durationSec } ?: 0f
    }

    private fun extractJsonObjectAfter(html: String, marker: String): String? {
        val startIndex = html.indexOf(marker)
        if (startIndex < 0) return null
        val jsonStart = html.indexOf('{', startIndex + marker.length)
        if (jsonStart < 0) return null
        var depth = 0
        var inString = false
        var escaped = false
        for (i in jsonStart until html.length) {
            val ch = html[i]
            if (inString) {
                if (escaped) {
                    escaped = false
                } else if (ch == '\\') {
                    escaped = true
                } else if (ch == '"') {
                    inString = false
                }
                continue
            }
            when (ch) {
                '"' -> inString = true
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) return html.substring(jsonStart, i + 1)
                }
            }
        }
        return null
    }

    private fun parseCaptionPayload(raw: String): List<TranscriptSegment> {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return emptyList()
        if (trimmed.startsWith("{")) {
            parseJson3(trimmed).takeIf { it.isNotEmpty() }?.let { return it }
        }
        if (trimmed.contains("<p") && trimmed.contains("t=\"")) {
            parseSrv3(trimmed).takeIf { it.isNotEmpty() }?.let { return it }
        }
        if (trimmed.contains("<text")) {
            parseXml(trimmed).takeIf { it.isNotEmpty() }?.let { return it }
        }
        if (trimmed.contains("WEBVTT") || trimmed.contains("-->")) {
            parseVtt(trimmed).takeIf { it.isNotEmpty() }?.let { return it }
        }
        return emptyList()
    }

    private fun parseJson3(raw: String): List<TranscriptSegment> {
        val events = JSONObject(raw).optJSONArray("events") ?: return emptyList()
        val out = mutableListOf<TranscriptSegment>()
        for (i in 0 until events.length()) {
            val ev = events.optJSONObject(i) ?: continue
            val segs = ev.optJSONArray("segs") ?: continue
            val text = buildString {
                for (j in 0 until segs.length()) {
                    append(segs.optJSONObject(j)?.optString("utf8").orEmpty())
                }
            }.replace('\u0000', ' ').replace(Regex("\\s+"), " ").trim()
            if (text.isBlank() || text == "\n") continue
            val times = normalizeTimes(ev.optDouble("tStartMs", 0.0).toFloat(), ev.optDouble("dDurationMs", 500.0).toFloat())
            out += TranscriptSegment(text, times.first, times.second)
        }
        return out
    }

    private fun parseSrv3(xml: String): List<TranscriptSegment> {
        val out = mutableListOf<TranscriptSegment>()
        val pRegex = Pattern.compile("<p\\s+t=\"(\\d+)\"\\s+d=\"(\\d+)\"[^>]*>([\\s\\S]*?)</p>")
        val matcher = pRegex.matcher(xml)
        while (matcher.find()) {
            val startMs = matcher.group(1)?.toFloatOrNull() ?: 0f
            val durMs = matcher.group(2)?.toFloatOrNull() ?: 500f
            val inner = matcher.group(3).orEmpty()
            var text = inner
            val sRegex = Pattern.compile("<s[^>]*>([^<]*)</s>")
            val sMatcher = sRegex.matcher(inner)
            if (sMatcher.find()) {
                text = buildString {
                    sMatcher.reset()
                    while (sMatcher.find()) append(sMatcher.group(1))
                }
            } else {
                text = inner.replace(Regex("<[^>]+>"), "")
            }
            text = decodeEntities(text).replace(Regex("\\s+"), " ").trim()
            if (text.isBlank()) continue
            val times = normalizeTimes(startMs, durMs)
            out += TranscriptSegment(text, times.first, times.second)
        }
        return out
    }

    private fun parseXml(xml: String): List<TranscriptSegment> {
        val out = mutableListOf<TranscriptSegment>()
        val regex = Pattern.compile("<text[^>]*start=\"([^\"]+)\"[^>]*dur=\"([^\"]+)\"[^>]*>([\\s\\S]*?)</text>")
        val matcher = regex.matcher(xml)
        while (matcher.find()) {
            val start = matcher.group(1)?.toFloatOrNull() ?: 0f
            val dur = matcher.group(2)?.toFloatOrNull() ?: 0.5f
            val text = decodeEntities(matcher.group(3).orEmpty().replace(Regex("<[^>]+>"), " "))
                .replace(Regex("\\s+"), " ")
                .trim()
            if (text.isBlank()) continue
            out += TranscriptSegment(text, start, dur)
        }
        return out
    }

    private fun parseVtt(vtt: String): List<TranscriptSegment> {
        val lines = vtt.split("\n")
        val out = mutableListOf<TranscriptSegment>()
        var i = 0
        while (i < lines.size) {
            val row = lines[i].trim()
            if (!row.contains("-->")) {
                i++
                continue
            }
            val parts = row.split("-->")
            val start = parseStamp(parts.getOrNull(0)?.trim()?.split(" ")?.firstOrNull().orEmpty())
            val end = parseStamp(parts.getOrNull(1)?.trim()?.split(" ")?.firstOrNull().orEmpty())
            i++
            val cue = buildString {
                while (i < lines.size && lines[i].trim().isNotEmpty()) {
                    append(lines[i].replace(Regex("<[^>]+>"), "").trim())
                    append(' ')
                    i++
                }
            }.trim()
            if (cue.isNotBlank()) {
                out += TranscriptSegment(cue, start, (end - start).coerceAtLeast(0.05f))
            }
        }
        return out
    }

    private fun mergeSegments(segments: List<TranscriptSegment>): List<SubtitleLine> {
        val out = mutableListOf<SubtitleLine>()
        var bufferText = ""
        var bufferStart = 0f
        var bufferEnd = 0f

        fun flush() {
            val text = bufferText.trim()
            if (text.isBlank()) {
                bufferText = ""
                return
            }
            var end = bufferEnd
            if (end <= bufferStart) end = bufferStart + 0.3f
            out += SubtitleLine("line-${out.size + 1}", text, bufferStart, end)
            bufferText = ""
        }

        for (segment in dedupeRollingSegments(segments)) {
            val txt = segment.text.trim()
            if (txt.isBlank() || Regex("^[\\[♪\\]\\s]+$").containsMatchIn(txt)) continue
            if (bufferText.isEmpty()) {
                val withoutOverlap = out.lastOrNull()?.text?.let { stripLeadingOverlap(it, txt) } ?: txt
                if (withoutOverlap.isBlank()) continue
                bufferText = withoutOverlap
                bufferStart = segment.offsetSec
                bufferEnd = segment.offsetSec + segment.durationSec
            } else {
                bufferText = appendWithoutOverlap(bufferText, txt)
                bufferEnd = segment.offsetSec + segment.durationSec
            }
            if (Regex("[.!?…:;]\\s*$").containsMatchIn(txt) || bufferText.length > 220) {
                flush()
            }
        }
        flush()
        return dedupeSubtitleLines(out)
    }

    private fun normalizeOverlapWord(word: String): String =
        word.trim().replace(Regex("^[\\.,;:!?…«»\"']+|[\\.,;:!?…«»\"']+$"), "").lowercase()

    private fun wordSuffixPrefixOverlap(wordCount: Int, previous: String, next: String): Int {
        val prevWords = previous.split(Regex("\\s+")).filter { it.isNotBlank() }
        val nextWords = next.split(Regex("\\s+")).filter { it.isNotBlank() }
        val maxOverlap = minOf(wordCount, prevWords.size, nextWords.size)
        for (overlap in maxOverlap downTo 1) {
            val suffix = prevWords.takeLast(overlap).map(::normalizeOverlapWord)
            val prefix = nextWords.take(overlap).map(::normalizeOverlapWord)
            if (suffix == prefix) return overlap
        }
        return 0
    }

    private fun stripLeadingOverlap(previous: String, next: String): String {
        val prev = previous.trim()
        val trimmedNext = next.trim()
        if (prev.isBlank() || trimmedNext.isBlank()) return trimmedNext
        if (trimmedNext.startsWith(prev, ignoreCase = true)) {
            return trimmedNext.substring(prev.length).trimStart()
        }

        val nextWords = trimmedNext.split(Regex("\\s+")).filter { it.isNotBlank() }
        val overlap = wordSuffixPrefixOverlap(nextWords.size, prev, trimmedNext)
        if (overlap > 0) {
            return nextWords.drop(overlap).joinToString(" ").trim()
        }
        return trimmedNext
    }

    private fun dedupeRollingSegments(segments: List<TranscriptSegment>): List<TranscriptSegment> {
        val out = mutableListOf<TranscriptSegment>()
        for (segment in segments) {
            val txt = segment.text.trim()
            if (txt.isBlank()) continue
            val last = out.lastOrNull()
            if (last == null) {
                out += segment
                continue
            }
            val lastTxt = last.text.trim()
            when {
                txt.equals(lastTxt, ignoreCase = true) -> {
                    out[out.lastIndex] = last.copy(
                        durationSec = (segment.offsetSec + segment.durationSec - last.offsetSec)
                            .coerceAtLeast(last.durationSec),
                    )
                }
                txt.startsWith(lastTxt, ignoreCase = true) -> {
                    out[out.lastIndex] = segment.copy(text = txt)
                }
                lastTxt.startsWith(txt, ignoreCase = true) -> Unit
                else -> {
                    val trimmed = stripLeadingOverlap(lastTxt, txt)
                    when {
                        trimmed.isBlank() -> {
                            out[out.lastIndex] = last.copy(
                                durationSec = (segment.offsetSec + segment.durationSec - last.offsetSec)
                                    .coerceAtLeast(last.durationSec),
                            )
                        }
                        trimmed.equals(lastTxt, ignoreCase = true) -> Unit
                        else -> out += segment.copy(text = trimmed)
                    }
                }
            }
        }
        return out
    }

    private fun appendWithoutOverlap(existing: String, next: String): String {
        val left = existing.trim()
        val right = next.trim()
        if (right.isBlank()) return left
        if (left.isBlank()) return right
        if (right.equals(left, ignoreCase = true)) return left
        if (right.startsWith(left, ignoreCase = true)) return right
        if (left.endsWith(right, ignoreCase = true)) return left

        val overlap = wordSuffixPrefixOverlap(
            wordCount = minOf(
                left.split(Regex("\\s+")).count { it.isNotBlank() },
                right.split(Regex("\\s+")).count { it.isNotBlank() },
            ),
            previous = left,
            next = right,
        )
        if (overlap > 0) {
            val rest = right.split(Regex("\\s+")).filter { it.isNotBlank() }.drop(overlap).joinToString(" ")
            return if (rest.isBlank()) left else "$left $rest"
        }
        return "$left $right"
    }

    private fun dedupeSubtitleLines(lines: List<SubtitleLine>): List<SubtitleLine> {
        if (lines.isEmpty()) return lines
        val out = mutableListOf<SubtitleLine>()
        for (line in lines) {
            val prev = out.lastOrNull()
            var txt = line.text.trim()
            if (prev != null) {
                txt = stripLeadingOverlap(prev.text.trim(), txt)
                if (txt.isBlank()) continue
            }
            val prevTxt = prev?.text?.trim().orEmpty()
            when {
                prev == null -> out += line.copy(text = txt)
                txt.equals(prevTxt, ignoreCase = true) -> Unit
                txt.startsWith(prevTxt, ignoreCase = true) &&
                    line.startSec - prev.endSec < 5f -> {
                    out[out.lastIndex] = line.copy(id = prev.id, text = txt)
                }
                prevTxt.startsWith(txt, ignoreCase = true) -> Unit
                else -> out += line.copy(text = txt)
            }
        }
        return out.mapIndexed { index, line -> line.copy(id = "line-${index + 1}") }
    }

    private fun normalizeTimes(offset: Float, duration: Float): Pair<Float, Float> {
        var o = offset
        var d = duration
        if (o > 500f || d > 100f) {
            o /= 1000f
            d /= 1000f
        }
        return o to d.coerceAtLeast(0.05f)
    }

    private fun parseStamp(stamp: String): Float {
        val cleaned = stamp.replace(',', '.')
        val parts = cleaned.split(':')
        return when (parts.size) {
            3 -> parts[0].toFloatOrNull()?.times(3600f).orZero() +
                parts[1].toFloatOrNull()?.times(60f).orZero() +
                parts[2].toFloatOrNull().orZero()
            2 -> parts[0].toFloatOrNull()?.times(60f).orZero() + parts[1].toFloatOrNull().orZero()
            else -> parts.firstOrNull()?.toFloatOrNull().orZero()
        }
    }

    private fun Float?.orZero() = this ?: 0f

    private fun decodeEntities(input: String): String =
        input
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")

    private fun String.withFmt(fmt: String): String {
        return if (contains("fmt=")) replace(Regex("fmt=[^&]+"), "fmt=$fmt")
        else if (contains('?')) "$this&fmt=$fmt" else "$this?fmt=$fmt"
    }

    private data class CaptionTrack(
        val languageCode: String,
        val kind: String,
        val baseUrl: String,
    )

    companion object {
        private const val MOBILE_UA =
            "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"
        private const val CAPTION_UA =
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_4) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/85.0.4183.83 Safari/537.36,gzip(gfe)"
        private const val INNERTUBE_ANDROID_UA =
            "com.google.android.youtube/20.10.38 (Linux; U; Android 14)"

        fun extractVideoId(input: String): String? {
            val raw = input.trim()
            if (raw.isEmpty()) return null
            if (Regex("^[A-Za-z0-9_-]{11}$").matches(raw)) return raw
            val match = Regex("(?:v=|youtu\\.be/|embed/)([A-Za-z0-9_-]{11})", RegexOption.IGNORE_CASE).find(raw)
            return match?.groupValues?.getOrNull(1)
        }
    }
}
