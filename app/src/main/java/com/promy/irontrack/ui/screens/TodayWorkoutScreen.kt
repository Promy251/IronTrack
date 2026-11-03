package com.promy.irontrack.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.promy.irontrack.data.model.ExerciseEntity
import com.promy.irontrack.ui.components.StreakCard
import com.promy.irontrack.ui.theme.AccentGreen
import com.promy.irontrack.ui.theme.AccentMuted
import com.promy.irontrack.ui.theme.AccentOnVolt
import com.promy.irontrack.ui.theme.AccentRed
import com.promy.irontrack.ui.theme.BgDark
import com.promy.irontrack.ui.theme.BorderSubtle
import com.promy.irontrack.ui.theme.SurfaceDark
import com.promy.irontrack.ui.theme.SurfaceLightDark
import com.promy.irontrack.ui.theme.TextMuted
import com.promy.irontrack.ui.theme.TextPrimary
import com.promy.irontrack.ui.theme.TextSecondary
import com.promy.irontrack.ui.viewmodel.ActiveExerciseItem
import com.promy.irontrack.ui.viewmodel.ActiveSet
import com.promy.irontrack.ui.viewmodel.WorkoutViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayWorkoutScreen(
    viewModel: WorkoutViewModel,
    onNavigateToHistory: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isWorkoutActive by viewModel.isWorkoutActive.collectAsState()
    val activeExercises by viewModel.activeExercises.collectAsState()
    val elapsedSeconds by viewModel.elapsedSeconds.collectAsState()
    val activeTitle by viewModel.activeWorkoutTitle.collectAsState()
    val allExercises by viewModel.allExercises.collectAsState()
    val streakInfo by viewModel.streakInfo.collectAsState()
    val personalRecords by viewModel.personalRecords.collectAsState()
    val allSessions by viewModel.allSessions.collectAsState()
    val isRestTimerActive by viewModel.isRestTimerActive.collectAsState()
    val restRemainingSeconds by viewModel.restRemainingSeconds.collectAsState()
    val isRestOver by viewModel.isRestOver.collectAsState()

    var showExercisePicker by remember { mutableStateOf(false) }
    var showFinishDialog by remember { mutableStateOf(false) }
    var finishNotes by remember { mutableStateOf("") }
    var showDiscardDialog by remember { mutableStateOf(false) }

    if (isWorkoutActive) {
        // --- ACTIVE WORKOUT LOGGING ---
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // Clean Active Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .testTag("active_workout_header"),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = activeTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    val mins = elapsedSeconds / 60
                    val secs = elapsedSeconds % 60
                    Text(
                        text = "%02d:%02d elapsed".format(mins, secs),
                        fontSize = 13.sp,
                        color = AccentMuted
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = { showDiscardDialog = true },
                        modifier = Modifier.testTag("discard_workout_button")
                    ) {
                        Text("Cancel", color = TextSecondary)
                    }

                    Button(
                        onClick = { showFinishDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentMuted, contentColor = AccentOnVolt),
                        shape = RoundedCornerShape(8.dp),
                        elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp),
                        modifier = Modifier.testTag("finish_workout_button")
                    ) {
                        Text("Finish", fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Exercise Sets List
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (activeExercises.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                            elevation = CardDefaults.cardElevation(0.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "No exercises added yet",
                                    fontWeight = FontWeight.Medium,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Tap below to add exercises to this workout.",
                                    fontSize = 13.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }

                items(activeExercises, key = { it.exercise.id }) { item ->
                    ActiveExerciseCard(
                        item = item,
                        prRecord = personalRecords[item.exercise.id],
                        onAddSet = { viewModel.addSet(item.exercise.id) },
                        onRemoveSet = { setIndex -> viewModel.removeSet(item.exercise.id, setIndex) },
                        onUpdateSet = { setIndex, weight, reps, completed, warmup ->
                            viewModel.updateSet(item.exercise.id, setIndex, weight, reps, completed, warmup)
                        },
                        onRemoveExercise = { viewModel.removeExerciseFromWorkout(item.exercise.id) }
                    )
                }

                item {
                    Button(
                        onClick = { showExercisePicker = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("add_exercise_to_workout_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = SurfaceDark, contentColor = TextPrimary),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                        elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add exercise", fontWeight = FontWeight.Medium)
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            // Rest Timer Bottom Bar
            if (isRestTimerActive) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .testTag("rest_timer_bar"),
                    shape = RoundedCornerShape(10.dp),
                    color = SurfaceDark,
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Countdown or "Rest over"
                        if (isRestOver) {
                            Text(
                                text = "Rest over",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = AccentRed
                            )
                        } else {
                            val mins = restRemainingSeconds / 60
                            val secs = restRemainingSeconds % 60
                            Text(
                                text = "Rest %d:%02d".format(mins, secs),
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = AccentMuted
                            )
                        }

                        // Controls: -15s, +15s, Skip
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (!isRestOver) {
                                Surface(
                                    onClick = { viewModel.adjustRestTimer(-15) },
                                    shape = RoundedCornerShape(6.dp),
                                    color = SurfaceLightDark,
                                    modifier = Modifier.testTag("rest_minus_15_button")
                                ) {
                                    Text(
                                        text = "-15s",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = TextSecondary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                    )
                                }
                            }

                            Surface(
                                onClick = { viewModel.adjustRestTimer(15) },
                                shape = RoundedCornerShape(6.dp),
                                color = SurfaceLightDark,
                                modifier = Modifier.testTag("rest_plus_15_button")
                            ) {
                                Text(
                                    text = "+15s",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextSecondary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                )
                            }

                            Surface(
                                onClick = { viewModel.stopRestTimer() },
                                shape = RoundedCornerShape(6.dp),
                                color = SurfaceLightDark,
                                modifier = Modifier.testTag("rest_skip_button")
                            ) {
                                Text(
                                    text = "Skip",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    } else {
        // --- TODAY WORKOUT DASHBOARD ---
        Box(
            modifier = modifier.fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp)
            ) {
                item {
                    Text(
                        text = "Today",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                // Week streak card (larger day circles with day letters below, streak on left, 3 of 3 this week on right)
                item {
                    StreakCard(streakInfo = streakInfo)
                }

                // Last Workout Card: two clean rows with name, date, duration, sets, volume
                item {
                    val lastWorkout = allSessions.firstOrNull()
                    if (lastWorkout == null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                            elevation = CardDefaults.cardElevation(0.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Last workout",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = AccentMuted,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "No workouts recorded yet. Start your first session!",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                        }
                    } else {
                        val dateFormatted = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(lastWorkout.startTime))
                        val durationMins = (lastWorkout.durationSeconds / 60).coerceAtLeast(1)

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable(onClick = onNavigateToHistory)
                                .testTag("last_workout_card"),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                            elevation = CardDefaults.cardElevation(0.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Last workout",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = AccentMuted,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "View in History ›",
                                        fontSize = 11.sp,
                                        color = TextMuted
                                    )
                                }

                                // Row 1: Workout name (left), Date · Duration (right)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = lastWorkout.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "$dateFormatted · $durationMins min",
                                        fontSize = 12.sp,
                                        color = TextSecondary
                                    )
                                }

                                // Row 2: Number of sets (left), Total volume (right)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${lastWorkout.totalSets} sets",
                                        fontSize = 13.sp,
                                        color = TextSecondary
                                    )
                                    Text(
                                        text = "${"%,.0f".format(lastWorkout.totalVolume)} kg total",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Pinned Bottom Button: full width, 16dp padding, just above navigation bar
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                BgDark.copy(alpha = 0.92f),
                                BgDark
                            )
                        )
                    )
                    .padding(16.dp)
            ) {
                Button(
                    onClick = { viewModel.startWorkout("Gym session") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("quick_start_workout_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentMuted, contentColor = AccentOnVolt),
                    shape = RoundedCornerShape(8.dp),
                    elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp)
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Start workout", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                }
            }
        }
    }

    // Modal Bottom Sheet: Select Exercise to add
    if (showExercisePicker) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { showExercisePicker = false },
            sheetState = sheetState,
            containerColor = SurfaceDark,
            shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Select exercise",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(10.dp))

                var searchQuery by remember { mutableStateOf("") }
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search exercises...", color = TextMuted) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                val filtered = allExercises.filter {
                    it.name.contains(searchQuery, ignoreCase = true) ||
                            it.category.contains(searchQuery, ignoreCase = true)
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filtered) { exercise ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.addExerciseToWorkout(exercise)
                                    showExercisePicker = false
                                },
                            shape = RoundedCornerShape(8.dp),
                            color = SurfaceLightDark,
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = exercise.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "${exercise.category} • ${exercise.equipment}",
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add",
                                    tint = AccentMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Finish Workout Dialog
    if (showFinishDialog) {
        AlertDialog(
            onDismissRequest = { showFinishDialog = false },
            containerColor = SurfaceDark,
            shape = RoundedCornerShape(8.dp),
            title = {
                Text("Finish workout", fontWeight = FontWeight.SemiBold, color = TextPrimary)
            },
            text = {
                Column {
                    Text("Save this session to your records?", color = TextSecondary)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = finishNotes,
                        onValueChange = { finishNotes = it },
                        placeholder = { Text("Notes (optional)", color = TextMuted) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.finishWorkout(finishNotes)
                        showFinishDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentMuted, contentColor = AccentOnVolt),
                    shape = RoundedCornerShape(8.dp),
                    elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp)
                ) {
                    Text("Save", fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showFinishDialog = false }) {
                    Text("Keep going", color = TextSecondary)
                }
            }
        )
    }

    // Discard Workout Dialog
    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            containerColor = SurfaceDark,
            shape = RoundedCornerShape(8.dp),
            title = {
                Text("Discard workout?", fontWeight = FontWeight.SemiBold, color = AccentRed)
            },
            text = {
                Text("Current workout progress will not be saved.", color = TextSecondary)
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.cancelWorkout()
                        showDiscardDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed, contentColor = TextPrimary),
                    shape = RoundedCornerShape(8.dp),
                    elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp)
                ) {
                    Text("Discard", fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text("Resume", color = TextSecondary)
                }
            }
        )
    }
}

/**
 * Clean & Flat Exercise card within active workout (8dp corners, 1dp border, no shadows)
 */
@Composable
fun ActiveExerciseCard(
    item: ActiveExerciseItem,
    prRecord: com.promy.irontrack.data.model.PersonalRecord?,
    onAddSet: () -> Unit,
    onRemoveSet: (Int) -> Unit,
    onUpdateSet: (Int, Double, Int, Boolean, Boolean) -> Unit,
    onRemoveExercise: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Exercise Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = item.exercise.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = item.exercise.category,
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                        if (prRecord != null && prRecord.maxWeight > 0.0) {
                            Text(text = "•", fontSize = 11.sp, color = TextMuted)
                            Text(
                                text = "PR: ${prRecord.maxWeight} kg",
                                fontSize = 11.sp,
                                color = AccentMuted,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                IconButton(onClick = onRemoveExercise, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove exercise",
                        tint = TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Column Header (sentence case)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Set", fontSize = 11.sp, color = TextMuted, fontWeight = FontWeight.Medium, modifier = Modifier.width(36.dp))
                Text("Kg", fontSize = 11.sp, color = TextMuted, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                Text("Reps", fontSize = 11.sp, color = TextMuted, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                Text("Done", fontSize = 11.sp, color = TextMuted, fontWeight = FontWeight.Medium, modifier = Modifier.width(42.dp))
            }

            // Set rows
            item.sets.forEachIndexed { index, set ->
                SetRowItem(
                    setNumber = index + 1,
                    set = set,
                    onWeightChange = { newW ->
                        onUpdateSet(index, newW, set.reps, set.isCompleted, set.isWarmup)
                    },
                    onRepsChange = { newR ->
                        onUpdateSet(index, set.weight, newR, set.isCompleted, set.isWarmup)
                    },
                    onToggleComplete = { completed ->
                        onUpdateSet(index, set.weight, set.reps, completed, set.isWarmup)
                    }
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Add set button
            TextButton(
                onClick = onAddSet,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = AccentMuted)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add set", fontSize = 13.sp, color = AccentMuted, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
fun SetRowItem(
    setNumber: Int,
    set: ActiveSet,
    onWeightChange: (Double) -> Unit,
    onRepsChange: (Int) -> Unit,
    onToggleComplete: (Boolean) -> Unit
) {
    var weightText by remember(set.weight) { mutableStateOf(if (set.weight > 0) set.weight.toString() else "") }
    var repsText by remember(set.reps) { mutableStateOf(set.reps.toString()) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .background(
                if (set.isCompleted) AccentGreen.copy(alpha = 0.08f) else Color.Transparent,
                RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Set number
        Text(
            text = "$setNumber",
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            color = if (set.isCompleted) AccentGreen else TextSecondary,
            modifier = Modifier.width(36.dp)
        )

        // Weight Input
        OutlinedTextField(
            value = weightText,
            onValueChange = { text ->
                weightText = text
                text.toDoubleOrNull()?.let { onWeightChange(it) }
            },
            modifier = Modifier
                .weight(1f)
                .height(46.dp),
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            shape = RoundedCornerShape(8.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Reps Input
        OutlinedTextField(
            value = repsText,
            onValueChange = { text ->
                repsText = text
                text.toIntOrNull()?.let { onRepsChange(it) }
            },
            modifier = Modifier
                .weight(1f)
                .height(46.dp),
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            shape = RoundedCornerShape(8.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Done Checkbox button
        IconButton(
            onClick = { onToggleComplete(!set.isCompleted) },
            modifier = Modifier
                .size(40.dp)
                .background(
                    if (set.isCompleted) AccentGreen else SurfaceLightDark,
                    RoundedCornerShape(8.dp)
                )
                .testTag("set_checkbox_$setNumber")
        ) {
            Icon(
                imageVector = if (set.isCompleted) Icons.Default.Check else Icons.Default.RadioButtonUnchecked,
                contentDescription = "Complete set",
                tint = if (set.isCompleted) AccentOnVolt else TextMuted,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
