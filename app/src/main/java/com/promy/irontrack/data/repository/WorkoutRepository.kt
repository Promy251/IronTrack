package com.promy.irontrack.data.repository

import com.promy.irontrack.data.local.WorkoutDao
import com.promy.irontrack.data.model.ExerciseEntity
import com.promy.irontrack.data.model.PersonalRecord
import com.promy.irontrack.data.model.StreakInfo
import com.promy.irontrack.data.model.UserProfileEntity
import com.promy.irontrack.data.model.WeightLogEntity
import com.promy.irontrack.data.model.WorkoutSessionEntity
import com.promy.irontrack.data.model.WorkoutSetEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import java.util.Calendar

@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutRepository(
    private val dao: WorkoutDao,
    private val currentUserIdFlow: Flow<String> = flowOf(""),
    private val getCurrentUserId: () -> String = { "" }
) {

    val allExercises: Flow<List<ExerciseEntity>> = currentUserIdFlow.flatMapLatest { uid ->
        dao.getAllExercises(uid)
    }
    val allSessions: Flow<List<WorkoutSessionEntity>> = currentUserIdFlow.flatMapLatest { uid ->
        dao.getAllSessions(uid)
    }
    val allWeightLogs: Flow<List<WeightLogEntity>> = currentUserIdFlow.flatMapLatest { uid ->
        dao.getAllWeightLogs(uid)
    }
    val chronologicalWeightLogs: Flow<List<WeightLogEntity>> = currentUserIdFlow.flatMapLatest { uid ->
        dao.getWeightLogsChronological(uid)
    }
    val userProfile: Flow<UserProfileEntity?> = currentUserIdFlow.flatMapLatest { uid ->
        dao.getUserProfile(uid)
    }
    val allCompletedSets: Flow<List<WorkoutSetEntity>> = currentUserIdFlow.flatMapLatest { uid ->
        dao.getAllCompletedSets(uid)
    }

    fun getSetsForSession(sessionId: Long): Flow<List<WorkoutSetEntity>> {
        return dao.getSetsForSession(sessionId)
    }

    suspend fun getSetsForSessionSync(sessionId: Long): List<WorkoutSetEntity> {
        return dao.getSetsForSessionSync(sessionId)
    }

    fun getSetsForExercise(exerciseId: Long): Flow<List<WorkoutSetEntity>> {
        return currentUserIdFlow.flatMapLatest { uid ->
            dao.getCompletedSetsForExercise(uid, exerciseId)
        }
    }

    /**
     * Calculates personal records across all exercises dynamically
     */
    val personalRecords: Flow<Map<Long, PersonalRecord>> = combine(
        allExercises,
        allCompletedSets
    ) { exercises, sets ->
        val setsByExercise = sets.groupBy { it.exerciseId }
        val prMap = mutableMapOf<Long, PersonalRecord>()

        exercises.forEach { exercise ->
            val exSets = setsByExercise[exercise.id].orEmpty()
            if (exSets.isNotEmpty()) {
                val maxWeightSet = exSets.maxByOrNull { it.weight }
                val maxWeight = maxWeightSet?.weight ?: 0.0
                val repsAtMaxWeight = maxWeightSet?.reps ?: 0

                val best1RM = exSets.maxOfOrNull {
                    PersonalRecord.calculate1RM(it.weight, it.reps)
                } ?: 0.0

                val maxVolumeSet = exSets.maxOfOrNull { it.weight * it.reps } ?: 0.0
                val lastDate = exSets.maxOfOrNull { it.timestamp } ?: 0L

                prMap[exercise.id] = PersonalRecord(
                    exerciseId = exercise.id,
                    exerciseName = exercise.name,
                    maxWeight = maxWeight,
                    maxRepsAtMaxWeight = repsAtMaxWeight,
                    bestEstimated1RM = best1RM,
                    maxVolumeSet = maxVolumeSet,
                    totalSetsLogged = exSets.size,
                    lastPerformed = lastDate
                )
            }
        }
        prMap
    }

    /**
     * Calculates workout streaks and weekly goal adherence
     */
    val streakInfo: Flow<StreakInfo> = combine(
        allSessions,
        userProfile
    ) { sessions, profile ->
        val weeklyGoal = profile?.weeklyWorkoutGoal ?: 4
        if (sessions.isEmpty()) {
            return@combine StreakInfo(
                currentStreakDays = 0,
                longestStreakDays = 0,
                thisWeekWorkoutsCount = 0,
                weeklyGoal = weeklyGoal,
                workoutsThisMonth = 0,
                lastWorkoutDate = null
            )
        }

        val cal = Calendar.getInstance()
        val now = System.currentTimeMillis()

        // Workouts this week (Monday to Sunday)
        cal.timeInMillis = now
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
        val startOfWeek = cal.timeInMillis

        // Workouts this month
        cal.set(Calendar.DAY_OF_MONTH, 1)
        val startOfMonth = cal.timeInMillis

        val thisWeekCount = sessions.count { it.startTime >= startOfWeek }
        val thisMonthCount = sessions.count { it.startTime >= startOfMonth }

        // Unique days worked out (in order)
        val uniqueDays = sessions.map { session ->
            val c = Calendar.getInstance().apply { timeInMillis = session.startTime }
            "${c.get(Calendar.YEAR)}-${c.get(Calendar.DAY_OF_YEAR)}"
        }.distinct()

        // Calculate consecutive day streak
        var currentStreak = 0
        val checkCal = Calendar.getInstance().apply { timeInMillis = now }
        val todayStr = "${checkCal.get(Calendar.YEAR)}-${checkCal.get(Calendar.DAY_OF_YEAR)}"
        checkCal.add(Calendar.DAY_OF_YEAR, -1)
        val yesterdayStr = "${checkCal.get(Calendar.YEAR)}-${checkCal.get(Calendar.DAY_OF_YEAR)}"

        var dayCursor = Calendar.getInstance().apply { timeInMillis = now }
        // If user worked out today or yesterday, streak is active
        val hasWorkoutToday = uniqueDays.contains(todayStr)
        val hasWorkoutYesterday = uniqueDays.contains(yesterdayStr)

        if (hasWorkoutToday || hasWorkoutYesterday) {
            if (!hasWorkoutToday) {
                // start counting from yesterday
                dayCursor.add(Calendar.DAY_OF_YEAR, -1)
            }
            while (true) {
                val key = "${dayCursor.get(Calendar.YEAR)}-${dayCursor.get(Calendar.DAY_OF_YEAR)}"
                if (uniqueDays.contains(key)) {
                    currentStreak++
                    dayCursor.add(Calendar.DAY_OF_YEAR, -1)
                } else {
                    break
                }
            }
        }

        // Longest streak
        val longestStreak = maxOf(currentStreak, if (sessions.isNotEmpty()) uniqueDays.size.coerceAtMost(7) else 0)

        StreakInfo(
            currentStreakDays = currentStreak,
            longestStreakDays = longestStreak,
            thisWeekWorkoutsCount = thisWeekCount,
            weeklyGoal = weeklyGoal,
            workoutsThisMonth = thisMonthCount,
            lastWorkoutDate = sessions.firstOrNull()?.startTime
        )
    }

    suspend fun insertExercise(exercise: ExerciseEntity): Long {
        val uid = if (exercise.userId.isNotBlank()) exercise.userId else getCurrentUserId()
        return dao.insertExercise(exercise.copy(userId = uid))
    }

    suspend fun deleteExercise(id: Long) {
        dao.deleteExerciseById(id)
    }

    /**
     * Saves completed workout, checks for PR achievements, and persists to Room
     */
    suspend fun saveWorkoutSession(
        title: String,
        startTime: Long,
        endTime: Long,
        durationSeconds: Long,
        notes: String,
        setsToSave: List<WorkoutSetEntity>
    ): List<String> {
        val completedSets = setsToSave.filter { it.isCompleted }
        val totalVolume = completedSets.sumOf { it.weight * it.reps }
        val totalSets = completedSets.size
        val uid = getCurrentUserId()

        val sessionId = dao.insertSession(
            WorkoutSessionEntity(
                userId = uid,
                title = title.ifBlank { "Workout Session" },
                startTime = startTime,
                endTime = endTime,
                durationSeconds = durationSeconds,
                totalVolume = totalVolume,
                totalSets = totalSets,
                notes = notes
            )
        )

        // Check PRs before saving
        val currentPrs = personalRecords.first()
        val prAnnouncements = mutableListOf<String>()

        val finalSets = setsToSave.map { set ->
            val prevPr = currentPrs[set.exerciseId]
            var isPr = false
            if (set.isCompleted && !set.isWarmup) {
                val current1RM = PersonalRecord.calculate1RM(set.weight, set.reps)
                val prevMaxWeight = prevPr?.maxWeight ?: 0.0
                val prev1RM = prevPr?.bestEstimated1RM ?: 0.0

                if (prevPr == null || set.weight > prevMaxWeight || current1RM > prev1RM) {
                    isPr = true
                    if (set.weight > prevMaxWeight && prevMaxWeight > 0.0) {
                        prAnnouncements.add("New PR on ${set.exerciseName}! Lifted ${set.weight}kg (+${"%.1f".format(set.weight - prevMaxWeight)}kg)")
                    }
                }
            }
            set.copy(sessionId = sessionId, userId = uid, isPr = isPr)
        }

        dao.insertSets(finalSets)
        return prAnnouncements
    }

    suspend fun deleteSession(sessionId: Long) {
        dao.deleteSessionById(sessionId)
    }

    suspend fun insertWeightLog(weight: Double, date: Long, note: String): Long {
        return dao.insertWeightLog(
            WeightLogEntity(
                userId = getCurrentUserId(),
                weight = weight,
                date = date,
                note = note
            )
        )
    }

    suspend fun deleteWeightLog(id: Long) {
        dao.deleteWeightLogById(id)
    }

    suspend fun upsertProfile(profile: UserProfileEntity) {
        val uid = if (profile.userId.isNotBlank()) profile.userId else getCurrentUserId()
        dao.upsertUserProfile(profile.copy(userId = uid))
    }

    suspend fun updateProfile(
        startingWeight: Double,
        targetWeight: Double,
        goalType: String,
        weightUnit: String,
        weeklyGoal: Int
    ) {
        val uid = getCurrentUserId()
        val existing = dao.getUserProfile(uid).firstOrNull()
        dao.upsertUserProfile(
            UserProfileEntity(
                userId = uid,
                name = existing?.name ?: "",
                email = existing?.email ?: "",
                startingWeight = startingWeight,
                targetWeight = targetWeight,
                goalType = goalType,
                weightUnit = weightUnit,
                weeklyWorkoutGoal = weeklyGoal
            )
        )
    }

    suspend fun clearUserData(userId: String) {
        if (userId.isNotBlank()) {
            dao.clearSessionsForUser(userId)
            dao.clearSetsForUser(userId)
            dao.clearWeightLogsForUser(userId)
            dao.clearProfileForUser(userId)
        }
    }
}
