package com.profconq.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface StudySetDao {
    @Query("SELECT * FROM study_sets ORDER BY created_at DESC")
    fun observeAllSets(): Flow<List<StudySetEntity>>

    @Query("SELECT * FROM study_sets ORDER BY created_at DESC")
    suspend fun getAllSets(): List<StudySetEntity>

    @Query("SELECT * FROM study_sets WHERE id = :setId LIMIT 1")
    suspend fun getSet(setId: String): StudySetEntity?

    @Query("DELETE FROM study_sets")
    suspend fun deleteAllSets()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSet(entity: StudySetEntity)

    @Query("DELETE FROM study_sets WHERE id = :setId")
    suspend fun deleteSet(setId: String)

    @Query(
        """
        SELECT c.* FROM cards c
        INNER JOIN study_set_words ssw ON c.id = ssw.word_id
        WHERE ssw.study_set_id = :setId
        ORDER BY ssw.added_at ASC
        """,
    )
    fun observeCardsInSet(setId: String): Flow<List<CardEntity>>

    @Query("SELECT word_id FROM study_set_words WHERE study_set_id = :setId")
    suspend fun getWordIdsInSet(setId: String): List<String>

    @Query(
        """
        SELECT c.id FROM cards c
        INNER JOIN study_set_words ssw ON c.id = ssw.word_id
        WHERE ssw.study_set_id = :setId AND c.known = 0
        ORDER BY ssw.added_at ASC
        """,
    )
    suspend fun getStudioWordIdsInSet(setId: String): List<String>

    @Query(
        """
        SELECT
            s.id AS setId,
            (
                SELECT COUNT(*)
                FROM study_set_words ssw
                INNER JOIN cards c ON c.id = ssw.word_id
                WHERE ssw.study_set_id = s.id AND c.known = 0
            ) AS studioCount
        FROM study_sets s
        """,
    )
    fun observeStudioCounts(): Flow<List<SetStudioCount>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWords(words: List<StudySetWordEntity>)

    @Query("DELETE FROM study_set_words WHERE study_set_id = :setId AND word_id = :wordId")
    suspend fun removeWordFromSet(setId: String, wordId: String)

    @Query("DELETE FROM study_set_words WHERE study_set_id = :setId")
    suspend fun clearWordsInSet(setId: String)

    @Query("UPDATE study_sets SET word_count = :count WHERE id = :setId")
    suspend fun updateWordCount(setId: String, count: Int)

    @Query("UPDATE study_sets SET name = :name WHERE id = :setId")
    suspend fun updateName(setId: String, name: String)

    @Query("UPDATE study_sets SET last_practiced_at = :at WHERE id = :setId")
    suspend fun updateLastPracticed(setId: String, at: Long)

    @Transaction
    suspend fun createSetWithWords(
        set: StudySetEntity,
        words: List<StudySetWordEntity>,
    ) {
        insertSet(set)
        if (words.isNotEmpty()) {
            insertWords(words)
        }
    }
}
