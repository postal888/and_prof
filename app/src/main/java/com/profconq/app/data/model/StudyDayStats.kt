package com.profconq.app.data.model

data class DailyStudyActivity(
    val dateKey: String,
    val cardsStudied: Int,
    val minutesActive: Int = 0,
)

data class StudyDaySession(
    val kind: String,
    val setName: String,
    val correct: Int,
    val incorrect: Int,
    val at: Long,
    val words: List<StudyTestWordStat> = emptyList(),
) {
    val total: Int get() = (correct + incorrect).coerceAtLeast(0)
}

data class StudyDayBucket(
    val dateKey: String,
    val sessions: List<StudyDaySession> = emptyList(),
) {
    val correct: Int get() = sessions.sumOf { it.correct }
    val incorrect: Int get() = sessions.sumOf { it.incorrect }
    val testsPlayed: Int get() = sessions.size
    val matchGames: Int get() = sessions.count { it.kind == LastStudyTestResult.KIND_MATCH }
    val choiceTests: Int get() = sessions.count { it.kind == LastStudyTestResult.KIND_CHOICE }
    val testTotal: Int get() = (correct + incorrect).coerceAtLeast(0)
    val accuracy: Int get() = if (testTotal > 0) ((correct * 100) / testTotal).coerceIn(0, 100) else 0
    val hasTests: Boolean get() = sessions.isNotEmpty()
    val words: List<StudyTestWordStat>
        get() = sessions
            .flatMap { it.words }
            .groupBy { it.wordId }
            .map { (_, list) ->
                val first = list.first()
                first.copy(
                    correct = list.sumOf { it.correct },
                    incorrect = list.sumOf { it.incorrect },
                    runs = list.sumOf { it.effectiveRuns },
                )
            }
}

data class StudyDayStats(
    val dateKey: String,
    val cardsStudied: Int,
    val minutesActive: Int,
    val tests: StudyDayBucket,
) {
    val hasActivity: Boolean
        get() = cardsStudied > 0 || minutesActive > 0 || tests.hasTests || tests.matchGames > 0
}
