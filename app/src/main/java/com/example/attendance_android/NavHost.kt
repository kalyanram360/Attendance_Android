package com.example.attendance_android

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.Image
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.compose.ui.layout.ContentScale
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.attendance_android.components.OnboardingScreen
import kotlinx.coroutines.delay
import com.example.attendance_android.components.StudentHomeScreen
import com.example.attendance_android.components.TeacherHomeScreen
import com.example.attendance_android.components.TeacherBLE
import com.example.attendance_android.ViewModels.TeacherClassViewModel
import com.example.attendance_android.components.AdvertisingScreen
import com.example.attendance_android.components.FaceEnrollmentScreen
import com.example.attendance_android.components.FaceVerifyScreen
import com.example.attendance_android.components.StudentBleScreen
import com.example.attendance_android.components.ProfileScreen

// Defines the routes for navigation


/**
 * Navigation Graph
 *
 * @param startDestination starting route (so MainActivity can decide Onboarding vs Home)
 * @param onOnboardingComplete callback that will be invoked when onboarding finishes (MainActivity
 *                              should update DataStore in a coroutine).
 */
@Composable
fun Navigation(
    startDestination: String = NavRoutes.Splash.route,
    onOnboardingComplete: () -> Unit = {}
) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = startDestination) {
        composable(NavRoutes.Splash.route) {
            SplashScreen(navController)
        }

        composable(NavRoutes.Onboarding.route) {
            // Your OnboardingScreen component — pass navController and a lambda that both
            // navigates (inside Navigation file) and notifies the caller to persist the flag.
            OnboardingScreen(
                navController = navController,
                onOnboardingComplete = {
                    // Navigate to Login and remove Onboarding from back stack
                    navController.navigate(NavRoutes.Home.route) {
                        popUpTo(NavRoutes.Onboarding.route) {
                            inclusive = true
                        }
                    }
                    // notify the host (MainActivity) to persist onboarding completion
                    onOnboardingComplete()
                }
            )

        }
        composable(NavRoutes.Home.route) {
            // Your HomeScreen component
            StudentHomeScreen(navController)
        }
        composable(NavRoutes.TeacherHome.route) {
            // Your TeacherHomeScreen component
            TeacherHomeScreen(navController=navController)
        }

        composable(NavRoutes.TeacherBLE.route) {
            // Scope ViewModel to this navigation entry
            val viewModel: TeacherClassViewModel = viewModel(it)
            TeacherBLE(
                navController = navController,
                viewModel = viewModel
            )
        }

        composable("advertising/{sectionsJson}/{subject}/{teacherEmail}") { backStackEntry ->
            val sectionsJson = backStackEntry.arguments?.getString("sectionsJson") ?: "[]"
            val subject = backStackEntry.arguments?.getString("subject") ?: ""
            val teacherEmail = backStackEntry.arguments?.getString("teacherEmail") ?: ""

            AdvertisingScreen(
                navController = navController,
                sectionsJson = sectionsJson,
                subject = subject,
                teacherEmail = teacherEmail
            )
        }

        composable("student_ble/{token}/{studentRoll}") {
            StudentBleScreen(
                navController = navController,
                tokenToMatch = it.arguments?.getString("token") ?: "",
                studentRollNo = it.arguments?.getString("studentRoll") ?: ""
            )
        }

        composable(NavRoutes.profile.route) {
            ProfileScreen(navController)
        }

        composable(NavRoutes.face_enroll.route){
            FaceEnrollmentScreen(
                onEnrolled = {navController.navigate("home")} )
        }
        composable(
            route = "${NavRoutes.FaceVerify.route}/{token}",
            arguments = listOf(navArgument("token") { type = NavType.StringType })
        ) { backStackEntry ->
            val token = backStackEntry.arguments?.getString("token") ?: ""
            FaceVerifyScreen(
                navController = navController,
                token = token
                // Add other necessary parameters for FaceVerifyScreen
            )
        }






    }
}

@Composable
fun SplashScreen(navController: NavController) {
    // This is a placeholder for a splash screen.
    LaunchedEffect(Unit) {
        delay(1500) // Simulate a delay for loading
        navController.navigate(NavRoutes.Onboarding.route) {
            // Remove splash screen from back stack
            popUpTo(NavRoutes.Splash.route) {
                inclusive = true
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color = Color(0xFF002444)),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.untitled_design),
            contentDescription = "App Logo",
            modifier = Modifier.size(120.dp),
            contentScale = ContentScale.Fit
        )
    }
}
