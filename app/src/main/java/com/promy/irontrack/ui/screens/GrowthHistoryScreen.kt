package com.promy.irontrack.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.promy.irontrack.data.model.WorkoutSessionEntity
import com.promy.irontrack.data.model.WorkoutSetEntity
import com.promy.irontrack.ui.theme.AccentMuted
import com.promy.irontrack.ui.theme.AccentOnVolt
import com.promy.irontrack.ui.theme.AccentRed
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

@Composable
fun GrowthHistoryScreen(
    viewModel: WorkoutViewModel,
    modifier: Modifier = Modifier
) {
    val sessions by viewModel.allSessions.collectAsState()
    var sessionToDelete by remember { mutableStateOf<WorkoutSessionEntity?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "History",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(12.dp))

        if (sessions.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No workout records yet", color = TextMuted, fontSize = 13.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(sessions, key = { it.id }) { session ->
                    SimplePastSessionCard(
                        session = session,
                        viewModel = viewModel,
                        onDelete = { sessionToDelete = session }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }

    if (sessionToDelete != null) {
        val s = sessionToDelete!!
        AlertDialog(
            onDismissRequest = { sessionToDelete = null },
            containerColor = SurfaceDark,
            shape = RoundedCornerShape(8.dp),
            title = { Text("Delete workout?", fontWeight = FontWeight.SemiBold, color = AccentRed) },
            text = { Text("Delete '${s.title}' from history?", color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteWorkoutSession(s.id)
                        sessionToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed, contentColor = TextPrimary),
                    shape = RoundedCornerShape(8.dp),
                    elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp)
                ) {
                    Text("Delete", fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { sessionToDelete = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
fun SimplePastSessionCard(
    session: WorkoutSessionEntity,
    viewModel: WorkoutViewModel,
    onDelete: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    var sets by remember { mutableStateOf<List<WorkoutSetEntity>>(emptyList()) }

    LaunchedEffect(isExpanded) {
        if (isExpanded && sets.isEmpty()) {
            sets = viewModel.getSessionSets(session.id)
        }
    }

    val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    val durationMins = session.durationSeconds / 60

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("past_session_${session.id}"),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = session.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Text(
                        text = "${dateFormat.format(Date(session.startTime))}${if (durationMins > 0) " • ${durationMins}m" else ""}",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }

                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${"%,.0f".format(session.totalVolume)} kg total • ${session.totalSets} sets",
                    fontSize = 12.sp,
                    color = AccentMuted,
                    fontWeight = FontWeight.Medium
                )

                Row(
                    modifier = Modifier.clickable { isExpanded = !isExpanded },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isExpanded) "Hide" else "Details",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val grouped = sets.groupBy { it.exerciseId }
                    grouped.forEach { (_, setList) ->
                        val exerciseName = setList.firstOrNull()?.exerciseName.orEmpty()
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(6.dp),
                            color = SurfaceLightDark,
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(
                                    text = exerciseName,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 12.sp,
                                    color = TextPrimary
                                )
                                setList.forEach { s ->
                                    Text(
                                        text = "Set ${s.setNumber}: ${s.weight} kg × ${s.reps} reps" + if (s.isPr) " • PR" else "",
                                        fontSize = 11.sp,
                                        color = if (s.isPr) AccentMuted else TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
