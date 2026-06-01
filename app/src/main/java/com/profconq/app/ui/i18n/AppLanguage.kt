package com.profconq.app.ui.i18n

enum class AppLanguage(val storageCode: Int) {
    RU(0),
    EN(1),
    PT(2),
    ;

    companion object {
        fun fromStorage(value: Int?): AppLanguage =
            entries.find { it.storageCode == value } ?: EN
    }
}

object SubtitleLanguage {
    const val PT = 0
    const val EN = 1
    const val RU = 2

    fun fromStorage(value: Int?): Int = value?.coerceIn(PT, RU) ?: PT

    fun toCode(value: Int): String = when (fromStorage(value)) {
        EN -> "en"
        RU -> "ru"
        else -> "pt"
    }

    fun shortCode(value: Int): String = when (fromStorage(value)) {
        EN -> "EN"
        RU -> "RU"
        else -> "PT"
    }

    fun promptLanguageName(value: Int): String = when (fromStorage(value)) {
        EN -> "English"
        RU -> "Russian"
        else -> "Portuguese"
    }

    fun label(value: Int, ui: AppLanguage): String = when (ui) {
        AppLanguage.RU -> when (value) {
            EN -> "English"
            RU -> "Русский"
            else -> "Português"
        }
        AppLanguage.EN -> when (value) {
            EN -> "English"
            RU -> "Russian"
            else -> "Portuguese"
        }
        AppLanguage.PT -> when (value) {
            EN -> "English"
            RU -> "Russo"
            else -> "Português"
        }
    }

    val all = listOf(PT, EN, RU)
}
