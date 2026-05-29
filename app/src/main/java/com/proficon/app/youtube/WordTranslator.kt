package com.proficon.app.youtube

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class WordTranslator(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build(),
) {
    private val cache = mutableMapOf<String, WordTranslationResult>()

    suspend fun translatePtRu(text: String, useChatGpt: Boolean): WordTranslationResult = withContext(Dispatchers.IO) {
        val trimmed = text.trim().take(500)
        if (trimmed.isEmpty()) throw IllegalArgumentException("Пустой текст")

        val cacheKey = "${if (useChatGpt) "gpt" else "mm"}:${trimmed.lowercase()}"
        cache[cacheKey]?.let { return@withContext it }

        val isSingleWord = !trimmed.contains(Regex("\\s"))
        val localInf = if (isSingleWord) PtVerbInfinitive.guess(trimmed) else null

        val rawTranslated = if (useChatGpt) {
            try {
                translateViaChatGpt(trimmed, isSingleWord)
            } catch (_: Exception) {
                translateViaMyMemory(trimmed)
            }
        } else {
            translateViaMyMemory(trimmed)
        }

        val (ruText, parsedInf) = PtVerbInfinitive.parseInfinitiveFromTranslation(rawTranslated)
        val ptInfinitive = if (isSingleWord) {
            PtVerbInfinitive.merge(localInf, parsedInf, trimmed)
        } else {
            null
        }

        val result = WordTranslationResult(
            ru = ruText,
            ptInfinitive = ptInfinitive,
        )
        cache[cacheKey] = result
        result
    }

    private fun translateViaChatGpt(text: String, isSingleWord: Boolean): String {
        val payloadText = if (isSingleWord) {
            """Portuguese word: "$text"
Translate to Russian (one line, translation only).
If this is a conjugated verb form, add a second line exactly: *инф. - <Portuguese infinitive>"""
        } else {
            text
        }
        val body = JSONObject().put("text", payloadText).toString()
        val request = Request.Builder()
            .url("https://gentechnet.com/api/book-translate")
            .post(body.toRequestBody("application/json".toMediaType()))
            .header("Accept", "application/json")
            .build()
        val response = client.newCall(request).execute()
        val responseBody = response.body?.string().orEmpty()
        if (!response.isSuccessful) {
            throw Exception("ChatGPT HTTP ${response.code}")
        }
        val json = JSONObject(responseBody)
        val translation = json.optString("translation").trim()
        if (translation.isEmpty()) {
            throw Exception("ChatGPT не вернул перевод")
        }
        return translation
    }

    private fun translateViaMyMemory(text: String): String {
        val encoded = URLEncoder.encode(text, "UTF-8")
        val url = "https://api.mymemory.translated.net/get?q=$encoded&langpair=pt|ru"
        val request = Request.Builder().url(url).get().build()
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            throw Exception("Ошибка перевода (${response.code})")
        }

        val body = response.body?.string() ?: throw Exception("Пустой ответ")
        val json = JSONObject(body)
        val translated = json
            .optJSONObject("responseData")
            ?.optString("translatedText")
            ?.trim()
            .orEmpty()

        if (translated.isEmpty()) throw Exception("MyMemory не вернул перевод")
        return translated
    }
}
