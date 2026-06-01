package com.profconq.app.api

import android.content.Context
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
 * Exchanges Firebase idToken for profconq.com session cookie (when POST /api/auth/firebase exists).
 */
class ProfconqSessionAuth(
    context: Context,
    private val authTokenProvider: suspend (forceRefresh: Boolean) -> String?,
) {
    private val cookieManager = CookieManager().apply {
        setCookiePolicy(CookiePolicy.ACCEPT_ALL)
    }

    val cookieClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .cookieJar(JavaNetCookieJar(cookieManager))
        .build()

    private var sessionEstablished = false

    suspend fun establishWebSession(forceRefreshToken: Boolean = true): Boolean = withContext(Dispatchers.IO) {
        if (sessionEstablished && cookieManager.cookieStore.cookies.isNotEmpty()) {
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
            sessionEstablished
        }
    }

    fun clearSession() {
        sessionEstablished = false
        cookieManager.cookieStore.removeAll()
    }
}
