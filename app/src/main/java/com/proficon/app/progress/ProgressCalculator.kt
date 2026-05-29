package com.proficon.app.progress

import com.proficon.app.data.local.DailyActivityEntity
import com.proficon.app.data.model.Collection
import com.proficon.app.data.model.DictionaryEntry
import com.proficon.app.data.model.ProgressSnapshot
import com.proficon.app.data.model.TopicRetention
import com.proficon.app.data.model.WeeklyActivityMetric
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.max
import kotlin.math.roundToInt

object ProgressCalculator {
    private const val B2_WORD_TARGET = 3300
    private val EXAM_DATE = LocalDate.of(2026, 10, 15)
    private const val AVG_B2_WORDS_PER_WEEK = 42

    data class ProgressInputs(
        val collections: List<Collection>,
        val dictionary: List<DictionaryEntry>,
        val dailyActivities: List<DailyActivityEntity>,
        val streak: Int,
        val bestStreak: Int,
        val resetAt: Long,
        val dailyGoalTarget: Int,
        val weeklyCardsGoal: Int,
        val weeklyYoutubeGoalMin: Int,
        val weeklyReadingGoal: Int,
        val weeklySpeechGoal: Int,
        val weeklyWritingGoal: Int,
    )

    fun calculate(inputs: ProgressInputs): ProgressSnapshot {
        val filteredDictionary = inputs.dictionary.filter { it.addedAt >= inputs.resetAt }
        val filteredActivities = inputs.dailyActivities.filter { activity ->
            activity.dateKey >= resetDateKey(inputs.resetAt)
        }

        val allCards = inputs.collections.flatMap { it.cards }
        val knownFromCards = allCards.count { it.known }
        val knownWords = filteredDictionary.size
        val studied = allCards.size.coerceAtLeast(1)

        val retention = if (allCards.isEmpty()) {
            0
        } else {
            ((knownFromCards.toFloat() / studied) * 100).roundToInt()
        }

        val weekAgoKey = ProgressTracker.dateKey(LocalDate.now().minusDays(7))
        val previousActivity = filteredActivities.lastOrNull { it.dateKey <= weekAgoKey }
        val previousRetention = previousActivity?.let { activity ->
            if (activity.totalCardsSnapshot <= 0) retention
            else ((activity.cardsKnownSnapshot.toFloat() / activity.totalCardsSnapshot) * 100).roundToInt()
        } ?: retention

        val readiness = ((knownWords.toFloat() / B2_WORD_TARGET) * 100).roundToInt().coerceIn(0, 100)
        val wordsToB2 = (B2_WORD_TARGET - knownWords).coerceAtLeast(0)

        val daysToExam = ChronoUnit.DAYS.between(LocalDate.now(), EXAM_DATE).toInt().coerceAtLeast(0)
        val weeksToExam = (daysToExam / 7).coerceAtLeast(1)

        val todayKey = ProgressTracker.todayKey()
        val todayActivity = filteredActivities.lastOrNull { it.dateKey == todayKey }
        val weekActivities = filteredActivities.filter {
            it.dateKey >= ProgressTracker.dateKey(LocalDate.now().minusDays(6))
        }

        val dailyGoalCurrent = todayActivity?.cardsStudied ?: 0
        val minutesToday = todayActivity?.minutesActive ?: 0
        val weeklyCards = weekActivities.sumOf { it.cardsStudied }
        val weeklyDictionaryWords = weekActivities.sumOf { it.dictionaryWordsAdded }
        val wordsPerWeek = weeklyDictionaryWords + (weeklyCards / 4)

        val paceDaysToB2 = if (wordsToB2 <= 0 || wordsPerWeek <= 0) {
            0
        } else {
            ((wordsToB2 / wordsPerWeek.toFloat()) * 7).roundToInt()
        }
        val predictedDate = LocalDate.now().plusDays(paceDaysToB2.toLong())
        val daysBeforeExam = (daysToExam - paceDaysToB2).coerceAtLeast(0)

        val currentLevel = when {
            readiness >= 75 -> "B2"
            readiness >= 55 -> "B1+"
            readiness >= 35 -> "B1"
            else -> "A2+"
        }

        val heatmap = buildHeatmap(filteredActivities, inputs.resetAt)
        val heatmapStats = buildHeatmapStats(filteredActivities, inputs.streak, inputs.bestStreak)
        val vocabularyTrend = buildVocabularyTrend(filteredDictionary)
        val vocabularyWeeklyGain = vocabularyTrend.lastOrNull()?.let { last ->
            val prev = vocabularyTrend.getOrNull(vocabularyTrend.lastIndex - 1) ?: last
            last - prev
        } ?: 0

        return ProgressSnapshot(
            daysToExam = daysToExam,
            weeksToExam = weeksToExam,
            currentLevel = currentLevel,
            b2ReadinessPercent = readiness,
            wordsToB2 = wordsToB2,
            knownWords = knownWords,
            b2WordTarget = B2_WORD_TARGET,
            onTrack = wordsToB2 <= 0 || wordsPerWeek * weeksToExam >= wordsToB2,
            streakDays = inputs.streak,
            dailyGoalCurrent = dailyGoalCurrent,
            dailyGoalTarget = inputs.dailyGoalTarget,
            minutesToday = minutesToday,
            weeklyCards = weeklyCards,
            weeklyCardsTarget = inputs.weeklyCardsGoal,
            retentionPercent = retention,
            previousRetentionPercent = previousRetention,
            weakTopic = detectWeakTopic(inputs.collections),
            wordsPerWeek = wordsPerWeek,
            avgB2WordsPerWeek = AVG_B2_WORDS_PER_WEEK,
            peerWordsRankTopPercent = peerRankPercent(wordsPerWeek),
            predictedB2DateLabel = formatDate(predictedDate),
            daysBeforeExamAtPace = daysBeforeExam,
            heatmap = heatmap,
            heatmapRecordStreak = heatmapStats.recordStreak,
            heatmapActiveDaysPercent = heatmapStats.activeDaysPercent,
            heatmapAverageScore = heatmapStats.averageScore,
            vocabularyTrend = vocabularyTrend,
            vocabularyWeeklyGain = vocabularyWeeklyGain,
            weeklyActivities = buildWeeklyActivities(weekActivities, inputs),
            topicStats = buildTopicStats(inputs.collections),
        )
    }

    private fun resetDateKey(resetAt: Long): String =
        if (resetAt <= 0L) {
            "1970-01-01"
        } else {
            ProgressTracker.dateKey(
                java.time.Instant.ofEpochMilli(resetAt)
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalDate(),
            )
        }

    private fun detectWeakTopic(collections: List<Collection>): String {
        return collections
            .filter { it.cards.size >= 3 }
            .minByOrNull { collection ->
                collection.cards.count { it.known }.toFloat() / collection.cards.size
            }
            ?.title
            ?.take(24)
            .orEmpty()
            .ifBlank { "—" }
    }

    private fun buildHeatmap(activities: List<DailyActivityEntity>, resetAt: Long): List<Int> {
        val activityByDate = activities.associateBy { it.dateKey }
        val defaultStart = LocalDate.now().minusDays(364)
        val resetStart = if (resetAt <= 0L) {
            defaultStart
        } else {
            java.time.Instant.ofEpochMilli(resetAt)
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDate()
        }
        val start = if (resetStart.isAfter(defaultStart)) resetStart else defaultStart
        val levels = mutableListOf<Int>()
        var date = start
        while (!date.isAfter(LocalDate.now())) {
            val key = ProgressTracker.dateKey(date)
            val score = activityByDate[key]?.let { ProgressTracker.activityScore(it) } ?: 0
            levels += ProgressTracker.heatmapLevel(score)
            date = date.plusDays(1)
        }
        while (levels.size < 365) {
            levels.add(0, 0)
        }
        return levels.takeLast(365)
    }

    private data class HeatmapStats(
        val recordStreak: Int,
        val activeDaysPercent: Int,
        val averageScore: Int,
    )

    private fun buildHeatmapStats(
        activities: List<DailyActivityEntity>,
        streak: Int,
        bestStreak: Int,
    ): HeatmapStats {
        val yearActivities = activities.filter {
            it.dateKey >= ProgressTracker.dateKey(LocalDate.now().minusDays(364))
        }
        val scores = yearActivities.map { ProgressTracker.activityScore(it) }
        val activeDays = scores.count { it > 0 }
        val activePercent = if (yearActivities.isEmpty()) {
            0
        } else {
            ((activeDays.toFloat() / 365) * 100).roundToInt()
        }
        val average = if (activeDays == 0) {
            0
        } else {
            (scores.sum().toFloat() / activeDays).roundToInt()
        }
        return HeatmapStats(
            recordStreak = max(bestStreak, streak),
            activeDaysPercent = activePercent,
            averageScore = average,
        )
    }

    private fun buildVocabularyTrend(dictionary: List<DictionaryEntry>): List<Int> {
        val today = LocalDate.now()
        return List(7) { index ->
            val end = today.minusDays((6 - index) * 7L)
            dictionary.count { entry ->
                val addedDate = java.time.Instant.ofEpochMilli(entry.addedAt)
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalDate()
                !addedDate.isAfter(end)
            }
        }
    }

    private fun buildWeeklyActivities(
        weekActivities: List<DailyActivityEntity>,
        inputs: ProgressInputs,
    ): List<WeeklyActivityMetric> {
        val youtubeMinutes = weekActivities.sumOf { it.youtubeSeconds } / 60f
        val wordsRead = weekActivities.sumOf { it.wordsRead }.toFloat()
        val speech = weekActivities.sumOf { it.speechRecordings }.toFloat()
        val writing = weekActivities.sumOf { it.writingTasks }.toFloat()
        val cards = weekActivities.sumOf { it.cardsStudied }.toFloat()

        return listOf(
            WeeklyActivityMetric("Карточки", cards, inputs.weeklyCardsGoal.toFloat(), "", 0xFF16A34A),
            WeeklyActivityMetric(
                "YouTube часы",
                youtubeMinutes,
                inputs.weeklyYoutubeGoalMin.toFloat(),
                "ч",
                0xFF3B82F6,
            ),
            WeeklyActivityMetric(
                "Слова в чтении",
                wordsRead,
                inputs.weeklyReadingGoal.toFloat(),
                "",
                0xFFEAB308,
            ),
            WeeklyActivityMetric(
                "Запись речи",
                speech,
                inputs.weeklySpeechGoal.toFloat(),
                "",
                0xFFA855F7,
            ),
            WeeklyActivityMetric(
                "Письменные задания",
                writing,
                inputs.weeklyWritingGoal.toFloat(),
                "",
                0xFFDC2626,
            ),
        )
    }

    private fun buildTopicStats(collections: List<Collection>): List<TopicRetention> {
        return collections
            .filter { it.cards.isNotEmpty() }
            .sortedByDescending { it.cards.size }
            .take(4)
            .map { collection ->
                val retention = ((collection.cards.count { it.known }.toFloat() / collection.cards.size) * 100)
                    .roundToInt()
                TopicRetention(
                    title = collection.title,
                    retentionPercent = retention,
                    colorArgb = topicColor(retention),
                )
            }
    }

    private fun topicColor(retention: Int): Long = when {
        retention >= 85 -> 0xFF16A34A
        retention >= 70 -> 0xFFEAB308
        else -> 0xFF3B82F6
    }

    private fun peerRankPercent(wordsPerWeek: Int): Int = when {
        wordsPerWeek >= AVG_B2_WORDS_PER_WEEK * 1.4 -> 10
        wordsPerWeek >= AVG_B2_WORDS_PER_WEEK * 1.2 -> 15
        wordsPerWeek >= AVG_B2_WORDS_PER_WEEK -> 25
        wordsPerWeek >= AVG_B2_WORDS_PER_WEEK * 0.8 -> 40
        else -> 60
    }

    private fun formatDate(date: LocalDate): String =
        "${date.dayOfMonth}.${date.monthValue.toString().padStart(2, '0')}.${date.year}"
}
