package com.profconq.app.data.model

object VocabLearnMark {
    const val DEFAULT = 1
    const val DAILY_CAP = 4
    const val MASTER = 5
    const val MASTER_STREAK_DAYS = 3

    fun level(raw: String?, known: Boolean = false, due: Boolean = false): Int {
        val normalized = when (raw?.trim()?.lowercase()) {
            "0", "off" -> 0
            "1" -> 1
            "2", "red", "weak" -> 2
            "3", "yellow", "medium" -> 3
            "4" -> 4
            "5", "green", "good" -> 5
            else -> -1
        }
        if (normalized in 0..5) return normalized
        return if (known) MASTER else DEFAULT
    }

    fun storage(level: Int): String? =
        if (level in 0..5) level.toString() else null

    fun due(level: Int): Boolean = level in 1..2

    fun known(level: Int): Boolean = level == MASTER
}
