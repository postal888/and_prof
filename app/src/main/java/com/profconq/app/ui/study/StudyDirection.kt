package com.profconq.app.ui.study

enum class StudyDirection {
    PT_TO_RU,
    RU_TO_PT,
    ;

    fun opposite(): StudyDirection = when (this) {
        PT_TO_RU -> RU_TO_PT
        RU_TO_PT -> PT_TO_RU
    }
}
