package com.proficon.app.data.model

enum class AppThemeMode(val storageCode: Int) {
    Dark(0),
    Light(1),
    ;

    companion object {
        fun fromStorage(code: Int?): AppThemeMode =
            entries.find { it.storageCode == code } ?: Dark
    }
}
