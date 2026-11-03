package com.promy.irontrack.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "weight_logs")
data class WeightLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String = "",
    val weight: Double, // in current unit (kg or lbs)
    val date: Long = System.currentTimeMillis(), // epoch millis
    val note: String = ""
)

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey
    val userId: String = "",
    val name: String = "",
    val email: String = "",
    val heightCm: Double = 175.0,
    val startingWeight: Double = 0.0,
    val targetWeight: Double = 0.0,
    val goalType: String = "MAINTAIN", // CUT, BULK, MAINTAIN
    val weightUnit: String = "kg", // "kg" or "lbs"
    val weeklyWorkoutGoal: Int = 3
)