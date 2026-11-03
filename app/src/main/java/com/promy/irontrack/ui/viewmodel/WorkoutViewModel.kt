package com.promy.irontrack.ui.viewmodel

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.promy.irontrack.data.local.AppDatabase
import com.promy.irontrack.data.local.SettingsDataStore
import com.promy.irontrack.data.model.ExerciseEntity
import com.promy.irontrack.data.model.PersonalRecord
import com.promy.irontrack.data.model.StreakInfo
import com.promy.irontrack.data.model.UserProfileEntity
import com.promy.irontrack.data.model.WeightLogEntity
import com.promy.irontrack.data.model.WorkoutSessionEntity
import com.promy.irontrack.data.model.WorkoutSetEntity
import com.promy.irontrack.data.repository.WorkoutRepository
import com.promy.irontrack.data.auth.AuthManager
import com.promy.irontrack.data.repository.FirestoreSyncRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

sealed interface SyncUiState {
    data object Idle : SyncUiState
    data object Syncing : SyncUiState
    data class Success(val message: String) : SyncUiState
    data class Error(val message: String) : SyncUiState
}

data class ActiveSet(
    val localId: String = UUID.randomUUID().toString(),
    val weight: Double = 0.0,
    val reps: Int = 10,
    val isCompleted: Boolean = false,
    val isWarmup: Boolean = false
)

data class ActiveExerciseItem(
    val exercise: ExerciseEntity,
    val sets: List<ActiveSet>
)

data class PrCelebration(
    val announcements: List<String>,
    val workoutTitle: String,
    val totalVolume: Double
)

class WorkoutViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: WorkoutRepository
    private val settingsDataStore = SettingsDataStore(application)
    val authManager = AuthManager.getInstance(application)
    val currentUser = authManager.currentUser
    private val firestoreSync: FirestoreSyncRepository

    private val _syncState = MutableStateFlow<SyncUiState>(SyncUiState.Idle)
    val syncState: StateFlow<SyncUiState> = _syncState.asStateFlow()

    private var lastAutoSyncedUid: String? = null

    init {
        val db = AppDatabase.getDatabase(application, viewModelScope)
        val userIdFlow = authManager.currentUser.map { it?.uid ?: "" }
        repository = WorkoutRepository(
            dao = db.workoutDao(),
            currentUserIdFlow = userIdFlow,
            getCurrentUserId = { authManager.getUserId() ?: "" }
        )
        firestoreSync = FirestoreSyncRepository(application, db.workoutDao())

        // Ensure default exercise library uses simplified names while preserving IDs
        viewModelScope.launch(Dispatchers.IO) {
            AppDatabase.migrateExerciseNames(db.workoutDao())
        }

        // Auto-sync whenever user logs in or starts up authenticated
        viewModelScope.launch {
            authManager.currentUser.collect { user ->
                if (user != null && user.uid != lastAutoSyncedUid) {
                    lastAutoSyncedUid = user.uid
                    autoSyncUser(user.uid)
                } else if (user == null) {
                    lastAutoSyncedUid = null
                }
            }
        }
    }

    val weightUnitPreference = settingsDataStore.weightUnit.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), "kg"
    )

    val defaultRestTimerPreference = settingsDataStore.defaultRestSeconds.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), 90
    )

    // Repository flows
    val allExercises = repository.allExercises.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val allSessions = repository.allSessions.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val weightLogs = repository.allWeightLogs.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val chronologicalWeightLogs = repository.chronologicalWeightLogs.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val userProfile = repository.userProfile.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), null
    )

    val personalRecords = repository.personalRecords.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap()
    )

    val streakInfo = repository.streakInfo.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000),
        StreakInfo(0, 0, 0, 4, 0, null)
    )

    val allCompletedSets = repository.allCompletedSets.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    // --- ACTIVE WORKOUT STATE ---
    private val _isWorkoutActive = MutableStateFlow(false)
    val isWorkoutActive: StateFlow<Boolean> = _isWorkoutActive.asStateFlow()

    private val _activeWorkoutTitle = MutableStateFlow("Gym Workout")
    val activeWorkoutTitle: StateFlow<String> = _activeWorkoutTitle.asStateFlow()

    private val _workoutStartTime = MutableStateFlow(0L)
    val workoutStartTime: StateFlow<Long> = _workoutStartTime.asStateFlow()

    private val _elapsedSeconds = MutableStateFlow(0L)
    val elapsedSeconds: StateFlow<Long> = _elapsedSeconds.asStateFlow()

    private val _activeExercises = MutableStateFlow<List<ActiveExerciseItem>>(emptyList())
    val activeExercises: StateFlow<List<ActiveExerciseItem>> = _activeExercises.asStateFlow()

    private var workoutTimerJob: Job? = null

    // --- REST TIMER STATE ---
    private val _isRestTimerActive = MutableStateFlow(false)
    val isRestTimerActive: StateFlow<Boolean> = _isRestTimerActive.asStateFlow()

    private val _restTotalSeconds = MutableStateFlow(90)
    val restTotalSeconds: StateFlow<Int> = _restTotalSeconds.asStateFlow()

    private val _restRemainingSeconds = MutableStateFlow(90)
    val restRemainingSeconds: StateFlow<Int> = _restRemainingSeconds.asStateFlow()
    private val _isRestOver = MutableStateFlow(false)
    val isRestOver: StateFlow<Boolean> = _isRestOver.asStateFlow()

    private var restTimerJob: Job? = null

    // PR Celebration Dialog state
    private val _prCelebration = MutableStateFlow<PrCelebration?>(null)
    val prCelebration: StateFlow<PrCelebration?> = _prCelebration.asStateFlow()

    fun dismissPrCelebration() {
        _prCelebration.value = null
    }

    // --- WORKOUT CONTROLS ---
    fun startWorkout(title: String = "Gym Session", initialExercises: List<ExerciseEntity> = emptyList()) {
        _activeWorkoutTitle.value = title
        _workoutStartTime.value = System.currentTimeMillis()
        _elapsedSeconds.value = 0L
        _isWorkoutActive.value = true

        val items = initialExercises.map { ex ->
            // Prepopulate with previous best or 3 starter sets
            val pr = personalRecords.value[ex.id]
            val lastWeight = pr?.maxWeight ?: if (ex.equipment == "Barbell") 40.0 else 15.0
            ActiveExerciseItem(
                exercise = ex,
                sets = listOf(
                    ActiveSet(weight = (lastWeight * 0.7).coerceAtLeast(10.0), reps = 12, isWarmup = true),
                    ActiveSet(weight = lastWeight, reps = 10),
                    ActiveSet(weight = lastWeight, reps = 8)
                )
            )
        }
        _activeExercises.value = items

        workoutTimerJob?.cancel()
        workoutTimerJob = viewModelScope.launch {
            while (_isWorkoutActive.value) {
                delay(1000L)
                _elapsedSeconds.value += 1
            }
        }
    }

    fun addExerciseToWorkout(exercise: ExerciseEntity) {
        val currentList = _activeExercises.value.toMutableList()
        if (currentList.none { it.exercise.id == exercise.id }) {
            val pr = personalRecords.value[exercise.id]
            val defaultWeight = pr?.maxWeight ?: if (exercise.equipment == "Barbell") 50.0 else 14.0
            currentList.add(
                ActiveExerciseItem(
                    exercise = exercise,
                    sets = listOf(
                        ActiveSet(weight = defaultWeight, reps = 10, isCompleted = false),
                        ActiveSet(weight = defaultWeight, reps = 10, isCompleted = false),
                        ActiveSet(weight = defaultWeight, reps = 8, isCompleted = false)
                    )
                )
            )
            _activeExercises.value = currentList
        }
    }

    fun removeExerciseFromWorkout(exerciseId: Long) {
        _activeExercises.value = _activeExercises.value.filter { it.exercise.id != exerciseId }
    }

    fun addSet(exerciseId: Long) {
        val currentList = _activeExercises.value.map { item ->
            if (item.exercise.id == exerciseId) {
                val lastSet = item.sets.lastOrNull()
                val nextSet = ActiveSet(
                    weight = lastSet?.weight ?: 20.0,
                    reps = lastSet?.reps ?: 10,
                    isCompleted = false,
                    isWarmup = false
                )
                item.copy(sets = item.sets + nextSet)
            } else {
                item
            }
        }
        _activeExercises.value = currentList
    }

    fun removeSet(exerciseId: Long, setIndex: Int) {
        val currentList = _activeExercises.value.map { item ->
            if (item.exercise.id == exerciseId) {
                val newSets = item.sets.toMutableList()
                if (setIndex in newSets.indices) {
                    newSets.removeAt(setIndex)
                }
                item.copy(sets = newSets)
            } else {
                item
            }
        }
        _activeExercises.value = currentList
    }

    fun updateSet(
        exerciseId: Long,
        setIndex: Int,
        weight: Double,
        reps: Int,
        isCompleted: Boolean,
        isWarmup: Boolean
    ) {
        var triggeredRestTimer = false
        var restSeconds = 90

        val currentList = _activeExercises.value.map { item ->
            if (item.exercise.id == exerciseId) {
                val newSets = item.sets.toMutableList()
                if (setIndex in newSets.indices) {
                    val wasCompleted = newSets[setIndex].isCompleted
                    newSets[setIndex] = newSets[setIndex].copy(
                        weight = weight,
                        reps = reps,
                        isCompleted = isCompleted,
                        isWarmup = isWarmup
                    )
                    // If newly checked as completed, trigger rest timer
                    if (!wasCompleted && isCompleted) {
                        triggeredRestTimer = true
                    }
                }
                item.copy(sets = newSets)
            } else {
                item
            }
        }
        _activeExercises.value = currentList
        if (triggeredRestTimer) {
            // Use user's default rest timer preference from DataStore
            val restSeconds = defaultRestTimerPreference.value
            startRestTimer(restSeconds)
        }

    }

    fun finishWorkout(notes: String = "") {
        val now = System.currentTimeMillis()
        val start = _workoutStartTime.value
        val duration = _elapsedSeconds.value
        val title = _activeWorkoutTitle.value

        val setsToSave = mutableListOf<WorkoutSetEntity>()
        _activeExercises.value.forEach { item ->
            item.sets.forEachIndexed { index, set ->
                if (set.isCompleted) {
                    setsToSave.add(
                        WorkoutSetEntity(
                            sessionId = 0,
                            exerciseId = item.exercise.id,
                            exerciseName = item.exercise.name,
                            setNumber = index + 1,
                            weight = set.weight,
                            reps = set.reps,
                            isCompleted = true,
                            isWarmup = set.isWarmup,
                            timestamp = now
                        )
                    )
                }
            }
        }

        if (setsToSave.isNotEmpty()) {
            viewModelScope.launch {
                val prs = repository.saveWorkoutSession(
                    title = title,
                    startTime = start,
                    endTime = now,
                    durationSeconds = duration,
                    notes = notes,
                    setsToSave = setsToSave
                )
                val totalVolume = setsToSave.sumOf { it.weight * it.reps }
                if (prs.isNotEmpty()) {
                    _prCelebration.value = PrCelebration(
                        announcements = prs,
                        workoutTitle = title,
                        totalVolume = totalVolume
                    )
                }
                // Auto backup to Cloud Firestore in background if authenticated
                val uid = authManager.getUserId()
                if (uid != null) {
                    firestoreSync.syncAllToCloud(uid)
                }
            }
        }

        cancelWorkout()
    }

    fun cancelWorkout() {
        _isWorkoutActive.value = false
        _activeExercises.value = emptyList()
        _elapsedSeconds.value = 0L
        workoutTimerJob?.cancel()
        stopRestTimer()
    }

    // --- REST TIMER CONTROLS ---
    fun startRestTimer(seconds: Int) {
        restTimerJob?.cancel()
        _isRestOver.value = false
        _restTotalSeconds.value = seconds
        _restRemainingSeconds.value = seconds
        _isRestTimerActive.value = true

        restTimerJob = viewModelScope.launch {
            while (_restRemainingSeconds.value > 0 && _isRestTimerActive.value) {
                delay(1000L)
                _restRemainingSeconds.value -= 1
            }
            if (_isRestTimerActive.value && _restRemainingSeconds.value <= 0) {
                _isRestOver.value = true
                triggerHapticFeedback()
            }
        }
    }

    fun adjustRestTimer(deltaSeconds: Int) {
        val current = _restRemainingSeconds.value
        val newTime = (current + deltaSeconds).coerceAtLeast(0)
        _restRemainingSeconds.value = newTime
        if (newTime <= 0) {
            _isRestOver.value = true
            restTimerJob?.cancel()
        } else {
            _isRestOver.value = false
            if (restTimerJob == null || restTimerJob?.isActive != true) {
                startRestTimer(newTime)
            }
        }
    }

    fun stopRestTimer() {
        _isRestTimerActive.value = false
        _isRestOver.value = false
        restTimerJob?.cancel()
    }
      

    private fun triggerHapticFeedback() {
        try {
            val context = getApplication<Application>()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createWaveform(longArrayOf(0, 200, 100, 200), -1)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(
                        VibrationEffect.createWaveform(longArrayOf(0, 200, 100, 200), -1)
                    )
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(300)
                }
            }
        } catch (_: Exception) {}
    }

    // --- WEIGHT LOGGING & GOALS ---
    fun logWeight(weight: Double, note: String = "", date: Long = System.currentTimeMillis()) {
        viewModelScope.launch {
            repository.insertWeightLog(weight, date, note)
            val uid = authManager.getUserId()
            if (uid != null) {
                firestoreSync.syncAllToCloud(uid)
            }
        }
    }

    fun deleteWeightLog(id: Long) {
        viewModelScope.launch {
            repository.deleteWeightLog(id)
            val uid = authManager.getUserId()
            if (uid != null) {
                firestoreSync.syncAllToCloud(uid)
            }
        }
    }

    fun updateProfile(
        startingWeight: Double,
        targetWeight: Double,
        goalType: String,
        weightUnit: String,
        weeklyGoal: Int
    ) {
        viewModelScope.launch {
            repository.updateProfile(
                startingWeight = startingWeight,
                targetWeight = targetWeight,
                goalType = goalType,
                weightUnit = weightUnit,
                weeklyGoal = weeklyGoal
            )
            val uid = authManager.getUserId()
            if (uid != null) {
                firestoreSync.syncAllToCloud(uid)
            }
        }
    }

    fun deleteWorkoutSession(id: Long) {
        viewModelScope.launch {
            repository.deleteSession(id)
        }
    }

    fun addCustomExercise(name: String, category: String, equipment: String, restSeconds: Int = 90) {
        viewModelScope.launch {
            repository.insertExercise(
                ExerciseEntity(
                    name = name.trim(),
                    category = category,
                    equipment = equipment,
                    isCustom = true,
                    defaultRestSeconds = restSeconds
                )
            )
        }
    }

    fun deleteExercise(id: Long) {
        viewModelScope.launch {
            repository.deleteExercise(id)
        }
    }

    suspend fun getSessionSets(sessionId: Long): List<WorkoutSetEntity> {
        return repository.getSetsForSessionSync(sessionId)
    }

    fun getSetsForExercise(exerciseId: Long): kotlinx.coroutines.flow.Flow<List<WorkoutSetEntity>> {
        return repository.getSetsForExercise(exerciseId)
    }

    fun updateProfile(name: String, targetWeight: Double) {
        viewModelScope.launch {
            val uid = authManager.getUserId() ?: ""
            val current = userProfile.value ?: UserProfileEntity(userId = uid)
            val updated = current.copy(
                userId = uid,
                name = name,
                targetWeight = targetWeight
            )
            repository.upsertProfile(updated)

    
            if (name.isNotBlank()) {
                try {
                    authManager.updateDisplayName(name)
                } catch (_: Exception) { }
            }

            if (uid.isNotBlank()) {
                firestoreSync.syncAllToCloud(uid)
            }
        }
    }

    fun setWeightUnit(unit: String) {
        viewModelScope.launch {
            settingsDataStore.setWeightUnit(unit)
            // also sync with room profile
            val current = userProfile.value ?: UserProfileEntity()
            repository.upsertProfile(current.copy(weightUnit = unit))
        }
    }

    fun setDefaultRestTimer(seconds: Int) {
        viewModelScope.launch {
            settingsDataStore.setDefaultRestSeconds(seconds)
        }
    }

    private fun autoSyncUser(userId: String) {
        viewModelScope.launch {
            _syncState.value = SyncUiState.Syncing
            val sessions = repository.allSessions.firstOrNull() ?: emptyList()
            val weight = repository.allWeightLogs.firstOrNull() ?: emptyList()

            // If clean install or new device, restore from Firestore into Room
            if (sessions.isEmpty() && weight.isEmpty()) {
                val restoreResult = firestoreSync.syncFromCloud(userId)
                _syncState.value = restoreResult.fold(
                    onSuccess = { count ->
                        if (count > 0) SyncUiState.Success("Restored $count items from Cloud")
                        else SyncUiState.Idle
                    },
                    onFailure = { err -> SyncUiState.Error("Auto-sync error: ${err.localizedMessage}") }
                )
            } else {
                // If local data exists, sync to Firestore
                val syncResult = firestoreSync.syncAllToCloud(userId)
                _syncState.value = syncResult.fold(
                    onSuccess = { count -> SyncUiState.Success("Synced $count items with Cloud") },
                    onFailure = { err -> SyncUiState.Error("Auto-sync error: ${err.localizedMessage}") }
                )
            }
        }
    }

    fun syncToCloud() {
        val uid = authManager.getUserId() ?: run {
            _syncState.value = SyncUiState.Error("Please log in to sync")
            return
        }
        viewModelScope.launch {
            _syncState.value = SyncUiState.Syncing
            val result = firestoreSync.syncAllToCloud(uid)
            _syncState.value = result.fold(
                onSuccess = { count -> SyncUiState.Success("Successfully backed up $count items to Cloud Firestore") },
                onFailure = { err -> SyncUiState.Error(err.localizedMessage ?: "Sync failed") }
            )
        }
    }

    fun restoreFromCloud() {
        val uid = authManager.getUserId() ?: run {
            _syncState.value = SyncUiState.Error("Please log in to restore")
            return
        }
        viewModelScope.launch {
            _syncState.value = SyncUiState.Syncing
            val result = firestoreSync.syncFromCloud(uid)
            _syncState.value = result.fold(
                onSuccess = { count -> SyncUiState.Success("Successfully restored $count items from Cloud Firestore") },
                onFailure = { err -> SyncUiState.Error(err.localizedMessage ?: "Restore failed") }
            )
        }
    }

    fun clearSyncMessage() {
        _syncState.value = SyncUiState.Idle
    }

    fun signOut() {
        val uid = authManager.getUserId()
        viewModelScope.launch {
            if (!uid.isNullOrBlank()) {
                repository.clearUserData(uid)
            }
            authManager.signOut()
        }
    }
}
