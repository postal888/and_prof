package com.profconq.app.api

import com.profconq.app.data.repository.WordNormalizer

/**
 * The outcome of merging the device's vocabulary with the server's.
 *
 * @property merged the list the server gets: every word from both sides once.
 * @property applyLocally server versions the device has to take over (new or newer there).
 */
data class VocabularyMergeResult(
    val merged: List<WebVocabWord>,
    val applyLocally: List<WebVocabWord>,
    val addedToApp: Int,
    val updatedInApp: Int,
    val sentToSite: Int,
)

/**
 * Joins both vocabularies by word. A word on one side only is kept; a word on both sides keeps the
 * version edited later, and [preferLocalOnTie] (the chosen sync source) breaks equal times, which
 * includes words neither side has a time for.
 *
 * Server words keep their id and their website review fields even when the device's content wins,
 * because the website's programs and progress point at those ids.
 */
fun mergeVocabulary(
    local: List<WebVocabWord>,
    server: List<WebVocabWord>,
    preferLocalOnTie: Boolean,
): VocabularyMergeResult {
    val localByKey = LinkedHashMap<String, WebVocabWord>()
    for (word in local) {
        val key = WordNormalizer.normalize(word.word)
        if (key.isNotEmpty()) localByKey.putIfAbsent(key, word)
    }

    val merged = mutableListOf<WebVocabWord>()
    val applyLocally = mutableListOf<WebVocabWord>()
    val seen = hashSetOf<String>()
    var addedToApp = 0
    var updatedInApp = 0
    var sentToSite = 0
    var maxId = server.maxOfOrNull { it.id } ?: 0

    for (remote in server) {
        val key = WordNormalizer.normalize(remote.word)
        if (key.isEmpty() || !seen.add(key)) continue
        val mine = localByKey[key]
        if (mine == null) {
            merged += remote
            applyLocally += remote
            addedToApp++
            continue
        }
        val localWins = when {
            mine.updatedAt > remote.updatedAt -> true
            mine.updatedAt < remote.updatedAt -> false
            else -> preferLocalOnTie
        }
        val differs = !sameContent(mine, remote)
        if (localWins) {
            merged += remote.copy(
                word = mine.word,
                translation = mine.translation,
                example = mine.example,
                exampleRu = mine.exampleRu,
                tag = mine.tag,
                infinitivo = mine.infinitivo,
                img = mine.img.ifBlank { remote.img },
                videoId = mine.videoId ?: remote.videoId,
                videoTitle = if (mine.videoId != null) mine.videoTitle else remote.videoTitle,
                updatedAt = maxOf(mine.updatedAt, remote.updatedAt),
            )
            if (differs) sentToSite++
        } else {
            merged += remote
            if (differs) {
                applyLocally += remote
                updatedInApp++
            }
        }
    }

    for ((key, mine) in localByKey) {
        if (key in seen) continue
        merged += mine.copy(id = ++maxId)
        sentToSite++
    }

    return VocabularyMergeResult(
        merged = merged,
        applyLocally = applyLocally,
        addedToApp = addedToApp,
        updatedInApp = updatedInApp,
        sentToSite = sentToSite,
    )
}

private fun sameContent(a: WebVocabWord, b: WebVocabWord): Boolean =
    a.word.trim() == b.word.trim() &&
        a.translation.trim() == b.translation.trim() &&
        a.example.trim() == b.example.trim() &&
        a.exampleRu.trim() == b.exampleRu.trim() &&
        a.infinitivo.trim() == b.infinitivo.trim()
