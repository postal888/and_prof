package com.profconq.app.studio

import com.profconq.app.data.model.WordCard
import com.profconq.app.ui.i18n.SubtitleLanguage
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

data class StudioRenderMeta(
    val hash: String,
    val voice: String,
    val dir: String,
    val speakNum: Boolean = true,
    val speakWord: Boolean = true,
    val speakTr: Boolean = true,
    val speakEx: Boolean = true,
    val speakExRu: Boolean = true,
    val clips: Int = 0,
    val at: Long = 0L,
)

data class StudioTtsJob(
    val text: String,
    val lang: String,
    val voiceId: String,
    val id: String,
)

data class StudioCollection(
    val id: String,
    val title: String,
    /** Local Room card ids, in play order. */
    val wordIds: List<String> = emptyList(),
    /** Website numeric vocab ids (for sync round-trip). */
    val webWordIds: List<Int> = emptyList(),
    val render: StudioRenderMeta? = null,
) {
    val resolvedCount: Int get() = wordIds.size
}

enum class StudioVoice(
    val key: String,
    val label: String,
    val elevenLabsId: String,
    val female: Boolean,
) {
    Antoni("antoni", "Antoni", "ErXwobaYiN019PkySvjV", false),
    Camila("camila", "Camila", "pFZP5JQG7iQjIQuC4Bku", true),
    Rafael("rafael", "Rafael", "nPczCjzI2devNBz1zQrb", false),
    ;

    companion object {
        fun fromKey(key: String?): StudioVoice =
            entries.find { it.key.equals(key?.trim(), ignoreCase = true) } ?: Antoni
    }
}

data class StudioPrefs(
    /** Forward direction: source → target (legacy name kept for sync). */
    val dirPtToRu: Boolean = true,
    val voice: String = StudioVoice.Antoni.key,
    val sourceLang: Int = SubtitleLanguage.PT,
    val targetLang: Int = SubtitleLanguage.RU,
    val speed: Float = 1f,
    val speedNum: Float = 1f,
    val speedWord: Float = 1f,
    val speedTr: Float = 1f,
    val speedEx: Float = 1f,
    val speedExRu: Float = 1f,
    val pauseAfterSec: Float = 1f,
    val pauseBetweenSec: Float = 1f,
    val pauseNumSec: Float = 1f,
    val pauseWordSec: Float = 1f,
    val pauseTrSec: Float = 1f,
    val pauseExSec: Float = 1f,
    val pauseExRuSec: Float = 1f,
    val pauseBetweenCardsSec: Float = 1f,
    val speakWord: Boolean = true,
    val speakTr: Boolean = true,
    val speakEx: Boolean = true,
    val speakExRu: Boolean = true,
    val speakNum: Boolean = true,
    val loop: Boolean = false,
    val shuffle: Boolean = false,
) {
    val sourceCode: String get() = SubtitleLanguage.toCode(sourceLang)
    val targetCode: String get() = SubtitleLanguage.toCode(targetLang)
    val forward: Boolean get() = dirPtToRu
}

data class StudioStage(
    val reveal: Int,
    val text: String,
    val lang: String,
    val kind: String,
)

enum class StudioDir {
    PtRu,
    RuPt,
}

val StudioWordTags = listOf(
    "geral",
    "substantivo",
    "verbo",
    "adjetivo",
    "adverbio",
    "pronome",
    "preposição",
    "conjunção",
    "expressão",
    "frase",
)

fun studioIsPhraseTag(tag: String?): Boolean {
    val key = tag.orEmpty().trim().lowercase()
    return key == "frase" || key == "expressão" || key == "expressao" || key == "phrase"
}

fun StudioPrefs.dir(): StudioDir = if (dirPtToRu) StudioDir.PtRu else StudioDir.RuPt

fun buildStudioStages(
    card: WordCard,
    cardNum: Int,
    prefs: StudioPrefs,
): List<StudioStage> {
    val stages = mutableListOf<StudioStage>()
    fun push(reveal: Int, text: String?, lang: String, kind: String) {
        val t = text?.trim().orEmpty()
        if (t.isEmpty() || t == "—") return
        stages += StudioStage(reveal = reveal, text = t, lang = lang, kind = kind)
    }

    val sourceText = card.pt.trim().ifEmpty { card.example.orEmpty().trim() }
    val targetText = card.ru.trim().ifEmpty { card.exampleTranslation.orEmpty().trim() }
    val ex = card.example.orEmpty().trim()
    val exTr = card.exampleTranslation.orEmpty().trim()
    val srcLang = prefs.sourceCode
    val tgtLang = prefs.targetCode
    val isPhrase = card.partOfSpeech.equals("frase", ignoreCase = true) ||
        card.partOfSpeech.equals("phrase", ignoreCase = true)

    if (prefs.speakNum && cardNum > 0) {
        val numLang = if (prefs.forward) srcLang else tgtLang
        push(-1, studioCardNumberSpeakText(cardNum, numLang), numLang, "num")
    }

    if (isPhrase) {
        val phraseSrc = ex.ifEmpty { sourceText }
        val phraseTgt = exTr.ifEmpty { targetText }
        if (prefs.forward) {
            if (prefs.speakWord) push(0, phraseSrc, srcLang, "word")
            if (prefs.speakTr) push(1, phraseTgt, tgtLang, "tr")
        } else {
            if (prefs.speakWord) push(0, phraseTgt, tgtLang, "word")
            if (prefs.speakTr) push(1, phraseSrc, srcLang, "tr")
        }
        if (stages.none { it.kind != "num" }) {
            push(
                0,
                if (prefs.forward) phraseSrc else phraseTgt,
                if (prefs.forward) srcLang else tgtLang,
                "word",
            )
        }
        return stages
    }

    if (prefs.forward) {
        if (prefs.speakWord) push(0, sourceText, srcLang, "word")
        if (prefs.speakTr) push(1, targetText, tgtLang, "tr")
    } else {
        if (prefs.speakWord) push(0, targetText, tgtLang, "word")
        if (prefs.speakTr) push(1, sourceText, srcLang, "tr")
    }
    if (prefs.speakEx && ex.isNotEmpty() && !ex.equals(sourceText, ignoreCase = true)) {
        push(2, ex, srcLang, "ex")
    }
    if (prefs.speakExRu && exTr.isNotEmpty() && !exTr.equals(targetText, ignoreCase = true)) {
        push(3, exTr, tgtLang, "exRu")
    }
    if (stages.none { it.kind != "num" }) {
        push(
            0,
            if (prefs.forward) sourceText else targetText,
            if (prefs.forward) srcLang else tgtLang,
            "word",
        )
    }
    return stages
}

fun studioCardNumberSpeakText(cardNum: Int, langCode: String): String {
    if (cardNum <= 0) return ""
    return when (langCode.lowercase()) {
        "ru" -> "карточка ${studioNumberWordRu(cardNum)}."
        "en" -> "card ${studioNumberWordEn(cardNum)}."
        else -> "cartão ${studioNumberWordPt(cardNum)}."
    }
}
private fun studioNumberWordEn(n: Int): String {
    val units = arrayOf(
        "zero", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine",
        "ten", "eleven", "twelve", "thirteen", "fourteen", "fifteen",
        "sixteen", "seventeen", "eighteen", "nineteen",
    )
    val tens = arrayOf(
        "", "", "twenty", "thirty", "forty", "fifty", "sixty", "seventy", "eighty", "ninety",
    )
    if (n < 0) return ""
    if (n < 20) return units[n]
    if (n < 100) {
        val t = n / 10
        val r = n % 10
        return if (r == 0) tens[t] else "${tens[t]}-${units[r]}"
    }
    if (n < 1000) {
        val h = n / 100
        val rest = n % 100
        val head = "${units[h]} hundred"
        return if (rest == 0) head else "$head ${studioNumberWordEn(rest)}"
    }
    return n.toString()
}

private fun studioNumberWordPt(n: Int): String {
    val units = arrayOf(
        "zero", "um", "dois", "três", "quatro", "cinco", "seis", "sete", "oito", "nove",
        "dez", "onze", "doze", "treze", "catorze", "quinze", "dezesseis", "dezessete", "dezoito", "dezenove",
    )
    val tens = arrayOf("", "", "vinte", "trinta", "quarenta", "cinquenta", "sessenta", "setenta", "oitenta", "noventa")
    val hundreds = arrayOf(
        "", "cento", "duzentos", "trezentos", "quatrocentos", "quinhentos",
        "seiscentos", "setecentos", "oitocentos", "novecentos",
    )
    if (n < 0) return ""
    if (n < 20) return units[n]
    if (n < 100) {
        val t = n / 10
        val r = n % 10
        return if (r == 0) tens[t] else "${tens[t]} e ${units[r]}"
    }
    if (n == 100) return "cem"
    if (n < 1000) {
        val h = n / 100
        val rest = n % 100
        val head = if (h == 1) "cento" else hundreds[h]
        return if (rest == 0) head else "$head e ${studioNumberWordPt(rest)}"
    }
    return n.toString()
}

private fun studioNumberWordRu(n: Int): String {
    val units = arrayOf(
        "ноль", "один", "два", "три", "четыре", "пять", "шесть", "семь", "восемь", "девять",
        "десять", "одиннадцать", "двенадцать", "тринадцать", "четырнадцать", "пятнадцать",
        "шестнадцать", "семнадцать", "восемнадцать", "девятнадцать",
    )
    val tens = arrayOf("", "", "двадцать", "тридцать", "сорок", "пятьдесят", "шестьдесят", "семьдесят", "восемьдесят", "девяносто")
    val hundreds = arrayOf("", "сто", "двести", "триста", "четыреста", "пятьсот", "шестьсот", "семьсот", "восемьсот", "девятьсот")
    if (n < 0) return ""
    if (n < 20) return units[n]
    if (n < 100) {
        val t = n / 10
        val r = n % 10
        return if (r == 0) tens[t] else "${tens[t]} ${units[r]}"
    }
    if (n < 1000) {
        val h = n / 100
        val rest = n % 100
        return if (rest == 0) hundreds[h] else "${hundreds[h]} ${studioNumberWordRu(rest)}"
    }
    return n.toString()
}

fun pauseAfterStage(stage: StudioStage, prefs: StudioPrefs): Float =
    prefs.pauseForKind(stage.kind)

fun StudioPrefs.speedForKind(kind: String): Float =
    when (kind) {
        "num" -> speedNum
        "word" -> speedWord
        "tr" -> speedTr
        "ex" -> speedEx
        else -> speedExRu
    }.coerceIn(0.5f, 2f)

fun StudioPrefs.pauseForKind(kind: String): Float =
    when (kind) {
        "num" -> pauseNumSec
        "word" -> pauseWordSec
        "tr" -> pauseTrSec
        "ex" -> pauseExSec
        "cards" -> pauseBetweenCardsSec
        else -> pauseExRuSec
    }.coerceIn(0f, 5f)

fun StudioPrefs.withStageSpeed(kind: String, value: Float): StudioPrefs {
    val v = value.coerceIn(0.5f, 2f)
    return when (kind) {
        "num" -> copy(speedNum = v)
        "word" -> copy(speedWord = v, speed = v)
        "tr" -> copy(speedTr = v)
        "ex" -> copy(speedEx = v)
        else -> copy(speedExRu = v)
    }
}

fun StudioPrefs.withStagePause(kind: String, value: Float): StudioPrefs {
    val v = value.coerceIn(0f, 5f)
    return when (kind) {
        "num" -> copy(pauseNumSec = v)
        "word" -> copy(pauseWordSec = v, pauseAfterSec = v)
        "tr" -> copy(pauseTrSec = v, pauseBetweenSec = v)
        "ex" -> copy(pauseExSec = v)
        "exRu" -> copy(pauseExRuSec = v)
        "cards" -> copy(pauseBetweenCardsSec = v)
        else -> this
    }
}

fun studioDirKey(prefs: StudioPrefs): String {
    val src = prefs.sourceCode
    val tgt = prefs.targetCode
    return if (prefs.forward) "$src-$tgt" else "$tgt-$src"
}

fun studioSpeakKey(prefs: StudioPrefs): String =
    "speak:" + listOf(prefs.speakNum, prefs.speakWord, prefs.speakTr, prefs.speakEx, prefs.speakExRu)
        .joinToString("") { if (it) "1" else "0" }

fun studioTtsClipId(text: String, lang: String, voiceId: String): String =
    listOf(voiceId, lang, studioTtsNormText(text)).joinToString("\u0001")

fun studioTtsDjb2(s: String): String {
    var h = 5381
    for (ch in s) {
        h = (h shl 5) + h xor ch.code
    }
    return java.lang.Integer.toUnsignedString(h, 36)
}

fun studioCollectionAudioJobs(cards: List<WordCard>, prefs: StudioPrefs): List<StudioTtsJob> {
    val voiceId = StudioVoice.fromKey(prefs.voice).elevenLabsId
    val seen = mutableSetOf<String>()
    val jobs = mutableListOf<StudioTtsJob>()
    cards.forEachIndexed { index, card ->
        buildStudioStages(card, index + 1, prefs).forEach { stage ->
            val text = studioTtsNormText(stage.text)
            if (text.isEmpty()) return@forEach
            val id = studioTtsClipId(text, stage.lang, voiceId)
            if (seen.add(id)) {
                jobs += StudioTtsJob(text = text, lang = stage.lang, voiceId = voiceId, id = id)
            }
        }
    }
    return jobs
}

fun studioCollectionAudioHash(cards: List<WordCard>, prefs: StudioPrefs): String {
    val jobs = studioCollectionAudioJobs(cards, prefs)
    val parts = buildList {
        add(studioDirKey(prefs))
        add(StudioVoice.fromKey(prefs.voice).elevenLabsId)
        add(studioSpeakKey(prefs))
        jobs.forEach { add("${it.lang}:${it.text}") }
        add(
            "p:" + listOf(
                prefs.pauseNumSec,
                prefs.pauseWordSec,
                prefs.pauseTrSec,
                prefs.pauseExSec,
                prefs.pauseExRuSec,
            ).joinToString(":") { "%.1f".format(Locale.US, it) },
        )
    }
    return studioTtsDjb2(parts.joinToString("|"))
}

fun studioCollectionIsRendered(
    collection: StudioCollection?,
    cards: List<WordCard>,
    prefs: StudioPrefs,
): Boolean {
    val render = collection?.render ?: return false
    if (cards.isEmpty() || render.hash.isBlank()) return false
    if (render.voice.isNotBlank() && render.voice != prefs.voice) return false
    if (render.dir.isNotBlank() && render.dir != studioDirKey(prefs)) return false
    return render.hash == studioCollectionAudioHash(cards, prefs)
}

fun studioTtsNormText(text: String): String {
    var s = text
        .replace(Regex("<[^>]+>"), " ")
        .replace("&nbsp;", " ", ignoreCase = true)
        .replace(Regex("&#\\d+;"), " ")
        .replace(Regex("&[a-z]+;", RegexOption.IGNORE_CASE), " ")
        .replace(Regex("[\\u200B-\\u200D\\uFEFF\\u00AD]"), "")
        .replace(Regex("\\s*\\*+\\s*(?:инф|inf(?:initive)?)\\.?\\s*[-–—:].*$", setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE)), "")
        .replace(Regex("\\n\\s*\\*.*$", RegexOption.MULTILINE), "")
        .replace(Regex("\\s+"), " ")
        .trim()
    if (s.isEmpty()) return ""
    val letters = s.replace(Regex("[^A-Za-zÀ-ÿА-Яа-яЁё]"), "")
    if (letters.length > 3 && letters == letters.uppercase()) {
        s = s.lowercase()
    }
    return s.take(2500)
}

fun studioTtsResolveLang(text: String, fallback: String): String {
    val fb = fallback.lowercase().trim().ifBlank { "pt" }
    val cyr = text.count { it in 'А'..'я' || it == 'Ё' || it == 'ё' }
    val lat = text.count { it.isLetter() && it !in 'А'..'я' && it != 'Ё' && it != 'ё' }
    return when {
        cyr > 0 && cyr >= lat -> "ru"
        lat > 0 && lat > cyr -> if (fb == "ru") "pt" else fb
        else -> fb
    }
}

fun parseStudioCollectionsJson(raw: String?): List<StudioCollection> {
    if (raw.isNullOrBlank()) return emptyList()
    return runCatching {
        val arr = JSONArray(raw)
        buildList {
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                normalizeStudioCollection(obj, i)?.let(::add)
            }
        }
    }.getOrDefault(emptyList())
}

fun normalizeStudioCollection(obj: JSONObject, idx: Int): StudioCollection? {
    val id = obj.optString("id").ifBlank { "col_${System.currentTimeMillis()}_$idx" }
    val title = obj.optString("title").ifBlank { "Collection ${idx + 1}" }
    val idsArr = obj.optJSONArray("wordIds") ?: obj.optJSONArray("ids")
    val webIds = mutableListOf<Int>()
    val localIds = mutableListOf<String>()
    if (idsArr != null) {
        for (j in 0 until idsArr.length()) {
            if (idsArr.isNull(j)) continue
            val asInt = idsArr.optInt(j, Int.MIN_VALUE)
            if (asInt != Int.MIN_VALUE && asInt != 0) {
                webIds += asInt
                continue
            }
            val asStr = idsArr.optString(j).trim()
            if (asStr.isNotEmpty()) {
                asStr.toIntOrNull()?.let { webIds += it } ?: run { localIds += asStr }
            }
        }
    }
    return StudioCollection(
        id = id,
        title = title,
        wordIds = localIds,
        webWordIds = webIds,
        render = parseStudioRenderMeta(obj.optJSONObject("render")),
    )
}

private fun parseStudioRenderMeta(obj: JSONObject?): StudioRenderMeta? {
    if (obj == null) return null
    val hash = obj.optString("hash").trim()
    if (hash.isEmpty()) return null
    val speak = obj.optJSONObject("speak")
    return StudioRenderMeta(
        hash = hash,
        voice = obj.optString("voice").trim(),
        dir = obj.optString("dir").trim(),
        speakNum = speak?.optBoolean("num", true) ?: true,
        speakWord = speak?.optBoolean("word", true) ?: true,
        speakTr = speak?.optBoolean("tr", true) ?: true,
        speakEx = speak?.optBoolean("ex", true) ?: true,
        speakExRu = speak?.optBoolean("exRu", true) ?: true,
        clips = obj.optInt("clips"),
        at = obj.optLong("at"),
    )
}

private fun StudioRenderMeta.toJson(): JSONObject =
    JSONObject()
        .put("hash", hash)
        .put("voice", voice)
        .put("dir", dir)
        .put(
            "speak",
            JSONObject()
                .put("num", speakNum)
                .put("word", speakWord)
                .put("tr", speakTr)
                .put("ex", speakEx)
                .put("exRu", speakExRu),
        )
        .put("clips", clips)
        .put("at", at)

fun studioCollectionsToJson(collections: List<StudioCollection>): String {
    val arr = JSONArray()
    collections.forEach { col ->
        val ids = JSONArray()
        if (col.webWordIds.isNotEmpty()) {
            col.webWordIds.forEach { ids.put(it) }
        } else {
            col.wordIds.forEach { ids.put(it) }
        }
        val obj = JSONObject()
            .put("id", col.id)
            .put("title", col.title)
            .put("wordIds", ids)
        col.render?.let { obj.put("render", it.toJson()) }
        arr.put(obj)
    }
    return arr.toString()
}
