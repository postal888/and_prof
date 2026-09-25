package com.profconq.app.analytics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Проверяется только политика wrapper'а: что уходит в Firebase Analytics, а что нет.
 * Сам вызов Firebase здесь не участвует — он есть только в объекте ProfconqAnalytics.
 */
class ProfconqAnalyticsTest {

    private val serverUuid = "3f2b7c1e-9d4a-4b6f-8a2c-51de7b0f4a93"

    @Test
    fun validServerUuid_isAccepted() {
        assertEquals(serverUuid, normalizeServerUserId(serverUuid))
    }

    @Test
    fun serverUuid_isNormalizedToLowerCase() {
        assertEquals(serverUuid, normalizeServerUserId(serverUuid.uppercase()))
    }

    @Test
    fun serverUuid_withSurroundingSpaces_isTrimmed() {
        assertEquals(serverUuid, normalizeServerUserId("  $serverUuid  "))
    }

    @Test
    fun email_isRejected() {
        assertNull(normalizeServerUserId("student@example.com"))
    }

    @Test
    fun anyValueWithAtSign_isRejected() {
        assertNull(normalizeServerUserId("$serverUuid@example.com"))
        assertNull(normalizeServerUserId("a@b"))
    }

    @Test
    fun hostLikeValue_isRejected() {
        assertNull(normalizeServerUserId("smtp.example.com"))
    }

    @Test
    fun firebaseUid_isRejectedBecauseItIsNotAServerUuid() {
        // Следствие правила «только серверный UUID»: вход через Google не даёт user_id.
        assertNull(normalizeServerUserId("Kq0YgVxTfPhOgQ2mZbC1dReFgh89"))
    }

    @Test
    fun truncatedOrOversizedValue_isRejected() {
        assertNull(normalizeServerUserId(serverUuid.substring(0..30)))
        assertNull(normalizeServerUserId("f".repeat(300)))
    }

    @Test
    fun blankOrMissingValue_clearsUserId() {
        assertNull(normalizeServerUserId(null))
        assertNull(normalizeServerUserId(""))
        assertNull(normalizeServerUserId("   "))
    }

    @Test
    fun plan_hasOnlyFreeOrPremium() {
        assertEquals(ProfconqAnalytics.PLAN_PREMIUM, planValue(true))
        assertEquals(ProfconqAnalytics.PLAN_FREE, planValue(false))
        assertTrue(setOf(planValue(true), planValue(false)) == setOf("premium", "free"))
    }

    @Test
    fun planWithoutServerAnswer_removesTheProperty() {
        assertNull(planValue(null))
    }

    @Test
    fun uiLanguage_normalizesToSupportedCodes() {
        assertEquals("ru", normalizeUiLanguage(0))
        assertEquals("en", normalizeUiLanguage(1))
        assertEquals("pt", normalizeUiLanguage(2))
    }

    /** Как именно i18n выбирает fallback — вне зоны ответственности аналитики; важно, что значение остаётся из разрешённого набора. */
    @Test
    fun unknownUiLanguageCode_stillYieldsASupportedCode() {
        val supported = setOf("ru", "en", "pt")
        assertTrue(supported.contains(normalizeUiLanguage(null)))
        assertTrue(supported.contains(normalizeUiLanguage(99)))
        assertTrue(supported.contains(normalizeUiLanguage(-1)))
    }

    @Test
    fun smokeTest_isUnavailableOnTheReleasePath() {
        assertFalse(isSmokeTestEnabled(debugBuild = false))
        assertTrue(isSmokeTestEnabled(debugBuild = true))
    }

    @Test
    fun collection_staysDisabledOnTheReleasePath() {
        assertFalse(isCollectionAllowed(debugBuild = false))
        assertTrue(isCollectionAllowed(debugBuild = true))
    }

    @Test
    fun analyticsEventNamesAndPropertiesAreStable() {
        assertEquals("analytics_smoke_test", ProfconqAnalytics.EVENT_SMOKE_TEST)
        assertEquals("plan", ProfconqAnalytics.USER_PROP_PLAN)
        assertEquals("ui_language", ProfconqAnalytics.USER_PROP_UI_LANGUAGE)
    }
}
