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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWords(words: List<StudySetWordEntity>)

    @Query("DELETE FROM study_set_words WHERE study_set_id = :setId AND word_id = :wordId")
    suspend fun removeWordFromSet(setId: String, wordId: String)

    @Query("DELETE FROM study_set_words WHERE study_set_id = :setId")
    suspend fun clearWordsInSet(setId: String)

    @Query("UPDATE study_sets SET word_count = :count WHERE id = :setId")
    suspend fun updateWordCount(setId: String, count: Int)

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
