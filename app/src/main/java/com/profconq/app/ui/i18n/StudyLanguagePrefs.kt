package com.profconq.app.ui.i18n

import androidx.compose.runtime.compositionLocalOf

data class StudyLanguagePrefs(
    val source: Int = SubtitleLanguage.PT,
    val target: Int = SubtitleLanguage.RU,
)

val LocalStudyLanguagePrefs = compositionLocalOf { StudyLanguagePrefs() }
