package com.promy.irontrack.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.promy.irontrack.data.model.ExerciseEntity
import com.promy.irontrack.data.model.PersonalRecord
import com.promy.irontrack.ui.theme.AccentMuted
import com.promy.irontrack.ui.theme.AccentOnVolt
import com.promy.irontrack.ui.theme.BorderSubtle
import com.promy.irontrack.ui.theme.SurfaceDark
import com.promy.irontrack.ui.theme.SurfaceLightDark
import com.promy.irontrack.ui.theme.TextMuted
import com.promy.irontrack.ui.theme.TextPrimary
import com.promy.irontrack.ui.theme.TextSecondary
import com.promy.irontrack.ui.viewmodel.WorkoutViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExercisesScreen(
    viewModel: WorkoutViewModel,
    modifier: Modifier = Modifier
) {
    val exercises by viewModel.allExercises.collectAsState()
    val personalRecords by viewModel.personalRecords.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedExerciseForDetail by remember { mutableStateOf<ExerciseEntity?>(null) }

    val categories = listOf("All", "Chest", "Back", "Legs", "Shoulders", "Arms", "Core")

    val filteredExercises = exercises.filter { ex ->
        val matchesCategory = (selectedCategory == "All" || ex.category.equals(selectedCategory, ignoreCase = true))
        val matchesSearch = ex.name.contains(searchQuery, ignoreCase = true) ||
                ex.equipment.contains(searchQuery, ignoreCase = true)
        matchesCategory && matchesSearch
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // Search Bar (sentence case, 8dp corners, 1dp border)
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search exercises...", color = TextMuted) },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = TextMuted)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("exercise_search_bar"),
                shape = RoundedCornerShape(8.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Muscle Category Filter Pills (8dp corners)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(categories) { category ->
                    val isSelected = selectedCategory == category
                    Surface(
                        modifier = Modifier
                            .clickable { selectedCategory = category }
                            .testTag("filter_category_$category"),
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) AccentMuted else SurfaceDark,
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
                    ) {
                        Text(
                            text = category,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected) AccentOnVolt else TextSecondary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // List of exercises (Flat cards, 8dp corners, 1dp border #2E2E2E)
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredExercises, key = { it.id }) { exercise ->
                    val pr = personalRecords[exercise.id]
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedExerciseForDetail = exercise }
                            .testTag("exercise_card_${exercise.id}"),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = exercise.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = TextPrimary
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "${exercise.category} • ${exercise.equipment}",
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                    if (pr != null && pr.maxWeight > 0.0) {
                                        Text(text = "•", fontSize = 11.sp, color = TextMuted)
                                        Text(
                                            text = "PR: ${pr.maxWeight} kg",
                                            fontSize = 11.sp,
                                            color = TextSecondary,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }

                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }

        // Floating Action Button (muted accent, no glow)
        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("add_custom_exercise_fab"),
            containerColor = AccentMuted,
            contentColor = AccentOnVolt,
            shape = CircleShape,
            elevation = androidx.compose.material3.FloatingActionButtonDefaults.elevation(0.dp, 0.dp)
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Add exercise")
        }
    }

    // Custom Exercise Creation Dialog
    if (showAddDialog) {
        AddCustomExerciseDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { name, cat, equip, rest ->
                viewModel.addCustomExercise(name, cat, equip, rest)
                showAddDialog = false
            }
        )
    }

    // Exercise Detail Sheet
    if (selectedExerciseForDetail != null) {
        val exercise = selectedExerciseForDetail!!
        val pr = personalRecords[exercise.id]
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(
            onDismissRequest = { selectedExerciseForDetail = null },
            sheetState = sheetState,
            containerColor = SurfaceDark,
            shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
        ) {
            ExerciseDetailSheetContent(
                exercise = exercise,
                pr = pr,
                viewModel = viewModel,
                onDismiss = { selectedExerciseForDetail = null }
            )
        }
    }
}

@Composable
fun ExerciseDetailSheetContent(
    exercise: ExerciseEntity,
    pr: PersonalRecord?,
    viewModel: WorkoutViewModel,
    onDismiss: () -> Unit
) {
    val exerciseSets by viewModel.getSetsForExercise(exercise.id).collectAsState(initial = emptyList())
    val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = exercise.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Text(
                    text = "${exercise.category} • ${exercise.equipment}",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }

            Button(
                onClick = {
                    viewModel.addExerciseToWorkout(exercise)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = AccentMuted, contentColor = AccentOnVolt),
                shape = RoundedCornerShape(8.dp),
                elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp)
            ) {
                Text("Add to workout", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Simple PR stats row (8dp corners, 1dp border)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceLightDark, RoundedCornerShape(8.dp))
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Best", fontSize = 10.sp, color = TextMuted, fontWeight = FontWeight.Medium)
                Text(
                    text = if (pr != null && pr.maxWeight > 0.0) "${pr.maxWeight} kg" else "--",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Est 1RM", fontSize = 10.sp, color = TextMuted, fontWeight = FontWeight.Medium)
                Text(
                    text = if (pr != null && pr.bestEstimated1RM > 0.0) "${"%.1f".format(pr.bestEstimated1RM)} kg" else "--",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AccentMuted
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Total sets", fontSize = 10.sp, color = TextMuted, fontWeight = FontWeight.Medium)
                Text(
                    text = "${exerciseSets.size}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Past sets",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (exerciseSets.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No past sets logged yet", color = TextMuted, fontSize = 12.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(exerciseSets.reversed()) { set ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = SurfaceLightDark,
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${set.weight} kg × ${set.reps} reps",
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp,
                                color = TextPrimary
                            )
                            Text(
                                text = dateFormat.format(Date(set.timestamp)),
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCustomExerciseDialog(
    onDismiss: () -> Unit,
    onAdd: (name: String, category: String, equipment: String, restSeconds: Int) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Chest") }
    var equipment by remember { mutableStateOf("Barbell") }

    val categories = listOf("Chest", "Back", "Legs", "Shoulders", "Arms", "Core")
    val equipments = listOf("Barbell", "Dumbbell", "Machine", "Cable", "Bodyweight")

    var categoryExpanded by remember { mutableStateOf(false) }
    var equipmentExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        shape = RoundedCornerShape(8.dp),
        title = {
            Text("New exercise", fontWeight = FontWeight.SemiBold, color = TextPrimary)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Exercise name") },
                    placeholder = { Text("E.g. Incline Smith Press", color = TextMuted) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = it }
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Muscle group") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape = RoundedCornerShape(8.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = {
                                    category = cat
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = equipmentExpanded,
                    onExpandedChange = { equipmentExpanded = it }
                ) {
                    OutlinedTextField(
                        value = equipment,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Equipment") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = equipmentExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape = RoundedCornerShape(8.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = equipmentExpanded,
                        onDismissRequest = { equipmentExpanded = false }
                    ) {
                        equipments.forEach { eq ->
                            DropdownMenuItem(
                                text = { Text(eq) },
                                onClick = {
                                    equipment = eq
                                    equipmentExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onAdd(name, category, equipment, 90)
                    }
                },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = AccentMuted, contentColor = AccentOnVolt),
                shape = RoundedCornerShape(8.dp),
                elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp)
            ) {
                Text("Add", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}
