package com.profconq.app.data.model

data class StudySet(
    val id: String,
    val name: String,
    val createdAt: Long,
    val lastPracticedAt: Long? = null,
    val wordCount: Int,
    val newCount: Int = 0,
    val dueCount: Int = 0,
    val masteredCount: Int = 0,
    /** Words that are not green (known) — Studio does not take the whole deck. */
    val studioCount: Int = 0,
) {
    val reviewableCount: Int get() = newCount + dueCount
    val canStartSession: Boolean get() = reviewableCount > 0
    val cardsCount: Int get() = if (reviewableCount > 0) reviewableCount else wordCount
    val testCount: Int get() = wordCount
}

/** Word in a study set — same fields as dictionary card row. */
typealias VocabularyWord = WordCard
