package com.promy.irontrack.data.model

data class PersonalRecord(
    val exerciseId: Long,
    val exerciseName: String,
    val maxWeight: Double, // Highest weight lifted
    val maxRepsAtMaxWeight: Int,
    val bestEstimated1RM: Double, // Calculated 1RM
    val maxVolumeSet: Double, // Best weight * reps in single set
    val totalSetsLogged: Int,
    val lastPerformed: Long
) {
    companion object {
        /**
         * Calculates Estimated 1RM using the widely accepted Epley formula:
         * 1RM = weight * (1 + reps / 30.0)
         * If reps == 1, 1RM equals weight.
         */
        fun calculate1RM(weight: Double, reps: Int): Double {
            if (reps <= 0 || weight <= 0.0) return 0.0
            if (reps == 1) return weight
            return weight * (1.0 + reps / 30.0)
        }
    }
}

data class WorkoutSessionWithSets(
    val session: WorkoutSessionEntity,
    val sets: List<WorkoutSetEntity>
)

data class StreakInfo(
    val currentStreakDays: Int,
    val longestStreakDays: Int,
    val thisWeekWorkoutsCount: Int,
    val weeklyGoal: Int,
    val workoutsThisMonth: Int,
    val lastWorkoutDate: Long?
)

data class GrowthAnalytics(
    val totalVolumeAllTime: Double,
    val totalWorkoutsAllTime: Int,
    val totalSetsAllTime: Int,
    val weeklyVolumeHistory: List<Pair<String, Double>>, // e.g. "W1", "W2"... -> volume
    val exercise1RMProgression: List<Pair<Long, Double>> // timestamp -> estimated 1RM
)
