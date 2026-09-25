package com.profconq.app.progress

import com.profconq.app.data.local.AppStateDao
import com.profconq.app.data.local.AppStateEntity
import com.profconq.app.data.local.DailyActivityDao
import com.profconq.app.data.local.DailyActivityEntity
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.max

object ProgressTracker {
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    fun dateKeyFromMillis(at: Long): String =
        Instant.ofEpochMilli(at).atZone(ZoneId.systemDefault()).toLocalDate().format(dateFormatter)

    fun millisAtStartOf(dateKey: String): Long =
        LocalDate.parse(dateKey, dateFormatter)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

    fun todayKey(): String = LocalDate.now().format(dateFormatter)

    fun isConsecutiveDay(previousKey: String, todayKey: String): Boolean =
        runCatching {
            ChronoUnit.DAYS.between(LocalDate.parse(previousKey), LocalDate.parse(todayKey)) == 1L
        }.getOrDefault(false)

    fun dateKey(date: LocalDate): String = date.format(dateFormatter)

    fun dateKeyFromInt(yyyyMmDd: Int): String {
        val value = yyyyMmDd.toString().padStart(8, '0')
        return LocalDate.of(
            value.substring(0, 4).toInt(),
            value.substring(4, 6).toInt(),
            value.substring(6, 8).toInt(),
        ).format(dateFormatter)
    }

    fun todayAsInt(): Int {
        val now = LocalDate.now()
        return now.year * 10_000 + now.monthValue * 100 + now.dayOfMonth
    }

    suspend fun ensureToday(
        dailyActivityDao: DailyActivityDao,
    ): DailyActivityEntity {
        val key = todayKey()
        return dailyActivityDao.getByDate(key) ?: DailyActivityEntity(dateKey = key).also {
            dailyActivityDao.upsert(it)
        }
    }

    suspend fun recordCardStudied(
        dailyActivityDao: DailyActivityDao,
        appStateDao: AppStateDao,
        cardsKnown: Int,
        totalCards: Int,
        minutes: Int = 1,
    ) {
        val today = ensureToday(dailyActivityDao)
        dailyActivityDao.upsert(
            today.copy(
                cardsStudied = today.cardsStudied + 1,
                minutesActive = today.minutesActive + minutes.coerceAtLeast(1),
                cardsKnownSnapshot = cardsKnown,
                totalCardsSnapshot = totalCards,
            ),
        )
        updateStreak(appStateDao)
    }

    suspend fun recordDictionaryWordAdded(
        dailyActivityDao: DailyActivityDao,
        appStateDao: AppStateDao,
        cardsKnown: Int,
        totalCards: Int,
    ) {
        val today = ensureToday(dailyActivityDao)
        dailyActivityDao.upsert(
            today.copy(
                dictionaryWordsAdded = today.dictionaryWordsAdded + 1,
                minutesActive = today.minutesActive + 1,
                cardsKnownSnapshot = cardsKnown,
                totalCardsSnapshot = totalCards,
            ),
        )
        updateStreak(appStateDao)
    }

    suspend fun recordYouTubeSeconds(
        dailyActivityDao: DailyActivityDao,
        appStateDao: AppStateDao,
        seconds: Int,
        cardsKnown: Int,
        totalCards: Int,
    ) {
        if (seconds <= 0) return
        val today = ensureToday(dailyActivityDao)
        val minutes = seconds / 60
        dailyActivityDao.upsert(
            today.copy(
                youtubeSeconds = today.youtubeSeconds + seconds,
                minutesActive = today.minutesActive + minutes.coerceAtLeast(1),
                cardsKnownSnapshot = cardsKnown,
                totalCardsSnapshot = totalCards,
            ),
        )
        updateStreak(appStateDao)
    }

    suspend fun recordReaderWords(
        dailyActivityDao: DailyActivityDao,
        appStateDao: AppStateDao,
        words: Int,
        minutes: Int,
        cardsKnown: Int,
        totalCards: Int,
    ) {
        if (words <= 0 && minutes <= 0) return
        val today = ensureToday(dailyActivityDao)
        dailyActivityDao.upsert(
            today.copy(
                wordsRead = today.wordsRead + words.coerceAtLeast(0),
                minutesActive = today.minutesActive + minutes.coerceAtLeast(if (words > 0) 1 else 0),
                cardsKnownSnapshot = cardsKnown,
                totalCardsSnapshot = totalCards,
            ),
        )
        updateStreak(appStateDao)
    }

    suspend fun recordSpeechRecording(
        dailyActivityDao: DailyActivityDao,
        appStateDao: AppStateDao,
        cardsKnown: Int,
        totalCards: Int,
    ) {
        val today = ensureToday(dailyActivityDao)
        dailyActivityDao.upsert(
            today.copy(
                speechRecordings = today.speechRecordings + 1,
                minutesActive = today.minutesActive + 2,
                cardsKnownSnapshot = cardsKnown,
                totalCardsSnapshot = totalCards,
            ),
        )
        updateStreak(appStateDao)
    }

    suspend fun updateStreak(appStateDao: AppStateDao) {
        val todayInt = todayAsInt()
        val lastActive = appStateDao.getInt(ProgressKeys.LAST_ACTIVE) ?: 0
        val currentStreak = appStateDao.getInt(ProgressKeys.STREAK) ?: 0
        val bestStreak = appStateDao.getInt(ProgressKeys.BEST_STREAK) ?: 0

        val newStreak = when {
            lastActive == todayInt -> currentStreak.coerceAtLeast(1)
            lastActive == 0 -> 1
            else -> {
                val lastDate = parseDateInt(lastActive)
                val daysBetween = ChronoUnit.DAYS.between(lastDate, LocalDate.now())
                when {
                    daysBetween <= 0L -> currentStreak.coerceAtLeast(1)
                    daysBetween == 1L -> max(currentStreak, 0) + 1
                    else -> 1
                }
            }
        }

        appStateDao.upsert(AppStateEntity(key = ProgressKeys.STREAK, intValue = newStreak))
        appStateDao.upsert(AppStateEntity(key = ProgressKeys.LAST_ACTIVE, intValue = todayInt))
        if (newStreak > bestStreak) {
            appStateDao.upsert(AppStateEntity(key = ProgressKeys.BEST_STREAK, intValue = newStreak))
        }
    }

    fun activityScore(entity: DailyActivityEntity): Int =
        entity.cardsStudied +
            entity.dictionaryWordsAdded +
            (entity.minutesActive / 5) +
            (entity.youtubeSeconds / 300) +
            (entity.wordsRead / 200) +
            entity.speechRecordings +
            entity.writingTasks

    fun heatmapLevel(score: Int): Int = when {
        score <= 0 -> 0
        score <= 2 -> 1
        score <= 5 -> 2
        score <= 10 -> 3
        else -> 4
    }

    private fun parseDateInt(value: Int): LocalDate {
        val text = value.toString().padStart(8, '0')
        return LocalDate.of(
            text.substring(0, 4).toInt(),
            text.substring(4, 6).toInt(),
            text.substring(6, 8).toInt(),
        )
    }
}
