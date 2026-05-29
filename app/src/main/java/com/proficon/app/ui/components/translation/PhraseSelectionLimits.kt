package com.proficon.app.ui.components.translation

import androidx.compose.ui.text.TextRange

object PhraseSelectionLimits {
    const val MAX_WORDS = 50

    fun wordCount(text: String): Int =
        text.trim()
            .split(Regex("\\s+"))
            .count { it.isNotEmpty() }

    fun clampToMaxWords(text: String, maxWords: Int = MAX_WORDS): String {
        val trimmed = text.trim().replace(Regex("\\s+"), " ")
        if (trimmed.isEmpty()) return ""
        val words = trimmed.split(' ')
        return if (words.size <= maxWords) {
            trimmed
        } else {
            words.take(maxWords).joinToString(" ")
        }
    }

    fun clampSelectionRange(text: String, selection: TextRange, maxWords: Int = MAX_WORDS): TextRange {
        if (selection.collapsed) return selection
        val start = selection.start.coerceIn(0, text.length)
        val end = selection.end.coerceIn(start, text.length)
        if (start == end) return TextRange(start, end)

        var words = 0
        var cursor = start
        var lastWordEnd = start
        val wordRegex = Regex("\\S+")
        while (cursor < end && words < maxWords) {
            val match = wordRegex.find(text, cursor) ?: break
            if (match.range.first >= end) break
            words++
            lastWordEnd = match.range.last + 1
            cursor = lastWordEnd
        }
        return TextRange(start, lastWordEnd.coerceIn(start, text.length))
    }

    fun extractPhrase(text: String, selection: TextRange): String {
        if (selection.collapsed || selection.start < 0 || selection.end > text.length) {
            return ""
        }
        val clamped = clampSelectionRange(text, selection)
        return text.substring(clamped.start, clamped.end)
            .trim()
            .replace(Regex("\\s+"), " ")
            .takeIf { it.any(Char::isLetter) }
            .orEmpty()
            .let { clampToMaxWords(it) }
    }
}
