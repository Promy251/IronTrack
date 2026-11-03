package com.promy.irontrack.data.repository

import android.content.Context
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import com.promy.irontrack.data.local.WorkoutDao
import com.promy.irontrack.data.model.UserProfileEntity
import com.promy.irontrack.data.model.WeightLogEntity
import com.promy.irontrack.data.model.WorkoutSessionEntity
import com.promy.irontrack.data.model.WorkoutSetEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class FirestoreSyncRepository(
    private val context: Context,
    private val workoutDao: WorkoutDao
) {
    private val db = Firebase.firestore
    private val TAG = "FirestoreSync"

    suspend fun syncAllToCloud(userId: String): Result<Int> = withContext(Dispatchers.IO) {
        try {
            require(userId.isNotBlank()) { "User must be authenticated to sync" }
            var itemsSynced = 0

            // 0. Ensure root user document exists so the 'users' collection and doc are directly visible in Firebase Console
            val firebaseUser = Firebase.auth.currentUser
            val userDoc = hashMapOf(
                "uid" to userId,
                "email" to (firebaseUser?.email ?: ""),
                "displayName" to (firebaseUser?.displayName?.ifBlank { null } 
                    ?: workoutDao.getUserProfile(userId).firstOrNull()?.name?.ifBlank { null } 
                    ?: "Athlete"),
                "lastSyncedAt" to FieldValue.serverTimestamp()
            )
            db.collection("users").document(userId)
                .set(userDoc, SetOptions.merge())
                .await()
            itemsSynced++

            // 1. Sync User Profile
            val profile = workoutDao.getUserProfile(userId).firstOrNull()
            if (profile != null) {
                val profileMap = hashMapOf(
                    "name" to profile.name,
                    "email" to (profile.email.ifBlank { firebaseUser?.email ?: "" }),
                    "heightCm" to profile.heightCm,
                    "startingWeight" to profile.startingWeight,
                    "targetWeight" to profile.targetWeight,
                    "goalType" to profile.goalType,
                    "weightUnit" to profile.weightUnit,
                    "weeklyWorkoutGoal" to profile.weeklyWorkoutGoal,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                db.collection("users").document(userId)
                    .collection("profile").document("current")
                    .set(profileMap, SetOptions.merge())
                    .await()
                itemsSynced++
            }

            // 2. Sync Weight Logs
            val weightLogs = workoutDao.getAllWeightLogs(userId).firstOrNull() ?: emptyList()
            for (log in weightLogs) {
                val logMap = hashMapOf(
                    "id" to log.id.toString(),
                    "weight" to log.weight,
                    "date" to log.date,
                    "note" to log.note,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                db.collection("users").document(userId)
                    .collection("weight_logs").document(log.id.toString())
                    .set(logMap, SetOptions.merge())
                    .await()
                itemsSynced++
            }

            // 3. Sync Workout Sessions
            val sessions = workoutDao.getAllSessions(userId).firstOrNull() ?: emptyList()
            for (session in sessions) {
                val sessionMap = hashMapOf(
                    "id" to session.id.toString(),
                    "title" to session.title,
                    "startTime" to session.startTime,
                    "endTime" to session.endTime,
                    "durationSeconds" to session.durationSeconds,
                    "totalVolume" to session.totalVolume,
                    "totalSets" to session.totalSets,
                    "notes" to session.notes,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                db.collection("users").document(userId)
                    .collection("sessions").document(session.id.toString())
                    .set(sessionMap, SetOptions.merge())
                    .await()
                itemsSynced++

                // Sync sets for this session
                val sets = workoutDao.getSetsForSessionSync(session.id)
                for (set in sets) {
                    val setMap = hashMapOf(
                        "id" to set.id.toString(),
                        "sessionId" to set.sessionId.toString(),
                        "exerciseId" to set.exerciseId.toString(),
                        "exerciseName" to set.exerciseName,
                        "setNumber" to set.setNumber,
                        "weight" to set.weight,
                        "reps" to set.reps,
                        "isCompleted" to set.isCompleted,
                        "isWarmup" to set.isWarmup,
                        "isPr" to set.isPr,
                        "timestamp" to set.timestamp,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    db.collection("users").document(userId)
                        .collection("sets").document(set.id.toString())
                        .set(setMap, SetOptions.merge())
                        .await()
                    itemsSynced++
                }
            }

            Log.i(TAG, "Successfully synced $itemsSynced items to Cloud Firestore")
            Result.success(itemsSynced)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync data to Firestore", e)
            Result.failure(e)
        }
    }

    suspend fun syncFromCloud(userId: String): Result<Int> = withContext(Dispatchers.IO) {
        try {
            require(userId.isNotBlank()) { "User must be authenticated to sync" }
            var itemsRestored = 0

            // 1. Restore Profile
            var restoredName: String? = null
            var restoredEmail: String? = null
            var heightCm = 175.0
            var startingWeight = 0.0
            var targetWeight = 0.0
            var goalType = "MAINTAIN"
            var weightUnit = "kg"
            var weeklyGoal = 3
            var hasProfileData = false

            val profileDoc = db.collection("users").document(userId)
                .collection("profile").document("current").get().await()
            if (profileDoc.exists()) {
                val data = profileDoc.data
                if (data != null) {
                    restoredName = data["name"] as? String
                    restoredEmail = data["email"] as? String
                    heightCm = (data["heightCm"] as? Number)?.toDouble() ?: 175.0
                    startingWeight = (data["startingWeight"] as? Number)?.toDouble() ?: 0.0
                    targetWeight = (data["targetWeight"] as? Number)?.toDouble() ?: 0.0
                    goalType = data["goalType"] as? String ?: "MAINTAIN"
                    weightUnit = data["weightUnit"] as? String ?: "kg"
                    weeklyGoal = (data["weeklyWorkoutGoal"] as? Number)?.toInt() ?: 3
                    hasProfileData = true
                }
            }

            // Fallback to root users/{userId} doc for name and email
            if (restoredName.isNullOrBlank() || restoredEmail.isNullOrBlank()) {
                val userDoc = db.collection("users").document(userId).get().await()
                if (userDoc.exists()) {
                    val rootData = userDoc.data
                    if (restoredName.isNullOrBlank()) {
                        restoredName = rootData?.get("name") as? String ?: rootData?.get("displayName") as? String
                    }
                    if (restoredEmail.isNullOrBlank()) {
                        restoredEmail = rootData?.get("email") as? String
                    }
                    hasProfileData = true
                }
            }

            val existingLocalProfile = workoutDao.getUserProfile(userId).firstOrNull()

            val finalName = restoredName?.ifBlank { null }
                ?: Firebase.auth.currentUser?.displayName?.ifBlank { null }
                ?: existingLocalProfile?.name?.ifBlank { null }
                ?: "Athlete"
            val finalEmail = restoredEmail?.ifBlank { null }
                ?: Firebase.auth.currentUser?.email?.ifBlank { null }
                ?: existingLocalProfile?.email?.ifBlank { null }
                ?: ""

            if (hasProfileData || Firebase.auth.currentUser != null) {
                workoutDao.upsertUserProfile(
                    UserProfileEntity(
                        userId = userId,
                        name = finalName,
                        email = finalEmail,
                        heightCm = heightCm,
                        startingWeight = startingWeight,
                        targetWeight = targetWeight,
                        goalType = goalType,
                        weightUnit = weightUnit,
                        weeklyWorkoutGoal = weeklyGoal
                    )
                )
                itemsRestored++
            }

            // 2. Restore Weight Logs
            val weightDocs = db.collection("users").document(userId)
                .collection("weight_logs").get().await()
            for (doc in weightDocs) {
                val weight = (doc.getDouble("weight")) ?: continue
                val date = (doc.getLong("date")) ?: System.currentTimeMillis()
                val note = doc.getString("note") ?: ""
                val id = doc.id.toLongOrNull() ?: 0L

                workoutDao.insertWeightLog(
                    WeightLogEntity(
                        id = id,
                        userId = userId,
                        weight = weight,
                        date = date,
                        note = note
                    )
                )
                itemsRestored++
            }

            // 3. Restore Sessions
            val sessionDocs = db.collection("users").document(userId)
                .collection("sessions").get().await()
            for (doc in sessionDocs) {
                val id = doc.id.toLongOrNull() ?: continue
                val title = doc.getString("title") ?: "Workout"
                val startTime = doc.getLong("startTime") ?: System.currentTimeMillis()
                val endTime = doc.getLong("endTime") ?: 0L
                val duration = doc.getLong("durationSeconds") ?: 0L
                val totalVolume = doc.getDouble("totalVolume") ?: 0.0
                val totalSets = doc.getLong("totalSets")?.toInt() ?: 0
                val notes = doc.getString("notes") ?: ""

                workoutDao.insertSession(
                    WorkoutSessionEntity(
                        id = id,
                        userId = userId,
                        title = title,
                        startTime = startTime,
                        endTime = endTime,
                        durationSeconds = duration,
                        totalVolume = totalVolume,
                        totalSets = totalSets,
                        notes = notes
                    )
                )
                itemsRestored++
            }

            // 4. Restore Sets
            val setDocs = db.collection("users").document(userId)
                .collection("sets").get().await()
            for (doc in setDocs) {
                val id = doc.id.toLongOrNull() ?: continue
                val sessionId = doc.getString("sessionId")?.toLongOrNull() ?: continue
                val exerciseId = doc.getString("exerciseId")?.toLongOrNull() ?: 1L
                val exerciseName = doc.getString("exerciseName") ?: ""
                val setNumber = doc.getLong("setNumber")?.toInt() ?: 1
                val weight = doc.getDouble("weight") ?: 0.0
                val reps = doc.getLong("reps")?.toInt() ?: 0
                val isCompleted = doc.getBoolean("isCompleted") ?: true
                val isWarmup = doc.getBoolean("isWarmup") ?: false
                val isPr = doc.getBoolean("isPr") ?: false
                val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()

                workoutDao.insertSet(
                    WorkoutSetEntity(
                        id = id,
                        sessionId = sessionId,
                        userId = userId,
                        exerciseId = exerciseId,
                        exerciseName = exerciseName,
                        setNumber = setNumber,
                        weight = weight,
                        reps = reps,
                        isCompleted = isCompleted,
                        isWarmup = isWarmup,
                        isPr = isPr,
                        timestamp = timestamp
                    )
                )
                itemsRestored++
            }

            Log.i(TAG, "Successfully restored $itemsRestored items from Cloud Firestore")
            Result.success(itemsRestored)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to restore data from Firestore", e)
            Result.failure(e)
        }
    }
}
