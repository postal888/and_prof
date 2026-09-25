package com.profconq.app.api

import android.content.Context
import com.profconq.app.auth.AuthUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.JavaNetCookieJar
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.net.CookieManager
import java.net.CookiePolicy
import java.net.HttpCookie
import java.net.URI
import java.util.concurrent.TimeUnit

/**
 * Site session: email/password cookies, or Firebase idToken → POST /api/auth/firebase.
 */
class ProfconqSessionAuth(
    context: Context,
    private val authTokenProvider: suspend (forceRefresh: Boolean) -> String?,
) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("profconq_web_session", Context.MODE_PRIVATE)
    private val cookieUri = URI(ProfconqApiConfig.BASE_URL)
    private val cookieManager = CookieManager().apply {
        setCookiePolicy(CookiePolicy.ACCEPT_ALL)
    }

    val cookieClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .cookieJar(JavaNetCookieJar(cookieManager))
        .build()

    private var sessionEstablished = false

    init {
        loadCookies()
        if (cookieManager.cookieStore.cookies.isNotEmpty()) {
            sessionEstablished = true
        }
    }

    suspend fun login(email: String, password: String): AuthUser = withContext(Dispatchers.IO) {
        val payload = JSONObject()
            .put("email", email.trim())
            .put("password", password)
            .toString()
        val request = Request.Builder()
            .url("${ProfconqApiConfig.BASE_URL}/api/auth/login")
            .post(payload.toRequestBody("application/json".toMediaType()))
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")
            .build()
        cookieClient.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            val json = runCatching { JSONObject(body) }.getOrNull()
            val error = json?.optString("error").orEmpty()
            when {
                response.isSuccessful -> {
                    sessionEstablished = true
                    saveCookies()
                    parseUser(json) ?: throw ProfconqApiException.HttpError(response.code, body)
                }
                response.code == 403 || error == "not_verified" ->
                    throw ProfconqApiException.EmailNotVerified()
                response.code == 401 || response.code == 400 || error == "invalid_credentials" ->
                    throw ProfconqApiException.InvalidCredentials()
                else -> throw ProfconqApiException.HttpError(response.code, body)
            }
        }
    }

    suspend fun fetchSessionUser(): AuthUser? = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("${ProfconqApiConfig.BASE_URL}/api/auth/me")
            .header("Accept", "application/json")
            .build()
        cookieClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@withContext null
            val json = runCatching { JSONObject(response.body?.string().orEmpty()) }.getOrNull()
            parseUser(json)?.also { sessionEstablished = true }
        }
    }

    suspend fun logout() = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url("${ProfconqApiConfig.BASE_URL}/api/auth/logout")
                .post("{}".toRequestBody("application/json".toMediaType()))
                .header("Accept", "application/json")
                .build()
            cookieClient.newCall(request).execute().close()
        }
        clearSession()
    }

    suspend fun establishWebSession(forceRefreshToken: Boolean = true): Boolean = withContext(Dispatchers.IO) {
        if (sessionEstablished && cookieManager.cookieStore.cookies.isNotEmpty()) {
            return@withContext true
        }
        if (fetchSessionUser() != null) {
            sessionEstablished = true
            saveCookies()
            return@withContext true
        }
        val idToken = authTokenProvider(forceRefreshToken) ?: return@withContext false
        val body = JSONObject().put("idToken", idToken).toString()
        val request = Request.Builder()
            .url("${ProfconqApiConfig.BASE_URL}/api/auth/firebase")
            .post(body.toRequestBody("application/json".toMediaType()))
            .header("Accept", "application/json")
            .build()
        cookieClient.newCall(request).execute().use { response ->
            sessionEstablished = response.isSuccessful
            if (sessionEstablished) saveCookies()
            sessionEstablished
        }
    }

    fun clearSession() {
        sessionEstablished = false
        cookieManager.cookieStore.removeAll()
        prefs.edit().remove(COOKIES_KEY).apply()
    }

    private fun parseUser(root: JSONObject?): AuthUser? {
        val raw = root?.optJSONObject("user") ?: root ?: return null
        val email = raw.optString("email").takeIf { it.isNotBlank() }
        val uid = raw.optString("id").takeIf { it.isNotBlank() }
            ?: raw.optString("uid").takeIf { it.isNotBlank() }
            ?: raw.optInt("id", -1).takeIf { it > 0 }?.toString()
            ?: email
            ?: return null
        return AuthUser(
            uid = uid,
            displayName = raw.optString("displayName").takeIf { it.isNotBlank() }
                ?: raw.optString("name").takeIf { it.isNotBlank() },
            email = email,
        )
    }

    private fun saveCookies() {
        val packed = cookieManager.cookieStore.cookies.joinToString("\n") { cookie ->
            listOf(
                cookie.name,
                cookie.value,
                cookie.domain.orEmpty(),
                cookie.path ?: "/",
                cookie.maxAge.toString(),
                if (cookie.secure) "1" else "0",
            ).joinToString("\t")
        }
        prefs.edit().putString(COOKIES_KEY, packed).apply()
    }

    private fun loadCookies() {
        val packed = prefs.getString(COOKIES_KEY, null) ?: return
        packed.lineSequence().forEach { line ->
            val parts = line.split("\t")
            if (parts.size < 2) return@forEach
            runCatching {
                val cookie = HttpCookie(parts[0], parts[1]).apply {
                    domain = parts.getOrNull(2)?.ifBlank { "profconq.com" } ?: "profconq.com"
                    path = parts.getOrNull(3)?.ifBlank { "/" } ?: "/"
                    parts.getOrNull(4)?.toLongOrNull()?.let { maxAge = it }
                    secure = parts.getOrNull(5) == "1"
                }
                cookieManager.cookieStore.add(cookieUri, cookie)
            }
        }
    }

    companion object {
        private const val COOKIES_KEY = "cookies_v1"
    }
}
