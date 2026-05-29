package com.proficon.app.ui.i18n

enum class AppLanguage(val storageCode: Int) {
    RU(0),
    EN(1),
    PT(2),
    ;

    companion object {
        fun fromStorage(value: Int?): AppLanguage =
            entries.find { it.storageCode == value } ?: RU
    }
}

object SubtitleLanguage {
    const val PT = 0
    const val EN = 1
    const val RU = 2
    const val ES = 3

    fun fromStorage(value: Int?): Int = value?.coerceIn(PT, ES) ?: PT

    fun toCode(value: Int): String = when (value.coerceIn(PT, ES)) {
        EN -> "en"
        RU -> "ru"
        ES -> "es"
        else -> "pt"
    }

    fun label(value: Int, ui: AppLanguage): String = when (ui) {
        AppLanguage.RU -> when (value) {
            EN -> "English"
            RU -> "Русский"
            ES -> "Español"
            else -> "Português"
        }
        AppLanguage.EN -> when (value) {
            EN -> "English"
            RU -> "Russian"
            ES -> "Spanish"
            else -> "Portuguese"
        }
        AppLanguage.PT -> when (value) {
            EN -> "English"
            RU -> "Russo"
            ES -> "Espanhol"
            else -> "Português"
        }
    }

    val all = listOf(PT, EN, RU, ES)
}
