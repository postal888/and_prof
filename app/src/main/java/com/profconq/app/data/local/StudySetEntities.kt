package com.profconq.app.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "study_sets")
data class StudySetEntity(
    @PrimaryKey val id: String,
    val name: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "last_practiced_at") val lastPracticedAt: Long? = null,
    @ColumnInfo(name = "word_count") val wordCount: Int,
)

@Entity(
    tableName = "study_set_words",
    primaryKeys = ["study_set_id", "word_id"],
    indices = [Index("study_set_id"), Index("word_id")],
    foreignKeys = [
        ForeignKey(
            entity = StudySetEntity::class,
            parentColumns = ["id"],
            childColumns = ["study_set_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = CardEntity::class,
            parentColumns = ["id"],
            childColumns = ["word_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class StudySetWordEntity(
    @ColumnInfo(name = "study_set_id") val studySetId: String,
    /** References [CardEntity.id] — the word row shown in the Dictionary table. */
    @ColumnInfo(name = "word_id") val wordId: String,
    @ColumnInfo(name = "added_at") val addedAt: Long,
)
