package com.profconq.app.data.local

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "collections")
data class CollectionEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "video_id") val videoId: String? = null,
    @ColumnInfo(name = "source_type") val sourceType: String? = null,
    @ColumnInfo(name = "folder_audio_path") val folderAudioPath: String? = null,
    @ColumnInfo(name = "folder_audio_paths") val folderAudioPaths: String? = null,
    @ColumnInfo(name = "folder_audio_labels") val folderAudioLabels: String? = null,
)

@Entity(
    tableName = "cards",
    foreignKeys = [
        ForeignKey(
            entity = CollectionEntity::class,
            parentColumns = ["id"],
            childColumns = ["collection_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("collection_id")],
)
data class CardEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "collection_id") val collectionId: String,
    val pt: String,
    val ru: String,
    val example: String? = null,
    val due: Boolean = true,
    val known: Boolean = false,
    @ColumnInfo(name = "image_path") val imagePath: String? = null,
    @ColumnInfo(name = "audio_path") val audioPath: String? = null,
    @ColumnInfo(name = "audio_paths") val audioPaths: String? = null,
    @ColumnInfo(name = "audio_labels") val audioLabels: String? = null,
    @ColumnInfo(name = "image_url") val imageUrl: String? = null,
    @ColumnInfo(name = "part_of_speech") val partOfSpeech: String? = null,
    val ipa: String? = null,
    @ColumnInfo(name = "source_title") val sourceTitle: String? = null,
    @ColumnInfo(name = "chapter_or_tag") val chapterOrTag: String? = null,
    @ColumnInfo(name = "example_translation") val exampleTranslation: String? = null,
    @ColumnInfo(name = "is_favorite") val isFavorite: Boolean = false,
)

@Entity(tableName = "dictionary")
data class DictionaryEntity(
    @PrimaryKey val id: String,
    val pt: String,
    val ru: String,
    val example: String? = null,
    @ColumnInfo(name = "added_at") val addedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "collection_id") val collectionId: String? = null,
    @ColumnInfo(name = "video_id") val videoId: String? = null,
)

@Entity(tableName = "app_state")
data class AppStateEntity(
    @PrimaryKey val key: String,
    @ColumnInfo(name = "int_value") val intValue: Int = 0,
    @ColumnInfo(name = "long_value") val longValue: Long = 0L,
)

@Entity(tableName = "daily_activity")
data class DailyActivityEntity(
    @PrimaryKey @ColumnInfo(name = "date_key") val dateKey: String,
    @ColumnInfo(name = "cards_studied") val cardsStudied: Int = 0,
    @ColumnInfo(name = "minutes_active") val minutesActive: Int = 0,
    @ColumnInfo(name = "youtube_seconds") val youtubeSeconds: Int = 0,
    @ColumnInfo(name = "words_read") val wordsRead: Int = 0,
    @ColumnInfo(name = "speech_recordings") val speechRecordings: Int = 0,
    @ColumnInfo(name = "writing_tasks") val writingTasks: Int = 0,
    @ColumnInfo(name = "dictionary_words_added") val dictionaryWordsAdded: Int = 0,
    @ColumnInfo(name = "cards_known_snapshot") val cardsKnownSnapshot: Int = 0,
    @ColumnInfo(name = "total_cards_snapshot") val totalCardsSnapshot: Int = 0,
)

@Entity(tableName = "reader_books")
data class ReaderBookEntity(
    @PrimaryKey val id: String,
    val title: String,
    val content: String,
    @ColumnInfo(name = "source_uri") val sourceUri: String? = null,
    @ColumnInfo(name = "scroll_paragraph") val scrollParagraph: Int = 0,
    @ColumnInfo(name = "bookmark_paragraph") val bookmarkParagraph: Int? = null,
    @ColumnInfo(name = "added_at") val addedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "youtube_watch_history")
data class YouTubeWatchHistoryEntity(
    @PrimaryKey @ColumnInfo(name = "video_id") val videoId: String,
    val title: String,
    val channel: String = "",
    @ColumnInfo(name = "thumbnail_url") val thumbnailUrl: String? = null,
    val duration: String? = null,
    @ColumnInfo(name = "is_short") val isShort: Boolean = false,
    @ColumnInfo(name = "last_watched_at") val lastWatchedAt: Long,
    @ColumnInfo(name = "last_position_sec") val lastPositionSec: Float = 0f,
    @ColumnInfo(name = "watch_count") val watchCount: Int = 1,
)

data class CollectionWithCards(
    @Embedded val collection: CollectionEntity,
    @Relation(parentColumn = "id", entityColumn = "collection_id")
    val cards: List<CardEntity>,
)
