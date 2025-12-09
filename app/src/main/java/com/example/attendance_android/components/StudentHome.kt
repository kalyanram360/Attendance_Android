package com.example.attendance_android.components

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.activity.compose.BackHandler
import com.example.attendance_android.NavRoutes
import com.example.attendance_android.data.DataStoreManager
import com.example.attendance_android.data.PresentDatabase
import com.example.attendance_android.data.PresentEntity
import com.example.attendance_android.ui.theme.Attendance_AndroidTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

data class ClassItem(
    val id: Int,
    val subject: String,
    val teacher: String,
    val time: String,
    val date: String,
    val attended: Boolean
)

private const val TAG = "StudentHome"

//name from datastore 


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentScreenContent(
    navController: NavController? = null,
    currentClass: ClassItem? = null,
    previousClasses: List<ClassItem> = emptyList(),
    onMarkAttendance: (ClassItem) -> Unit = {}
) {
    val context = LocalContext.current
    val dataStore = remember { DataStoreManager(context) }

    val branch by dataStore.branch.collectAsState(initial = "")
    val section by dataStore.section.collectAsState(initial = "")
    val yearStr by dataStore.year.collectAsState(initial = "")
    val studentRoll by dataStore.rollNumber.collectAsState(initial = "")

    val backendurl = "https://attendance-app-backend-zr4c.onrender.com"

    var fetchedCurrentClass by remember { mutableStateOf<ClassItem?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var lastError by remember { mutableStateOf<String?>(null) }

    fun safeYear(s: String): Int? = try { s.toInt() } catch (e: Exception) { null }

    LaunchedEffect(branch, section, yearStr) {
        lastError = null
        fetchedCurrentClass = null

        if (branch.isBlank() || section.isBlank() || yearStr.isBlank()) {
            return@LaunchedEffect
        }

        val year = safeYear(yearStr)
        if (year == null) {
            lastError = "Invalid year: $yearStr"
            return@LaunchedEffect
        }

        isLoading = true
        try {
            val result = fetchCurrentClassForStudent(backendurl, branch, section, year)
            fetchedCurrentClass = result
        } catch (e: Exception) {
            lastError = e.message ?: "unknown error"
        } finally {
            isLoading = false
        }
    }

    val now = LocalDateTime.now()
    val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")
    val current = fetchedCurrentClass ?: currentClass

    val attendedFromDb = remember { mutableStateListOf<PresentEntity>() }
    LaunchedEffect(Unit) {
        val dao = PresentDatabase.getInstance(context).presentDao()
        dao.getAll().collect { list ->
            attendedFromDb.clear()
            attendedFromDb.addAll(list)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Header Section
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Current Session",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Track your attendance in real-time",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Loading/Error States
        if (isLoading) {
            item {
                LoadingCard()
            }
        } else if (lastError != null) {
            item {
                ErrorCard(message = lastError ?: "Unknown error")
            }
        } else if (current == null) {
            item {
                NoClassCard(branch = branch, section = section, year = yearStr)
            }
        } else {
            // Current Class Card
            item {
                CurrentClassCard(
                    classItem = current,
                    isFetched = fetchedCurrentClass != null,
                    onMarkAttendance = {
//                        val roll = studentRoll.ifBlank { "323103382034" }
                        navController?.navigate("${NavRoutes.FaceVerify.route}/${current.time}")
                    }
                )
            }
        }

        // Stats Overview
        item {
            StatsCard(totalAttended = attendedFromDb.size)
        }

        // Previous Classes Section
        item {
            Text(
                text = "Attendance History",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (attendedFromDb.isEmpty()) {
            item {
                EmptyHistoryCard()
            }
        } else {
            items(attendedFromDb) { present ->
                AttendanceHistoryCard(present = present)
            }
        }

        // Bottom spacer
        item {
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
fun CurrentClassCard(
    classItem: ClassItem,
    isFetched: Boolean,
    onMarkAttendance: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                            MaterialTheme.colorScheme.primary
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Subject and Teacher
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = classItem.subject,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = classItem.teacher,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                        )
                    }
                }

                Divider(
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.3f),
                    thickness = 1.dp
                )

                // Time and Token Info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Schedule,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = classItem.date,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                            )
                        }

                        if (isFetched) {
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "Token: ${classItem.time}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    // Status Badge
                    Surface(
                        shape = CircleShape,
                        color = if (classItem.attended)
                            Color(0xFF4CAF50)
                        else
                            MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (classItem.attended) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Text(
                                text = if (classItem.attended) "Present" else "Not Marked",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (classItem.attended) Color.White else MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }

                // Mark Attendance Button
                if (!classItem.attended) {
                    Button(
                        onClick = onMarkAttendance,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = MaterialTheme.shapes.large,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.onPrimary,
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Mark Attendance",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LoadingCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(40.dp),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Checking for ongoing class...",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ErrorCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

@Composable
fun NoClassCard(branch: String, section: String, year: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.EventBusy,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(56.dp)
                )
                Text(
                    text = "No Ongoing Class",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (branch.isNotBlank() && section.isNotBlank()) {
                    Text(
                        text = "$branch • Section $section • Year $year",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun StatsCard(totalAttended: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CalendarMonth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column {
                    Text(
                        text = "Classes Attended",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "$totalAttended",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyHistoryCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.History,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    text = "No attendance history",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Your attended classes will appear here",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun AttendanceHistoryCard(present: PresentEntity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        MaterialTheme.colorScheme.primaryContainer,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Class,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            // Content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = present.subject,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = present.teacher ?: "Unknown",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                val sdf = SimpleDateFormat("dd MMM yyyy • HH:mm", Locale.getDefault())
                val dt = sdf.format(Date(present.createdAt))
                Text(
                    text = dt,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            // Status Badge
            Surface(
                shape = CircleShape,
                color = Color(0xFF4CAF50).copy(alpha = 0.15f)
            ) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier
                        .padding(8.dp)
                        .size(20.dp)
                )
            }
        }
    }
}

// Keep your existing fetch function
suspend fun fetchCurrentClassForStudent(
    backendBaseUrl: String,
    branch: String,
    section: String,
    year: Int
): ClassItem? = withContext(Dispatchers.IO) {
    try {
        val finalUrl = "$backendBaseUrl/api/class/current?branch=${URLEncoder.encode(branch, "utf-8")}&section=${URLEncoder.encode(section, "utf-8")}&year=$year"
        Log.d(TAG, "GET $finalUrl")
        val url = URL(finalUrl)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10_000
            readTimeout = 10_000
        }

        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val text = stream.bufferedReader().use { it.readText() }
        conn.disconnect()

        Log.d(TAG, "Response code=$code, body=$text")

        if (code in 200..299 && text.isNotEmpty()) {
            val root = JSONObject(text)
            val exists = root.optBoolean("exists", false)
            if (!exists) {
                Log.d(TAG, "Server: exists=false -> no ongoing class")
                return@withContext null
            }

            val data = root.optJSONObject("data") ?: run {
                Log.w(TAG, "Server returned exists=true but data=null")
                return@withContext null
            }

            val teacherName = data.optJSONObject("teacher")?.optString("name", "Unknown Teacher") ?: "Unknown Teacher"
            val token = data.optString("token", "")
            val createdAtIso = data.optString("createdAt", null)

            val dateTimeString = if (!createdAtIso.isNullOrBlank()) {
                try {
                    val instant = Instant.parse(createdAtIso)
                    val ldt = LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
                    val fmt = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")
                    ldt.format(fmt)
                } catch (e: Exception) {
                    Log.w(TAG, "createdAt parse failed: ${e.message}")
                    createdAtIso
                }
            } else {
                val fmt = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")
                LocalDateTime.now().format(fmt)
            }

            var matchedSectionFound = false
            val branches = data.optJSONArray("branches")
            if (branches != null) {
                for (i in 0 until branches.length()) {
                    val b = branches.optJSONObject(i) ?: continue
                    if (b.optString("branchName", "").equals(branch, ignoreCase = true)) {
                        val secs = b.optJSONArray("sections")
                        if (secs != null) {
                            for (j in 0 until secs.length()) {
                                val s = secs.optJSONObject(j) ?: continue
                                val sName = s.optString("sectionName", "")
                                val sYear = s.optInt("year", -1)
                                if (sName.equals(section, ignoreCase = true) && sYear == year) {
                                    matchedSectionFound = true
                                    break
                                }
                            }
                        }
                    }
                    if (matchedSectionFound) break
                }
            }

            if (!matchedSectionFound) {
                Log.w(TAG, "Returned data did not contain matched section/year")
                return@withContext null
            }

            val subjectDisplay = data.optString("subject")

            return@withContext ClassItem(
                id = (data.optString("_id", token)).hashCode(),
                subject = subjectDisplay,
                teacher = teacherName,
                time = token,
                date = dateTimeString,
                attended = false
            )
        } else {
            Log.w(TAG, "Server returned non-2xx or empty body: code=$code")
            return@withContext null
        }
    } catch (e: Exception) {
        Log.e(TAG, "fetchCurrentClassForStudent failed: ${e.message}", e)
        return@withContext null
    }
}



@Composable
fun StudentHomeScreen(
    navController: NavController? = null,
    currentClass: ClassItem? = null,
    previousClasses: List<ClassItem> = emptyList(),
    onMarkAttendance: (ClassItem) -> Unit = {}
) {
    val context = LocalContext.current
    val dataStore = remember { DataStoreManager(context) }
    val name by dataStore.name.collectAsState(initial = "")
    
    // Prevent back navigation from Home screen
    BackHandler(enabled = true) {
        // Do nothing - prevent going back to Onboarding
    }
    
    Scaffold(
        topBar = {
            HeaderWithProfile(fullname = name, collegeName = "GVPCE", navController = navController)
        },
        bottomBar = {
            FooterNavPrimary(
                onHome = { },
                onClasses = { },
                onSettings = { },
                selected = "HOME"
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { innerPadding ->
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

@Preview(showBackground = true)
@Composable
fun StudentHomeScreenPreview() {
    Attendance_AndroidTheme {
        StudentScreenContent(navController = null, onMarkAttendance = {})
    }
}