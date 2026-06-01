package com.profconq.app.data

object WordLimitPolicy {
    /** Free tier cap for vocabulary words/cards. */
    const val FREE_LIMIT = 10
    const val UNLIMITED = Int.MAX_VALUE
}

class WordLimitReachedException(
    val count: Int,
    val limit: Int = WordLimitPolicy.FREE_LIMIT,
) : Exception("Word limit: $count/$limit")
