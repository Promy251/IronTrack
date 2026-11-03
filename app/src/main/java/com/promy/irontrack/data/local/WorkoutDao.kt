package com.promy.irontrack.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.promy.irontrack.data.model.ExerciseEntity
import com.promy.irontrack.data.model.UserProfileEntity
import com.promy.irontrack.data.model.WeightLogEntity
import com.promy.irontrack.data.model.WorkoutSessionEntity
import com.promy.irontrack.data.model.WorkoutSetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {

    // === EXERCISES ===
    // Returns built-in exercises (isCustom = 0) OR custom exercises created by this user
    @Query("SELECT * FROM exercises WHERE isCustom = 0 OR userId = :userId ORDER BY category ASC, name ASC")
    fun getAllExercises(userId: String): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises WHERE id = :id")
    suspend fun getExerciseById(id: Long): ExerciseEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercise(exercise: ExerciseEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercises(exercises: List<ExerciseEntity>)

    @Query("DELETE FROM exercises WHERE id = :id")
    suspend fun deleteExerciseById(id: Long)

    @Query("UPDATE exercises SET name = :newName WHERE name = :oldName AND isCustom = 0")
    suspend fun updateExerciseName(oldName: String, newName: String)

    @Query("UPDATE workout_sets SET exerciseName = :newName WHERE exerciseName = :oldName")
    suspend fun updateSetExerciseName(oldName: String, newName: String)

    @Query("SELECT COUNT(*) FROM exercises")
    suspend fun getExerciseCount(): Int

    // === SESSIONS ===
    @Query("SELECT * FROM workout_sessions WHERE userId = :userId ORDER BY startTime DESC")
    fun getAllSessions(userId: String): Flow<List<WorkoutSessionEntity>>

    @Query("SELECT * FROM workout_sessions WHERE id = :id")
    suspend fun getSessionById(id: Long): WorkoutSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: WorkoutSessionEntity): Long

    @Update
    suspend fun updateSession(session: WorkoutSessionEntity)

    @Query("DELETE FROM workout_sessions WHERE id = :id")
    suspend fun deleteSessionById(id: Long)

    // === SETS ===
    @Query("SELECT * FROM workout_sets WHERE sessionId = :sessionId ORDER BY setNumber ASC")
    fun getSetsForSession(sessionId: Long): Flow<List<WorkoutSetEntity>>

    @Query("SELECT * FROM workout_sets WHERE sessionId = :sessionId ORDER BY setNumber ASC")
    suspend fun getSetsForSessionSync(sessionId: Long): List<WorkoutSetEntity>

    @Query("SELECT * FROM workout_sets WHERE userId = :userId AND exerciseId = :exerciseId AND isCompleted = 1 ORDER BY timestamp ASC")
    fun getCompletedSetsForExercise(userId: String, exerciseId: Long): Flow<List<WorkoutSetEntity>>

    @Query("SELECT * FROM workout_sets WHERE userId = :userId AND isCompleted = 1 ORDER BY timestamp DESC")
    fun getAllCompletedSets(userId: String): Flow<List<WorkoutSetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSets(sets: List<WorkoutSetEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSet(set: WorkoutSetEntity): Long

    @Query("DELETE FROM workout_sets WHERE id = :id")
    suspend fun deleteSetById(id: Long)

    // === WEIGHT LOGS ===
    @Query("SELECT * FROM weight_logs WHERE userId = :userId ORDER BY date DESC")
    fun getAllWeightLogs(userId: String): Flow<List<WeightLogEntity>>

    @Query("SELECT * FROM weight_logs WHERE userId = :userId ORDER BY date ASC")
    fun getWeightLogsChronological(userId: String): Flow<List<WeightLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeightLog(weightLog: WeightLogEntity): Long

    @Query("DELETE FROM weight_logs WHERE id = :id")
    suspend fun deleteWeightLogById(id: Long)

    // === USER PROFILE ===
    @Query("SELECT * FROM user_profile WHERE userId = :userId")
    fun getUserProfile(userId: String): Flow<UserProfileEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUserProfile(profile: UserProfileEntity)

    // === CLEAR USER DATA ON SIGN OUT ===
    @Query("DELETE FROM workout_sessions WHERE userId = :userId")
    suspend fun clearSessionsForUser(userId: String)

    @Query("DELETE FROM workout_sets WHERE userId = :userId")
    suspend fun clearSetsForUser(userId: String)

    @Query("DELETE FROM weight_logs WHERE userId = :userId")
    suspend fun clearWeightLogsForUser(userId: String)

    @Query("DELETE FROM user_profile WHERE userId = :userId")
    suspend fun clearProfileForUser(userId: String)
}