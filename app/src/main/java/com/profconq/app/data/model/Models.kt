package com.profconq.app.data.model

import com.profconq.app.reader.ReaderFontSize
import com.profconq.app.ui.i18n.AppLanguage
import com.profconq.app.ui.i18n.SubtitleLanguage

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
    val audioPaths: List<String> = emptyList(),
    val audioLabels: Map<String, String> = emptyMap(),
    val imageUrl: String? = null,
    val partOfSpeech: String? = null,
    val ipa: String? = null,
    val sourceTitle: String? = null,
    val chapterOrTag: String? = null,
    val exampleTranslation: String? = null,
    val isFavorite: Boolean = false,
    val learnMark: String? = null,
) {
    /** Local file path or remote URL for the image slot. */
    val displayImage: String? get() = imageUrl ?: imagePath

    fun resolvedAudioPaths(): List<String> =
        audioPaths.ifEmpty { audioPath?.let(::listOf) ?: emptyList() }

    fun audioTitle(path: String, index: Int, defaultTitle: (Int) -> String): String =
        audioLabels[path]?.takeIf { it.isNotBlank() } ?: defaultTitle(index)
}

data class Collection(
    val id: String,
    val title: String,
    val description: String? = null,
    val videoId: String? = null,
    val sourceType: String? = null,
    val folderAudioPath: String? = null,
    val folderAudioPaths: List<String> = emptyList(),
    val folderAudioLabels: Map<String, String> = emptyMap(),
    val cards: List<WordCard> = emptyList(),
) {
    fun resolvedFolderAudioPaths(): List<String> =
        folderAudioPaths.ifEmpty { folderAudioPath?.let(::listOf) ?: emptyList() }

    fun folderAudioTitle(path: String, index: Int, defaultTitle: (Int) -> String): String =
        folderAudioLabels[path]?.takeIf { it.isNotBlank() } ?: defaultTitle(index)
}

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
    /** Keep YouTube audio playing when switching to other tabs. */
    val youtubeBackgroundPlayback: Boolean = false,
    /** When true, tapping a word saves the surrounding phrase (between punctuation) as card example. */
    val wordContextExampleEnabled: Boolean = true,
    val themeMode: AppThemeMode = AppThemeMode.Dark,
    val subtitleFontSizeLevel: Int = SubtitleFontSize.DEFAULT_LEVEL,
    val readerFontSizeLevel: Int = ReaderFontSize.DEFAULT_LEVEL,
    val uiLanguage: Int = AppLanguage.DEFAULT.storageCode,
    /** Language of tapped words / YouTube subtitle track (pt, en, ru, es). */
    val translationSourceLanguage: Int = SubtitleLanguage.PT,
    /** Translation stored on cards (pt, en, ru, es). */
    val translationTargetLanguage: Int = SubtitleLanguage.RU,
    val readerLineSpacingPercent: Int = ReaderLineSpacing.DEFAULT_PERCENT,
    val readerAutoScrollEnabled: Boolean = false,
    val readerAutoScrollSpeed: Int = ReaderAutoScroll.DEFAULT_SPEED,
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

object ReaderAutoScroll {
    const val MIN_SPEED = 1
    const val MAX_SPEED = 20
    const val DEFAULT_SPEED = 10

    private fun normalized(speed: Int): Float {
        val clamped = speed.coerceIn(MIN_SPEED, MAX_SPEED)
        return (clamped - MIN_SPEED).toFloat() / (MAX_SPEED - MIN_SPEED).toFloat()
    }

    fun speedPercent(speed: Int): Int {
        val clamped = speed.coerceIn(MIN_SPEED, MAX_SPEED)
        return if (MAX_SPEED == MIN_SPEED) {
            100
        } else {
            ((clamped - MIN_SPEED) * 100) / (MAX_SPEED - MIN_SPEED)
        }
    }

    fun tickDelayMs(speed: Int): Long {
        val t = normalized(speed)
        return (100L - (t * 82f).toLong()).coerceIn(18L, 100L)
    }

    fun scrollStepPx(speed: Int): Float {
        val t = normalized(speed)
        return 1.5f + t * 7.5f
    }
}
