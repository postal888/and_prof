package com.profconq.app.study

import android.content.Context
import com.profconq.app.data.model.StudyDayBucket
import com.profconq.app.data.model.VocabLearnMark
import com.profconq.app.data.repository.ProfconqRepository
import com.profconq.app.progress.ProgressTracker
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject

data class VocabLampDayState(
    val lastDay: String? = null,
    val failed: Boolean = false,
    val appliedFrom: Int? = null,
    val fifthStreak: Int = 0,
    val reachedFourOn: String? = null,
    val lastPerfectDay: String? = null,
)

data class VocabLampDecision(
    val newLevel: Int? = null,
    val state: VocabLampDayState,
)

object VocabTestLamps {
    fun decide(
        currentLevel: Int,
        correct: Int,
        incorrect: Int,
        today: String,
        prev: VocabLampDayState,
    ): VocabLampDecision {
        val attempts = correct + incorrect
        if (attempts <= 0) return VocabLampDecision(state = prev)

        val level = currentLevel.let { if (it <= 0) VocabLearnMark.DEFAULT else it }

        if (incorrect > 0) {
            val revert = if (prev.lastDay == today) prev.appliedFrom else null
            return VocabLampDecision(
                newLevel = revert,
                state = VocabLampDayState(
                    lastDay = today,
                    failed = true,
                    appliedFrom = null,
                    fifthStreak = 0,
                    reachedFourOn = prev.reachedFourOn.takeUnless { it == today },
                    lastPerfectDay = prev.lastPerfectDay.takeUnless { it == today },
                ),
            )
        }

        if (prev.lastDay == today) return VocabLampDecision(state = prev)

        if (level >= VocabLearnMark.MASTER) {
            return VocabLampDecision(
                state = prev.copy(
                    lastDay = today,
                    failed = false,
                    appliedFrom = null,
                    lastPerfectDay = today,
                ),
            )
        }

        if (level < VocabLearnMark.DAILY_CAP) {
            val to = (level + 1).coerceAtMost(VocabLearnMark.DAILY_CAP)
            return VocabLampDecision(
                newLevel = to,
                state = VocabLampDayState(
                    lastDay = today,
                    failed = false,
                    appliedFrom = level,
                    fifthStreak = 0,
                    reachedFourOn = if (to == VocabLearnMark.DAILY_CAP) today else prev.reachedFourOn,
                    lastPerfectDay = today,
                ),
            )
        }

        val lastPerfect = prev.lastPerfectDay
        val consecutive = lastPerfect != null && ProgressTracker.isConsecutiveDay(lastPerfect, today)
        val lastPerfectCounted = lastPerfect != null && lastPerfect != prev.reachedFourOn
        val streak = if (consecutive && lastPerfectCounted) prev.fifthStreak + 1 else 1

        if (streak >= VocabLearnMark.MASTER_STREAK_DAYS) {
            return VocabLampDecision(
                newLevel = VocabLearnMark.MASTER,
                state = VocabLampDayState(
                    lastDay = today,
                    failed = false,
                    appliedFrom = VocabLearnMark.DAILY_CAP,
                    fifthStreak = streak,
                    reachedFourOn = prev.reachedFourOn,
                    lastPerfectDay = today,
                ),
            )
        }

        return VocabLampDecision(
            state = VocabLampDayState(
                lastDay = today,
                failed = false,
                appliedFrom = null,
                fifthStreak = streak,
                reachedFourOn = prev.reachedFourOn,
                lastPerfectDay = today,
            ),
        )
    }
}

class VocabLampDayStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun get(wordId: String): VocabLampDayState {
        val raw = prefs.getString(wordId, null) ?: return VocabLampDayState()
        return runCatching {
            val obj = JSONObject(raw)
            VocabLampDayState(
                lastDay = obj.optString("last").takeIf { it.isNotBlank() },
                failed = obj.optBoolean("failed"),
                appliedFrom = obj.optInt("from", -1).takeIf { it >= 0 },
                fifthStreak = obj.optInt("streak"),
                reachedFourOn = obj.optString("four").takeIf { it.isNotBlank() },
                lastPerfectDay = obj.optString("perfect").takeIf { it.isNotBlank() },
            )
        }.getOrDefault(VocabLampDayState())
    }

    fun put(wordId: String, state: VocabLampDayState) {
        prefs.edit()
            .putString(
                wordId,
                JSONObject()
                    .put("last", state.lastDay.orEmpty())
                    .put("failed", state.failed)
                    .put("from", state.appliedFrom ?: -1)
                    .put("streak", state.fifthStreak)
                    .put("four", state.reachedFourOn.orEmpty())
                    .put("perfect", state.lastPerfectDay.orEmpty())
                    .toString(),
            )
            .apply()
    }

    private companion object {
        const val PREFS = "profconq_vocab_lamps"
    }
}

class VocabTestLampTracker(
    context: Context,
    private val repository: ProfconqRepository,
) {
    private val store = VocabLampDayStore(context)
    private val lock = Mutex()

    suspend fun onTestDay(day: StudyDayBucket) {
        lock.withLock {
            val ids = day.words.map { it.wordId }.distinct()
            if (ids.isEmpty()) return
            val cards = repository.getCardsByIds(ids).associateBy { it.id }
            for (word in day.words) {
                val card = cards[word.wordId] ?: continue
                val current = VocabLearnMark.level(card.learnMark, card.known, card.due)
                val prev = store.get(word.wordId)
                val decision = VocabTestLamps.decide(
                    currentLevel = current,
                    correct = word.correct,
                    incorrect = word.incorrect,
                    today = day.dateKey,
                    prev = prev,
                )
                val nextLevel = decision.newLevel
                if (nextLevel != null && nextLevel != current) {
                    repository.setCardLampLevel(word.wordId, nextLevel)
                }
                if (decision.state != prev) {
                    store.put(word.wordId, decision.state)
                }
            }
        }
    }
}
