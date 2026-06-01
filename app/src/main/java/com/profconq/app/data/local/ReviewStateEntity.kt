package com.profconq.app.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "review_state",
    primaryKeys = ["word_id"],
    foreignKeys = [
        ForeignKey(
            entity = CardEntity::class,
            parentColumns = ["id"],
            childColumns = ["word_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class ReviewStateEntity(
    @ColumnInfo(name = "word_id") val wordId: String,
    @ColumnInfo(name = "interval_days") val intervalDays: Int = 0,
    @ColumnInfo(name = "due_at") val dueAt: Long = 0L,
    @ColumnInfo(name = "last_reviewed_at") val lastReviewedAt: Long = 0L,
    @ColumnInfo(name = "times_shown") val timesShown: Int = 0,
    @ColumnInfo(name = "times_known") val timesKnown: Int = 0,
    @ColumnInfo(name = "times_again") val timesAgain: Int = 0,
    @ColumnInfo(name = "is_mastered") val isMastered: Boolean = false,
)
