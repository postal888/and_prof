package com.profconq.app.api

enum class SyncPrimary {
    APP,
    SITE,
    ;

    companion object {
        fun fromStored(value: Int): SyncPrimary =
            if (value == 1) SITE else APP
    }

    fun toStored(): Int = if (this == SITE) 1 else 0
}
