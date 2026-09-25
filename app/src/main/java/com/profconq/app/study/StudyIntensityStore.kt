package com.profconq.app.study

import android.content.Context
import com.profconq.app.data.model.StudyDayBucket
import com.profconq.app.data.model.StudyIntensitySnapshot
import com.profconq.app.data.model.StudyTestWordStat
import com.profconq.app.data.model.StudyWordDayCell
import com.profconq.app.data.model.WordCard
import com.profconq.app.progress.ProgressTracker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.time.LocalDate

class StudyIntensityStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val lock = Any()
    private val _snapshot = MutableStateFlow(load())
    val snapshot: StateFlow<StudyIntensitySnapshot> = _snapshot.asStateFlow()

    fun recordTestWords(dateKey: String, words: List<StudyTestWordStat>) {
        if (words.isEmpty()) return
        mutate { days ->
            val bucket = days[dateKey].orEmpty().toMutableMap()
            words.forEach { word ->
                val prev = bucket[word.wordId] ?: emptyCell(dateKey, word.wordId, word.pt, word.ru)
                bucket[word.wordId] = prev.copy(
                    pt = word.pt.ifBlank { prev.pt },
                    ru = word.ru.ifBlank { prev.ru },
                    testRuns = prev.testRuns + word.effectiveRuns,
                    correct = prev.correct + word.correct,
                    incorrect = prev.incorrect + word.incorrect,
                )
            }
            days + (dateKey to bucket)
        }
    }

    fun recordCard(card: WordCard, known: Boolean, at: Long = System.currentTimeMillis()) {
        val dateKey = ProgressTracker.dateKeyFromMillis(at)
        mutate { days ->
            val bucket = days[dateKey].orEmpty().toMutableMap()
            val prev = bucket[card.id] ?: emptyCell(dateKey, card.id, card.pt, card.ru)
            bucket[card.id] = prev.copy(
                pt = card.pt.ifBlank { prev.pt },
                ru = card.ru.ifBlank { prev.ru },
                cardRuns = prev.cardRuns + 1,
                correct = prev.correct + if (known) 1 else 0,
                incorrect = prev.incorrect + if (known) 0 else 1,
            )
            days + (dateKey to bucket)
        }
    }

    fun recordStudio(card: WordCard, at: Long = System.currentTimeMillis()) {
        val dateKey = ProgressTracker.dateKeyFromMillis(at)
        mutate { days ->
            val bucket = days[dateKey].orEmpty().toMutableMap()
            val prev = bucket[card.id] ?: emptyCell(dateKey, card.id, card.pt, card.ru)
            bucket[card.id] = prev.copy(
                pt = card.pt.ifBlank { prev.pt },
                ru = card.ru.ifBlank { prev.ru },
                studioRuns = prev.studioRuns + 1,
            )
            days + (dateKey to bucket)
        }
    }

    fun importTestBuckets(days: Map<String, StudyDayBucket>) {
        if (days.isEmpty()) return
        mutate { current ->
            var next = current
            days.forEach { (key, bucket) ->
                val existing = next[key].orEmpty()
                if (existing.values.sumOf { it.testRuns } > 0) return@forEach
                val words = bucket.words
                if (words.isEmpty()) return@forEach
                val merged = existing.toMutableMap()
                words.forEach { word ->
                    val prev = merged[word.wordId] ?: emptyCell(key, word.wordId, word.pt, word.ru)
                    merged[word.wordId] = prev.copy(
                        pt = word.pt.ifBlank { prev.pt },
                        ru = word.ru.ifBlank { prev.ru },
                        testRuns = prev.testRuns + word.effectiveRuns,
                        correct = prev.correct + word.correct,
                        incorrect = prev.incorrect + word.incorrect,
                    )
                }
                next = next + (key to merged)
            }
            next
        }
    }

    private fun mutate(block: (Map<String, Map<String, StudyWordDayCell>>) -> Map<String, Map<String, StudyWordDayCell>>) {
        synchronized(lock) {
            val updated = prune(block(_snapshot.value.days))
            _snapshot.value = StudyIntensitySnapshot(updated)
            prefs.edit().putString(KEY_DAYS, daysToJson(updated)).apply()
        }
    }

    private fun prune(days: Map<String, Map<String, StudyWordDayCell>>): Map<String, Map<String, StudyWordDayCell>> {
        if (days.size <= KEEP_DAYS) return days
        val cutoff = ProgressTracker.dateKey(LocalDate.now().minusDays(KEEP_DAYS.toLong()))
        return days.filterKeys { it >= cutoff }
    }

    private fun load(): StudyIntensitySnapshot {
        val raw = prefs.getString(KEY_DAYS, null) ?: return StudyIntensitySnapshot()
        return runCatching {
            val obj = JSONObject(raw)
            val days = buildMap {
                val keys = obj.keys()
                while (keys.hasNext()) {
                    val dateKey = keys.next()
                    val day = obj.optJSONObject(dateKey) ?: continue
                    val cells = buildMap {
                        val wordKeys = day.keys()
                        while (wordKeys.hasNext()) {
                            val wordId = wordKeys.next()
                            val item = day.optJSONObject(wordId) ?: continue
                            put(wordId, cellFromJson(dateKey, wordId, item))
                        }
                    }
                    if (cells.isNotEmpty()) put(dateKey, cells)
                }
            }
            StudyIntensitySnapshot(days)
        }.getOrDefault(StudyIntensitySnapshot())
    }

    private fun daysToJson(days: Map<String, Map<String, StudyWordDayCell>>): String {
        val obj = JSONObject()
        days.forEach { (dateKey, cells) ->
            val day = JSONObject()
            cells.forEach { (wordId, cell) ->
                day.put(
                    wordId,
                    JSONObject()
                        .put("pt", cell.pt)
                        .put("ru", cell.ru)
                        .put("t", cell.testRuns)
                        .put("c", cell.cardRuns)
                        .put("s", cell.studioRuns)
                        .put("ok", cell.correct)
                        .put("bad", cell.incorrect),
                )
            }
            obj.put(dateKey, day)
        }
        return obj.toString()
    }

    private fun cellFromJson(dateKey: String, wordId: String, item: JSONObject): StudyWordDayCell =
        StudyWordDayCell(
            dateKey = dateKey,
            wordId = wordId,
            pt = item.optString("pt"),
            ru = item.optString("ru"),
            testRuns = item.optInt("t"),
            cardRuns = item.optInt("c"),
            studioRuns = item.optInt("s"),
            correct = item.optInt("ok"),
            incorrect = item.optInt("bad"),
        )

    private fun emptyCell(dateKey: String, wordId: String, pt: String, ru: String) =
        StudyWordDayCell(dateKey = dateKey, wordId = wordId, pt = pt, ru = ru)

    private companion object {
        const val PREFS = "profconq_study_intensity"
        const val KEY_DAYS = "days.v1"
        const val KEEP_DAYS = 90
    }
}
