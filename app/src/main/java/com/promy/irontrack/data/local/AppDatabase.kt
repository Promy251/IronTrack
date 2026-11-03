package com.promy.irontrack.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.promy.irontrack.data.model.ExerciseEntity
import com.promy.irontrack.data.model.UserProfileEntity
import com.promy.irontrack.data.model.WeightLogEntity
import com.promy.irontrack.data.model.WorkoutSessionEntity
import com.promy.irontrack.data.model.WorkoutSetEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        ExerciseEntity::class,
        WorkoutSessionEntity::class,
        WorkoutSetEntity::class,
        WeightLogEntity::class,
        UserProfileEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun workoutDao(): WorkoutDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "iron_track_database"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(DatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialData(database.workoutDao())
                    }
                }
            }

            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        migrateExerciseNames(database.workoutDao())
                    }
                }
            }
        }

        suspend fun populateInitialData(dao: WorkoutDao) {
            // Seed standard gym exercises with short, simple names in sentence case without repeating equipment
            val defaultExercises = listOf(
                // Chest
                ExerciseEntity(name = "Bench press", category = "Chest", equipment = "Barbell", defaultRestSeconds = 120),
                ExerciseEntity(name = "Incline press", category = "Chest", equipment = "Dumbbell", defaultRestSeconds = 90),
                ExerciseEntity(name = "Chest fly", category = "Chest", equipment = "Cable", defaultRestSeconds = 60),
                ExerciseEntity(name = "Dip", category = "Chest", equipment = "Bodyweight", defaultRestSeconds = 90),
                
                // Back
                ExerciseEntity(name = "Deadlift", category = "Back", equipment = "Barbell", defaultRestSeconds = 180),
                ExerciseEntity(name = "Barbell row", category = "Back", equipment = "Barbell", defaultRestSeconds = 90),
                ExerciseEntity(name = "Pull-up", category = "Back", equipment = "Bodyweight", defaultRestSeconds = 90),
                ExerciseEntity(name = "Lat pulldown", category = "Back", equipment = "Cable", defaultRestSeconds = 75),
                ExerciseEntity(name = "Seated row", category = "Back", equipment = "Cable", defaultRestSeconds = 75),
                
                // Legs
                ExerciseEntity(name = "Squat", category = "Legs", equipment = "Barbell", defaultRestSeconds = 150),
                ExerciseEntity(name = "Romanian deadlift", category = "Legs", equipment = "Barbell", defaultRestSeconds = 120),
                ExerciseEntity(name = "Leg press", category = "Legs", equipment = "Machine", defaultRestSeconds = 90),
                ExerciseEntity(name = "Leg curl", category = "Legs", equipment = "Machine", defaultRestSeconds = 60),
                ExerciseEntity(name = "Calf raise", category = "Legs", equipment = "Machine", defaultRestSeconds = 45),
                
                // Shoulders
                ExerciseEntity(name = "Overhead press", category = "Shoulders", equipment = "Barbell", defaultRestSeconds = 120),
                ExerciseEntity(name = "Lateral raise", category = "Shoulders", equipment = "Dumbbell", defaultRestSeconds = 60),
                ExerciseEntity(name = "Face pull", category = "Shoulders", equipment = "Cable", defaultRestSeconds = 60),
                ExerciseEntity(name = "Shoulder press", category = "Shoulders", equipment = "Dumbbell", defaultRestSeconds = 90),
                
                // Arms
                ExerciseEntity(name = "Bicep curl", category = "Arms", equipment = "Barbell", defaultRestSeconds = 60),
                ExerciseEntity(name = "Incline curl", category = "Arms", equipment = "Dumbbell", defaultRestSeconds = 60),
                ExerciseEntity(name = "Tricep pushdown", category = "Arms", equipment = "Cable", defaultRestSeconds = 60),
                ExerciseEntity(name = "Skull crusher", category = "Arms", equipment = "Barbell", defaultRestSeconds = 75),
                
                // Core
                ExerciseEntity(name = "Hanging leg raise", category = "Core", equipment = "Bodyweight", defaultRestSeconds = 60),
                ExerciseEntity(name = "Plank", category = "Core", equipment = "Bodyweight", defaultRestSeconds = 60),
                ExerciseEntity(name = "Crunch", category = "Core", equipment = "Cable", defaultRestSeconds = 60)
            )
            dao.insertExercises(defaultExercises)
        }

        suspend fun migrateExerciseNames(dao: WorkoutDao) {
            val renameMap = mapOf(
                "Barbell Bench Press" to "Bench press",
                "Incline Dumbbell Press" to "Incline press",
                "Cable Chest Fly" to "Chest fly",
                "Dips" to "Dip",
                "Barbell Bent-Over Row" to "Barbell row",
                "Pull-Ups" to "Pull-up",
                "Lat Pulldown" to "Lat pulldown",
                "Seated Cable Row" to "Seated row",
                "Barbell Squat" to "Squat",
                "Romanian Deadlift" to "Romanian deadlift",
                "Leg Press" to "Leg press",
                "Leg Curl" to "Leg curl",
                "Standing Calf Raise" to "Calf raise",
                "Overhead Barbell Press" to "Overhead press",
                "Dumbbell Lateral Raise" to "Lateral raise",
                "Face Pull" to "Face pull",
                "Dumbbell Shoulder Press" to "Shoulder press",
                "Barbell Bicep Curl" to "Bicep curl",
                "Incline Dumbbell Curl" to "Incline curl",
                "Tricep Rope Pushdown" to "Tricep pushdown",
                "Skull Crushers" to "Skull crusher",
                "Hanging Leg Raise" to "Hanging leg raise",
                "Plank" to "Plank",
                "Cable Crunch" to "Crunch"
            )

            renameMap.forEach { (oldName, newName) ->
                dao.updateExerciseName(oldName, newName)
                dao.updateSetExerciseName(oldName, newName)
            }
        }
    }
}
