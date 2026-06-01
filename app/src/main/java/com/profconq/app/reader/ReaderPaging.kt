package com.profconq.app.reader

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.profconq.app.data.model.ReaderLineSpacing
import com.profconq.app.data.model.SubtitleFontSize

object ReaderPaging {
    const val PARAGRAPHS_PER_PAGE = 10
    const val BUFFER_PAGES = 10
}

/**
 * Reader font: 9 levels (-4…4), linear from old max (1.25×) to 3× that (3.75×).
 */
object ReaderFontSize {
    const val MIN_LEVEL = -4
    const val MAX_LEVEL = 4
    const val DEFAULT_LEVEL = 0

    private const val BASE_FONT_SP = 13f
    private const val BASE_LINE_HEIGHT_SP = 18f

    private val minScale: Float =
        SubtitleFontSize.scaleForLevel(SubtitleFontSize.MAX_LEVEL)
    private val maxScale: Float = minScale * 3f

    fun scaleForLevel(level: Int): Float {
        val clamped = level.coerceIn(MIN_LEVEL, MAX_LEVEL)
        val step = (clamped - MIN_LEVEL).toFloat() / (MAX_LEVEL - MIN_LEVEL)
        return minScale + step * (maxScale - minScale)
    }

    fun fontSizeForLevel(level: Int): TextUnit =
        (BASE_FONT_SP * scaleForLevel(level)).sp

    fun lineHeightForLevel(level: Int, lineSpacingPercent: Int): TextUnit {
        val spacingScale = lineSpacingPercent.coerceIn(
            ReaderLineSpacing.MIN_PERCENT,
            ReaderLineSpacing.MAX_PERCENT,
        ) / 100f
        val lineScale = scaleForLevel(level) / minScale
        return (BASE_LINE_HEIGHT_SP * minScale * lineScale * spacingScale).sp
    }

    /** Matches [lineHeightForLevel] minus glyph height — same rhythm as BasicTextField in select mode. */
    fun flowRowLineGap(fontSizeLevel: Int, lineSpacingPercent: Int): Dp {
        val fontSizeSp = fontSizeForLevel(fontSizeLevel).value
        val lineHeightSp = lineHeightForLevel(fontSizeLevel, lineSpacingPercent).value
        return (lineHeightSp - fontSizeSp * 0.92f).coerceAtLeast(2f).dp
    }

    fun labelForLevel(level: Int): String {
        val percent = (scaleForLevel(level) / minScale * 100f).toInt()
        return "$percent%"
    }

    /** Maps legacy subtitle slider (-2…2) to reader scale (-4…4). */
    fun fromSubtitleLevel(subtitleLevel: Int): Int {
        val s = subtitleLevel.coerceIn(SubtitleFontSize.MIN_LEVEL, SubtitleFontSize.MAX_LEVEL)
        return (s * 2).coerceIn(MIN_LEVEL, MAX_LEVEL)
    }
}
