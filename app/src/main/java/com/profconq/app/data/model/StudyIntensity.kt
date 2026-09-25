package com.profconq.app.data.model

data class StudyWordDayCell(
    val dateKey: String,
    val wordId: String,
    val pt: String,
    val ru: String,
    val testRuns: Int = 0,
    val cardRuns: Int = 0,
    val studioRuns: Int = 0,
    val correct: Int = 0,
    val incorrect: Int = 0,
) {
    val totalRuns: Int get() = testRuns + cardRuns + studioRuns
    val judged: Int get() = (correct + incorrect).coerceAtLeast(0)
    val errorRate: Float get() = if (judged > 0) incorrect.toFloat() / judged else 0f
    val errorPercent: Int get() = if (judged > 0) ((incorrect * 100) / judged).coerceIn(0, 100) else 0
    val hasActivity: Boolean get() = totalRuns > 0 || judged > 0
}

data class StudyDayIntensity(
    val dateKey: String,
    val testRuns: Int = 0,
    val cardRuns: Int = 0,
    val studioRuns: Int = 0,
    val correct: Int = 0,
    val incorrect: Int = 0,
) {
    val totalRuns: Int get() = testRuns + cardRuns + studioRuns
    val judged: Int get() = (correct + incorrect).coerceAtLeast(0)
    val errorRate: Float get() = if (judged > 0) incorrect.toFloat() / judged else 0f
    val errorPercent: Int get() = if (judged > 0) ((incorrect * 100) / judged).coerceIn(0, 100) else 0
    val hasActivity: Boolean get() = totalRuns > 0 || judged > 0
}

data class StudyIntensitySnapshot(
    val days: Map<String, Map<String, StudyWordDayCell>> = emptyMap(),
) {
    val activeDays: Set<String>
        get() = days.filterValues { cells -> cells.values.any { it.hasActivity } }.keys

    fun day(dateKey: String): StudyDayIntensity {
        val cells = days[dateKey]?.values.orEmpty()
        return StudyDayIntensity(
            dateKey = dateKey,
            testRuns = cells.sumOf { it.testRuns },
            cardRuns = cells.sumOf { it.cardRuns },
            studioRuns = cells.sumOf { it.studioRuns },
            correct = cells.sumOf { it.correct },
            incorrect = cells.sumOf { it.incorrect },
        )
    }

    fun series(dateKeys: List<String>): List<StudyDayIntensity> =
        dateKeys.map { day(it) }

    fun wordsOn(dateKey: String): List<StudyWordDayCell> =
        days[dateKey]?.values
            ?.filter { it.hasActivity }
            ?.sortedWith(
                compareByDescending<StudyWordDayCell> { it.totalRuns }
                    .thenByDescending { it.errorPercent }
                    .thenBy { it.pt.lowercase() },
            )
            .orEmpty()

    fun wordCell(wordId: String): StudyWordDayCell? {
        days.values.asSequence()
            .mapNotNull { it[wordId] }
            .maxByOrNull { it.dateKey }
            ?.let { latest ->
                return latest
            }
        return null
    }

    fun wordSeries(wordId: String, dateKeys: List<String>): List<StudyWordDayCell> {
        val label = wordCell(wordId)
        return dateKeys.map { key ->
            days[key]?.get(wordId) ?: StudyWordDayCell(
                dateKey = key,
                wordId = wordId,
                pt = label?.pt.orEmpty(),
                ru = label?.ru.orEmpty(),
            )
        }
    }
}
