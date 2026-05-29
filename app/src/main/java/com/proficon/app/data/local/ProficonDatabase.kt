package com.proficon.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.proficon.app.progress.ProgressKeys
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
    ],
    version = 6,
    exportSchema = false,
)
abstract class ProficonDatabase : RoomDatabase() {
    abstract fun collectionDao(): CollectionDao
    abstract fun dictionaryDao(): DictionaryDao
    abstract fun appStateDao(): AppStateDao
    abstract fun readerBookDao(): ReaderBookDao
    abstract fun dailyActivityDao(): DailyActivityDao
    abstract fun youtubeWatchHistoryDao(): YouTubeWatchHistoryDao

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

        fun create(context: Context): ProficonDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                ProficonDatabase::class.java,
                "proficon.db",
            )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                .addCallback(SeedCallback())
                .build()
        }
    }
}

private class SeedCallback : RoomDatabase.Callback()

fun ProficonDatabase.seedIfEmpty(scope: CoroutineScope) {
    scope.launch(Dispatchers.IO) {
        val dao = collectionDao()
        if (dao.getCollections().isNotEmpty()) return@launch

        val now = System.currentTimeMillis()
        dao.insertCollection(
            CollectionEntity(
                id = "pt-a1",
                title = "Portuguese A1 Basics",
                description = "Основные фразы A1",
                createdAt = now,
                sourceType = "manual",
            ),
        )
        dao.insertCollection(
            CollectionEntity(
                id = "travel",
                title = "Travel Essentials",
                description = "Слова для поездок",
                createdAt = now + 1,
                sourceType = "manual",
            ),
        )
        dao.insertCollection(
            CollectionEntity(
                id = "col-words",
                title = "Слова",
                description = "Основной набор",
                createdAt = now + 2,
                sourceType = "manual",
            ),
        )

        dao.insertCards(
            listOf(
                CardEntity("1", "pt-a1", "bom dia", "доброе утро", "Bom dia, como está?", due = true),
                CardEntity("2", "pt-a1", "obrigado", "спасибо", "Muito obrigado pela ajuda.", due = true),
                CardEntity("3", "pt-a1", "desculpa", "извините", "Desculpa, estou atrasado.", due = true),
                CardEntity("4", "pt-a1", "prazer", "приятно познакомиться", "Muito prazer em conhecê-lo.", due = true),
                CardEntity("5", "travel", "aeroporto", "аэропорт", "O aeroporto fica longe do centro.", due = true),
                CardEntity("6", "travel", "bagagem", "багаж", "Onde posso deixar a bagagem?", due = true),
                CardEntity("7", "travel", "bilhete", "билет", "Comprei o bilhete online.", due = true),
                CardEntity("8", "col-words", "lembrar", "вспоминать", "Eu lembro daquela viagem.", due = true),
                CardEntity("9", "col-words", "saudade", "тоска", "Tenho saudade de casa.", due = true),
                CardEntity("10", "col-words", "virar", "поворачивать", "Vira à esquerda.", due = true),
            ),
        )

        appStateDao().upsert(AppStateEntity(ProgressKeys.STREAK, 0))
        appStateDao().upsert(AppStateEntity(ProgressKeys.DAILY_GOAL, 50))
        appStateDao().upsert(AppStateEntity(ProgressKeys.WEEKLY_CARDS_GOAL, 250))
        appStateDao().upsert(AppStateEntity(ProgressKeys.WEEKLY_YOUTUBE_GOAL_MIN, 210))
        appStateDao().upsert(AppStateEntity(ProgressKeys.WEEKLY_READING_GOAL, 5000))
        appStateDao().upsert(AppStateEntity(ProgressKeys.WEEKLY_SPEECH_GOAL, 5))
        appStateDao().upsert(AppStateEntity(ProgressKeys.WEEKLY_WRITING_GOAL, 3))
    }
}
