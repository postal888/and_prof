package com.profconq.app.youtube

import com.profconq.app.api.ProfconqApiException
import com.profconq.app.data.WordLimitPolicy
import com.profconq.app.ui.i18n.SubtitleLanguage
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
    private val authTokenProvider: suspend (forceRefresh: Boolean) -> String? = { null },
) {
    private val cache = mutableMapOf<String, WordTranslationResult>()

    suspend fun translate(
        text: String,
        fromLang: Int,
        toLang: Int,
        useChatGpt: Boolean,
    ): WordTranslationResult = withContext(Dispatchers.IO) {
        val from = SubtitleLanguage.fromStorage(fromLang)
        val to = SubtitleLanguage.fromStorage(toLang)
        val trimmed = text.trim().take(500)
        if (trimmed.isEmpty()) throw IllegalArgumentException("Empty text")

        val cacheKey = "${if (useChatGpt) "gpt" else "mm"}:$from|$to:${trimmed.lowercase()}"
        cache[cacheKey]?.let { return@withContext it }

        val isSingleWord = !trimmed.contains(Regex("\\s"))
        val localInf = if (isSingleWord && from == SubtitleLanguage.PT) {
            PtVerbInfinitive.guess(trimmed)
        } else {
            null
        }

        val rawTranslated = if (useChatGpt) {
            try {
                translateViaChatGpt(trimmed, from, to, isSingleWord)
            } catch (error: TranslationException) {
                throw error
            } catch (error: ProfconqApiException) {
                throw mapApiException(error)
            } catch (_: Exception) {
                translateViaMyMemory(trimmed, from, to)
            }
        } else {
            translateViaMyMemory(trimmed, from, to)
        }

        val (targetText, parsedInf) = if (to == SubtitleLanguage.RU) {
            PtVerbInfinitive.parseInfinitiveFromTranslation(rawTranslated)
        } else {
            rawTranslated.trim() to null
        }
        val ptInfinitive = if (isSingleWord && from == SubtitleLanguage.PT) {
            PtVerbInfinitive.merge(localInf, parsedInf, trimmed)
        } else {
            null
        }

        val result = WordTranslationResult(
            ru = targetText,
            ptInfinitive = ptInfinitive,
        )
        cache[cacheKey] = result
        result
    }

    /** @deprecated Use [translate] with explicit language pair. */
    suspend fun translatePtRu(text: String, useChatGpt: Boolean): WordTranslationResult =
        translate(text, SubtitleLanguage.PT, SubtitleLanguage.RU, useChatGpt)

    private fun mapApiException(error: ProfconqApiException): TranslationException = when (error) {
        is ProfconqApiException.Unauthorized -> TranslationException.AuthRequired(error.message ?: "")
        is ProfconqApiException.WordLimit -> TranslationException.WordLimit(error.count, error.limit)
        is ProfconqApiException.HttpError ->
            TranslationException.ServerError(error.message ?: "Server error")
        is ProfconqApiException.PromoInvalid,
        is ProfconqApiException.PromoAlreadyRedeemed,
        -> TranslationException.ServerError(error.message ?: "Server error")
    }

    private suspend fun translateViaChatGpt(
        text: String,
        fromLang: Int,
        toLang: Int,
        isSingleWord: Boolean,
    ): String {
        val token = authTokenProvider(false)
        if (token.isNullOrBlank()) {
            throw TranslationException.AuthRequired()
        }

        val fromName = SubtitleLanguage.promptLanguageName(fromLang)
        val toName = SubtitleLanguage.promptLanguageName(toLang)
        val payloadText = if (isSingleWord) {
            buildString {
                append("$fromName word: \"")
                append(text)
                append("\"\nTranslate to $toName (one line, translation only).")
                if (fromLang == SubtitleLanguage.PT) {
                    append(
                        "\nIf this is a conjugated verb form, add a second line exactly: " +
                            "*инф. - <Portuguese infinitive>",
                    )
                }
            }
        } else {
            "Translate the following from $fromName to $toName. Reply with the translation only.\n\n$text"
        }
        val body = JSONObject().put("text", payloadText).toString()
        val response = executeChatGptRequest(body, token)
        if (response.first == 401) {
            val refreshedToken = authTokenProvider(true)
            if (!refreshedToken.isNullOrBlank() && refreshedToken != token) {
                val refreshedResponse = executeChatGptRequest(body, refreshedToken)
                return parseChatGptResponse(refreshedResponse.first, refreshedResponse.second)
            }
            throw TranslationException.AuthRequired()
        }
        return parseChatGptResponse(response.first, response.second)
    }

    private fun executeChatGptRequest(body: String, bearerToken: String): Pair<Int, String> {
        val builder = Request.Builder()
            .url("https://profconq.com/api/book-translate")
            .post(body.toRequestBody("application/json".toMediaType()))
            .header("Accept", "application/json")
            .header("Authorization", "Bearer $bearerToken")
        client.newCall(builder.build()).execute().use { response ->
            return response.code to response.body?.string().orEmpty()
        }
    }

    private fun parseChatGptResponse(code: Int, responseBody: String): String {
        when (code) {
            401 -> throw TranslationException.AuthRequired()
            402 -> {
                val json = runCatching { JSONObject(responseBody) }.getOrNull()
                throw TranslationException.WordLimit(
                    count = json?.optInt("count") ?: 0,
                    limit = json?.optInt("limit") ?: WordLimitPolicy.FREE_LIMIT,
                )
            }
        }
        if (code !in 200..299) {
            throw Exception("ChatGPT HTTP $code")
        }
        val json = JSONObject(responseBody)
        val translation = json.optString("translation").trim()
        if (translation.isEmpty()) {
            throw Exception("Empty ChatGPT translation")
        }
        return translation
    }

    private fun translateViaMyMemory(text: String, fromLang: Int, toLang: Int): String {
        val fromCode = SubtitleLanguage.toCode(fromLang)
        val toCode = SubtitleLanguage.toCode(toLang)
        val encoded = URLEncoder.encode(text, "UTF-8")
        val url = "https://api.mymemory.translated.net/get?q=$encoded&langpair=$fromCode|$toCode"
        val request = Request.Builder().url(url).get().build()
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            throw Exception("Translation HTTP ${response.code}")
        }

        val body = response.body?.string() ?: throw Exception("Empty response")
        val json = JSONObject(body)
        val translated = json
            .optJSONObject("responseData")
            ?.optString("translatedText")
            ?.trim()
            .orEmpty()

        if (translated.isEmpty()) throw Exception("MyMemory returned empty translation")
        return translated
    }
}
