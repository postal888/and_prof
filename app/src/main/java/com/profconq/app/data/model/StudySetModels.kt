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
) {
    val reviewableCount: Int get() = newCount + dueCount
    val canStartSession: Boolean get() = reviewableCount > 0
}

/** Word in a study set — same fields as dictionary card row. */
typealias VocabularyWord = WordCard
