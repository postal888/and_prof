package com.profconq.app.api

import com.profconq.app.billing.BillingCatalogue
import com.profconq.app.billing.BillingContract
import com.profconq.app.billing.BillingError
import com.profconq.app.billing.JsonLite
import com.profconq.app.billing.PremiumSnapshot
import com.profconq.app.billing.billingErrorOf
import com.profconq.app.billing.strOr
import com.profconq.app.billing.verifyRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * A billing failure carries a status code and the server's stable error name only. The response
 * body is never kept, so a purchase token cannot escape through `error.message`.
 */
class BillingApiException(
    val error: BillingError,
    val http: Int,
    val errorName: String?,
) : Exception("Billing request failed (http $http)")

interface BillingApi {
    suspend fun products(): BillingCatalogue

    suspend fun status(): PremiumSnapshot

    /** The only place a purchase token leaves the process, with exactly the two allowed keys. */
    suspend fun verify(productId: String, purchaseToken: String): PremiumSnapshot
}

/**
 * Billing endpoints. One call uses exactly one kind of authorization: the bearer transport first,
 * and only after an `Unauthorized` answer does the same call run again over the separate cookie
 * transport. Any other bearer answer (409, 429, 502) is final and is never retried over cookies.
 */
class ProfconqBillingApi(
    private val authTokenProvider: suspend (forceRefresh: Boolean) -> String?,
    private val sessionAuth: ProfconqSessionAuth? = null,
) : BillingApi {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .build()

    override suspend fun products(): BillingCatalogue = withContext(Dispatchers.IO) {
        // The catalogue is public: neither bearer nor cookie, so there is nothing to fall back from.
        val (code, body) = request(client, "GET", BillingContract.PRODUCTS_PATH, null, null)
        BillingCatalogue.parse(requireRoot(code, body))
    }

    override suspend fun status(): PremiumSnapshot = withContext(Dispatchers.IO) {
        PremiumSnapshot.parse(authorizedJson("GET", BillingContract.STATUS_PATH, null))
    }

    override suspend fun verify(productId: String, purchaseToken: String): PremiumSnapshot {
        val body = verifyRequestBody(productId, purchaseToken)
        return withContext(Dispatchers.IO) {
            PremiumSnapshot.parse(authorizedJson("POST", BillingContract.VERIFY_PATH, body))
        }
    }

    /**
     * Bearer first. Only an `unauthorized` answer switches transports, and the fallback is a second,
     * separate request over the web session's cookie client with no bearer header at all: the two
     * authentications are never mixed in one call, and every other failure travels unchanged.
     */
    private suspend fun authorizedJson(
        method: String,
        path: String,
        jsonBody: String?,
    ): Map<String, Any?> = withContext(Dispatchers.IO) {
        val bearer = runCatching { callWithBearer(method, path, jsonBody) }
        bearer.getOrElse { failure ->
            if (failure !is BillingApiException || failure.error != BillingError.Unauthorized) {
                throw failure
            }
            val auth = sessionAuth ?: throw failure
            if (!auth.establishWebSession()) throw failure
            val (code, body) = request(auth.cookieClient, method, path, jsonBody, null)
            requireRoot(code, body)
        }
    }

    private suspend fun callWithBearer(method: String, path: String, jsonBody: String?): Map<String, Any?> {
        val token = bearerToken(forceRefresh = false)
            ?: throw unauthorized()
        val (code, body) = request(client, method, path, jsonBody, token)
        if (code != 401) return requireRoot(code, body)
        // One force-refresh is the documented recovery for an expired id token; /status and
        // /verify both authenticate before any work, so a second 401 means this transport is out.
        val refreshed = bearerToken(forceRefresh = true) ?: throw unauthorized()
        val second = request(client, method, path, jsonBody, refreshed)
        return requireRoot(second.first, second.second)
    }

    private suspend fun bearerToken(forceRefresh: Boolean): String? =
        runCatching { authTokenProvider(forceRefresh) }.getOrNull()?.takeIf { it.isNotBlank() }

    private fun unauthorized(): BillingApiException =
        BillingApiException(BillingError.Unauthorized, 401, "unauthorized")

    private fun requireRoot(code: Int, body: String): Map<String, Any?> {
        val root = runCatching { JsonLite.parseObject(body) }.getOrNull()
        val errorName = root?.strOr("error")
        if (code == 401) throw BillingApiException(BillingError.Unauthorized, code, errorName)
        if (code !in 200..299) {
            throw BillingApiException(billingErrorOf(code, errorName), code, errorName)
        }
        if (root == null) throw BillingApiException(BillingError.Unknown, code, null)
        return root
    }

    private fun request(
        client: OkHttpClient,
        method: String,
        path: String,
        jsonBody: String?,
        bearerToken: String?,
    ): Pair<Int, String> {
        val builder = Request.Builder()
            .url("${ProfconqApiConfig.BASE_URL}$path")
            .header("Accept", "application/json")
        if (bearerToken != null) builder.header("Authorization", "Bearer $bearerToken")
        when (method) {
            "POST" -> builder.post((jsonBody ?: "").toRequestBody(JSON))
            "PUT" -> builder.put((jsonBody ?: "").toRequestBody(JSON))
            else -> builder.get()
        }
        return try {
            client.newCall(builder.build()).execute().use { response ->
                response.code to response.body?.string().orEmpty()
            }
        } catch (network: IOException) {
            // Deliberately dropped: an IOException text can carry the host or the request line,
            // and this call's body holds a purchase token. Only the stable error survives.
            throw BillingApiException(BillingError.Network, 0, null)
        }
    }

    private companion object {
        val JSON = "application/json".toMediaType()
    }
}
