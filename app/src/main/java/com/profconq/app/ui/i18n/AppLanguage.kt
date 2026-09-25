package com.profconq.app.ui.i18n

enum class AppLanguage(val storageCode: Int) {
    RU(0),
    EN(1),
    PT(2),
    ;

    fun displayName(ui: AppLanguage = this): String = when (ui) {
        RU -> when (this) {
            RU -> "Русский"
            EN -> "Английский"
            PT -> "Португальский"
        }
        EN -> when (this) {
            RU -> "Russian"
            EN -> "English"
            PT -> "Portuguese"
        }
        PT -> when (this) {
            RU -> "Russo"
            EN -> "Inglês"
            PT -> "Português"
        }
    }

    companion object {
        fun fromStorage(value: Int?): AppLanguage =
            entries.find { it.storageCode == value } ?: DEFAULT

        val DEFAULT: AppLanguage = PT
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

    fun label(value: Int, ui: AppLanguage): String = when (fromStorage(value)) {
        EN -> AppLanguage.EN.displayName(ui)
        RU -> AppLanguage.RU.displayName(ui)
        else -> AppLanguage.PT.displayName(ui)
    }

    val all = listOf(PT, EN, RU)
}
