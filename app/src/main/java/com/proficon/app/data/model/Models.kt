package com.proficon.app.data.model

import com.proficon.app.reader.ReaderFontSize

data class WordCard(
    val id: String,
    val collectionId: String,
    val pt: String,
    val ru: String,
    val example: String? = null,
    val due: Boolean = true,
    val known: Boolean = false,
    val imagePath: String? = null,
    val audioPath: String? = null,
)

data class Collection(
    val id: String,
    val title: String,
    val description: String? = null,
    val videoId: String? = null,
    val sourceType: String? = null,
    val folderAudioPath: String? = null,
    val cards: List<WordCard> = emptyList(),
)

data class TodayPlan(
    val dueCount: Int,
    val newCount: Int,
    val streak: Int,
)

data class DictionaryEntry(
    val id: String,
    val pt: String,
    val ru: String,
    val example: String? = null,
    val collectionId: String? = null,
    val videoId: String? = null,
    val addedAt: Long = 0L,
)

data class ReaderBook(
    val id: String,
    val title: String,
    val content: String,
    val sourceUri: String? = null,
    val scrollParagraph: Int = 0,
    val bookmarkParagraph: Int? = null,
    val addedAt: Long = System.currentTimeMillis(),
)

data class AppSettings(
    val useChatGptTranslation: Boolean = true,
    val phraseCopyEnabled: Boolean = true,
    /** When true, tapping a word saves the surrounding phrase (between punctuation) as card example. */
    val wordContextExampleEnabled: Boolean = true,
    val themeMode: AppThemeMode = AppThemeMode.Dark,
    val subtitleFontSizeLevel: Int = SubtitleFontSize.DEFAULT_LEVEL,
    val readerFontSizeLevel: Int = ReaderFontSize.DEFAULT_LEVEL,
    val uiLanguage: Int = 0,
    val subtitleLanguage: Int = 0,
    val readerLineSpacingPercent: Int = ReaderLineSpacing.DEFAULT_PERCENT,
)

object SubtitleFontSize {
    const val MIN_LEVEL = -2
    const val MAX_LEVEL = 2
    const val DEFAULT_LEVEL = 0

    fun scaleForLevel(level: Int): Float = when (level.coerceIn(MIN_LEVEL, MAX_LEVEL)) {
        -2 -> 0.85f
        -1 -> 0.92f
        0 -> 1.0f
        1 -> 1.12f
        2 -> 1.25f
        else -> 1.0f
    }

    fun labelForLevel(level: Int): String = when (level.coerceIn(MIN_LEVEL, MAX_LEVEL)) {
        -2 -> "Очень мелко"
        -1 -> "Мелко"
        0 -> "Обычный"
        1 -> "Крупно"
        2 -> "Очень крупно"
        else -> "Обычный"
    }
}

enum class SubtitleCursorMode {
    Tap,
    Select,
}

object ReaderLineSpacing {
    const val MIN_PERCENT = 90
    const val MAX_PERCENT = 220
    const val DEFAULT_PERCENT = 135
}
