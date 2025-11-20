package com.example.attendance_android.components

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.compose.ui.text.style.TextOverflow
import com.example.attendance_android.ui.theme.Attendance_AndroidTheme
import com.example.attendance_android.ui.theme.StatusMissed
import com.example.attendance_android.ui.theme.StatusPresent
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// Simple data model for a class record
data class ClassItem(
    val id: Int,
    val subject: String,
    val teacher: String,
    val time: String,        // e.g., "10:00 - 10:50"
    val date: String,        // e.g., "Nov 20, 2025"
    val attended: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentScreenContent(
    navController: NavController? = null,                           // optional navController
    currentClass: ClassItem? = null,
    previousClasses: List<ClassItem> = emptyList(),
    onMarkAttendance: (ClassItem) -> Unit = {}                     // callback for button
) {
    // If nothing provided, show sample data
    val now = LocalDateTime.now()
    val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")
    val sampleCurrent = ClassItem(
        id = 1,
        subject = "Data Structures",
        teacher = "Prof. Rao",
        time = "10:00 - 10:50",
        date = now.format(formatter),
        attended = false
    )
    val current = currentClass ?: sampleCurrent
    val previous = previousClasses.ifEmpty {
        remember {
            List(8) { idx ->
                ClassItem(
                    id = idx + 2,
                    subject = listOf("OS", "DBMS", "ML", "Networks", "Compiler", "SE")[idx % 6],
                    teacher = listOf("Dr. Sharma", "Ms. Gupta", "Prof. Rao", "Mr. Das")[idx % 4],
                    time = "09:00 - 09:50",
                    date = now.minusDays((idx + 1).toLong()).format(formatter),
                    attended = idx % 3 != 0 // some attended / missed
                )
            }
        }
    }


        Column(
            modifier = Modifier
                .fillMaxSize()

                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Current class card
            Text(
                text = "Current Class",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Row(
                    Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(current.subject, style = MaterialTheme.typography.headlineSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.height(4.dp))
                        Text("Teacher: ${current.teacher}", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(6.dp))
                        Text("${current.date} • ${current.time}", style = MaterialTheme.typography.bodySmall)
                    }

                    // Mark attendance button
                    Column(horizontalAlignment = Alignment.End) {
                        Button(
                            onClick = { onMarkAttendance(current) },
                            modifier = Modifier
                                .width(140.dp)
                                .height(44.dp)
                        ) {
                            Text(if (current.attended) "Marked" else "Mark Attendance")
                        }
                        Spacer(Modifier.height(8.dp))
                        if (current.attended) {
                            Text("Status: Present", style = MaterialTheme.typography.bodySmall, color = StatusPresent)
                        } else {
                            Text("Status: Not marked", style = MaterialTheme.typography.bodySmall, color = StatusMissed)
                        }
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            // Previous classes header
            Text(
                text = "Previous Classes",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))

            // List of previous classes
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(previous) { cls ->
                    PreviousClassRow(cls = cls, onClick = {
                        // default behavior: navigate to details if navController supplied
                        if (navController != null) {
                            // Assuming you have a details route, otherwise replace as needed
                            // navController.navigate("class_details/${cls.id}")
                        }
                    })
                }
            }
        }
    }


@Composable
fun StudentHomeScreen(
    navController: NavController? = null,
    currentClass: ClassItem? = null,
    previousClasses: List<ClassItem> = emptyList(),
    onMarkAttendance: (ClassItem) -> Unit = {}
) {
    Scaffold(
        topBar = {
            // Use your header composable (it will be placed below status bar by Scaffold)
            HeaderWithProfile(fullname = "Kalyan", collegeName = "GVPCE", onProfileClick = {
                // optional profile click
            })
        },
        bottomBar = {
            FooterNavPrimary(
                onHome = { /* navController?.navigate(NavRoutes.Home.route) */ },
                onClasses = { /* navController?.navigate(NavRoutes.Classes.route) */ },
                onSettings = { /* navController?.navigate(NavRoutes.Settings.route) */ },
                selected = "HOME"
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        // IMPORTANT: apply innerPadding so content is not hidden behind bars
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
        ) {
            StudentScreenContent(
                navController = navController,
                currentClass = currentClass,
                previousClasses = previousClasses,
                onMarkAttendance = onMarkAttendance
            )
        }
    }
}


@Composable
private fun PreviousClassRow(cls: ClassItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Row(modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(cls.subject, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(2.dp))
                Text("${cls.date} • ${cls.time}", style = MaterialTheme.typography.bodySmall)
            }

            // small pill showing status
            Surface(
                shape = MaterialTheme.shapes.extraSmall,
                tonalElevation = 0.dp,
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Box(modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (cls.attended) "Present" else "Missed",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (cls.attended) StatusPresent else StatusMissed
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
fun StudentHomeScreenPreview() {
    Attendance_AndroidTheme {
        // For preview only, pass null navController and use defaults
        StudentHomeScreen(navController = null, onMarkAttendance = { /* preview */ })
    }
}
