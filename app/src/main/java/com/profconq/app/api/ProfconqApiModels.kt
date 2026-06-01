package com.profconq.app.api

data class AccountInfo(
    val uid: String,
    val email: String?,
    val displayName: String?,
    val isPremium: Boolean,
    val wordCount: Int,
    val wordLimit: Int,
)

data class DictionarySyncWord(
    val id: String,
    val pt: String,
    val ru: String,
    val example: String? = null,
    val collectionId: String? = null,
    val videoId: String? = null,
    val deleted: Boolean = false,
    val updatedAt: Long,
)

data class DictionaryPullResult(
    val words: List<DictionarySyncWord>,
    val wordCount: Int,
    val wordLimit: Int,
    val serverTime: Long,
)

data class DictionaryPushResult(
    val wordCount: Int,
    val wordLimit: Int,
    val synced: Int,
    val serverTime: Long,
)
