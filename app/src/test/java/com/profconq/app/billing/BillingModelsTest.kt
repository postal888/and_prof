package com.profconq.app.billing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Server-contract tests: the shapes the billing endpoints answer with, the fixed request body,
 * and the status-to-error mapping. No Android types are involved on purpose.
 */
class BillingModelsTest {

    @Test
    fun productsParsesServerCatalogue() {
        val catalogue = BillingCatalogue.parse(
            """
            {"products":[{"productId":"profconq_premium","basePlans":[
              {"basePlanId":"monthly","recurrence":"P1M"},
              {"basePlanId":"annual","recurrence":"P1Y"}]}],
             "packageName":"com.profconq.app"}
            """.trimIndent()
        )
        assertEquals("com.profconq.app", catalogue.packageName)
        assertEquals(listOf("monthly", "annual"), catalogue.premiumPlans.map { it.basePlanId })
        assertTrue(catalogue.allows("monthly"))
        assertTrue(catalogue.allows("annual"))
    }

    @Test
    fun statusParsesFreeAndStaleAndEmptyShapes() {
        val free = PremiumSnapshot.parse(
            """{"plan":"free","wordLimit":10,"entitlements":[],"governing":null,"stale":false,"checkedAt":1700000000000}"""
        )
        assertFalse(free.isPremium)
        assertEquals(10, free.wordLimit)
        assertTrue(free.entitlements.isEmpty())
        assertNull(free.governing)
        assertFalse(free.stale)

        val stale = PremiumSnapshot.parse(
            """{"plan":"premium","wordLimit":null,"entitlements":[],"governing":null,"stale":true,"checkedAt":1}"""
        )
        assertTrue(stale.isPremium)
        assertTrue("stale must not read as lost access", stale.stale)
        assertNull("JSON null wordLimit means unlimited", stale.wordLimit)
    }

    @Test
    fun verifyResponseParsesEntitlementsAndGovernning() {
        val snapshot = PremiumSnapshot.parse(
            """
            {"plan":"premium","wordLimit":null,
             "entitlements":[{"id":"gpe_1","source":"google_play","productId":"profconq_premium",
               "basePlanId":"monthly","status":"active","startedAt":"2026-09-01T00:00:00Z",
               "expiresAt":"2026-10-01T00:00:00Z","autoRenews":true,"lastVerifiedAt":"2026-09-20T00:00:00Z"}],
             "governing":{"id":"gpe_1","source":"google_play","productId":"profconq_premium",
               "basePlanId":"monthly","status":"active","expiresAt":"2026-10-01T00:00:00Z"},
             "wordCount":42,"renewed":true,"expiresAt":"2026-10-01T00:00:00Z","state":"active"}
            """.trimIndent()
        )
        assertTrue(snapshot.isPremium)
        assertEquals(1, snapshot.entitlements.size)
        val entry = snapshot.entitlements.first()
        assertEquals("gpe_1", entry.id)
        assertEquals("google_play", entry.source)
        assertEquals("monthly", entry.basePlanId)
        assertTrue(entry.autoRenews)
        assertNotNull(snapshot.governing)
        assertEquals("active", snapshot.governingStatus)
        assertEquals(42, snapshot.wordCount)
        assertEquals(true, snapshot.renewed)
        assertEquals("2026-10-01T00:00:00Z", snapshot.expiresAt)
    }

    @Test
    fun everyServerStateParsesWithoutPremiumInvention() {
        for (state in PremiumSnapshot.knownStates) {
            val premium = state == "active" || state == "canceled_but_active" || state == "in_grace_period"
            val snapshot = PremiumSnapshot.parse(
                """{"plan":"${if (premium) "premium" else "free"}","wordLimit":${if (premium) "null" else "10"},""" +
                    """"entitlements":[{"id":"gpe_1","source":"google_play","productId":"profconq_premium",""" +
                    """"basePlanId":"monthly","status":"$state","startedAt":null,"expiresAt":null,""" +
                    """"autoRenews":false,"lastVerifiedAt":null}],"governing":null,"stale":false,"checkedAt":1}"""
            )
            assertEquals(state, premium, snapshot.isPremium)
            assertEquals(state, snapshot.entitlements.single().status)
        }
    }

    @Test
    fun verifyBodyCarriesExactlyTwoKeys() {
        val body = verifyRequestBody("profconq_premium", "tok=abc\"def")
        val parsed = JsonLite.parseObject(body)
        assertEquals(listOf("productId", "purchaseToken"), parsed.keys.toList())
        assertEquals("profconq_premium", parsed["productId"])
        assertEquals("tok=abc\"def", parsed["purchaseToken"])
        // Nothing the server must not take from the client may appear in the body.
        for (forbidden in listOf("basePlanId", "offerToken", "orderId", "plan", "expiresAt", "price", "autoRenewing")) {
            assertFalse("$forbidden leaked", body.contains(forbidden))
        }
    }

    @Test
    fun httpCodesMapToDocumentedErrors() {
        assertEquals(BillingError.InvalidRequest, billingErrorOf(400))
        assertEquals(BillingError.Unauthorized, billingErrorOf(401))
        assertEquals(BillingError.ProductNotAllowed, billingErrorOf(403))
        assertEquals(BillingError.OwnedByOtherAccount, billingErrorOf(409))
        assertEquals(BillingError.RateLimited, billingErrorOf(429))
        assertEquals(BillingError.UpstreamUnavailable, billingErrorOf(502))
        assertEquals(BillingError.ServiceUnavailable, billingErrorOf(503))
        assertEquals(BillingError.None, billingErrorOf(200))
        assertEquals(BillingError.Unknown, billingErrorOf(520))
        assertEquals(BillingError.OwnedByOtherAccount, billingErrorOf(409, "token_owned_by_other_account"))
        assertEquals(BillingError.UpstreamUnavailable, billingErrorOf(502, "google_unavailable"))
        assertEquals(BillingError.InvalidRequest, billingErrorOf(400, "input"))
    }

    @Test
    fun catalogueFilterKeepsOnlyThePremiumProductAndKnownPlans() {
        val catalogue = BillingCatalogue.parse(
            """
            {"products":[
              {"productId":"profconq_premium","basePlans":[
                {"basePlanId":"monthly","recurrence":"P1M"},
                {"basePlanId":"annual","recurrence":"P1Y"},
                {"basePlanId":"weekly","recurrence":"P7D"},
                {"basePlanId":"signup_offer","recurrence":null}]},
              {"productId":"other_product","basePlans":[{"basePlanId":"monthly","recurrence":"P1M"}]}]}
            """.trimIndent()
        )
        assertEquals(1, catalogue.products.size)
        assertEquals(BillingContract.PREMIUM_PRODUCT_ID, catalogue.products.single().productId)
        assertEquals(listOf("monthly", "annual"), catalogue.premiumPlans.map { it.basePlanId })
        assertFalse(catalogue.allows("weekly"))
        assertFalse(catalogue.allows("signup_offer"))
    }

    @Test
    fun purchaseTokenNeverAppearsInTexts() {
        val token = "super-secret-purchase-token"
        val purchase = ClientPurchase(
            state = ClientPurchaseState.PURCHASED,
            purchaseToken = token,
            productId = BillingContract.PREMIUM_PRODUCT_ID,
            suspended = false,
        )
        assertFalse(purchase.toString().contains(token))
        val digest = tokenDigest(token)
        assertEquals(64, digest.length)
        assertFalse(digest.contains(token))
        assertFalse(purchase.tokenDigest.contains(token))
        // The play-sheet payload is a secret too, and it must not ride along in a log line either.
        val plan = PlanView(
            basePlanId = BillingContract.BASE_PLAN_MONTHLY,
            title = "Profconq Premium",
            price = "9,99 ₽",
            offerToken = token,
        )
        assertFalse(plan.toString().contains(token))
        for (text in listOf(purchase.toString(), plan.toString(), digest)) {
            assertFalse("a raw secret leaked into $text", text.contains(token))
        }
    }
}
