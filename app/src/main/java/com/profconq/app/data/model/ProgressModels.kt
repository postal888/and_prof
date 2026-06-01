package com.profconq.app.data.model

data class ProgressSnapshot(
    val userName: String = "Алексей",
    val examLabel: String = "CELPE-BRAS • OUTUBRO 2026",
    val daysToExam: Int = 0,
    val weeksToExam: Int = 0,
    val currentLevel: String = "B1+",
    val b2ReadinessPercent: Int = 0,
    val wordsToB2: Int = 0,
    val knownWords: Int = 0,
    val b2WordTarget: Int = 3300,
    val onTrack: Boolean = true,
    val streakDays: Int = 0,
    val dailyGoalCurrent: Int = 0,
    val dailyGoalTarget: Int = 50,
    val minutesToday: Int = 0,
    val weeklyCards: Int = 0,
    val weeklyCardsTarget: Int = 250,
    val retentionPercent: Int = 0,
    val previousRetentionPercent: Int = 0,
    val weakTopic: String = "subjuntivo",
    val wordsPerWeek: Int = 0,
    val avgB2WordsPerWeek: Int = 42,
    val peerWordsRankTopPercent: Int = 15,
    val predictedB2DateLabel: String = "",
    val daysBeforeExamAtPace: Int = 0,
    val heatmap: List<Int> = emptyList(),
    val heatmapRecordStreak: Int = 0,
    val heatmapActiveDaysPercent: Int = 0,
    val heatmapAverageScore: Int = 0,
    val vocabularyTrend: List<Int> = emptyList(),
    val vocabularyWeeklyGain: Int = 0,
    val weeklyActivities: List<WeeklyActivityMetric> = emptyList(),
    val topicStats: List<TopicRetention> = emptyList(),
)

data class WeeklyActivityMetric(
    val label: String,
    val current: Float,
    val target: Float,
    val unit: String,
    val colorArgb: Long,
)

data class TopicRetention(
    val title: String,
    val retentionPercent: Int,
    val colorArgb: Long,
)
