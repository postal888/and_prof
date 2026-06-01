package com.profconq.app.study

import com.profconq.app.data.local.ReviewStateEntity

object SrsEngine {
    val intervalsDays: List<Int> = listOf(1, 3, 7, 14, 30)

    private const val DAY_MS = 24 * 60 * 60 * 1000L

    fun onKnown(state: ReviewStateEntity?, wordId: String, now: Long): ReviewStateEntity {
        val current = state ?: ReviewStateEntity(wordId = wordId)
        val currentIndex = intervalsDays.indexOf(current.intervalDays)
        val nextIndex = if (currentIndex == -1) {
            0
        } else {
            (currentIndex + 1).coerceAtMost(intervalsDays.lastIndex)
        }
        val nextInterval = intervalsDays[nextIndex]
        val mastered = current.intervalDays >= intervalsDays.last()
        return current.copy(
            intervalDays = nextInterval,
            dueAt = now + nextInterval * DAY_MS,
            lastReviewedAt = now,
            timesShown = current.timesShown + 1,
            timesKnown = current.timesKnown + 1,
            isMastered = mastered,
        )
    }

    /** User tapped «Не знаю» — reset interval and make the word due immediately for the next session. */
    fun onAgain(state: ReviewStateEntity?, wordId: String, now: Long): ReviewStateEntity {
        val current = state ?: ReviewStateEntity(wordId = wordId)
        return current.copy(
            intervalDays = intervalsDays.first(),
            dueAt = now,
            lastReviewedAt = now,
            timesShown = current.timesShown + 1,
            timesAgain = current.timesAgain + 1,
            isMastered = false,
        )
    }
}
