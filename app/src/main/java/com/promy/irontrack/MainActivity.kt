package com.promy.irontrack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.promy.irontrack.ui.screens.ExercisesScreen
import com.promy.irontrack.ui.screens.GrowthHistoryScreen
import com.promy.irontrack.ui.screens.LoginScreen
import com.promy.irontrack.ui.screens.ProfileScreen
import com.promy.irontrack.ui.screens.SignUpScreen
import com.promy.irontrack.ui.screens.TodayWorkoutScreen
import com.promy.irontrack.ui.screens.VerifyEmailScreen
import com.promy.irontrack.ui.theme.AccentGreen
import com.promy.irontrack.ui.theme.AccentMuted
import com.promy.irontrack.ui.theme.AccentOnVolt
import com.promy.irontrack.ui.theme.BgDark
import com.promy.irontrack.ui.theme.BorderSubtle
import com.promy.irontrack.ui.theme.IronTrackTheme
import com.promy.irontrack.ui.theme.SurfaceDark
import com.promy.irontrack.ui.theme.TextMuted
import com.promy.irontrack.ui.theme.TextPrimary
import com.promy.irontrack.ui.theme.TextSecondary
import com.promy.irontrack.ui.viewmodel.WorkoutViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            IronTrackTheme(darkTheme = true) {
                IronTrackApp()
            }
        }
    }
}

enum class AuthScreen {
    LOGIN,
    SIGN_UP
}

enum class NavTab(val title: String, val icon: ImageVector, val tag: String) {
    TODAY("Today", Icons.Default.FitnessCenter, "nav_today"),
    EXERCISES("Exercises", Icons.Default.EmojiEvents, "nav_exercises"),
    HISTORY("History", Icons.Default.TrendingUp, "nav_history"),
    PROFILE("Profile", Icons.Default.Person, "nav_profile")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IronTrackApp(
    viewModel: WorkoutViewModel = viewModel()
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val isEmailVerified by viewModel.authManager.isEmailVerified.collectAsState()
    var authScreen by rememberSaveable { mutableStateOf(AuthScreen.LOGIN) }
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val isWorkoutActive by viewModel.isWorkoutActive.collectAsState()

    // 1. Unauthenticated flow: Only Login and Sign-up screens without bottom navigation
    if (currentUser == null) {
        when (authScreen) {
            AuthScreen.LOGIN -> {
                LoginScreen(
                    onLoginSuccess = {
                        selectedTab = 0
                    },
                    onNavigateToSignUp = {
                        authScreen = AuthScreen.SIGN_UP
                    }
                )
            }
            AuthScreen.SIGN_UP -> {
                SignUpScreen(
                    onSignUpSuccess = {
                        selectedTab = 0
                    },
                    onNavigateToLogin = {
                        authScreen = AuthScreen.LOGIN
                    }
                )
            }
        }
        return
    }

    // 2. Unverified email flow: Gate main app until email is verified
    if (!isEmailVerified) {
        VerifyEmailScreen(
            userEmail = currentUser?.email ?: "",
            onVerified = {
                selectedTab = 0
            },
            onSignOut = {
                viewModel.signOut()
                authScreen = AuthScreen.LOGIN
            }
        )
        return
    }

    // 2. Authenticated flow: Bottom navigation and main tabs
    BackHandler(enabled = selectedTab != 0) {
        selectedTab = 0
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            if (selectedTab != 0) {
                CenterAlignedTopAppBar(
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = BgDark,
                        titleContentColor = TextPrimary
                    ),
                    title = {
                        Text(
                            text = "IronTrack",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 17.sp,
                            color = TextPrimary
                        )
                    },
                    actions = {
                        if (isWorkoutActive) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = AccentMuted.copy(alpha = 0.15f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                                modifier = Modifier.padding(end = 12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .background(AccentGreen, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Active session",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = AccentMuted
                                    )
                                }
                            }
                        }
                    }
                )
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = BgDark,
                tonalElevation = 0.dp,
                windowInsets = WindowInsets.navigationBars
            ) {
                NavTab.entries.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.title,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        label = {
                            Text(
                                text = tab.title,
                                fontSize = 11.sp,
                                fontWeight = if (selectedTab == index) FontWeight.SemiBold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = AccentOnVolt,
                            selectedTextColor = TextPrimary,
                            indicatorColor = AccentMuted,
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted
                        ),
                        modifier = Modifier.testTag(tab.tag)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(BgDark)
        ) {
            when (selectedTab) {
                0 -> TodayWorkoutScreen(
                    viewModel = viewModel,
                    onNavigateToHistory = { selectedTab = 2 }
                )
                1 -> ExercisesScreen(viewModel = viewModel)
                2 -> GrowthHistoryScreen(viewModel = viewModel)
                3 -> ProfileScreen(viewModel = viewModel)
            }
        }
    }
}
