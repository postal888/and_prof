package com.profconq.app.ui.i18n

import com.profconq.app.billing.BillingNotice
import com.profconq.app.billing.PremiumStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The premium section ships in all three app languages, so every state and every notice has to
 * resolve to real text in each of them. A missing branch would fall through to another language
 * silently, which is exactly what these checks pin down.
 */
class PremiumStringsTest {

    private val languages = listOf(AppLanguage.RU, AppLanguage.EN, AppLanguage.PT)

    @Test
    fun everyPremiumStatusHasItsOwnTextInAllLanguages() {
        for (language in languages) {
            val strings = UiStrings.forLanguage(language)
            val texts = PremiumStatus.values().map { status ->
                val text = strings.premiumStatusText(status)
                assertTrue("$language/$status is blank", text.isNotBlank())
                assertFalse("$language/$status looks untranslated", text.startsWith("TODO"))
                text
            }
            assertEquals(
                "$language must not reuse a status label",
                texts.size,
                texts.distinct().size,
            )
        }
    }

    @Test
    fun everyBillingNoticeHasTextInAllLanguages() {
        // PendingVerification is deliberately shown as the upstream answer: the server could not
        // confirm yet, which is the one thing the user can act on. Generic is the baseline itself.
        val aliases = setOf(BillingNotice.PendingVerification)
        for (language in languages) {
            val strings = UiStrings.forLanguage(language)
            val texts = BillingNotice.values().map { notice ->
                val text = strings.premiumNoticeText(notice)
                assertTrue("$language/$notice is blank", text.isNotBlank())
                if (notice !in aliases && notice != BillingNotice.Generic) {
                    assertNotEquals(
                        "$language/$notice must not read as the generic failure",
                        strings.premiumNoticeGeneric,
                        text,
                    )
                }
                text
            }
            assertEquals(
                "$language must not reuse a notice text",
                texts.size - aliases.size,
                texts.distinct().size,
            )
        }
    }

    @Test
    fun russianUsesCyrillicAndTheOtherLanguagesNeverDo() {
        for (language in languages) {
            val strings = UiStrings.forLanguage(language)
            val texts = premiumTexts(strings)
            val hasCyrillic = texts.any { it.any { char -> char in 'а'..'ѳ' || char in 'А'..'Ѳ' } }
            if (language == AppLanguage.RU) {
                assertTrue("Russian premium text lost its Cyrillic", hasCyrillic)
            } else {
                assertFalse("$language contains Cyrillic", hasCyrillic)
            }
        }
    }

    @Test
    fun theThreeLanguagesAreActuallyDifferentTexts() {
        val ru = premiumTexts(UiStrings.forLanguage(AppLanguage.RU))
        val en = premiumTexts(UiStrings.forLanguage(AppLanguage.EN))
        val pt = premiumTexts(UiStrings.forLanguage(AppLanguage.PT))
        assertEquals(ru.size, en.size)
        assertEquals(ru.size, pt.size)
        assertNotEquals("RU and EN hold the same texts", ru, en)
        assertNotEquals("PT and EN hold the same texts", pt, en)
        assertTrue("RU and EN share too much", ru.zip(en).count { (a, b) -> a == b } <= 1)
        assertTrue("PT and EN share too much", pt.zip(en).count { (a, b) -> a == b } <= 2)
    }

    @Test
    fun theExpiryLineCarriesTheServerValue() {
        for (language in languages) {
            val value = "2026-10-01"
            val line = UiStrings.forLanguage(language).premiumExpiresAt(value)
            assertTrue("$language drops the expiry value: $line", line.contains(value))
        }
    }

    private fun premiumTexts(strings: UiStrings): List<String> = listOf(
        strings.premiumSectionTitle,
        strings.premiumHint,
        strings.premiumStatusLoading,
        strings.premiumStatusActive,
        strings.premiumStatusStale,
        strings.premiumStatusPending,
        strings.premiumStatusVerifying,
        strings.premiumStatusFree,
        strings.premiumStatusUnavailable,
        strings.premiumStatusRestoring,
        strings.premiumStatusRestoring,
        strings.premiumAutoRenewsOn,
        strings.premiumAutoRenewsOff,
        strings.premiumBuy,
        strings.premiumRestore,
        strings.premiumSignInFirst,
        strings.premiumOpen,
        strings.premiumNoPlans,
        strings.premiumNoticeSignedOut,
        strings.premiumNoticeCanceled,
        strings.premiumNoticeRestoring,
        strings.premiumNoticePlanUnavailable,
        strings.premiumNoticeActivityUnavailable,
        strings.premiumNoticeBillingUnavailable,
        strings.premiumNoticeInvalidRequest,
        strings.premiumNoticeNetwork,
        strings.premiumNoticeUnauthorized,
        strings.premiumNoticeOwnedByOther,
        strings.premiumNoticeRateLimited,
        strings.premiumNoticeUpstream,
        strings.premiumNoticeServiceUnavailable,
        strings.premiumNoticeVerified,
        strings.premiumNoticeNothingToRestore,
        strings.premiumNoticeGeneric,
    ) + PremiumStatus.values().map { strings.premiumStatusText(it) } +
        BillingNotice.values().map { strings.premiumNoticeText(it) }
}
