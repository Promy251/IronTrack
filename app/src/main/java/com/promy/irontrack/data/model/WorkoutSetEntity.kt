package com.promy.irontrack.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "workout_sets",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("sessionId"),
        Index("exerciseId")
    ]
)
data class WorkoutSetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionId: Long,
    val userId: String = "",
    val exerciseId: Long,
    val exerciseName: String,
    val setNumber: Int,
    val weight: Double,
    val reps: Int,
    val isCompleted: Boolean = true,
    val isWarmup: Boolean = false,
    val isPr: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)