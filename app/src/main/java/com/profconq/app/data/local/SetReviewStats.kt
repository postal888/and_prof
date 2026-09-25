package com.profconq.app.data.local

import androidx.room.ColumnInfo

data class SetReviewStats(
    @ColumnInfo(name = "newCount") val newCount: Int,
    @ColumnInfo(name = "dueCount") val dueCount: Int,
    @ColumnInfo(name = "masteredCount") val masteredCount: Int,
)

data class SetStudioCount(
    @ColumnInfo(name = "setId") val setId: String,
    @ColumnInfo(name = "studioCount") val studioCount: Int,
)
