package com.profconq.app.api

sealed class ProfconqApiException(message: String) : Exception(message) {
    class Unauthorized(message: String = "Войдите email и паролем с profconq.com") : ProfconqApiException(message)
    class InvalidCredentials(message: String = "Неверный email или пароль") : ProfconqApiException(message)
    class EmailNotVerified(message: String = "Подтвердите email на profconq.com") : ProfconqApiException(message)
    class WordLimit(val count: Int, val limit: Int) :
        ProfconqApiException("Лимит слов: $count из $limit. Premium скоро.")
    class PromoInvalid(message: String = "Промокод не найден") : ProfconqApiException(message)
    class PromoAlreadyRedeemed(message: String = "Промокод уже использован") : ProfconqApiException(message)
    class HttpError(val code: Int, body: String?) :
        ProfconqApiException("Ошибка сервера ($code)")
}
