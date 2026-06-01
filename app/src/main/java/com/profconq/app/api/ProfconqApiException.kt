package com.profconq.app.api

sealed class ProfconqApiException(message: String) : Exception(message) {
    class Unauthorized(message: String = "Требуется вход через Google") : ProfconqApiException(message)
    class WordLimit(val count: Int, val limit: Int) :
        ProfconqApiException("Лимит слов: $count из $limit. Premium скоро.")
    class PromoInvalid(message: String = "Промокод не найден") : ProfconqApiException(message)
    class PromoAlreadyRedeemed(message: String = "Промокод уже использован") : ProfconqApiException(message)
    class HttpError(val code: Int, body: String?) :
        ProfconqApiException("Ошибка сервера ($code)")
}
