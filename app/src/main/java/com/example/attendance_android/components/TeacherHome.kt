package com.example.attendance_android.components
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.runtime.rememberCoroutineScope
import com.example.attendance_android.data.ClassDatabase
import com.example.attendance_android.data.ClassEntity
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
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
            HeaderWithProfile(fullname = fullname, collegeName = collegeName, navController = navController) // assumes this composable exists
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
                                restoreState=true
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

            // Show saved/archived classes from Room DB below the start button
            val context = LocalContext.current
            val scope = rememberCoroutineScope()
            val saved = remember { mutableStateListOf<ClassEntity>() }

            LaunchedEffect(Unit) {
                val dao = ClassDatabase.getInstance(context).classDao()
                dao.getAll().collect { list ->
                    saved.clear()
                    saved.addAll(list)
                }
            }

            if (saved.isEmpty()) {
                // Placeholder when no saved classes
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No previous classes", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Archived classes will appear here.", style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    Text("Previous Classes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))
                    // Simple list
                    for (c in saved) {
                        Card(modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = c.subject ?: "Untitled", fontWeight = FontWeight.SemiBold)
                                    val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
                                    val dt = sdf.format(Date(c.createdAt))
                                    Text(text = dt, style = MaterialTheme.typography.bodySmall)
                                }
                                Text("Token: ${c.token}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}
