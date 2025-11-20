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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Attendance_AndroidTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    // Create DataStoreManager once
                    val context = this
                    val dataStore = remember { DataStoreManager(context) }

                    // collect onboarding flag from DataStore
                    val isOnboardingDone by dataStore.isOnboardingComplete.collectAsState(initial = false)
                    val isStudent by dataStore.isStudent.collectAsState(initial = false)
                    val role by dataStore.userRole.collectAsState(initial = "")


                    // coroutine scope for calling suspend functions from UI callbacks
                    val scope = rememberCoroutineScope()

                    // Decide start destination based on DataStore flag
                    val startDestination =
                        when (role.trim().uppercase()) {
                            "STUDENT" -> NavRoutes.Home.route
                            "TEACHER" -> NavRoutes.TeacherHome.route
                            else -> NavRoutes.Splash.route
                        }


                    // Provide Navigation graph and pass a callback to persist onboarding completion
                    Navigation(
                        startDestination = startDestination,
                        onOnboardingComplete = {
                            // persist onboardingComplete in DataStore on a coroutine
                            scope.launch {
                                dataStore.setOnboardingComplete(true)
                            }
                        }
                    )
                }
            }
        }
    }
}


