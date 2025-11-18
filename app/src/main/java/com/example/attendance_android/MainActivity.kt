package com.example.attendance_android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.example.attendance_android.ui.theme.Attendance_AndroidTheme
import com.example.attendance_android.components.OnboardingScreen
import com.example.attendance_android.data.DataStoreManager
import androidx.compose.runtime.remember
import kotlinx.coroutines.launch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Attendance_AndroidTheme {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Create DataStoreManager once
                    val context = this
                    val dataStore = remember { DataStoreManager(context) }

                    // collect onboarding flag from DataStore
                    val isOnboardingDone by dataStore.isOnboardingComplete.collectAsState(initial = false)

                    // coroutine scope for calling suspend functions from UI callbacks
                    val scope = rememberCoroutineScope()

                    if (!isOnboardingDone) {
                        // Show onboarding; when completed, set flag in DataStore
                        OnboardingScreen(
                            onOnboardingComplete = {
                                scope.launch {
                                    dataStore.setOnboardingComplete(true)
                                }
                            }
                        )
                    } else {
                        // Replace this with your real Home screen
                        HomeScreen()
                    }
                }
            }
        }
    }
}

@Composable
fun HomeScreen() {
    // Small placeholder; replace with real home UI
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Welcome — Home Screen")
        }
    }
}
