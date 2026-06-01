package com.profconq.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.profconq.app.progress.ProgressKeys
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        CollectionEntity::class,
        CardEntity::class,
        DictionaryEntity::class,
        AppStateEntity::class,
        ReaderBookEntity::class,
        DailyActivityEntity::class,
        YouTubeWatchHistoryEntity::class,
        StudySetEntity::class,
        StudySetWordEntity::class,
        ReviewStateEntity::class,
    ],
    version = 11,
    exportSchema = false,
)
abstract class ProfconqDatabase : RoomDatabase() {
    abstract fun collectionDao(): CollectionDao
    abstract fun studySetDao(): StudySetDao
    abstract fun dictionaryDao(): DictionaryDao
    abstract fun appStateDao(): AppStateDao
    abstract fun readerBookDao(): ReaderBookDao
    abstract fun dailyActivityDao(): DailyActivityDao
    abstract fun youtubeWatchHistoryDao(): YouTubeWatchHistoryDao
    abstract fun reviewStateDao(): ReviewStateDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE collections ADD COLUMN video_id TEXT")
                db.execSQL("ALTER TABLE collections ADD COLUMN source_type TEXT")
                db.execSQL("ALTER TABLE collections ADD COLUMN folder_audio_path TEXT")
                db.execSQL("ALTER TABLE cards ADD COLUMN image_path TEXT")
                db.execSQL("ALTER TABLE cards ADD COLUMN audio_path TEXT")
                db.execSQL("ALTER TABLE dictionary ADD COLUMN collection_id TEXT")
                db.execSQL("ALTER TABLE dictionary ADD COLUMN video_id TEXT")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS reader_books (
                        id TEXT NOT NULL PRIMARY KEY,
                        title TEXT NOT NULL,
                        content TEXT NOT NULL,
                        source_uri TEXT,
                        scroll_paragraph INTEGER NOT NULL DEFAULT 0,
                        added_at INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE app_state ADD COLUMN long_value INTEGER NOT NULL DEFAULT 0")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS daily_activity (
                        date_key TEXT NOT NULL PRIMARY KEY,
                        cards_studied INTEGER NOT NULL DEFAULT 0,
                        minutes_active INTEGER NOT NULL DEFAULT 0,
                        youtube_seconds INTEGER NOT NULL DEFAULT 0,
                        words_read INTEGER NOT NULL DEFAULT 0,
                        speech_recordings INTEGER NOT NULL DEFAULT 0,
                        writing_tasks INTEGER NOT NULL DEFAULT 0,
                        dictionary_words_added INTEGER NOT NULL DEFAULT 0,
                        cards_known_snapshot INTEGER NOT NULL DEFAULT 0,
                        total_cards_snapshot INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent(),
                )
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS youtube_watch_history (
                        video_id TEXT NOT NULL PRIMARY KEY,
                        title TEXT NOT NULL,
                        channel TEXT NOT NULL DEFAULT '',
                        thumbnail_url TEXT,
                        duration TEXT,
                        is_short INTEGER NOT NULL DEFAULT 0,
                        last_watched_at INTEGER NOT NULL,
                        last_position_sec REAL NOT NULL DEFAULT 0,
                        watch_count INTEGER NOT NULL DEFAULT 1
                    )
                    """.trimIndent(),
                )
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE reader_books ADD COLUMN bookmark_paragraph INTEGER",
                )
            }
        }

        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE collections ADD COLUMN folder_audio_paths TEXT")
                db.execSQL("ALTER TABLE cards ADD COLUMN audio_paths TEXT")
            }
        }

        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE collections ADD COLUMN folder_audio_labels TEXT")
                db.execSQL("ALTER TABLE cards ADD COLUMN audio_labels TEXT")
            }
        }

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS review_state (
                        word_id TEXT NOT NULL PRIMARY KEY,
                        interval_days INTEGER NOT NULL DEFAULT 0,
                        due_at INTEGER NOT NULL DEFAULT 0,
                        last_reviewed_at INTEGER NOT NULL DEFAULT 0,
                        times_shown INTEGER NOT NULL DEFAULT 0,
                        times_known INTEGER NOT NULL DEFAULT 0,
                        times_again INTEGER NOT NULL DEFAULT 0,
                        is_mastered INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY(word_id) REFERENCES cards(id) ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE cards ADD COLUMN image_url TEXT")
                db.execSQL("ALTER TABLE cards ADD COLUMN part_of_speech TEXT")
                db.execSQL("ALTER TABLE cards ADD COLUMN ipa TEXT")
                db.execSQL("ALTER TABLE cards ADD COLUMN source_title TEXT")
                db.execSQL("ALTER TABLE cards ADD COLUMN chapter_or_tag TEXT")
                db.execSQL("ALTER TABLE cards ADD COLUMN example_translation TEXT")
                db.execSQL("ALTER TABLE cards ADD COLUMN is_favorite INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS study_sets (
                        id TEXT NOT NULL PRIMARY KEY,
                        name TEXT NOT NULL,
                        created_at INTEGER NOT NULL,
                        last_practiced_at INTEGER,
                        word_count INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS study_set_words (
                        study_set_id TEXT NOT NULL,
                        word_id TEXT NOT NULL,
                        added_at INTEGER NOT NULL,
                        PRIMARY KEY(study_set_id, word_id),
                        FOREIGN KEY(study_set_id) REFERENCES study_sets(id) ON DELETE CASCADE,
                        FOREIGN KEY(word_id) REFERENCES cards(id) ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_study_set_words_study_set_id ON study_set_words(study_set_id)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_study_set_words_word_id ON study_set_words(word_id)",
                )
            }
        }

        fun create(context: Context): ProfconqDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                ProfconqDatabase::class.java,
                "profconq.db",
            )
                .addMigrations(
                    MIGRATION_1_2,
                    MIGRATION_2_3,
                    MIGRATION_3_4,
                    MIGRATION_4_5,
                    MIGRATION_5_6,
                    MIGRATION_6_7,
                    MIGRATION_7_8,
                    MIGRATION_8_9,
                    MIGRATION_9_10,
                    MIGRATION_10_11,
                )
                .addCallback(SeedCallback())
                .build()
        }
    }
}

private class SeedCallback : RoomDatabase.Callback()

fun ProfconqDatabase.seedIfEmpty(scope: CoroutineScope) {
    scope.launch(Dispatchers.IO) {
        val state = appStateDao()
        if (state.getInt(ProgressKeys.STREAK) != null) return@launch

        state.upsert(AppStateEntity(ProgressKeys.STREAK, 0))
        state.upsert(AppStateEntity(ProgressKeys.DAILY_GOAL, 50))
        state.upsert(AppStateEntity(ProgressKeys.WEEKLY_CARDS_GOAL, 250))
        state.upsert(AppStateEntity(ProgressKeys.WEEKLY_YOUTUBE_GOAL_MIN, 210))
        state.upsert(AppStateEntity(ProgressKeys.WEEKLY_READING_GOAL, 5000))
        state.upsert(AppStateEntity(ProgressKeys.WEEKLY_SPEECH_GOAL, 5))
        state.upsert(AppStateEntity(ProgressKeys.WEEKLY_WRITING_GOAL, 3))
    }
}
