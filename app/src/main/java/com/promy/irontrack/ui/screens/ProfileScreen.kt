package com.promy.irontrack.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

@Composable
fun ProfileScreen(
    viewModel: WorkoutViewModel,
    modifier: Modifier = Modifier
) {
    var showBodyWeightScreen by remember { mutableStateOf(false) }
    var showChangePasswordScreen by remember { mutableStateOf(false) }

    if (showBodyWeightScreen) {
        BodyWeightScreen(
            viewModel = viewModel,
            onNavigateBack = { showBodyWeightScreen = false },
            modifier = modifier
        )
        return
    }

    if (showChangePasswordScreen) {
        ChangePasswordScreen(
            onNavigateBack = { showChangePasswordScreen = false },
            modifier = modifier
        )
        return
    }

    val weightLogs by viewModel.weightLogs.collectAsState()
    val profile by viewModel.userProfile.collectAsState()
    val weightUnitPref by viewModel.weightUnitPreference.collectAsState()
    val restTimerPref by viewModel.defaultRestTimerPreference.collectAsState()
    val authUser by viewModel.currentUser.collectAsState()

    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showSignOutDialog by remember { mutableStateOf(false) }

    val name = profile?.name?.ifBlank { null }
        ?: authUser?.displayName?.ifBlank { null }
        ?: authUser?.email?.substringBefore("@")?.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        ?: "Athlete"
    val email = authUser?.email?.ifBlank { null }
        ?: profile?.email?.ifBlank { null }
        ?: ""
    val initial = name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "A"

    val targetWeight = profile?.targetWeight ?: 0.0
    val currentWeight = weightLogs.firstOrNull()?.weight ?: profile?.startingWeight ?: 0.0

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // 1. Centered Header: circular avatar with first initial, bold name, grey email, edit profile button
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    modifier = Modifier.size(76.dp),
                    shape = CircleShape,
                    color = AccentMuted
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = initial,
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentOnVolt
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                if (email.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = email,
                        fontSize = 14.sp,
                        color = TextSecondary
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                TextButton(
                    onClick = { showEditProfileDialog = true },
                    modifier = Modifier.testTag("edit_profile_button")
                ) {
                    Text(
                        text = "Edit profile",
                        color = AccentMuted,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // 2. Section: BODY
        item {
            ProfileSection(title = "BODY") {
                ProfileRowItem(
                    icon = Icons.Default.FitnessCenter,
                    label = "Current weight",
                    value = if (currentWeight > 0.0) "$currentWeight $weightUnitPref" else "Not set",
                    onClick = { showBodyWeightScreen = true },
                    testTag = "profile_row_current_weight"
                )
                HorizontalDivider(color = BorderSubtle, thickness = 1.dp)
                ProfileRowItem(
                    icon = Icons.Default.Flag,
                    label = "Target weight",
                    value = if (targetWeight > 0.0) "$targetWeight $weightUnitPref" else "Not set",
                    onClick = { showEditProfileDialog = true },
                    testTag = "profile_row_target_weight"
                )
                HorizontalDivider(color = BorderSubtle, thickness = 1.dp)
                ProfileRowItem(
                    icon = Icons.Default.ShowChart,
                    label = "Body weight history",
                    showChevron = true,
                    onClick = { showBodyWeightScreen = true },
                    testTag = "profile_row_body_weight_history"
                )
            }
        }

        // 3. Section: SETTINGS
        item {
            ProfileSection(title = "SETTINGS") {
                // Units row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .testTag("profile_row_units"),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Units",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("kg", "lb").forEach { u ->
                            val isSelected = weightUnitPref == u
                            Surface(
                                modifier = Modifier
                                    .clickable { viewModel.setWeightUnit(u) }
                                    .testTag("unit_toggle_$u"),
                                shape = RoundedCornerShape(6.dp),
                                color = if (isSelected) AccentMuted else SurfaceLightDark,
                                border = BorderStroke(1.dp, BorderSubtle)
                            ) {
                                Text(
                                    text = u,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isSelected) AccentOnVolt else TextSecondary,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = BorderSubtle, thickness = 1.dp)

                // Default rest timer row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .testTag("profile_row_rest_timer"),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Default rest timer",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(60, 90, 120).forEach { sec ->
                            val isSelected = restTimerPref == sec
                            Surface(
                                modifier = Modifier
                                    .clickable { viewModel.setDefaultRestTimer(sec) }
                                    .testTag("rest_timer_$sec"),
                                shape = RoundedCornerShape(6.dp),
                                color = if (isSelected) AccentMuted else SurfaceLightDark,
                                border = BorderStroke(1.dp, BorderSubtle)
                            ) {
                                Text(
                                    text = "${sec}s",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isSelected) AccentOnVolt else TextSecondary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. Section: ABOUT
        item {
            ProfileSection(title = "ABOUT") {
                ProfileRowItem(
                    icon = Icons.Default.Info,
                    label = "App version",
                    value = "1.0.0",
                    testTag = "profile_row_app_version"
                )
            }
        }

        // 5. Section: ACCOUNT
        item {
            ProfileSection(title = "ACCOUNT") {
                ProfileRowItem(
                    icon = Icons.Default.Lock,
                    label = "Change password",
                    showChevron = true,
                    onClick = { showChangePasswordScreen = true },
                    testTag = "profile_row_change_password"
                )

                HorizontalDivider(color = BorderSubtle, thickness = 1.dp)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showSignOutDialog = true }
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                        .testTag("log_out_row"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Logout,
                        contentDescription = "Log out",
                        tint = AccentRed,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Log out",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AccentRed
                    )
                }
            }
        }
    }

    // Dialog: Edit Profile (Name, Target weight)
    if (showEditProfileDialog) {
        var inputName by remember { mutableStateOf(name) }
        var inputTarget by remember { mutableStateOf(if (targetWeight > 0.0) targetWeight.toString() else "") }

        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            containerColor = SurfaceDark,
            title = { Text("Edit profile", color = TextPrimary, fontWeight = FontWeight.SemiBold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = inputName,
                        onValueChange = { inputName = it },
                        label = { Text("Name") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_profile_name")
                    )
                    OutlinedTextField(
                        value = inputTarget,
                        onValueChange = { inputTarget = it },
                        label = { Text("Target weight ($weightUnitPref)") },
                        placeholder = { Text("e.g. 75.0") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_profile_target")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val tw = inputTarget.toDoubleOrNull() ?: targetWeight
                        viewModel.updateProfile(inputName.trim(), tw)
                        showEditProfileDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentMuted, contentColor = AccentOnVolt),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("save_profile_button")
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfileDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // Dialog: Sign Out confirmation
    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            containerColor = SurfaceDark,
            title = { Text("Log out", color = TextPrimary, fontWeight = FontWeight.SemiBold) },
            text = { Text("Are you sure you want to log out of IronTrack?", color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        showSignOutDialog = false
                        viewModel.signOut()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("confirm_sign_out_button")
                ) {
                    Text("Log out", color = TextPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun ProfileSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextMuted,
            letterSpacing = 0.5.sp,
            modifier = Modifier.padding(start = 4.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            border = BorderStroke(1.dp, BorderSubtle),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                content()
            }
        }
    }
}

@Composable
private fun ProfileRowItem(
    icon: ImageVector,
    label: String,
    value: String? = null,
    showChevron: Boolean = false,
    onClick: (() -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    testTag: String = ""
) {
    val modifier = Modifier
        .fillMaxWidth()
        .then(
            if (onClick != null) Modifier.clickable(onClick = onClick)
            else Modifier
        )
        .padding(horizontal = 16.dp, vertical = 14.dp)
        .then(
            if (testTag.isNotBlank()) Modifier.testTag(testTag)
            else Modifier
        )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary
            )
        }

        if (trailingContent != null) {
            trailingContent()
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (value != null) {
                    Text(
                        text = value,
                        fontSize = 14.sp,
                        color = TextSecondary
                    )
                }
                if (showChevron) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
