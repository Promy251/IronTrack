package com.promy.irontrack.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workout_sessions")
data class WorkoutSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String = "",
    val title: String,
    val startTime: Long, // epoch millis
    val endTime: Long = 0, // epoch millis
    val durationSeconds: Long = 0,
    val totalVolume: Double = 0.0,
    val totalSets: Int = 0,
    val notes: String = ""
)