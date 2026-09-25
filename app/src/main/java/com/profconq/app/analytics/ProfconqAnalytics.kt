package com.profconq.app.analytics

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics
import com.profconq.app.BuildConfig
import com.profconq.app.ui.i18n.AppLanguage

/**
 * Единственная точка обращения к FirebaseAnalytics в прикладном коде.
 *
 * Здесь защищаются три правила: в аналитику не уходит ничего похожего на PII; идентификатор
 * пользователя принимается только как серверный непрозрачный UUID, иначе user_id не устанавливается;
 * сбор выключен по умолчанию и включается только в debug-сборке, пока не принято решение по
 * consent/privacy.
 */
object ProfconqAnalytics {

    const val EVENT_SMOKE_TEST = "analytics_smoke_test"
    const val USER_PROP_PLAN = "plan"
    const val USER_PROP_UI_LANGUAGE = "ui_language"

    const val PLAN_FREE = "free"
    const val PLAN_PREMIUM = "premium"

    private const val TAG = "ProfconqAnalytics"

    private var analytics: FirebaseAnalytics? = null

    fun init(context: Context) {
        if (analytics == null) {
            analytics = FirebaseAnalytics.getInstance(context.applicationContext)
        }
        // Манифест уже выключает сбор до инициализации SDK, поэтому здесь решается только debug/release.
        setAnalyticsEnabled(isCollectionAllowed(BuildConfig.DEBUG))
    }

    fun setAnalyticsEnabled(enabled: Boolean) {
        runCatching { analytics?.setAnalyticsCollectionEnabled(enabled) }
            .onFailure { Log.w(TAG, "не удалось переключить сбор аналитики") }
    }

    fun identifyUser(serverUserId: String?) {
        val safeId = normalizeServerUserId(serverUserId)
        if (safeId == null) {
            // Исходное значение не логируется: на этом пути оно умеет быть адресом почты.
            if (!serverUserId.isNullOrBlank()) Log.w(TAG, "user_id не прошёл проверку и сброшен")
            clearUserId()
            return
        }
        runCatching { analytics?.setUserId(safeId) }.onFailure { Log.w(TAG, "не удалось установить user_id") }
    }

    /** Выход: идентичность и свойства предыдущего аккаунта не должны остаться на устройстве. */
    fun clearUser() {
        clearUserId()
        setPlan(null)
    }

    fun setPlan(premium: Boolean?) {
        setUserProperty(USER_PROP_PLAN, planValue(premium))
    }

    fun setUiLanguage(languageCode: Int?) {
        setUserProperty(USER_PROP_UI_LANGUAGE, normalizeUiLanguage(languageCode))
    }

    /** Проверка связки в DebugView. В release-сборке событие не отправляется. */
    fun debugSmokeTest() {
        if (!isSmokeTestEnabled(BuildConfig.DEBUG)) return
        val bundle = Bundle().apply {
            putInt("sdk_ok", 1)
            putString("build_type", "debug")
        }
        runCatching { analytics?.logEvent(EVENT_SMOKE_TEST, bundle) }
    }

    private fun clearUserId() {
        runCatching { analytics?.setUserId(null) }.onFailure { Log.w(TAG, "не удалось сбросить user_id") }
    }

    private fun setUserProperty(name: String, value: String?) {
        runCatching { analytics?.setUserProperty(name, value) }
            .onFailure { Log.w(TAG, "не удалось обновить свойство пользователя") }
    }
}

private const val MAX_ID_LENGTH = 64
private val UUID_PATTERN =
    Regex("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$", RegexOption.IGNORE_CASE)
private val EMAIL_LIKE = Regex("^[^@\\s.]+@[^@\\s.]+\\.[^@\\s.]+$|^[^@\\s]+\\.[a-z]{2,}$")

/**
 * Разрешён только серверный непрозрачный UUID. Пустое значение, что угодно с «@», похожее на адрес
 * почты или нестандартной формы отклоняется: вызывающий код передаёт сюда поле auth-контракта,
 * которое сейчас умеет быть email-ом.
 */
internal fun normalizeServerUserId(raw: String?): String? {
    val value = raw?.trim() ?: return null
    if (value.isEmpty() || value.length > MAX_ID_LENGTH) return null
    if (value.contains('@')) return null
    if (EMAIL_LIKE.matches(value)) return null
    if (!UUID_PATTERN.matches(value)) return null
    return value.lowercase()
}

/** plan принимает только два значения; unknown означает «сервер ещё не ответил» и свойство снимает. */
internal fun planValue(premium: Boolean?): String? = when (premium) {
    true -> ProfconqAnalytics.PLAN_PREMIUM
    false -> ProfconqAnalytics.PLAN_FREE
    null -> null
}

internal fun normalizeUiLanguage(languageCode: Int?): String =
    when (AppLanguage.fromStorage(languageCode)) {
        AppLanguage.RU -> "ru"
        AppLanguage.EN -> "en"
        AppLanguage.PT -> "pt"
    }

internal fun isCollectionAllowed(debugBuild: Boolean): Boolean = debugBuild

internal fun isSmokeTestEnabled(debugBuild: Boolean): Boolean = debugBuild
