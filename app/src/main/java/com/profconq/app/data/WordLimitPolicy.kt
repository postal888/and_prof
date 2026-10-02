package com.profconq.app.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The last refused add, whichever screen made it, so one app-wide prompt can offer Premium instead
 * of each screen phrasing the refusal on its own.
 */
object WordLimitEvents {
    private val _reached = MutableStateFlow<WordLimitReachedException?>(null)
    val reached: StateFlow<WordLimitReachedException?> = _reached.asStateFlow()

    fun report(error: WordLimitReachedException) {
        _reached.value = error
    }

    fun consume() {
        _reached.value = null
    }
}

object WordLimitPolicy {
    /** Free tier cap for vocabulary words/cards. */
    const val FREE_LIMIT = 10
    const val UNLIMITED = Int.MAX_VALUE
}

class WordLimitReachedException(
    val count: Int,
    val limit: Int = WordLimitPolicy.FREE_LIMIT,
) : Exception("Word limit: $count/$limit")
