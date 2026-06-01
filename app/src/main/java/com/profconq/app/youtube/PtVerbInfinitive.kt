package com.profconq.app.youtube

/**
 * Guesses Portuguese verb infinitive (-ar / -er / -ir) from a single word form.
 * Heuristic only — may miss irregular verbs or over-match rare cases.
 */
object PtVerbInfinitive {
    private val INFINITIVE_PATTERN = Regex("""^[a-záàâãéêíóôõúç]{3,}(ar|er|ir)$""")

    fun guess(ptWord: String): String? {
        val core = normalize(ptWord) ?: return null
        if (core.contains(' ')) return null

        if (INFINITIVE_PATTERN.matches(core)) return core

        // Gerund
        when {
            core.endsWith("ando") && core.length > 5 -> return core.removeSuffix("ando") + "ar"
            core.endsWith("endo") && core.length > 5 -> return core.removeSuffix("endo") + "er"
            core.endsWith("indo") && core.length > 5 -> return core.removeSuffix("indo") + "ir"
        }

        // Past participle
        when {
            core.endsWith("ados") && core.length > 5 -> return core.removeSuffix("ados") + "ar"
            core.endsWith("adas") && core.length > 5 -> return core.removeSuffix("adas") + "ar"
            core.endsWith("ado") && core.length > 4 -> return core.removeSuffix("ado") + "ar"
            core.endsWith("ada") && core.length > 4 -> return core.removeSuffix("ada") + "ar"
            core.endsWith("idos") && core.length > 5 -> return core.removeSuffix("idos") + "ir"
            core.endsWith("ido") && core.length > 4 -> return core.removeSuffix("ido") + "ir"
        }

        // Preterite
        when {
            core.endsWith("aram") && core.length > 5 -> return core.removeSuffix("aram") + "ar"
            core.endsWith("eram") && core.length > 5 -> return core.removeSuffix("eram") + "er"
            core.endsWith("iram") && core.length > 5 -> return core.removeSuffix("iram") + "ir"
            core.endsWith("ou") && core.length > 4 -> return core.removeSuffix("ou") + "ar"
            core.endsWith("eu") && core.length > 4 -> return core.removeSuffix("eu") + "er"
            core.endsWith("iu") && core.length > 4 -> return core.removeSuffix("iu") + "ir"
        }

        // Present / subjunctive plural
        when {
            core.endsWith("amos") && core.length > 5 -> return core.removeSuffix("amos") + "ar"
            core.endsWith("ámos") && core.length > 5 -> return core.removeSuffix("ámos") + "ar"
            core.endsWith("emos") && core.length > 5 -> return core.removeSuffix("emos") + "er"
            core.endsWith("imos") && core.length > 5 -> return core.removeSuffix("imos") + "ir"
            core.endsWith("am") && core.length > 4 && !core.endsWith("iam") -> return core.removeSuffix("am") + "ar"
        }

        if (core.length >= 5) {
            when {
                core.endsWith("as") -> return core.removeSuffix("as") + "ar"
                core.endsWith("es") -> return core.removeSuffix("es") + "er"
                core.endsWith("e") -> return core.removeSuffix("e") + "ar"
                core.endsWith("o") -> return core.removeSuffix("o") + "ar"
            }
        }

        return null
    }

    fun shouldShowInfinitive(ptForm: String, infinitive: String?): Boolean {
        if (infinitive.isNullOrBlank()) return false
        val norm = normalize(ptForm) ?: return true
        return !norm.equals(infinitive, ignoreCase = true)
    }

    fun merge(localGuess: String?, parsedFromTranslation: String?, ptForm: String): String? {
        val inf = parsedFromTranslation?.takeIf { it.isNotBlank() } ?: localGuess
        return inf?.takeIf { shouldShowInfinitive(ptForm, it) }
    }

    fun parseInfinitiveFromTranslation(raw: String): Pair<String, String?> {
        val infLine = Regex(
            """(?m)^\s*\*?\s*инф\.?\s*[-–:]\s*([a-záàâãéêíóôõúç]{2,})\s*$""",
            RegexOption.IGNORE_CASE,
        ).find(raw)
        if (infLine != null) {
            val inf = infLine.groupValues[1].trim()
            val ru = raw.replace(infLine.value, "").trim()
            return ru to inf
        }
        return raw.trim() to null
    }

    private fun normalize(word: String): String? {
        val raw = word.trim()
        if (raw.isEmpty() || raw.contains(' ')) return null
        val core = raw
            .trim('«', '»', '"', '\'', '(', ')', '“', '”')
            .lowercase()
            .replace(Regex("[.,!?;:…]+$"), "")
        if (core.length < 3) return null
        if (!core.all { it.isLetter() || it in "áàâãéêíóôõúç" }) return null
        return core
    }
}
