package com.profconq.app.billing

import java.security.MessageDigest

/**
 * Server-side billing contract. Everything here is plain Kotlin so that parsing, filtering and
 * error mapping can be unit tested without an Android runtime.
 */
object BillingContract {
    const val PREMIUM_PRODUCT_ID = "profconq_premium"
    const val BASE_PLAN_MONTHLY = "monthly"
    const val BASE_PLAN_ANNUAL = "annual"

    const val PRODUCTS_PATH = "/api/billing/products"
    const val STATUS_PATH = "/api/billing/status"
    const val VERIFY_PATH = "/api/billing/google/verify"

    /** Products this client may ever talk about; the server allowlist is intersected with this. */
    val knownProductIds: Set<String> = setOf(PREMIUM_PRODUCT_ID)
    val knownBasePlans: Set<String> = setOf(BASE_PLAN_MONTHLY, BASE_PLAN_ANNUAL)

    /** Display order for the two plans; anything else has no place in the UI. */
    fun planRank(basePlanId: String): Int? = when (basePlanId) {
        BASE_PLAN_MONTHLY -> 0
        BASE_PLAN_ANNUAL -> 1
        else -> null
    }
}

class JsonParseException(message: String) : Exception(message)

/**
 * Minimal JSON reader: unit tests run against android.jar stubs where `org.json` throws and no
 * JSON dependency may be added, so the fixed billing contract is read with this parser.
 * Values come back as Map / List / String / Long / Double / Boolean / null.
 */
object JsonLite {
    fun parse(text: String): Any? {
        val reader = Reader(text)
        reader.skipWhitespace()
        val value = reader.readValue()
        reader.skipWhitespace()
        if (!reader.atEnd) throw JsonParseException("trailing characters")
        return value
    }

    @Suppress("UNCHECKED_CAST")
    fun parseObject(text: String): Map<String, Any?> = parse(text) as? Map<String, Any?>
        ?: throw JsonParseException("expected an object")

    fun quote(value: String): String {
        val sb = StringBuilder(value.length + 2)
        sb.append('"')
        value.forEach { ch ->
            when (ch) {
                '"' -> sb.append("\\\"")
                '\\' -> sb.append("\\\\")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                else -> if (ch.code < 0x20) {
                    sb.append("\\u").append(String.format("%04x", ch.code))
                } else {
                    sb.append(ch)
                }
            }
        }
        return sb.append('"').toString()
    }

    fun stringify(value: Any?): String = when (value) {
        null -> "null"
        is Boolean -> value.toString()
        is Number -> value.toString()
        is String -> quote(value)
        is Map<*, *> -> value.entries.joinToString(",", "{", "}") { (k, v) ->
            quote(k.toString()) + ":" + stringify(v)
        }
        is List<*> -> value.joinToString(",", "[", "]") { stringify(it) }
        else -> quote(value.toString())
    }

    private class Reader(private val text: String) {
        private var pos = 0

        val atEnd: Boolean get() = pos >= text.length

        private fun peek(): Char? = text.getOrNull(pos)

        fun skipWhitespace() {
            while (pos < text.length && text[pos].isWhitespace()) pos++
        }

        fun readValue(): Any? {
            skipWhitespace()
            val ch = peek() ?: throw JsonParseException("unexpected end of input")
            return when (ch) {
                '{' -> readObject()
                '[' -> readArray()
                '"' -> readString()
                't' -> { readLiteral("true"); true }
                'f' -> { readLiteral("false"); false }
                'n' -> { readLiteral("null"); null }
                else -> if (ch == '-' || ch.isDigit()) readNumber()
                else throw JsonParseException("unexpected character '$ch'")
            }
        }

        private fun readObject(): Map<String, Any?> {
            expect('{')
            val out = LinkedHashMap<String, Any?>()
            skipWhitespace()
            if (peek() == '}') {
                pos++
                return out
            }
            while (true) {
                skipWhitespace()
                val key = readString()
                skipWhitespace()
                expect(':')
                out[key] = readValue()
                skipWhitespace()
                when (peek()) {
                    ',' -> pos++
                    '}' -> { pos++; return out }
                    else -> throw JsonParseException("expected ',' or '}'")
                }
            }
        }

        private fun readArray(): List<Any?> {
            expect('[')
            val out = ArrayList<Any?>()
            skipWhitespace()
            if (peek() == ']') {
                pos++
                return out
            }
            while (true) {
                out.add(readValue())
                skipWhitespace()
                when (peek()) {
                    ',' -> pos++
                    ']' -> { pos++; return out }
                    else -> throw JsonParseException("expected ',' or ']'")
                }
            }
        }

        private fun readString(): String {
            expect('"')
            val sb = StringBuilder()
            while (true) {
                val ch = peek() ?: throw JsonParseException("unterminated string")
                pos++
                if (ch == '"') return sb.toString()
                if (ch != '\\') {
                    sb.append(ch)
                    continue
                }
                val esc = peek() ?: throw JsonParseException("bad escape")
                pos++
                when (esc) {
                    '"' -> sb.append('"')
                    '\\' -> sb.append('\\')
                    '/' -> sb.append('/')
                    'b' -> sb.append('\b')
                    'f' -> sb.append('\u000C')
                    'n' -> sb.append('\n')
                    'r' -> sb.append('\r')
                    't' -> sb.append('\t')
                    'u' -> {
                        val hex = text.substring(pos, pos + 4)
                        pos += 4
                        sb.append(hex.toInt(16).toChar())
                    }
                    else -> throw JsonParseException("bad escape '\\$esc'")
                }
            }
        }

        private fun readNumber(): Any {
            val start = pos
            if (peek() == '-') pos++
            while (!atEnd && (text[pos].isDigit() || text[pos] in ".eE+-")) pos++
            val raw = text.substring(start, pos)
            return raw.toLongOrNull() ?: raw.toDoubleOrNull()
                ?: throw JsonParseException("invalid number '$raw'")
        }

        private fun readLiteral(literal: String) {
            if (!text.startsWith(literal, pos)) throw JsonParseException("expected '$literal'")
            pos += literal.length
        }

        private fun expect(ch: Char) {
            skipWhitespace()
            if (peek() != ch) throw JsonParseException("expected '$ch'")
            pos++
        }
    }
}

fun Map<String, Any?>.strOr(key: String): String? = (this[key] as? String)?.takeIf { it.isNotEmpty() }

fun Map<String, Any?>.longOrNull(key: String): Long? = when (val v = this[key]) {
    is Long -> v
    is Int -> v.toLong()
    is Double -> v.toLong()
    is String -> v.toLongOrNull()
    else -> null
}

fun Map<String, Any?>.booleanOr(key: String, default: Boolean = false): Boolean =
    (this[key] as? Boolean) ?: default

fun Map<String, Any?>.isNull(key: String): Boolean = containsKey(key) && this[key] == null

@Suppress("UNCHECKED_CAST")
fun Map<String, Any?>.objOr(key: String): Map<String, Any?>? = this[key] as? Map<String, Any?>

@Suppress("UNCHECKED_CAST")
fun Map<String, Any?>.arrOr(key: String): List<Any?> = this[key] as? List<Any?> ?: emptyList()

@Suppress("UNCHECKED_CAST")
fun Map<String, Any?>.objsOr(key: String): List<Map<String, Any?>> =
    arrOr(key).mapNotNull { it as? Map<String, Any?> }

enum class ServerPlan {
    Premium,
    Free,
    Unknown,
    ;

    companion object {
        fun from(value: String?): ServerPlan = when (value) {
            "premium" -> Premium
            "free" -> Free
            else -> Unknown
        }
    }
}

data class EntitlementEntry(
    val id: String,
    val source: String,
    val productId: String?,
    val basePlanId: String?,
    val status: String,
    val startedAt: String?,
    val expiresAt: String?,
    val autoRenews: Boolean,
    val lastVerifiedAt: String?,
)

data class GoverningEntitlement(
    val id: String,
    val source: String,
    val productId: String?,
    val basePlanId: String?,
    val status: String,
    val expiresAt: String?,
)

/**
 * The only source of Premium on the client: a snapshot the server confirmed. `wordLimit == null`
 * means unlimited (the server sends JSON null for premium accounts); [stale] means the server
 * could not re-confirm a stored purchase with Google, which never removes already-granted access.
 */
data class PremiumSnapshot(
    val plan: ServerPlan,
    val wordLimit: Int?,
    val entitlements: List<EntitlementEntry>,
    val governing: GoverningEntitlement?,
    val stale: Boolean,
    val checkedAt: Long?,
    val wordCount: Int?,
    val state: String?,
    val expiresAt: String?,
    val renewed: Boolean?,
) {
    val isPremium: Boolean get() = plan == ServerPlan.Premium

    /** Statuses the server reported for the entitlement that decides access. */
    val governingStatus: String? get() = governing?.status ?: entitlements.firstOrNull()?.status

    companion object {
        /**
         * Status names the server can report. They are used for labels only: Premium always comes
         * from [plan], never from a locally re-derived state, so a pending/paused/expired purchase
         * can never become Premium on the device against the server answer.
         */
        val knownStates: Set<String> = setOf(
            "active",
            "canceled_but_active",
            "in_grace_period",
            "pending",
            "on_hold",
            "paused",
            "expired",
            "revoked",
            "refunded",
            "replaced",
            "unknown",
        )

        fun parse(root: Map<String, Any?>): PremiumSnapshot {
            val wordLimit = if (root.isNull("wordLimit")) null else root.longOrNull("wordLimit")?.toInt()
            return PremiumSnapshot(
                plan = ServerPlan.from(root.strOr("plan")),
                wordLimit = wordLimit,
                entitlements = root.objsOr("entitlements").map { it.parseEntitlement() },
                governing = root.objOr("governing")?.parseGoverning(),
                stale = root.booleanOr("stale"),
                checkedAt = root.longOrNull("checkedAt"),
                wordCount = if (root.isNull("wordCount")) null else root.longOrNull("wordCount")?.toInt(),
                state = root.strOr("state"),
                expiresAt = root.strOr("expiresAt"),
                renewed = if ("renewed" in root) root.booleanOr("renewed") else null,
            )
        }

        fun parse(text: String): PremiumSnapshot = parse(JsonLite.parseObject(text))
    }
}

private fun Map<String, Any?>.parseEntitlement(): EntitlementEntry = EntitlementEntry(
    id = strOr("id").orEmpty(),
    source = strOr("source").orEmpty(),
    productId = strOr("productId"),
    basePlanId = strOr("basePlanId"),
    status = strOr("status") ?: "unknown",
    startedAt = strOr("startedAt"),
    expiresAt = strOr("expiresAt"),
    autoRenews = booleanOr("autoRenews"),
    lastVerifiedAt = strOr("lastVerifiedAt"),
)

private fun Map<String, Any?>.parseGoverning(): GoverningEntitlement = GoverningEntitlement(
    id = strOr("id").orEmpty(),
    source = strOr("source").orEmpty(),
    productId = strOr("productId"),
    basePlanId = strOr("basePlanId"),
    status = strOr("status") ?: "unknown",
    expiresAt = strOr("expiresAt"),
)

data class CatalogPlan(val basePlanId: String, val recurrence: String?)

data class CatalogProduct(val productId: String, val basePlans: List<CatalogPlan>)

/**
 * Server catalogue from `GET /api/billing/products`. It is the purchase allowlist: an offer may
 * only be launched when its base plan is listed here for the premium product.
 */
data class BillingCatalogue(val products: List<CatalogProduct>, val packageName: String?) {
    val premiumPlans: List<CatalogPlan>
        get() = products
            .filter { it.productId == BillingContract.PREMIUM_PRODUCT_ID }
            .flatMap { it.basePlans }
            .filter { BillingContract.knownBasePlans.contains(it.basePlanId) && it.recurrence != null }
            .distinctBy { it.basePlanId }
            .sortedBy { BillingContract.planRank(it.basePlanId) ?: Int.MAX_VALUE }

    fun allows(basePlanId: String): Boolean = premiumPlans.any { it.basePlanId == basePlanId }

    companion object {
        fun parse(root: Map<String, Any?>): BillingCatalogue = BillingCatalogue(
            products = root.objsOr("products")
                .filter { it.strOr("productId") == BillingContract.PREMIUM_PRODUCT_ID }
                .map { product ->
                    CatalogProduct(
                        productId = BillingContract.PREMIUM_PRODUCT_ID,
                        basePlans = product.objsOr("basePlans")
                            .mapNotNull { plan ->
                                val id = plan.strOr("basePlanId") ?: return@mapNotNull null
                                CatalogPlan(id, plan.strOr("recurrence"))
                            },
                    )
                },
            packageName = root.strOr("packageName"),
        )

        fun parse(text: String): BillingCatalogue = parse(JsonLite.parseObject(text))
    }
}

/** Entitlement states shown in Profile. `Stale` keeps Premium: the server could not re-confirm. */
enum class PremiumStatus {
    Loading,
    Unavailable,
    Free,
    Active,
    Pending,
    Verifying,
    Stale,

    /** `ITEM_ALREADY_OWNED`: the owned purchase is being brought back, not a failure. */
    Restoring,
}

/** User-facing outcomes. Every one of them maps to exactly one RU/EN/PT string. */
enum class BillingNotice {
    SignedOut,
    Canceled,

    /** `ITEM_ALREADY_OWNED` recovery is running: the purchase is owned, we are re-reading it. */
    Restoring,
    BillingUnavailable,
    InvalidRequest,
    Network,
    Unauthorized,
    OwnedByOther,
    RateLimited,
    UpstreamUnavailable,
    ServiceUnavailable,
    PendingVerification,
    Verified,
    NothingToRestore,
    Generic,

    /** No standard, priced base-plan record came back, so this plan cannot be bought right now. */
    PlanUnavailable,

    /** The screen that owns the Play sheet is gone, so no purchase can be started. */
    ActivityUnavailable,
}

/** BillingClient response codes the user has to see, kept as ints for the same reason as above. */
object PlayResponseCode {
    const val OK = 0
    const val USER_CANCELED = 1
    const val SERVICE_UNAVAILABLE = 2
    const val BILLING_UNAVAILABLE = 3
    const val ITEM_UNAVAILABLE = 4
    const val DEVELOPER_ERROR = 5
    const val ERROR = 6
    const val ITEM_ALREADY_OWNED = 7
    const val NETWORK_ERROR = 12
    const val SERVICE_DISCONNECTED = -1
    const val FEATURE_NOT_SUPPORTED = -2
    const val SERVICE_TIMEOUT = -3
}

/**
 * Play response code → notice. `debugMessage` is intentionally not consulted: it is Play's free
 * text and can carry request details, while the code is the documented, stable signal.
 * `ITEM_ALREADY_OWNED` is not an error here: the purchase exists, so the client restores it.
 */
fun billingNoticeOfResponseCode(code: Int): BillingNotice = when (code) {
    PlayResponseCode.USER_CANCELED -> BillingNotice.Canceled
    PlayResponseCode.ITEM_ALREADY_OWNED -> BillingNotice.Restoring
    PlayResponseCode.BILLING_UNAVAILABLE,
    PlayResponseCode.FEATURE_NOT_SUPPORTED,
    -> BillingNotice.BillingUnavailable
    PlayResponseCode.NETWORK_ERROR -> BillingNotice.Network
    PlayResponseCode.SERVICE_UNAVAILABLE,
    PlayResponseCode.SERVICE_DISCONNECTED,
    PlayResponseCode.SERVICE_TIMEOUT,
    PlayResponseCode.ERROR,
    PlayResponseCode.ITEM_UNAVAILABLE,
    PlayResponseCode.DEVELOPER_ERROR,
    -> BillingNotice.Generic
    else -> BillingNotice.Generic
}

/**
 * A plan row in Profile. Name and price always come from `ProductDetails`, never from code. There
 * is deliberately no offer token here: the UI state must not hold one, and a purchase always
 * re-reads Play for the token it launches with.
 */
class PlanView(
    val basePlanId: String,
    val title: String,
    val price: String,
) {
    override fun toString(): String = "PlanView($basePlanId, $title, $price)"
}

data class BillingUiState(
    val status: PremiumStatus = PremiumStatus.Loading,
    val plans: List<PlanView> = emptyList(),
    val selectedBasePlanId: String = BillingContract.BASE_PLAN_MONTHLY,
    val busy: Boolean = false,
    val notice: BillingNotice? = null,
    val expiresAt: String? = null,
    val autoRenews: Boolean? = null,

    /** A full catalogue load is running right now. */
    val plansLoading: Boolean = false,

    /** True only after a load in which both the server and Play answered. Buys need this. */
    val plansFresh: Boolean = false,

    /** Why the list cannot be bought, when that is the case. Never a raw Play or HTTP message. */
    val plansFailure: PlansFailure? = null,

    /** Safe counts and documented codes of the last load, for the compact code testers can read. */
    val plansDiagnostics: PlansDiagnostics? = null,
)

enum class BillingError {
    None,
    Unauthorized,
    InvalidRequest,
    ProductNotAllowed,
    OwnedByOtherAccount,
    RateLimited,
    UpstreamUnavailable,
    ServiceUnavailable,
    Network,
    Unknown,
}

/**
 * One place where an HTTP status (plus the server's stable `error` name) becomes a UI outcome.
 * The raw body is never carried along: it is server text and could echo request data.
 */
fun billingErrorOf(http: Int, serverError: String? = null): BillingError = when (serverError) {
    "token_owned_by_other_account" -> BillingError.OwnedByOtherAccount
    "product_not_allowed", "product_mismatch" -> BillingError.ProductNotAllowed
    "google_unavailable" -> BillingError.UpstreamUnavailable
    "billing_unconfigured" -> BillingError.ServiceUnavailable
    "rate_limited" -> BillingError.RateLimited
    "input", "invalid_purchase" -> BillingError.InvalidRequest
    "unauthorized" -> BillingError.Unauthorized
    else -> when (http) {
        400 -> BillingError.InvalidRequest
        401 -> BillingError.Unauthorized
        403 -> BillingError.ProductNotAllowed
        409 -> BillingError.OwnedByOtherAccount
        429 -> BillingError.RateLimited
        502 -> BillingError.UpstreamUnavailable
        503 -> BillingError.ServiceUnavailable
        in 200..299 -> BillingError.None
        else -> BillingError.Unknown
    }
}

/** Purchase.PurchaseState values, kept as plain ints so tests need no Play classes. */
object ClientPurchaseState {
    const val UNSPECIFIED = 0
    const val PURCHASED = 1
    const val PENDING = 2
}

/**
 * A Play purchase reduced to plain data. The token is deliberately absent from [toString]: a
 * purchase token must never reach a log, an exception message or Analytics.
 */
class ClientPurchase(
    val state: Int,
    val purchaseToken: String,
    val productId: String?,
    val suspended: Boolean,
    /** Play's sub code for the purchases-updated callback; no other callback has one. */
    val subResponseCode: Int = PlaySubResponseCode.NoApplicable,
) {
    val isPurchased: Boolean get() = state == ClientPurchaseState.PURCHASED
    val isPending: Boolean get() = state == ClientPurchaseState.PENDING
    val tokenDigest: String get() = tokenDigest(purchaseToken)
    val subReason: BillingSubReason get() = billingSubReasonOf(subResponseCode)

    override fun toString(): String =
        "ClientPurchase(state=$state, productId=$productId, suspended=$suspended, token=redacted)"
}

/** Purchase states the client never treats as owned access on its own. */
fun isUnspecifiedPurchaseState(state: Int): Boolean = state == ClientPurchaseState.UNSPECIFIED

/** SHA-256 used only as an in-process de-duplication key; never sent, never logged as a secret. */
fun tokenDigest(token: String): String {
    val bytes = MessageDigest.getInstance("SHA-256").digest(token.toByteArray())
    return bytes.joinToString("") { (it.toInt() and 0xFF).toString(16).padStart(2, '0') }
}

/**
 * The whole `/verify` body: exactly two keys. Anything else the client knows about the purchase is
 * Google's or the server's answer, never the client's claim, and the route rejects extra keys.
 */
fun verifyRequestBody(productId: String, purchaseToken: String): String =
    JsonLite.stringify(
        linkedMapOf<String, Any?>(
            "productId" to productId,
            "purchaseToken" to purchaseToken,
        )
    )




