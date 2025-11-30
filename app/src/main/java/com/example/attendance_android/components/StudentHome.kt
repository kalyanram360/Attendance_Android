package com.example.attendance_android.components

// --- imports ---
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.attendance_android.data.DataStoreManager
import com.example.attendance_android.data.PresentDatabase
import com.example.attendance_android.data.PresentEntity
import com.example.attendance_android.ui.theme.Attendance_AndroidTheme
import com.example.attendance_android.ui.theme.StatusMissed
import com.example.attendance_android.ui.theme.StatusPresent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.collect
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// --- data model ---
data class ClassItem(
    val id: Int,
    val subject: String,
    val teacher: String,
    val time: String,        // e.g., token or "10:00 - 10:50"
    val date: String,        // e.g., "dd MMM yyyy, HH:mm"
    val attended: Boolean
)

private const val TAG = "StudentHome"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentScreenContent(
    navController: NavController? = null,                           // optional navController
    currentClass: ClassItem? = null,
    previousClasses: List<ClassItem> = emptyList(),
    onMarkAttendance: (ClassItem) -> Unit = {}                     // callback for button
) {
    val context = LocalContext.current
    val dataStore = remember { DataStoreManager(context) }

    // DataStore-backed values (strings)
    val branch by dataStore.branch.collectAsState(initial = "")
    val section by dataStore.section.collectAsState(initial = "")
    val yearStr by dataStore.year.collectAsState(initial = "")
    val studentRoll by dataStore.rollNumber.collectAsState(initial = "")

    // backend base URL (adjust if needed)
    val backendurl = "https://attendance-app-backend-zr4c.onrender.com"

    // UI state
    var fetchedCurrentClass by remember { mutableStateOf<ClassItem?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var lastError by remember { mutableStateOf<String?>(null) }

    // Debug: show DataStore values in logs
    LaunchedEffect(branch, section, yearStr) {
        Log.d(TAG, "DataStore values changed: branch='$branch' section='$section' year='$yearStr'")
    }

    // Helper: safe year parse
    fun safeYear(s: String): Int? = try { s.toInt() } catch (e: Exception) { null }

    // Fetch when inputs are ready
    LaunchedEffect(branch, section, yearStr) {
        Log.d(TAG, "LaunchedEffect triggered for fetching class: branch='$branch' section='$section' year='$yearStr'")

        // Reset states
        lastError = null
        fetchedCurrentClass = null

        if (branch.isBlank() || section.isBlank() || yearStr.isBlank()) {
            Log.d(TAG, "Skipping fetch - missing branch/section/year")
            return@LaunchedEffect
        }

        val year = safeYear(yearStr)
        if (year == null) {
            Log.w(TAG, "Year parse failed for '$yearStr'")
            lastError = "Invalid year: $yearStr"
            return@LaunchedEffect
        }

        isLoading = true
        try {
            val result = fetchCurrentClassForStudent(backendurl, branch, section, year)
            fetchedCurrentClass = result
            Log.d(TAG, "fetchCurrentClassForStudent returned: $result")
        } catch (e: Exception) {
            Log.e(TAG, "Exception while fetching current class: ${e.message}", e)
            lastError = e.message ?: "unknown error"
        } finally {
            isLoading = false
        }
    }

    // Prepare "current" for UI: prefer fetched -> provided -> dummy
    val now = LocalDateTime.now()
    val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")
    val current = fetchedCurrentClass ?: currentClass ?: ClassItem(
        id = 1,
        subject = "Data Structures",
        teacher = "Dr. Singh",
        time = "10:00 - 10:50",
        date = now.format(formatter),
        attended = false
    )

    val previous = previousClasses.ifEmpty {
        remember {
            List(8) { idx ->
                ClassItem(
                    id = idx + 2,
                    subject = listOf("OS", "DBMS", "ML", "Networks", "Compiler", "SE")[idx % 6],
                    teacher = listOf("Dr. Sharma", "Ms. Gupta", "Prof. Rao", "Mr. Das")[idx % 4],
                    time = "09:00 - 09:50",
                    date = now.minusDays((idx + 1).toLong()).format(formatter),
                    attended = idx % 3 != 0
                )
            }
        }
    }

    // --- UI ---
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = "Current Class",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(8.dp))

        // Debug row (temporary) to verify DataStore values on screen
        Text(
            text = "DBG: branch='$branch' section='$section' year='$yearStr'",
            style = MaterialTheme.typography.labelSmall
        )
        Spacer(Modifier.height(6.dp))

        // Loading / error / no-class notices
        when {
            isLoading -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Checking for ongoing class...")
                }
                Spacer(Modifier.height(8.dp))
            }
            lastError != null -> {
                Text("Error: $lastError", color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(8.dp))
            }
            fetchedCurrentClass == null && (branch.isNotBlank() && section.isNotBlank() && yearStr.isNotBlank()) -> {
                Text("No ongoing class found for $branch $section (year $yearStr)")
                Spacer(Modifier.height(8.dp))
            }
        }

        // Card - show current info (either fetched or default)
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
                    // If fetched class used token in time field, show token label; else show time
                    if (fetchedCurrentClass != null) {
                        Text("Started: ${current.date}", style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(2.dp))
                        Text("Token: ${current.time}", style = MaterialTheme.typography.bodySmall)
                    } else {
                        Text("${current.date} • ${current.time}", style = MaterialTheme.typography.bodySmall)
                    }
                }

                // Mark attendance button
                Column(horizontalAlignment = Alignment.End) {
                    Button(
                        onClick = {
                            val roll = studentRoll.ifBlank { "323103382034" }
                            navController?.navigate("student_ble/${current.time}/${roll}")
                        },
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

        // Load attended classes from PresentDatabase
        val attendedFromDb = remember { mutableStateListOf<PresentEntity>() }
        LaunchedEffect(Unit) {
            val dao = PresentDatabase.getInstance(context).presentDao()
            dao.getAll().collect { list ->
                attendedFromDb.clear()
                attendedFromDb.addAll(list)
            }
        }

        // Display Previous/Attended Classes
        if (attendedFromDb.isEmpty()) {
            Text(
                text = "Previous Classes",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "No attended classes yet",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Attend classes to see your history here.",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            Text(
                text = "Previous Classes",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))

            // List of attended classes from database
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(attendedFromDb) { present ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(72.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(present.subject, fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "Teacher: ${present.teacher ?: "Unknown"}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Spacer(Modifier.height(2.dp))
                                val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
                                val dt = sdf.format(Date(present.createdAt))
                                Text(
                                    dt,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text("Present", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Fetch current class for a branch/section/year.
 * Returns a ClassItem or null.
 * Adds detailed logging (Logcat).
 */
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

            // teacher name
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

            // Confirm section exists in returned object (defensive)
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
                Log.w(TAG, "Returned data did not contain matched section/year - treating as no class")
                return@withContext null
            }

            val subjectDisplay = data.optString("subject")

            return@withContext ClassItem(
                id = (data.optString("_id", token)).hashCode(),
                subject = subjectDisplay,
                teacher = teacherName,
                time = token, // token shown in "time" slot; change as needed
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
    Scaffold(
        topBar = {
            // Use your header composable (it will be placed below status bar by Scaffold)
            HeaderWithProfile(fullname = "Kalyan", collegeName = "GVPCE", navController = navController)
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
@Preview(showBackground = true)
@Composable
fun StudentHomeScreenPreview() {
    Attendance_AndroidTheme {
        StudentScreenContent(navController = null, onMarkAttendance = {})
    }
}