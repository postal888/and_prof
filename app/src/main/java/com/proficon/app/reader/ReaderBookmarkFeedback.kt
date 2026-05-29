package com.proficon.app.reader

/** Short-lived UI notice after toggling a bookmark. [pageNumber] is null when removed. */
data class ReaderBookmarkFeedback(
    val pageNumber: Int?,
    val nonce: Long = System.currentTimeMillis(),
)
