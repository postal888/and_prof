package com.profconq.app.ui.navigation

import com.profconq.app.ui.i18n.UiStrings

enum class MainTab {
    Home,
    Study,
    Reader,
    Practice,
    Dictionary,
    Profile,
    ;

    fun label(strings: UiStrings): String = when (this) {
        Home -> strings.tabHome
        Study -> strings.tabStudy
        Reader -> strings.tabRead
        Practice -> strings.tabVideo
        Dictionary -> strings.tabDictionary
        Profile -> strings.tabProfile
    }
}
