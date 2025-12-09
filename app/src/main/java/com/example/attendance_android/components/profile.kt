package com.example.attendance_android.components

// ---------- Imports ----------
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.attendance_android.data.DataStoreManager
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch

// ---------- Profile Screen ----------
@Composable
fun ProfileScreen(
    navController: NavController,
) {
    val context = LocalContext.current
    val dataStore = remember { DataStoreManager(context) }
    val scope = rememberCoroutineScope()

    // collect DataStore values
    val isStudent by dataStore.isStudent.collectAsState(initial = false)
    val name by dataStore.name.collectAsState(initial = "") // note: DataStoreManager uses NAME key
    val email by dataStore.email.collectAsState(initial = "")
    val rollNo by dataStore.rollNumber.collectAsState(initial = "")
    val branch by dataStore.branch.collectAsState(initial = "")
    val section by dataStore.section.collectAsState(initial = "")
    val year by dataStore.year.collectAsState(initial = "")

    // Header will show first letter as avatar if name available
    val displayName = if (name.isNotBlank()) name else email.takeWhile { it != '@' }.ifEmpty { "User" }
    val avatarLetter = displayName.trim().take(1).uppercase()

    Scaffold(
        topBar = {
            // show header with profile initial and college name (change college string if you store it)
            HeaderWithProfile(fullname = displayName, collegeName = "GVPCE", navController = navController)
        },
        bottomBar = {
            FooterNavPrimary(
                onHome = { navController.navigateUp() }, // example behavior
                onClasses = { /* navigate to classes if you want */ },
                onSettings = { /* navigate to settings */ },
                selected = "HOME"
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(6.dp))

            // Avatar circle with initial
            Box(
                modifier = Modifier
                    .size(84.dp)
                    .background(MaterialTheme.colorScheme.primary, shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = avatarLetter,
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(12.dp))

            // Name + role
            Text(
                text = displayName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = if (isStudent) "Student" else "Teacher",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(20.dp))

            // Card for details
            Card(
                modifier = Modifier
                    .fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)) {

                    // Common fields
                    InfoRow(label = "Email", value = if (email.isNotBlank()) email else "—")

                    Spacer(Modifier.height(8.dp))

                    if (isStudent) {
                        // Student-specific fields
                        InfoRow(label = "Roll Number", value = if (rollNo.isNotBlank()) rollNo else "—")
                        Spacer(Modifier.height(8.dp))
                        InfoRow(label = "Branch", value = if (branch.isNotBlank()) branch else "—")
                        Spacer(Modifier.height(8.dp))
                        InfoRow(label = "Section", value = if (section.isNotBlank()) section else "—")
                        Spacer(Modifier.height(8.dp))
                        InfoRow(label = "Year", value = if (year.isNotBlank()) year else "—")
                    } else {
                        // Teacher view: show email + optional other fields (college, etc.)
                        // If you store college in DataStore expose it and show here.
                        InfoRow(label = "Info", value = "Tap Edit to add more details")
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            // Logout button - clears all DataStore preferences and navigates to onboarding
            Button(
                onClick = {
                    scope.launch {
                        dataStore.clearAllPreferences()
                        // Navigate to onboarding route after clearing
                        navController.navigate("onboarding") {
                            popUpTo(0) // Clear back stack
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Logout")
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(text = "$label:", modifier = Modifier.width(110.dp), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}


@Preview
@Composable
fun ProfileScreenPreview() {
    ProfileScreen(navController = rememberNavController())
}

