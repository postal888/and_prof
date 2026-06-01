package com.profconq.app.youtube

sealed class TranslationException(message: String) : Exception(message) {
    class AuthRequired(message: String = "Войдите через Google для перевода ChatGPT") :
        TranslationException(message)

    class WordLimit(val count: Int, val limit: Int) :
        TranslationException("Достигнут лимит слов ($count/$limit)")

    class ServerError(message: String) : TranslationException(message)
}
