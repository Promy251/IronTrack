package com.promy.irontrack.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.promy.irontrack.data.auth.AuthManager
import com.promy.irontrack.ui.theme.AccentGreen
import com.promy.irontrack.ui.theme.AccentOnVolt
import com.promy.irontrack.ui.theme.AccentRed
import com.promy.irontrack.ui.theme.AccentVolt
import com.promy.irontrack.ui.theme.BgDark
import com.promy.irontrack.ui.theme.BorderSubtle
import com.promy.irontrack.ui.theme.SurfaceDark
import com.promy.irontrack.ui.theme.TextMuted
import com.promy.irontrack.ui.theme.TextPrimary
import com.promy.irontrack.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun VerifyEmailScreen(
    userEmail: String,
    onVerified: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val authManager = remember { AuthManager.getInstance(context) }
    val scope = rememberCoroutineScope()

    var isChecking by remember { mutableStateOf(false) }
    var isResending by remember { mutableStateOf(false) }
    var cooldownSeconds by remember { mutableIntStateOf(0) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    // 60-second cooldown timer for resend button
    LaunchedEffect(cooldownSeconds) {
        if (cooldownSeconds > 0) {
            delay(1000L)
            cooldownSeconds -= 1
        }
    }

    // Prevent hardware back button from navigating to main app without verification
    BackHandler {
        onSignOut()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(horizontal = 24.dp)
            .testTag("verify_email_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(SurfaceDark)
                    .border(1.dp, BorderSubtle, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Email,
                    contentDescription = null,
                    tint = AccentVolt,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Title
            Text(
                text = "Verify your email",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Subtitle with user email
            Text(
                text = "We sent a verification link to:",
                fontSize = 14.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = userEmail.ifEmpty { "your email" },
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = AccentVolt,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Hint box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceDark)
                    .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Text(
                    text = "Please open the email and click the confirmation link to activate your account. If you don't see it, be sure to check your spam or junk folder.",
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
            }

            // Success feedback
            if (successMessage != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = successMessage!!,
                    fontSize = 13.sp,
                    color = AccentGreen,
                    textAlign = TextAlign.Center
                )
            }

            // Error feedback
            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = errorMessage!!,
                    fontSize = 13.sp,
                    color = AccentRed,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // "I've verified" primary button
            Button(
                onClick = {
                    if (isChecking) return@Button
                    isChecking = true
                    errorMessage = null
                    successMessage = null
                    scope.launch {
                        try {
                            val verified = authManager.reloadUser()
                            if (verified) {
                                onVerified()
                            } else {
                                errorMessage = "Email is not verified yet. Please tap the link in your email and try again."
                            }
                        } catch (e: Exception) {
                            errorMessage = e.message ?: "Failed to verify email status"
                        } finally {
                            isChecking = false
                        }
                    }
                },
                enabled = !isChecking,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("verify_email_check_button"),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentVolt,
                    contentColor = AccentOnVolt
                )
            ) {
                if (isChecking) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = AccentOnVolt
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Checking...", fontWeight = FontWeight.SemiBold)
                } else {
                    Text("I've verified", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // "Resend email" button with 60-second cooldown
            OutlinedButton(
                onClick = {
                    if (cooldownSeconds > 0 || isResending) return@OutlinedButton
                    isResending = true
                    errorMessage = null
                    successMessage = null
                    scope.launch {
                        try {
                            authManager.sendVerificationEmail()
                            cooldownSeconds = 60
                            successMessage = "Verification email sent! Check your inbox."
                        } catch (e: Exception) {
                            errorMessage = e.message ?: "Failed to resend email. Please try again later."
                        } finally {
                            isResending = false
                        }
                    }
                },
                enabled = cooldownSeconds == 0 && !isResending,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("verify_email_resend_button"),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = if (cooldownSeconds > 0) TextMuted else TextPrimary
                )
            ) {
                if (isResending) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = TextPrimary
                    )
                } else if (cooldownSeconds > 0) {
                    Text("Resend email in ${cooldownSeconds}s", fontSize = 14.sp)
                } else {
                    Text("Resend email", fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Log out text button
            TextButton(
                onClick = {
                    authManager.signOut {
                        onSignOut()
                    }
                },
                modifier = Modifier.testTag("verify_email_logout_button")
            ) {
                Text(
                    text = "Log out",
                    color = AccentRed,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
