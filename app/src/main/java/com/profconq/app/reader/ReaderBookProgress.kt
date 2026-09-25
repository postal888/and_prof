package com.profconq.app.reader

import com.profconq.app.data.model.ReaderBook
import com.profconq.app.ui.components.translation.splitBookParagraphs

fun ReaderBook.readingProgressFraction(): Float {
    if (content.isBlank()) return 0f
    val paragraphCount = splitBookParagraphs(content).size.coerceAtLeast(1)
    val totalPages = readerPageCount(paragraphCount)
    val currentPage = (scrollParagraph / ReaderPaging.PARAGRAPHS_PER_PAGE).coerceAtLeast(0)
    return ((currentPage + 1).toFloat() / totalPages).coerceIn(0f, 1f)
}

fun readerPageCount(paragraphCount: Int): Int =
    ((paragraphCount + ReaderPaging.PARAGRAPHS_PER_PAGE - 1) / ReaderPaging.PARAGRAPHS_PER_PAGE)
        .coerceAtLeast(1)

fun ReaderBook.readingProgressPercent(): Int =
    (readingProgressFraction() * 100).toInt()
