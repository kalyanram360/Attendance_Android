//package com.example.attendance_android
//
//import android.os.Bundle
//import androidx.activity.ComponentActivity
//import androidx.activity.compose.setContent
//import androidx.activity.enableEdgeToEdge
//import androidx.compose.material3.Surface
//import androidx.compose.runtime.collectAsState
//import androidx.compose.runtime.getValue
//import androidx.compose.runtime.rememberCoroutineScope
//import androidx.compose.runtime.remember
//import com.example.attendance_android.ui.theme.Attendance_AndroidTheme
//import com.example.attendance_android.data.DataStoreManager
//import kotlinx.coroutines.launch
//import androidx.compose.runtime.Composable
//import androidx.compose.foundation.layout.Column
//import androidx.compose.foundation.layout.fillMaxSize
//import androidx.compose.foundation.layout.padding
//import androidx.compose.material3.Text
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.unit.dp
//import androidx.compose.foundation.layout.Box
//
//class MainActivity : ComponentActivity() {
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        enableEdgeToEdge()
//        setContent {
//            Attendance_AndroidTheme {
//                Surface(modifier = Modifier.fillMaxSize()) {
//                    // Create DataStoreManager once
//                    val context = this
//                    val dataStore = remember { DataStoreManager(context) }
//
//                    // collect onboarding flag from DataStore
//                    val isOnboardingDone by dataStore.isOnboardingComplete.collectAsState(initial = false)
//                    val isStudent by dataStore.isStudent.collectAsState(initial = false)
//                    val role by dataStore.userRole.collectAsState(initial = "")
//
//
//                    // coroutine scope for calling suspend functions from UI callbacks
//                    val scope = rememberCoroutineScope()
//
//                    // Decide start destination based on DataStore flag
//                    val startDestination =
//                        when (role.trim().uppercase()) {
//                            "STUDENT" -> NavRoutes.Home.route
//                            "TEACHER" -> NavRoutes.TeacherHome.route
//                            else -> NavRoutes.Splash.route
//                        }
//
//
//                    // Provide Navigation graph and pass a callback to persist onboarding completion
//                    Navigation(
//                        startDestination = startDestination,
//                        onOnboardingComplete = {
//                            // persist onboardingComplete in DataStore on a coroutine
//                            scope.launch {
//                                dataStore.setOnboardingComplete(true)
//                            }
//                        }
//                    )
//                }
//            }
//        }
//    }
//}
//
//

package com.example.attendance_android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.attendance_android.components.OnboardingScreen
import com.example.attendance_android.components.StudentHomeScreen
import com.example.attendance_android.components.TeacherBLE
import com.example.attendance_android.components.TeacherHomeScreen
import com.example.attendance_android.data.DataStoreManager
import com.example.attendance_android.ui.theme.Attendance_AndroidTheme
import com.example.attendance_android.ViewModels.TeacherClassViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            Attendance_AndroidTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    // Create DataStoreManager once
                    val dataStore = remember { DataStoreManager(this) }

                    // collect onboarding flag from DataStore and role
                    val isOnboardingDone by dataStore.isOnboardingComplete.collectAsState(initial = false)
                    val role by dataStore.userRole.collectAsState(initial = "")

                    // nav controller created here so we can control initial navigation logic
                    val navController = rememberNavController()

                    // coroutine scope for saving onboarding flag
                    val scope = rememberCoroutineScope()

                    // ALWAYS start at Splash so NavHost can restore properly after rotation
                    NavHost(
                        navController = navController,
                        startDestination = NavRoutes.Splash.route
                    ) {
                        composable(NavRoutes.Splash.route) {
                            // The splash will decide where to go after a short delay,
                            // based on the values we read from DataStore above.
                            LaunchedEffect(role, isOnboardingDone) {
                                // small splash delay for UX
                                delay(1000)

                                // Decide destination:
                                // If onboarding not done -> Onboarding
                                // Else if role == STUDENT -> Home
                                // Else if role == TEACHER -> TeacherHome
                                // else -> Onboarding (fallback)
                                when {
                                    !isOnboardingDone -> {
                                        navController.navigate(NavRoutes.Onboarding.route) {
                                            popUpTo(NavRoutes.Splash.route) { inclusive = true }
                                        }
                                    }
                                    role.trim().uppercase() == "STUDENT" -> {
                                        navController.navigate(NavRoutes.Home.route) {
                                            popUpTo(NavRoutes.Splash.route) { inclusive = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                    role.trim().uppercase() == "TEACHER" -> {
                                        navController.navigate(NavRoutes.TeacherHome.route) {
                                            popUpTo(NavRoutes.Splash.route) { inclusive = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                    else -> {
                                        // fallback: onboarding
                                        navController.navigate(NavRoutes.Onboarding.route) {
                                            popUpTo(NavRoutes.Splash.route) { inclusive = true }
                                        }
                                    }
                                }
                            }

                            // simple splash UI
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "Splash")
                            }
                        }

                        composable(NavRoutes.Onboarding.route) {
                            OnboardingScreen(
                                navController = navController,
                                onOnboardingComplete = {
                                    // persist onboardingComplete in DataStore on a coroutine
                                    scope.launch {
                                        dataStore.setOnboardingComplete(true)
                                    }
                                    // after onboarding completes, we'll route user to the correct home
                                    // The OnboardingScreen's own lambda earlier used to do navigation;
                                    // but here we let the NavHost flow continue. Optionally navigate now:
                                    // Decide where to go using role stored in DataStore:
                                    val currentRole = role.trim().uppercase()
                                    if (currentRole == "TEACHER") {
                                        navController.navigate(NavRoutes.TeacherHome.route) {
                                            popUpTo(NavRoutes.Onboarding.route) { inclusive = true }
                                            launchSingleTop = true
                                        }
                                    } else {
                                        navController.navigate(NavRoutes.Home.route) {
                                            popUpTo(NavRoutes.Onboarding.route) { inclusive = true }
                                            launchSingleTop = true
                                        }
                                    }
                                }
                            )
                        }

                        composable(NavRoutes.Home.route) {
                            StudentHomeScreen(navController = navController)
                        }

                        composable(NavRoutes.TeacherHome.route) {
                            TeacherHomeScreen(navController = navController)
                        }

                        composable(NavRoutes.TeacherBLE.route) { backStackEntry ->
                            // scope ViewModel to this nav backstack entry so it survives rotation
                            val vm: TeacherClassViewModel = viewModel(backStackEntry)
                            TeacherBLE(
                                navController = navController,
                                viewModel = vm,
                                fullname = "Professor",
                                collegeName = "GVPCE",
                                onStartClass = { _, _, _ -> /* optional callback */ }
                            )
                        }
                    } // NavHost
                } // Surface
            } // Theme
        } // setContent
    } // onCreate
}
