package com.profconq.app.youtube

object SubtitlePhraseExtractor {
    private val phraseBoundaries = setOf(
        '.', ',', '!', '?', ';', ':', '—', '–', '-',
        '"', '\'', '(', ')', '«', '»', '…',
    )

    fun extractAroundWord(text: String, wordStart: Int, wordEnd: Int): String {
        if (text.isEmpty() || wordStart < 0 || wordEnd > text.length || wordStart >= wordEnd) {
            return text.trim()
        }

        var left = wordStart
        while (left > 0 && text[left - 1] !in phraseBoundaries) {
            left--
        }

        var right = wordEnd
        while (right < text.length && text[right] !in phraseBoundaries) {
            right++
        }

        return text.substring(left, right).trim()
    }

    fun extractAroundWord(text: String, cleanWord: String): String {
        if (cleanWord.isBlank()) return text.trim()

        val lowerText = text.lowercase()
        val lowerWord = cleanWord.lowercase()
        var searchFrom = 0

        while (searchFrom < text.length) {
            val index = lowerText.indexOf(lowerWord, searchFrom)
            if (index < 0) break

            val end = index + cleanWord.length
            val beforeOk = index == 0 || !text[index - 1].isLetter()
            val afterOk = end >= text.length || !text[end].isLetter()
            if (beforeOk && afterOk) {
                return extractAroundWord(text, index, end)
            }
            searchFrom = index + 1
        }

        return cleanWord
    }
}
