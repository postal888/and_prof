package com.profconq.app.api

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
import java.util.concurrent.TimeUnit

/**
 * Session cookie for profconq.com admin API endpoints.
 * Credentials are verified on the server only — never stored in the APK.
 */
class ProfconqAdminSession {
    private val cookieManager = CookieManager().apply {
        setCookiePolicy(CookiePolicy.ACCEPT_ALL)
    }

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .cookieJar(JavaNetCookieJar(cookieManager))
        .build()

    suspend fun login(username: String, password: String): Result<String> = withContext(Dispatchers.IO) {
        val body = JSONObject()
            .put("username", username.trim())
            .put("password", password)
            .toString()
        val request = Request.Builder()
            .url("${ProfconqApiConfig.BASE_URL}/api/admin/login")
            .post(body.toRequestBody("application/json".toMediaType()))
            .header("Accept", "application/json")
            .build()
        runCatching {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    error("invalid_credentials")
                }
                val json = JSONObject(response.body?.string().orEmpty())
                json.optString("username").ifBlank { username.trim() }
            }
        }
    }

    suspend fun currentUsername(): String? = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("${ProfconqApiConfig.BASE_URL}/api/admin/me")
            .get()
            .header("Accept", "application/json")
            .build()
        runCatching {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@runCatching null
                val json = JSONObject(response.body?.string().orEmpty())
                json.optString("username").takeIf { it.isNotBlank() }
            }
        }.getOrNull()
    }

    suspend fun logout() = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("${ProfconqApiConfig.BASE_URL}/api/admin/logout")
            .post("{}".toRequestBody("application/json".toMediaType()))
            .header("Accept", "application/json")
            .build()
        runCatching { client.newCall(request).execute().close() }
        cookieManager.cookieStore.removeAll()
    }

    fun clearSession() {
        cookieManager.cookieStore.removeAll()
    }
}
