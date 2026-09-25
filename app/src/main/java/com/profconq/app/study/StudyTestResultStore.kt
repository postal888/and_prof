package com.profconq.app.study

import android.content.Context
import com.profconq.app.data.model.LastStudyTestResult
import com.profconq.app.data.model.StudyDayBucket
import com.profconq.app.data.model.StudyDaySession
import com.profconq.app.data.model.StudyTestWordStat
import com.profconq.app.progress.ProgressTracker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

class StudyTestResultStore(
    context: Context,
    private val intensity: StudyIntensityStore,
    private val onDayUpdated: (StudyDayBucket) -> Unit = {},
) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val _last = MutableStateFlow(loadLast())
    val last: StateFlow<LastStudyTestResult?> = _last.asStateFlow()

    private val _days = MutableStateFlow(loadDays())
    val days: StateFlow<Map<String, StudyDayBucket>> = _days.asStateFlow()

    init {
        intensity.importTestBuckets(_days.value)
    }

    fun save(result: LastStudyTestResult) {
        val key = ProgressTracker.dateKeyFromMillis(result.at)
        val current = _days.value[key] ?: StudyDayBucket(dateKey = key)
        val session = StudyDaySession(
            kind = result.kind,
            setName = result.setName,
            correct = result.correct,
            incorrect = result.incorrect,
            at = result.at,
            words = result.words,
        )
        val updated = _days.value + (key to current.copy(sessions = current.sessions + session))
        _last.value = result
        _days.value = updated
        prefs.edit()
            .putString(KEY_SET_ID, result.setId)
            .putString(KEY_SET_NAME, result.setName)
            .putString(KEY_KIND, result.kind)
            .putInt(KEY_CORRECT, result.correct)
            .putInt(KEY_INCORRECT, result.incorrect)
            .putLong(KEY_AT, result.at)
            .putString(KEY_WORDS, wordsToArray(result.words).toString())
            .putString(KEY_DAYS, daysToJson(updated))
            .apply()
        intensity.recordTestWords(key, result.words)
        onDayUpdated(updated.getValue(key))
    }

    fun clearIfSet(setId: String) {
        if (_last.value?.setId == setId) {
            prefs.edit()
                .remove(KEY_SET_ID)
                .remove(KEY_SET_NAME)
                .remove(KEY_KIND)
                .remove(KEY_CORRECT)
                .remove(KEY_INCORRECT)
                .remove(KEY_AT)
                .remove(KEY_WORDS)
                .apply()
            _last.value = null
        }
    }

    private fun loadLast(): LastStudyTestResult? {
        val at = prefs.getLong(KEY_AT, 0L)
        if (at <= 0L) return null
        val setId = prefs.getString(KEY_SET_ID, null)?.takeIf { it.isNotBlank() } ?: return null
        return LastStudyTestResult(
            setId = setId,
            setName = prefs.getString(KEY_SET_NAME, "").orEmpty(),
            kind = prefs.getString(KEY_KIND, LastStudyTestResult.KIND_CHOICE)
                ?: LastStudyTestResult.KIND_CHOICE,
            correct = prefs.getInt(KEY_CORRECT, 0),
            incorrect = prefs.getInt(KEY_INCORRECT, 0),
            at = at,
            words = wordsFromJson(prefs.getString(KEY_WORDS, null)),
        )
    }

    private fun loadDays(): Map<String, StudyDayBucket> {
        val raw = prefs.getString(KEY_DAYS, null) ?: return emptyMap()
        return runCatching {
            val obj = JSONObject(raw)
            buildMap {
                val keys = obj.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val day = obj.optJSONObject(key) ?: continue
                    put(key, bucketFromJson(key, day))
                }
            }
        }.getOrDefault(emptyMap())
    }

    private fun bucketFromJson(key: String, day: JSONObject): StudyDayBucket {
        val sessionsArr = day.optJSONArray("sessions")
        val sessions = if (sessionsArr != null && sessionsArr.length() > 0) {
            buildList {
                for (i in 0 until sessionsArr.length()) {
                    val item = sessionsArr.optJSONObject(i) ?: continue
                    add(
                        StudyDaySession(
                            kind = item.optString("kind", LastStudyTestResult.KIND_CHOICE),
                            setName = item.optString("name"),
                            correct = item.optInt("ok"),
                            incorrect = item.optInt("bad"),
                            at = item.optLong("at"),
                            words = wordsFromArray(item.optJSONArray("words")),
                        ),
                    )
                }
            }
        } else {
            val correct = day.optInt("ok")
            val incorrect = day.optInt("bad")
            val tests = day.optInt("tests")
            val games = day.optInt("games")
            val words = wordsFromArray(day.optJSONArray("words"))
            if (correct == 0 && incorrect == 0 && tests == 0 && games == 0 && words.isEmpty()) {
                emptyList()
            } else {
                val kind = if (games > 0) LastStudyTestResult.KIND_MATCH else LastStudyTestResult.KIND_CHOICE
                List(tests.coerceAtLeast(1)) { index ->
                    StudyDaySession(
                        kind = if (index < games) LastStudyTestResult.KIND_MATCH else kind,
                        setName = "",
                        correct = if (index == 0) correct else 0,
                        incorrect = if (index == 0) incorrect else 0,
                        at = 0L,
                        words = if (index == 0) words else emptyList(),
                    )
                }
            }
        }
        return StudyDayBucket(dateKey = key, sessions = sessions)
    }

    private fun daysToJson(days: Map<String, StudyDayBucket>): String {
        val obj = JSONObject()
        days.forEach { (key, day) ->
            val sessions = JSONArray()
            day.sessions.forEach { session ->
                sessions.put(
                    JSONObject()
                        .put("kind", session.kind)
                        .put("name", session.setName)
                        .put("ok", session.correct)
                        .put("bad", session.incorrect)
                        .put("at", session.at)
                        .put("words", wordsToArray(session.words)),
                )
            }
            obj.put(key, JSONObject().put("sessions", sessions))
        }
        return obj.toString()
    }

    private fun wordsToArray(words: List<StudyTestWordStat>): JSONArray {
        val array = JSONArray()
        words.forEach { word ->
            array.put(
                JSONObject()
                    .put("id", word.wordId)
                    .put("pt", word.pt)
                    .put("ru", word.ru)
                    .put("ok", word.correct)
                    .put("bad", word.incorrect)
                    .put("runs", word.effectiveRuns),
            )
        }
        return array
    }

    private fun wordsFromArray(array: JSONArray?): List<StudyTestWordStat> {
        if (array == null || array.length() == 0) return emptyList()
        return buildList {
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                val id = obj.optString("id").takeIf { it.isNotBlank() } ?: continue
                add(
                    StudyTestWordStat(
                        wordId = id,
                        pt = obj.optString("pt"),
                        ru = obj.optString("ru"),
                        correct = obj.optInt("ok"),
                        incorrect = obj.optInt("bad"),
                        runs = obj.optInt("runs").let { if (it > 0) it else 1 },
                    ),
                )
            }
        }
    }

    private fun wordsFromJson(raw: String?): List<StudyTestWordStat> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching { wordsFromArray(JSONArray(raw)) }.getOrDefault(emptyList())
    }

    private companion object {
        const val PREFS = "profconq_study_test"
        const val KEY_SET_ID = "last.set_id"
        const val KEY_SET_NAME = "last.set_name"
        const val KEY_KIND = "last.kind"
        const val KEY_CORRECT = "last.correct"
        const val KEY_INCORRECT = "last.incorrect"
        const val KEY_AT = "last.at"
        const val KEY_WORDS = "last.words"
        const val KEY_DAYS = "days.v1"
    }
}
