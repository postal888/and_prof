package com.profconq.app.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VocabularyMergeTest {

    private fun word(id: Int, pt: String, ru: String, updatedAt: Long = 0L, reps: Int = 0) =
        WebVocabWord(id = id, word = pt, translation = ru, updatedAt = updatedAt, reps = reps)

    @Test
    fun keepsWordsFromBothSides() {
        val result = mergeVocabulary(
            local = listOf(word(1, "casa", "дом")),
            server = listOf(word(7, "gato", "кот")),
            preferLocalOnTie = true,
        )

        assertEquals(listOf("gato", "casa"), result.merged.map { it.word })
        assertEquals(listOf("gato"), result.applyLocally.map { it.word })
        assertEquals(1, result.addedToApp)
        assertEquals(1, result.sentToSite)
    }

    @Test
    fun deviceOnlyWordsGetIdsAboveTheServers() {
        val result = mergeVocabulary(
            local = listOf(word(1, "casa", "дом"), word(2, "mesa", "стол")),
            server = listOf(word(40, "gato", "кот")),
            preferLocalOnTie = true,
        )

        assertEquals(listOf(40, 41, 42), result.merged.map { it.id })
    }

    @Test
    fun laterEditWinsWhicheverSideItIsOn() {
        val newerOnServer = mergeVocabulary(
            local = listOf(word(1, "casa", "дом", updatedAt = 100)),
            server = listOf(word(5, "casa", "жилище", updatedAt = 200)),
            preferLocalOnTie = true,
        )
        assertEquals("жилище", newerOnServer.merged.single().translation)
        assertEquals(1, newerOnServer.updatedInApp)

        val newerOnDevice = mergeVocabulary(
            local = listOf(word(1, "casa", "дом", updatedAt = 300)),
            server = listOf(word(5, "casa", "жилище", updatedAt = 200)),
            preferLocalOnTie = false,
        )
        assertEquals("дом", newerOnDevice.merged.single().translation)
        assertTrue(newerOnDevice.applyLocally.isEmpty())
        assertEquals(1, newerOnDevice.sentToSite)
    }

    @Test
    fun selectedSourceBreaksATie() {
        val local = listOf(word(1, "casa", "дом"))
        val server = listOf(word(5, "casa", "жилище"))

        assertEquals("дом", mergeVocabulary(local, server, preferLocalOnTie = true).merged.single().translation)
        assertEquals("жилище", mergeVocabulary(local, server, preferLocalOnTie = false).merged.single().translation)
    }

    @Test
    fun deviceWinKeepsTheWebsitesIdAndReviewProgress() {
        val result = mergeVocabulary(
            local = listOf(word(1, "casa", "дом", updatedAt = 300)),
            server = listOf(word(5, "casa", "жилище", updatedAt = 200, reps = 4)),
            preferLocalOnTie = false,
        )

        val merged = result.merged.single()
        assertEquals(5, merged.id)
        assertEquals(4, merged.reps)
        assertEquals("дом", merged.translation)
    }

    @Test
    fun matchesWordsIgnoringCaseAndPunctuation() {
        val result = mergeVocabulary(
            local = listOf(word(1, "Avó,", "бабушка")),
            server = listOf(word(5, "avó", "бабушка")),
            preferLocalOnTie = true,
        )

        assertEquals(1, result.merged.size)
        assertEquals(0, result.addedToApp)
    }

    @Test
    fun identicalWordsChangeNothing() {
        val result = mergeVocabulary(
            local = listOf(word(1, "casa", "дом")),
            server = listOf(word(5, "casa", "дом")),
            preferLocalOnTie = false,
        )

        assertTrue(result.applyLocally.isEmpty())
        assertEquals(0, result.sentToSite)
        assertEquals(0, result.updatedInApp)
    }
}
