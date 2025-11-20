package com.example.attendance_android.components
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.attendance_android.components.FooterNavPrimary
import com.example.attendance_android.components.HeaderWithProfile
import com.example.attendance_android.NavRoutes

@Composable
fun TeacherHomeScreen(
    navController: NavController,
    fullname: String = "Professor",
    collegeName: String = "GVPCE",
    onStartClassRoute: String = "bleAdvertise" // route to navigate to
) {
    Scaffold(
        topBar = {
            HeaderWithProfile(fullname = fullname, collegeName = collegeName) // assumes this composable exists
        },
        bottomBar = {
            FooterNavPrimary(
                onHome = { navController.navigate(NavRoutes.TeacherHome.route) { launchSingleTop = true } },
                onClasses = { /* optional nav */ },
                onSettings = { /* optional nav */ },
                selected = "CLASSES"
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        // content area — apply innerPadding so header/footer + system bars are respected
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Top box: "New Class" title and large card button
            Text(
                text = "New Class",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                shape = MaterialTheme.shapes.medium,
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Centered large start button
                    Button(
                        onClick = {
                            // navigate to bleAdvertise route
                            navController.navigate(NavRoutes.TeacherBLE.route) {
                                launchSingleTop = true
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .align(Alignment.Center),
                        shape = MaterialTheme.shapes.extraLarge
                    ) {
                        Text(text = "Start a Class", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Filler content area (dashed X in your sketch) — teacher can show upcoming/active classes here
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                // Placeholder content — replace with real list later
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("No active classes", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Start a new class to begin broadcasting BLE advertisements.", style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                }
            }
        }
    }
}
