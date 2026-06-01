package com.profconq.app.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface ReviewStateDao {
    @Query("SELECT * FROM review_state WHERE word_id = :wordId LIMIT 1")
    suspend fun get(wordId: String): ReviewStateEntity?

    @Query(
        """
        SELECT c.* FROM cards c
        INNER JOIN study_set_words s ON s.word_id = c.id
        LEFT JOIN review_state r ON r.word_id = c.id
        WHERE s.study_set_id = :setId
          AND (r.word_id IS NULL OR r.due_at <= :now)
          AND (r.is_mastered IS NULL OR r.is_mastered = 0)
        ORDER BY
            CASE WHEN r.word_id IS NULL THEN 0 ELSE 1 END,
            COALESCE(r.times_again, 0) DESC,
            r.due_at ASC
        """,
    )
    suspend fun getDueCardsForSet(setId: String, now: Long): List<CardEntity>

    @Query(
        """
        SELECT
            COUNT(CASE WHEN r.word_id IS NULL THEN 1 END) AS newCount,
            COUNT(CASE WHEN r.word_id IS NOT NULL AND r.due_at <= :now AND r.is_mastered = 0 THEN 1 END) AS dueCount,
            COUNT(CASE WHEN r.is_mastered = 1 THEN 1 END) AS masteredCount
        FROM study_set_words s
        LEFT JOIN review_state r ON r.word_id = s.word_id
        WHERE s.study_set_id = :setId
        """,
    )
    suspend fun getSetStats(setId: String, now: Long): SetReviewStats

    @Upsert
    suspend fun upsert(state: ReviewStateEntity)
}
