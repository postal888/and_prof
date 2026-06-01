package com.profconq.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface CollectionDao {
    @Transaction
    @Query("SELECT * FROM collections ORDER BY created_at ASC")
    fun observeCollectionsWithCards(): Flow<List<CollectionWithCards>>

    @Query("SELECT * FROM collections ORDER BY created_at ASC")
    suspend fun getCollections(): List<CollectionEntity>

    @Query("SELECT * FROM collections WHERE id = :collectionId LIMIT 1")
    suspend fun getCollection(collectionId: String): CollectionEntity?

    @Query("SELECT * FROM collections WHERE video_id = :videoId LIMIT 1")
    suspend fun getCollectionByVideoId(videoId: String): CollectionEntity?

    @Query("SELECT * FROM cards WHERE collection_id = :collectionId")
    suspend fun getCardsForCollection(collectionId: String): List<CardEntity>

    @Query("SELECT * FROM cards WHERE id IN (:ids)")
    suspend fun getCardsByIds(ids: List<String>): List<CardEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCollection(collection: CollectionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCards(cards: List<CardEntity>)

    @Query("UPDATE collections SET title = :title WHERE id = :collectionId")
    suspend fun updateCollectionTitle(collectionId: String, title: String)

    @Query("UPDATE collections SET description = :description WHERE id = :collectionId")
    suspend fun updateCollectionDescription(collectionId: String, description: String?)

    @Query("UPDATE collections SET folder_audio_path = :path WHERE id = :collectionId")
    suspend fun updateCollectionFolderAudio(collectionId: String, path: String?)

    @Query(
        """
        UPDATE collections
        SET folder_audio_paths = :pathsJson,
            folder_audio_path = :legacyPath,
            folder_audio_labels = :labelsJson
        WHERE id = :collectionId
        """,
    )
    suspend fun updateCollectionFolderAudios(
        collectionId: String,
        pathsJson: String?,
        legacyPath: String?,
        labelsJson: String? = null,
    )

    @Query("UPDATE collections SET folder_audio_labels = :labelsJson WHERE id = :collectionId")
    suspend fun updateCollectionFolderAudioLabels(collectionId: String, labelsJson: String?)

    @Query("DELETE FROM cards WHERE collection_id = :collectionId")
    suspend fun deleteCardsForCollection(collectionId: String)

    @Query("UPDATE cards SET due = :due, known = :known WHERE id = :cardId")
    suspend fun updateCardProgress(cardId: String, due: Boolean, known: Boolean)

    @Query(
        """
        UPDATE cards
        SET pt = :pt, ru = :ru, example = :example, image_path = :imagePath,
            audio_path = :audioPath, audio_paths = :audioPathsJson,
            audio_labels = :audioLabelsJson,
            image_url = :imageUrl, part_of_speech = :partOfSpeech, ipa = :ipa,
            source_title = :sourceTitle, chapter_or_tag = :chapterOrTag,
            example_translation = :exampleTranslation, is_favorite = :isFavorite
        WHERE id = :cardId
        """,
    )
    suspend fun updateCardContent(
        cardId: String,
        pt: String,
        ru: String,
        example: String?,
        imagePath: String?,
        audioPath: String?,
        audioPathsJson: String? = null,
        audioLabelsJson: String? = null,
        imageUrl: String? = null,
        partOfSpeech: String? = null,
        ipa: String? = null,
        sourceTitle: String? = null,
        chapterOrTag: String? = null,
        exampleTranslation: String? = null,
        isFavorite: Boolean = false,
    )

    @Query("UPDATE cards SET is_favorite = :isFavorite WHERE id = :cardId")
    suspend fun updateCardFavorite(cardId: String, isFavorite: Boolean)

    @Query("DELETE FROM cards WHERE id = :cardId")
    suspend fun deleteCard(cardId: String)

    @Query("DELETE FROM collections WHERE id = :collectionId")
    suspend fun deleteCollection(collectionId: String)
}

@Dao
interface DictionaryDao {
    @Query("SELECT * FROM dictionary ORDER BY added_at DESC")
    fun observeDictionary(): Flow<List<DictionaryEntity>>

    @Query("SELECT * FROM dictionary")
    suspend fun getAll(): List<DictionaryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: DictionaryEntity)

    @Query("DELETE FROM dictionary WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM dictionary")
    suspend fun deleteAll()
}

@Dao
interface AppStateDao {
    @Query("SELECT int_value FROM app_state WHERE key = :key LIMIT 1")
    fun observeInt(key: String): Flow<Int?>

    @Query("SELECT long_value FROM app_state WHERE key = :key LIMIT 1")
    fun observeLong(key: String): Flow<Long?>

    @Query("SELECT int_value FROM app_state WHERE key = :key LIMIT 1")
    suspend fun getInt(key: String): Int?

    @Query("SELECT long_value FROM app_state WHERE key = :key LIMIT 1")
    suspend fun getLong(key: String): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: AppStateEntity)

    @Query("DELETE FROM app_state WHERE key = :key")
    suspend fun deleteKey(key: String)
}

@Dao
interface DailyActivityDao {
    @Query("SELECT * FROM daily_activity ORDER BY date_key ASC")
    fun observeAll(): Flow<List<DailyActivityEntity>>

    @Query("SELECT * FROM daily_activity WHERE date_key = :dateKey LIMIT 1")
    suspend fun getByDate(dateKey: String): DailyActivityEntity?

    @Query("SELECT * FROM daily_activity WHERE date_key >= :fromKey ORDER BY date_key ASC")
    suspend fun getSince(fromKey: String): List<DailyActivityEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: DailyActivityEntity)

    @Query("DELETE FROM daily_activity")
    suspend fun deleteAll()
}

@Dao
interface ReaderBookDao {
    @Query("SELECT * FROM reader_books ORDER BY added_at DESC")
    fun observeBooks(): Flow<List<ReaderBookEntity>>

    @Query("SELECT * FROM reader_books WHERE id = :bookId LIMIT 1")
    suspend fun getBook(bookId: String): ReaderBookEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(book: ReaderBookEntity)

    @Query("UPDATE reader_books SET scroll_paragraph = :paragraph WHERE id = :bookId")
    suspend fun updateScrollParagraph(bookId: String, paragraph: Int)

    @Query("UPDATE reader_books SET bookmark_paragraph = :paragraph WHERE id = :bookId")
    suspend fun updateBookmark(bookId: String, paragraph: Int?)

    @Query("DELETE FROM reader_books WHERE id = :bookId")
    suspend fun deleteBook(bookId: String)
}

@Dao
interface YouTubeWatchHistoryDao {
    @Query("SELECT * FROM youtube_watch_history ORDER BY last_watched_at DESC LIMIT :limit")
    fun observeRecent(limit: Int = 30): Flow<List<YouTubeWatchHistoryEntity>>

    @Query("SELECT * FROM youtube_watch_history WHERE video_id = :videoId LIMIT 1")
    suspend fun getByVideoId(videoId: String): YouTubeWatchHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: YouTubeWatchHistoryEntity)

    @Query("DELETE FROM youtube_watch_history WHERE video_id = :videoId")
    suspend fun delete(videoId: String)

    @Query("DELETE FROM youtube_watch_history")
    suspend fun deleteAll()
}
