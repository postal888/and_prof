package com.profconq.app.ui.navigation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Intent-driven tab open (screenshots / notification deep links). */
object TabOpenHub {
    private val _pending = MutableStateFlow<MainTab?>(null)
    val pending: StateFlow<MainTab?> = _pending.asStateFlow()

    fun request(tab: MainTab) {
        _pending.value = tab
    }

    fun consume() {
        _pending.value = null
    }

    fun parse(raw: String?): MainTab? {
        if (raw.isNullOrBlank()) return null
        return when (raw.trim().lowercase()) {
            "home" -> MainTab.Home
            "study", "estudo", "deck" -> MainTab.Study
            "studio", "estudio", "estudo_studio" -> MainTab.Studio
            "reader", "leitura", "read" -> MainTab.Reader
            "video", "practice", "youtube", "video_tab" -> MainTab.Practice
            "dictionary", "dicionario", "dict" -> MainTab.Dictionary
            "profile", "perfil" -> MainTab.Profile
            else -> runCatching { MainTab.valueOf(raw.trim().replaceFirstChar { it.uppercase() }) }.getOrNull()
        }
    }
}
