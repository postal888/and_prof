package com.profconq.app.api

import com.profconq.app.data.WordLimitPolicy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object ProfconqApiConfig {
    const val BASE_URL = "https://profconq.com"
}

data class VocabularyPushResult(
    val wordCount: Int,
    val wordLimit: Int = WordLimitPolicy.FREE_LIMIT,
    /** The server kept its own, much larger vocabulary instead of this push (shrink guard). */
    val refused: Boolean = false,
)

/**
 * How a `/api/auth/me` read ended, kept apart from anything it said. "The server named this account
 * as free" and "the server did not answer" are different statements, and only the first of them may
 * change what the profile shows: reading a refusal as a free account is what took a premium user's
 * word limit down to 10 while their purchase was still standing.
 */
enum class MeRead {
    /** 2xx with an account in it. */
    Answered,

    /** 401 or 403: the request carried no valid session. */
    Rejected,

    /** Anything else, including a call that never reached the server. */
    Unreachable,
}

/** Plain Kotlin on purpose, so the three endings are distinguishable in a test without a device. */
fun meReadOf(code: Int): MeRead = when (code) {
    in 200..299 -> MeRead.Answered
    401, 403 -> MeRead.Rejected
    else -> MeRead.Unreachable
}

/** The `/me` fields this client reads, with every JSON type already taken out. */
data class MeFields(
    val email: String?,
    val plan: String?,
    val wordCount: Int?,
    val wordLimit: Int?,
)

/** One read of the account: how it ended, the status it ended with, and what it said if it answered. */
data class MeOutcome(val read: MeRead, val code: Int, val fields: MeFields?)

/**
 * The profile an answered `/me` described. The server sends JSON null for a premium account's limit,
 * which is its way of saying "no limit" rather than "no value", so it becomes the unlimited one here
 * and never the free one.
 */
fun accountOf(fields: MeFields, uid: String, email: String?, displayName: String?): AccountInfo {
    val isPremium = fields.plan == "premium"
    return AccountInfo(
        uid = uid,
        email = email ?: fields.email,
        displayName = displayName,
        isPremium = isPremium,
        wordCount = fields.wordCount ?: 0,
        wordLimit = when {
            isPremium -> WordLimitPolicy.UNLIMITED
            fields.wordLimit == null -> WordLimitPolicy.FREE_LIMIT
            else -> fields.wordLimit
        },
    )
}

class ProfconqApiClient(
    private val authTokenProvider: suspend (forceRefresh: Boolean) -> String?,
    private val sessionAuth: ProfconqSessionAuth? = null,
    private val bearerClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .build(),
) {
    private val ttsClient: OkHttpClient = bearerClient.newBuilder()
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    suspend fun pullVocabulary(): List<WebVocabWord> = withContext(Dispatchers.IO) {
        pullWithBearer().getOrElse { bearerError ->
            if (sessionAuth != null && sessionAuth.establishWebSession()) {
                return@withContext pullWithCookies().getOrElse { cookieError ->
                    throw preferAuthError(bearerError, cookieError)
                }
            }
            throw preferAuthError(bearerError, null)
        }
    }

    suspend fun pushVocabulary(words: List<WebVocabWord>): VocabularyPushResult = withContext(Dispatchers.IO) {
        val wordsJson = JSONArray().apply { words.forEach { put(it.toJson()) } }
        val payload = JSONObject().put("json", JSONObject().put("words", wordsJson))
        val jsonBody = payload.toString()

        pushWithBearer(jsonBody, words.size).getOrElse { bearerError ->
            if (sessionAuth != null && sessionAuth.establishWebSession()) {
                return@withContext pushWithCookies(jsonBody, words.size).getOrElse { cookieError ->
                    throw preferAuthError(bearerError, cookieError)
                }
            }
            throw preferAuthError(bearerError, null)
        }
    }

    suspend fun pullSyncJson(key: String): Any? = withContext(Dispatchers.IO) {
        pullSyncJsonWithBearer(key).getOrElse { bearerError ->
            if (sessionAuth != null && sessionAuth.establishWebSession()) {
                return@withContext pullSyncJsonWithCookies(key).getOrElse { cookieError ->
                    throw preferAuthError(bearerError, cookieError)
                }
            }
            throw preferAuthError(bearerError, null)
        }
    }

    suspend fun pushSyncJson(key: String, json: Any) = withContext(Dispatchers.IO) {
        val payload = JSONObject().put("json", json)
        val body = payload.toString()
        pushSyncJsonWithBearer(key, body).getOrElse { bearerError ->
            if (sessionAuth != null && sessionAuth.establishWebSession()) {
                return@withContext pushSyncJsonWithCookies(key, body).getOrElse { cookieError ->
                    throw preferAuthError(bearerError, cookieError)
                }
            }
            throw preferAuthError(bearerError, null)
        }
    }

    /**
     * ElevenLabs clip via the website API: cache lookup, then render.
     * Guest `/api/elevenlabs-tts` is a last resort when not signed in.
     */
    suspend fun fetchStudioTts(
        text: String,
        lang: String,
        voiceId: String,
        force: Boolean = false,
    ): ByteArray =
        withContext(Dispatchers.IO) {
            val payload = JSONObject()
                .put("text", text)
                .put("lang", lang)
                .put("voice_id", voiceId)
                .apply { if (force) put("force", true) }
                .toString()
            val guestPayload = JSONObject()
                .put("text", text)
                .put("language_code", lang)
                .put("voice_id", voiceId)
                .toString()

            if (force) {
                audioWithAuth("/api/sync/tts/render", payload)
                    ?: audioGuest("/api/elevenlabs-tts", guestPayload)
                    ?: throw ProfconqApiException.HttpError(503, "tts unavailable")
            } else {
                audioWithAuth("/api/sync/tts/lookup", payload)
                    ?: audioWithAuth("/api/sync/tts/render", payload)
                    ?: audioGuest("/api/elevenlabs-tts", guestPayload)
                    ?: throw ProfconqApiException.HttpError(503, "tts unavailable")
            }
        }

    private suspend fun audioWithAuth(path: String, jsonBody: String): ByteArray? {
        audioWithBearer(path, jsonBody).getOrElse { bearerError ->
            if (bearerError is ProfconqApiException.Unauthorized &&
                sessionAuth != null &&
                sessionAuth.establishWebSession()
            ) {
                return audioWithCookies(path, jsonBody).getOrElse { null }
            }
            return null
        }?.let { return it }
        if (sessionAuth != null && sessionAuth.establishWebSession()) {
            return audioWithCookies(path, jsonBody).getOrNull()
        }
        return null
    }

    private suspend fun audioWithBearer(path: String, jsonBody: String): Result<ByteArray?> =
        runCatching {
            val token = authTokenProvider(false) ?: throw ProfconqApiException.Unauthorized()
            var result = executeAudio(ttsClient, "POST", path, jsonBody, useBearer = true, token)
            if (result.first == 401) {
                val refreshed = authTokenProvider(true) ?: throw ProfconqApiException.Unauthorized()
                result = executeAudio(ttsClient, "POST", path, jsonBody, useBearer = true, refreshed)
            }
            parseAudio(result.first, result.second)
        }

    private fun audioWithCookies(path: String, jsonBody: String): Result<ByteArray?> = runCatching {
        val client = sessionAuth?.cookieClient ?: error("No cookie client")
        val result = executeAudio(client, "POST", path, jsonBody, useBearer = false)
        parseAudio(result.first, result.second)
    }

    private fun audioGuest(path: String, jsonBody: String): ByteArray? = runCatching {
        val result = executeAudio(ttsClient, "POST", path, jsonBody, useBearer = false)
        parseAudio(result.first, result.second)
    }.getOrNull()

    private fun parseAudio(code: Int, bytes: ByteArray): ByteArray? {
        if (code == 401) throw ProfconqApiException.Unauthorized()
        if (code == 404 || code == 204) return null
        if (code !in 200..299) return null
        if (bytes.size < 200) return null
        val looksJson = bytes.size < 80 &&
            bytes.firstOrNull()?.toInt()?.toChar() == '{'
        if (looksJson) return null
        return bytes
    }

    private fun executeAudio(
        client: OkHttpClient,
        method: String,
        path: String,
        jsonBody: String?,
        useBearer: Boolean,
        bearerToken: String = "",
    ): Pair<Int, ByteArray> {
        val url = "${ProfconqApiConfig.BASE_URL}$path"
        val builder = Request.Builder()
            .url(url)
            .header("Accept", "audio/mpeg")
        if (useBearer && bearerToken.isNotBlank()) {
            builder.header("Authorization", "Bearer $bearerToken")
        }
        when (method) {
            "PUT" -> builder.put(jsonBody!!.toRequestBody("application/json".toMediaType()))
            "POST" -> builder.post(jsonBody!!.toRequestBody("application/json".toMediaType()))
            else -> builder.get()
        }
        client.newCall(builder.build()).execute().use { response ->
            return response.code to (response.body?.bytes() ?: ByteArray(0))
        }
    }

    /**
     * The cloud profile, from `/api/auth/me` alone. A read that was refused or never answered throws
     * instead of coming back as a free account: the caller has nothing better to do with that than to
     * keep the profile it last had.
     */
    suspend fun fetchAccount(uid: String, email: String?, displayName: String?): AccountInfo =
        withContext(Dispatchers.IO) {
            val outcome = fetchMe()
            val fields = outcome.fields
            when {
                outcome.read == MeRead.Answered && fields != null ->
                    accountOf(fields, uid, email, displayName)
                outcome.read == MeRead.Rejected -> throw ProfconqApiException.Unauthorized()
                else -> throw ProfconqApiException.HttpError(outcome.code, null)
            }
        }

    suspend fun redeemPromoCode(code: String): AccountInfo = withContext(Dispatchers.IO) {
        val payload = JSONObject().put("code", code.trim()).toString()
        redeemWithBearer(payload).getOrElse { bearerError ->
            if (sessionAuth != null && sessionAuth.establishWebSession()) {
                return@withContext redeemWithCookies(payload).getOrElse { cookieError ->
                    throw preferAuthError(bearerError, cookieError)
                }
            }
            throw preferAuthError(bearerError, null)
        }
    }

    /**
     * One read of the account, in at most two requests. A bearer that was refused says the web
     * session is missing, and only then is one established and the account read over the cookies.
     * A server that did not answer is returned exactly as it is: starting a session behind one adds
     * requests that cannot say anything new, which is how one expired session became a storm.
     *
     * The bearer token is never force-refreshed here. A fresh token changes nothing about what the
     * account is entitled to, and that refresh was the second of the four calls.
     */
    private suspend fun fetchMe(): MeOutcome = withContext(Dispatchers.IO) {
        val bearer = meWithBearer()
        if (bearer.read != MeRead.Rejected) return@withContext bearer
        val session = sessionAuth ?: return@withContext bearer
        if (!session.establishWebSession()) return@withContext bearer
        meWithCookies()
    }

    private suspend fun meWithBearer(): MeOutcome = try {
        val token = authTokenProvider(false) ?: return MeOutcome(MeRead.Rejected, 401, null)
        val (code, body) = execute(bearerClient, "/api/auth/me", "GET", null, token)
        meOutcomeOf(code, body)
    } catch (failure: Exception) {
        MeOutcome(MeRead.Unreachable, 0, null)
    }

    private suspend fun meWithCookies(): MeOutcome = try {
        val client = sessionAuth?.cookieClient ?: return MeOutcome(MeRead.Rejected, 401, null)
        val (code, body) = execute(client, "GET", "/api/auth/me", null, useBearer = false)
        meOutcomeOf(code, body)
    } catch (failure: Exception) {
        MeOutcome(MeRead.Unreachable, 0, null)
    }

    /** A 2xx with no account in it is not an answer about the account either. */
    private fun meOutcomeOf(code: Int, body: String): MeOutcome {
        val read = meReadOf(code)
        if (read != MeRead.Answered) return MeOutcome(read, code, null)
        val user = runCatching { JSONObject(body).optJSONObject("user") }.getOrNull()
            ?: return MeOutcome(MeRead.Unreachable, code, null)
        return MeOutcome(
            read = MeRead.Answered,
            code = code,
            fields = MeFields(
                email = user.optString("email").takeIf { it.isNotBlank() },
                plan = user.optString("plan").takeIf { it.isNotBlank() },
                wordCount = if (user.has("wordCount") && !user.isNull("wordCount")) user.optInt("wordCount") else null,
                wordLimit = if (user.has("wordLimit") && !user.isNull("wordLimit")) user.optInt("wordLimit") else null,
            ),
        )
    }

    private suspend fun redeemWithBearer(payload: String): Result<AccountInfo> = runCatching {
        val (code, body) = authorizedBearerRequest("POST", "/api/promo/redeem", payload)
        parsePromoRedeem(code, body)
    }

    private suspend fun redeemWithCookies(payload: String): Result<AccountInfo> = runCatching {
        val client = sessionAuth?.cookieClient ?: error("No cookie client")
        val (code, body) = execute(client, "POST", "/api/promo/redeem", payload, useBearer = false)
        parsePromoRedeem(code, body)
    }

    private fun parsePromoRedeem(code: Int, body: String): AccountInfo {
        if (code == 401) throw ProfconqApiException.Unauthorized()
        if (code == 404) throw ProfconqApiException.PromoInvalid()
        if (code == 409) throw ProfconqApiException.PromoAlreadyRedeemed()
        if (code !in 200..299) throw ProfconqApiException.HttpError(code, body)
        val json = JSONObject(body)
        val user = json.getJSONObject("user")
        val isPremium = user.optString("plan") == "premium"
        return AccountInfo(
            uid = "",
            email = user.optString("email").takeIf { it.isNotBlank() },
            displayName = null,
            isPremium = isPremium,
            wordCount = user.optInt("wordCount"),
            wordLimit = parseWordLimit(user, isPremium),
        )
    }

    private fun parseWordLimit(user: JSONObject?, isPremium: Boolean): Int {
        if (isPremium) return WordLimitPolicy.UNLIMITED
        if (user == null) return WordLimitPolicy.FREE_LIMIT
        if (user.has("wordLimit") && user.isNull("wordLimit")) return WordLimitPolicy.UNLIMITED
        return user.optInt("wordLimit", WordLimitPolicy.FREE_LIMIT)
    }

    private suspend fun pullSyncJsonWithBearer(key: String): Result<Any?> = runCatching {
        val (code, body) = authorizedBearerRequest("GET", "/api/sync/$key", null)
        parseSyncJsonPull(code, body)
    }

    private suspend fun pullSyncJsonWithCookies(key: String): Result<Any?> = runCatching {
        val client = sessionAuth?.cookieClient ?: error("No cookie client")
        val (code, body) = execute(client, "GET", "/api/sync/$key", null, useBearer = false)
        parseSyncJsonPull(code, body)
    }

    private suspend fun pushSyncJsonWithBearer(key: String, jsonBody: String): Result<Unit> =
        runCatching {
            val (code, body) = authorizedBearerRequest("PUT", "/api/sync/$key", jsonBody)
            if (code == 401) throw ProfconqApiException.Unauthorized()
            if (code !in 200..299) throw ProfconqApiException.HttpError(code, body)
        }

    private suspend fun pushSyncJsonWithCookies(key: String, jsonBody: String): Result<Unit> =
        runCatching {
            val client = sessionAuth?.cookieClient ?: error("No cookie client")
            val (code, body) = execute(client, "PUT", "/api/sync/$key", jsonBody, useBearer = false)
            if (code == 401) throw ProfconqApiException.Unauthorized()
            if (code !in 200..299) throw ProfconqApiException.HttpError(code, body)
        }

    private fun parseSyncJsonPull(code: Int, body: String): Any? {
        if (code == 401) throw ProfconqApiException.Unauthorized()
        if (code == 204) return null
        if (code !in 200..299) throw ProfconqApiException.HttpError(code, body)
        val root = runCatching { JSONObject(body) }.getOrNull() ?: return null
        return root.opt("json")
    }

    private suspend fun pullWithBearer(): Result<List<WebVocabWord>> = runCatching {
        val (code, body) = authorizedBearerRequest("GET", "/api/sync/vocabulary", null)
        parseVocabularyPull(code, body)
    }

    private suspend fun pullWithCookies(): Result<List<WebVocabWord>> = runCatching {
        val client = sessionAuth?.cookieClient ?: error("No cookie client")
        val (code, body) = execute(client, "GET", "/api/sync/vocabulary", null, useBearer = false)
        parseVocabularyPull(code, body)
    }

    private suspend fun pushWithBearer(jsonBody: String, localCount: Int): Result<VocabularyPushResult> =
        runCatching {
            val (code, body) = authorizedBearerRequest("PUT", "/api/sync/vocabulary", jsonBody)
            parseVocabularyPush(code, body, localCount)
        }

    private suspend fun pushWithCookies(jsonBody: String, localCount: Int): Result<VocabularyPushResult> =
        runCatching {
            val client = sessionAuth?.cookieClient ?: error("No cookie client")
            val (code, body) = execute(client, "PUT", "/api/sync/vocabulary", jsonBody, useBearer = false)
            parseVocabularyPush(code, body, localCount)
        }

    private suspend fun authorizedBearerRequest(
        method: String,
        path: String,
        jsonBody: String?,
    ): Pair<Int, String> {
        val token = authTokenProvider(false)
            ?: throw ProfconqApiException.Unauthorized()
        return execute(bearerClient, path, method, jsonBody, token).let { first ->
            if (first.first == 401) {
                val refreshed = authTokenProvider(true)
                    ?: throw ProfconqApiException.Unauthorized()
                execute(bearerClient, path, method, jsonBody, refreshed)
            } else {
                first
            }
        }
    }

    private fun execute(
        client: OkHttpClient,
        path: String,
        method: String,
        jsonBody: String?,
        bearerToken: String,
    ): Pair<Int, String> = execute(client, method, path, jsonBody, useBearer = true, bearerToken)

    private fun execute(
        client: OkHttpClient,
        method: String,
        path: String,
        jsonBody: String?,
        useBearer: Boolean,
        bearerToken: String = "",
    ): Pair<Int, String> {
        val url = "${ProfconqApiConfig.BASE_URL}$path"
        val builder = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
        if (useBearer && bearerToken.isNotBlank()) {
            builder.header("Authorization", "Bearer $bearerToken")
        }
        when (method) {
            "PUT" -> builder.put(jsonBody!!.toRequestBody("application/json".toMediaType()))
            "POST" -> builder.post(jsonBody!!.toRequestBody("application/json".toMediaType()))
            else -> builder.get()
        }
        client.newCall(builder.build()).execute().use { response ->
            return response.code to response.body?.string().orEmpty()
        }
    }

    private fun preferAuthError(bearer: Throwable, cookie: Throwable?): Throwable {
        if (bearer is ProfconqApiException.Unauthorized || cookie is ProfconqApiException.Unauthorized) {
            return ProfconqApiException.Unauthorized(
                "Сервер profconq.com не принимает вход через Google. " +
                    "Нужно обновить backend: server/integration/ (Firebase для /api/sync/vocabulary). " +
                    "На сайте войдите тем же email, что в Google.",
            )
        }
        return cookie ?: bearer
    }

    private fun parseVocabularyPull(code: Int, body: String): List<WebVocabWord> {
        if (code == 401) throw ProfconqApiException.Unauthorized()
        if (code == 204) return emptyList()
        if (code !in 200..299) throw ProfconqApiException.HttpError(code, body)
        val json = runCatching { JSONObject(body) }.getOrNull()
        val words = json?.optJSONObject("json")?.optJSONArray("words")
            ?: json?.optJSONArray("words")
        return WebVocabWord.parseWordsArray(words)
    }

    private fun parseVocabularyPush(code: Int, body: String, localCount: Int): VocabularyPushResult {
        if (code == 401) throw ProfconqApiException.Unauthorized()
        if (code == 402) {
            val json = runCatching { JSONObject(body) }.getOrNull()
            throw ProfconqApiException.WordLimit(
                count = json?.optInt("count") ?: localCount,
                limit = json?.optInt("limit") ?: WordLimitPolicy.FREE_LIMIT,
            )
        }
        if (code !in 200..299) throw ProfconqApiException.HttpError(code, body)
        val json = runCatching { JSONObject(body) }.getOrNull()
        return VocabularyPushResult(
            wordCount = json?.takeIf { it.has("wordCount") }?.optInt("wordCount") ?: localCount,
            wordLimit = json?.optInt("wordLimit") ?: WordLimitPolicy.FREE_LIMIT,
            refused = json?.optString("ignored").orEmpty().isNotEmpty(),
        )
    }
}
