package com.profconq.app.ui.i18n

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/**
 * The profile tagline is the one line the app shows about the whole product, so it reads as written
 * in the language it is shown in: the English line had been left in another language's capitalisation
 * and wording, while the Russian and Portuguese lines were already agreed and stay as they are.
 */
class ProfileTaglineStringsTest {

    @Test
    fun englishTaglineIsSentenceCaseAndConquered() {
        assertEquals("Proficiency conquered", UiStrings.forLanguage(AppLanguage.EN).profileTagline)
    }

    @Test
    fun russianAndPortugueseTaglinesAreUnchanged() {
        assertEquals("Proficiência Conquistada", UiStrings.forLanguage(AppLanguage.RU).profileTagline)
        assertEquals("Proficiência Conquistada", UiStrings.forLanguage(AppLanguage.PT).profileTagline)
    }

    @Test
    fun noLanguageStillCarriesTheOldWord() {
        for (language in listOf(AppLanguage.RU, AppLanguage.EN, AppLanguage.PT)) {
            val text = UiStrings.forLanguage(language).profileTagline
            assertFalse("$language must not say the old thing", text.contains("achieved", ignoreCase = true))
        }
    }
}
