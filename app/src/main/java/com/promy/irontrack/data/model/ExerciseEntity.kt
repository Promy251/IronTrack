package com.promy.irontrack.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exercises")
data class ExerciseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String = "",
    val name: String,
    val category: String, // Chest, Back, Legs, Shoulders, Arms, Core
    val equipment: String = "Barbell", // Barbell, Dumbbell, Machine, Cable, Bodyweight
    val isCustom: Boolean = false,
    val defaultRestSeconds: Int = 90
)