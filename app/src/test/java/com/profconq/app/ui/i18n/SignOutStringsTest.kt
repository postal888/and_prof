package com.profconq.app.ui.i18n

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Sign-out is the one destructive-looking action Profile offers, so its wording is pinned here in
 * all three app languages: the label has to name the account it releases, and the dialog has to
 * promise that the words on the device survive it.
 */
class SignOutStringsTest {

    private val languages = listOf(AppLanguage.RU, AppLanguage.EN, AppLanguage.PT)

    @Test
    fun theLabelNamesTheAccountBeingReleased() {
        assertEquals(
            "Выйти из аккаунта",
            UiStrings.forLanguage(AppLanguage.RU).profileSignOut,
        )
        assertEquals("Sign out", UiStrings.forLanguage(AppLanguage.EN).profileSignOut)
        assertEquals("Sair da conta", UiStrings.forLanguage(AppLanguage.PT).profileSignOut)
    }

    @Test
    fun theDialogTitleIsTheLabelAskedBack() {
        for (language in languages) {
            val strings = UiStrings.forLanguage(language)
            assertEquals(
                "$language dialog title must be the confirmed action as a question",
                "${strings.profileSignOut}?",
                strings.signOutDialogTitle,
            )
        }
    }

    @Test
    fun theDialogBodyKeepsTheLocalData() {
        // The reassurance names the device in the language the user reads the app in.
        val deviceWord = mapOf(
            AppLanguage.RU to "устройств",
            AppLanguage.EN to "device",
            AppLanguage.PT to "dispositiv",
        )
        for (language in languages) {
            val strings = UiStrings.forLanguage(language)
            val body = strings.signOutDialogBody
            assertTrue("$language body is blank", body.isNotBlank())
            assertTrue(
                "$language body does not say the data stays on the device: $body",
                body.contains(deviceWord.getValue(language)),
            )
            assertNotEquals("$language body must not repeat the title", strings.signOutDialogTitle, body)
        }
        val bodies = languages.map { UiStrings.forLanguage(it).signOutDialogBody }
        assertEquals(bodies.size, bodies.distinct().size)
    }

    @Test
    fun theFailureNoticeAndTheCancelActionExistInEveryLanguage() {
        for (language in languages) {
            val strings = UiStrings.forLanguage(language)
            assertTrue("$language sign-out failure text is blank", strings.signOutFailed.isNotBlank())
            assertTrue("$language cancel text is blank", strings.cancel.isNotBlank())
            // A failed sign-out must not read like the button that triggered it.
            assertNotEquals(strings.profileSignOut, strings.signOutFailed)
        }
    }

    @Test
    fun russianUsesCyrillicAndTheOtherLanguagesNeverDo() {
        for (language in languages) {
            val texts = signOutTexts(UiStrings.forLanguage(language))
            val hasCyrillic = texts.any { line -> line.any { char -> char in 'а'..'ѳ' || char in 'А'..'Ѳ' } }
            if (language == AppLanguage.RU) {
                assertTrue("Russian sign-out text lost its Cyrillic", hasCyrillic)
            } else {
                assertFalse("$language contains Cyrillic", hasCyrillic)
            }
        }
    }

    @Test
    fun everyTextIsTranslatedForItsLanguage() {
        val englishLabel = UiStrings.forLanguage(AppLanguage.EN).profileSignOut
        for (language in listOf(AppLanguage.RU, AppLanguage.PT)) {
            signOutTexts(UiStrings.forLanguage(language)).forEach { text ->
                assertTrue("$language has a blank sign-out text", text.isNotBlank())
                assertFalse("$language text looks untranslated: $text", text.startsWith("TODO"))
                assertFalse("$language borrows the English label", text == englishLabel)
            }
        }
    }

    @Test
    fun theThreeLanguagesAreActuallyDifferentTexts() {
        val ru = signOutTexts(UiStrings.forLanguage(AppLanguage.RU))
        val en = signOutTexts(UiStrings.forLanguage(AppLanguage.EN))
        val pt = signOutTexts(UiStrings.forLanguage(AppLanguage.PT))
        assertEquals(ru.size, en.size)
        assertEquals(ru.size, pt.size)
        assertNotEquals("RU and EN hold the same texts", ru, en)
        assertNotEquals("PT and EN hold the same texts", pt, en)
        assertEquals("Only one text may be shared between RU and EN", 0, ru.zip(en).count { (a, b) -> a == b })
        assertEquals("Only one text may be shared between PT and EN", 0, pt.zip(en).count { (a, b) -> a == b })
    }

    private fun signOutTexts(strings: UiStrings): List<String> = listOf(
        strings.profileSignOut,
        strings.signOutDialogTitle,
        strings.signOutDialogBody,
        strings.signOutFailed,
        strings.cancel,
    )
}
