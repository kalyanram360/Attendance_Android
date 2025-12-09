package com.example.attendance_android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import com.example.attendance_android.ui.theme.Attendance_AndroidTheme
import com.example.attendance_android.data.DataStoreManager
import kotlinx.coroutines.launch
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Box
import com.example.attendance_android.data.EMBEDDING
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.example.attendance_android.ViewModels.TeacherClassViewModel
import com.example.attendance_android.components.AdvertisingScreen
import com.example.attendance_android.components.FaceEnrollmentScreen
import com.example.attendance_android.components.FaceVerifyScreen
import com.example.attendance_android.components.OnboardingScreen
import com.example.attendance_android.components.ProfileScreen
import com.example.attendance_android.components.StudentBleScreen
import com.example.attendance_android.components.StudentHomeScreen
import com.example.attendance_android.components.TeacherHomeScreen
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import com.example.attendance_android.components.TeacherBLE
import java.util.concurrent.ThreadLocalRandom.current
import kotlinx.coroutines.flow.first
import com.example.attendance_android.NavRoutes.Attendance_View
import com.example.attendance_android.components.AttendanceViewScreen
import android.Manifest
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.delay
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.size

class MainActivity : ComponentActivity() {
   override fun onCreate(savedInstanceState: Bundle?) {
       super.onCreate(savedInstanceState)
       enableEdgeToEdge()
       
       // Request permissions when app starts
       requestPermissions()
       
       setContent {
           Attendance_AndroidTheme {
               Surface(modifier = Modifier.fillMaxSize()) {
                   // Create DataStoreManager once
                   val dataStore = remember { DataStoreManager(this@MainActivity) }
                   val roll = dataStore.rollNumber

                   // collect onboarding flag and role as Flows -> State
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
                           // Display splash screen with logo while loading
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

                           // The splash decides where to go after a short delay or immediately,
                           // based on DataStore values. We need to call suspend functions (loadEmbedding),
                           // so use LaunchedEffect.
                           LaunchedEffect(key1 = role, key2 = isOnboardingDone) {
                               // small optional splash delay for UX
                               delay(1500)

                               when {
                                   !isOnboardingDone -> {
                                       navController.navigate(NavRoutes.Onboarding.route) {
                                           popUpTo(NavRoutes.Splash.route) { inclusive = true }
                                       }
                                   }
                                   role.trim().uppercase() == "TEACHER" -> {
                                       navController.navigate(NavRoutes.TeacherHome.route) {
                                           popUpTo(NavRoutes.Splash.route) { inclusive = true }
                                           launchSingleTop = true
                                           restoreState = true
                                       }
                                   }
                                   role.trim().uppercase() == "STUDENT" -> {
                                       // IMPORTANT: check for saved embedding (suspend call)
                                       val emb = try {
                                           dataStore.loadEmbedding()
                                       } catch (e: Exception) {
                                           null
                                       }

                                       if (emb == null) {
                                           // no embedding => go to face enrollment
                                           navController.navigate(NavRoutes.face_enroll.route) {
                                               popUpTo(NavRoutes.Splash.route) { inclusive = true }
                                               launchSingleTop = true
                                           }
                                       } else {
                                           // embedding exists => go to student home
                                           navController.navigate(NavRoutes.Home.route) {
                                               popUpTo(NavRoutes.Splash.route) { inclusive = true }
                                               launchSingleTop = true
                                               restoreState = true
                                           }
                                       }
                                   }
                                   else -> {
                                       // fallback
                                       navController.navigate(NavRoutes.Onboarding.route) {
                                           popUpTo(NavRoutes.Splash.route) { inclusive = true }
                                       }
                                   }
                               }
                           }
                       }

                       // Onboarding
                       composable(NavRoutes.Onboarding.route) {
                           OnboardingScreen(
                               navController = navController,
                               onOnboardingComplete = {
                                   // persist onboardingComplete in DataStore on a coroutine
                                   scope.launch { dataStore.setOnboardingComplete(true) }
                                   // After onboarding you can navigate to enrollment or home —
                                   // the Splash logic will handle routing on next start,
                                   // but we can navigate directly here as well:
                                   val currentRole = runBlocking { dataStore.userRole.firstOrNull() ?: "" } // optional
                                   if (currentRole.trim().uppercase() == "STUDENT") {
                                       navController.navigate(NavRoutes.face_enroll.route) {
                                           popUpTo(NavRoutes.Onboarding.route) { inclusive = true }
                                       }
                                   } else {
                                       navController.navigate(NavRoutes.TeacherHome.route) {
                                           popUpTo(NavRoutes.Onboarding.route) { inclusive = true }
                                       }
                                   }
                               }
                           )
                       }

                       // Face enrollment screen route (you will create this composable)
                       composable(NavRoutes.face_enroll.route) {
                           FaceEnrollmentScreen(onEnrolled = {
                               // after enrollment, mark onboarding complete & go to home
                               scope.launch {
                                   dataStore.setOnboardingComplete(true)
                                   navController.navigate(NavRoutes.Home.route) {
                                       popUpTo(0)
                                   }
                               }
                           })
                       }

                       // Student Home
                       composable(NavRoutes.Home.route) {
                           StudentHomeScreen(navController = navController)
                       }

                       // Teacher Home
                       composable(NavRoutes.TeacherHome.route) {
                           TeacherHomeScreen(navController = navController)
                       }

                       // Teacher BLE (existing)
                       composable(NavRoutes.TeacherBLE.route) { backStackEntry ->
                           val vm: TeacherClassViewModel = viewModel(backStackEntry)
                           TeacherBLE(
                               navController = navController,
                               viewModel = vm,
                               fullname = "Professor",
                               collegeName = "GVPCE",
                               onStartClass = { _, _, _ -> /* optional callback */ }
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
                       composable("student_ble/{token}/{studentRoll}") {backStackEntry ->
                           StudentBleScreen(
                               navController = navController,
                               tokenToMatch = backStackEntry.arguments?.getString("token") ?: "",
                               studentRollNo = backStackEntry.arguments?.getString("studentRoll") ?: ""
                           )
                       }
                       composable(NavRoutes.profile.route) {
                           ProfileScreen(navController)
                       }

                       composable(
                           route = "${NavRoutes.FaceVerify.route}/{token}",
                           arguments = listOf(navArgument("token") { type = NavType.StringType; defaultValue = "" })
                       ) { backStackEntry ->

                           val token = backStackEntry.arguments?.getString("token")

                           val rollFlow = dataStore.rollNumber   // or your ViewModel’s rollNumberFlow
                           val scope = rememberCoroutineScope()

                           FaceVerifyScreen(
                               navController = navController,
                               token = token,
                               onSuccessNavigateBack = {
                                   scope.launch {
                                       val rollValue = rollFlow.first()
                                       navController.navigate("student_ble/$token/$rollValue") {
                                           popUpTo(0)
                                       }
                                   }
                               }
                           )
                       }

                       composable(NavRoutes.Attendance_View.route){backStackEntry ->
                           AttendanceViewScreen(navController = navController)

                       }



                   } // NavHost
               } // Surface
           } // Theme
       } // setContent

   }

   private fun requestPermissions() {
       val permissions = mutableListOf(
           Manifest.permission.CAMERA,
           Manifest.permission.ACCESS_FINE_LOCATION,
           Manifest.permission.ACCESS_COARSE_LOCATION
       )
       
       // Add Bluetooth permissions for Android 12+
       if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
           permissions.add(Manifest.permission.BLUETOOTH_SCAN)
           permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
       }
       
       val permissionLauncher = registerForActivityResult(
           ActivityResultContracts.RequestMultiplePermissions()
       ) { permissions ->
           // Handle permission results
           val deniedPermissions = permissions.filter { !it.value }.map { it.key }
           if (deniedPermissions.isNotEmpty()) {
               // Log denied permissions
               android.util.Log.w("Permissions", "Denied: $deniedPermissions")
           }
       }
       
       permissionLauncher.launch(permissions.toTypedArray())
   }
}



// package com.example.attendance_android

// import android.os.Bundle
// import androidx.activity.ComponentActivity
// import androidx.activity.compose.setContent
// import androidx.activity.enableEdgeToEdge
// import androidx.compose.foundation.layout.Box
// import androidx.compose.foundation.layout.fillMaxSize
// import androidx.compose.foundation.layout.padding
// import androidx.compose.material3.Surface
// import androidx.compose.material3.Text
// import androidx.compose.runtime.*
// import androidx.compose.ui.Alignment
// import androidx.compose.ui.Modifier
// import androidx.compose.ui.unit.dp
// import androidx.lifecycle.viewmodel.compose.viewModel
// import androidx.navigation.compose.NavHost
// import androidx.navigation.compose.composable
// import androidx.navigation.compose.rememberNavController
// import com.example.attendance_android.components.OnboardingScreen
// import com.example.attendance_android.components.StudentHomeScreen
// import com.example.attendance_android.components.TeacherBLE
// import com.example.attendance_android.components.TeacherHomeScreen
// import com.example.attendance_android.data.DataStoreManager
// import com.example.attendance_android.ui.theme.Attendance_AndroidTheme
// import com.example.attendance_android.ViewModels.TeacherClassViewModel
// import kotlinx.coroutines.delay
// import kotlinx.coroutines.launch

// class MainActivity : ComponentActivity() {
//     override fun onCreate(savedInstanceState: Bundle?) {
//         super.onCreate(savedInstanceState)
//         enableEdgeToEdge()

//         setContent {
//             Attendance_AndroidTheme {
//                 Surface(modifier = Modifier.fillMaxSize()) {
//                     // Create DataStoreManager once
//                     val dataStore = remember { DataStoreManager(this) }

//                     // collect onboarding flag from DataStore and role
//                     val isOnboardingDone by dataStore.isOnboardingComplete.collectAsState(initial = false)
//                     val role by dataStore.userRole.collectAsState(initial = "")

//                     // nav controller created here so we can control initial navigation logic
//                     val navController = rememberNavController()

//                     // coroutine scope for saving onboarding flag
//                     val scope = rememberCoroutineScope()

//                     // ALWAYS start at Splash so NavHost can restore properly after rotation
//                     NavHost(
//                         navController = navController,
//                         startDestination = NavRoutes.Splash.route
//                     ) {
//                         composable(NavRoutes.Splash.route) {
//                             // The splash will decide where to go after a short delay,
//                             // based on the values we read from DataStore above.
//                             LaunchedEffect(role, isOnboardingDone) {
//                                 // small splash delay for UX
//                                 delay(1000)

//                                 // Decide destination:
//                                 // If onboarding not done -> Onboarding
//                                 // Else if role == STUDENT -> Home
//                                 // Else if role == TEACHER -> TeacherHome
//                                 // else -> Onboarding (fallback)
//                                 when {
//                                     !isOnboardingDone -> {
//                                         navController.navigate(NavRoutes.Onboarding.route) {
//                                             popUpTo(NavRoutes.Splash.route) { inclusive = true }
//                                         }
//                                     }
//                                     role.trim().uppercase() == "STUDENT" -> {
//                                         navController.navigate(NavRoutes.Home.route) {
//                                             popUpTo(NavRoutes.Splash.route) { inclusive = true }
//                                             launchSingleTop = true
//                                             restoreState = true
//                                         }
//                                     }
//                                     role.trim().uppercase() == "TEACHER" -> {
//                                         navController.navigate(NavRoutes.TeacherHome.route) {
//                                             popUpTo(NavRoutes.Splash.route) { inclusive = true }
//                                             launchSingleTop = true
//                                             restoreState = true
//                                         }
//                                     }
//                                     else -> {
//                                         // fallback: onboarding
//                                         navController.navigate(NavRoutes.Onboarding.route) {
//                                             popUpTo(NavRoutes.Splash.route) { inclusive = true }
//                                         }
//                                     }
//                                 }
//                             }

//                             // simple splash UI
//                             Box(
//                                 modifier = Modifier
//                                     .fillMaxSize()
//                                     .padding(16.dp),
//                                 contentAlignment = Alignment.Center
//                             ) {
//                                 Text(text = "Splash")
//                             }
//                         }

//                         composable(NavRoutes.Onboarding.route) {
//                             OnboardingScreen(
//                                 navController = navController,
//                                 onOnboardingComplete = {
//                                     // persist onboardingComplete in DataStore on a coroutine
//                                     scope.launch {
//                                         dataStore.setOnboardingComplete(true)
//                                     }
//                                     // after onboarding completes, we'll route user to the correct home
//                                     // The OnboardingScreen's own lambda earlier used to do navigation;
//                                     // but here we let the NavHost flow continue. Optionally navigate now:
//                                     // Decide where to go using role stored in DataStore:
//                                     val currentRole = role.trim().uppercase()
//                                     if (currentRole == "TEACHER") {
//                                         navController.navigate(NavRoutes.TeacherHome.route) {
//                                             popUpTo(NavRoutes.Onboarding.route) { inclusive = true }
//                                             launchSingleTop = true
//                                         }
//                                     } else {
//                                         navController.navigate(NavRoutes.Home.route) {
//                                             popUpTo(NavRoutes.Onboarding.route) { inclusive = true }
//                                             launchSingleTop = true
//                                         }
//                                     }
//                                 }
//                             )
//                         }

//                         composable(NavRoutes.Home.route) {
//                             StudentHomeScreen(navController = navController)
//                         }

//                         composable(NavRoutes.TeacherHome.route) {
//                             TeacherHomeScreen(navController = navController)
//                         }

//                         composable(NavRoutes.TeacherBLE.route) { backStackEntry ->
//                             // scope ViewModel to this nav backstack entry so it survives rotation
//                             val vm: TeacherClassViewModel = viewModel(backStackEntry)
//                             TeacherBLE(
//                                 navController = navController,
//                                 viewModel = vm,
//                                 fullname = "Professor",
//                                 collegeName = "GVPCE",
//                                 onStartClass = { _, _, _ -> /* optional callback */ }
//                             )
//                         }
//                     } // NavHost
//                 } // Surface
//             } // Theme
//         } // setContent
//     } // onCreate
// }
