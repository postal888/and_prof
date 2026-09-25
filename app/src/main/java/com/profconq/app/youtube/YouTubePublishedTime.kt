package com.profconq.app.youtube

object YouTubePublishedTime {
    private const val MINUTE = 60_000L
    private const val HOUR = 60 * MINUTE
    private const val DAY = 24 * HOUR

    fun parseToMillis(text: String?, now: Long = System.currentTimeMillis()): Long? {
        val raw = text?.trim()?.lowercase().orEmpty()
        if (raw.isEmpty()) return null
        val amount = Regex("""(\d+)""").find(raw)?.value?.toLongOrNull() ?: 1L
        val ago = when {
            containsAny(raw, "ano", "year") -> amount * 365 * DAY
            containsAny(raw, "mese", "mês", "mes ", "month") -> amount * 30 * DAY
            containsAny(raw, "semana", "week") -> amount * 7 * DAY
            containsAny(raw, "dia", "day") -> amount * DAY
            containsAny(raw, "hora", "hour") -> amount * HOUR
            containsAny(raw, "minuto", "minute") -> amount * MINUTE
            containsAny(raw, "segundo", "second") -> amount * 1000
            else -> return null
        }
        return (now - ago).coerceAtLeast(0L)
    }

    private fun containsAny(text: String, vararg tokens: String): Boolean =
        tokens.any { token -> text.contains(token) }
}
