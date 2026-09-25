package com.profconq.app.data.model

data class StudyTestWordStat(
    val wordId: String,
    val pt: String,
    val ru: String,
    val correct: Int,
    val incorrect: Int,
    val runs: Int = 0,
) {
    val attempts: Int get() = (correct + incorrect).coerceAtLeast(0)
    val accuracy: Int get() = if (attempts > 0) ((correct * 100) / attempts).coerceIn(0, 100) else 0
    val effectiveRuns: Int get() = if (runs > 0) runs else 1
}

data class LastStudyTestResult(
    val setId: String,
    val setName: String,
    val kind: String,
    val correct: Int,
    val incorrect: Int,
    val at: Long,
    val words: List<StudyTestWordStat> = emptyList(),
) {
    val total: Int get() = (correct + incorrect).coerceAtLeast(0)
    val accuracy: Int get() = if (total > 0) ((correct * 100) / total).coerceIn(0, 100) else 0
    val wordRuns: Int
        get() = if (words.isNotEmpty()) words.sumOf { it.effectiveRuns } else total

    companion object {
        const val KIND_CHOICE = "choice"
        const val KIND_MATCH = "match"
        const val KIND_DAY = "day"
    }
}

fun Map<String, StudyTestWordStat>.recordAnswer(card: WordCard, ok: Boolean): Map<String, StudyTestWordStat> {
    val prev = this[card.id] ?: StudyTestWordStat(
        wordId = card.id,
        pt = card.pt,
        ru = card.ru,
        correct = 0,
        incorrect = 0,
        runs = 0,
    )
    return this + (card.id to prev.copy(
        correct = prev.correct + if (ok) 1 else 0,
        incorrect = prev.incorrect + if (ok) 0 else 1,
        runs = if (prev.runs > 0) prev.runs else 1,
    ))
}

fun MutableMap<String, StudyTestWordStat>.putAnswer(card: WordCard, ok: Boolean) {
    val prev = this[card.id] ?: StudyTestWordStat(
        wordId = card.id,
        pt = card.pt,
        ru = card.ru,
        correct = 0,
        incorrect = 0,
        runs = 0,
    )
    this[card.id] = prev.copy(
        correct = prev.correct + if (ok) 1 else 0,
        incorrect = prev.incorrect + if (ok) 0 else 1,
        runs = if (prev.runs > 0) prev.runs else 1,
    )
}
