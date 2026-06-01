package com.profconq.app.study

import com.profconq.app.data.local.ReviewStateEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SrsEngineTest {
    private val wordId = "word-1"
    private val now = 1_700_000_000_000L
    private val dayMs = 24 * 60 * 60 * 1000L

    @Test
    fun newWord_known_setsIntervalOneDay() {
        val result = SrsEngine.onKnown(state = null, wordId = wordId, now = now)
        assertEquals(1, result.intervalDays)
        assertEquals(now + dayMs, result.dueAt)
        assertFalse(result.isMastered)
        assertEquals(1, result.timesKnown)
    }

    @Test
    fun intervalSeven_again_resetsToOneDay() {
        val existing = ReviewStateEntity(
            wordId = wordId,
            intervalDays = 7,
            dueAt = now,
            timesShown = 3,
            timesKnown = 2,
        )
        val result = SrsEngine.onAgain(existing, wordId, now)
        assertEquals(1, result.intervalDays)
        assertEquals(now, result.dueAt)
        assertFalse(result.isMastered)
        assertEquals(1, result.timesAgain)
    }

    @Test
    fun intervalThirty_known_becomesMastered() {
        val existing = ReviewStateEntity(
            wordId = wordId,
            intervalDays = 30,
            dueAt = now - dayMs,
            timesShown = 5,
            timesKnown = 4,
        )
        val result = SrsEngine.onKnown(existing, wordId, now)
        assertTrue(result.isMastered)
        assertEquals(30, result.intervalDays)
    }

    @Test
    fun known_progressesThroughIntervals() {
        var state: ReviewStateEntity? = null
        for (expected in listOf(1, 3, 7, 14, 30)) {
            state = SrsEngine.onKnown(state, wordId, now)
            assertEquals(expected, state.intervalDays)
        }
    }
}
