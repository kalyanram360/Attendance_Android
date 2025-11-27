////package com.example.attendance_android.components
////
////import android.content.res.Configuration
////import androidx.compose.foundation.clickable
////import androidx.compose.foundation.layout.*
////import androidx.compose.foundation.lazy.LazyColumn
////import androidx.compose.foundation.lazy.items
////import androidx.compose.material3.*
////import androidx.compose.material3.ExperimentalMaterial3Api
////import androidx.compose.material3.TopAppBar
////import androidx.compose.runtime.Composable
////import androidx.compose.runtime.remember
////import androidx.compose.ui.Alignment
////import androidx.compose.ui.Modifier
////import androidx.compose.ui.platform.LocalContext
////import androidx.compose.ui.text.font.FontWeight
////import androidx.compose.ui.tooling.preview.Preview
////import androidx.compose.ui.unit.dp
////import androidx.navigation.NavController
////import androidx.compose.ui.text.style.TextOverflow
////import com.example.attendance_android.ui.theme.Attendance_AndroidTheme
////import com.example.attendance_android.ui.theme.StatusMissed
////import com.example.attendance_android.ui.theme.StatusPresent
////import java.time.LocalDateTime
////import java.time.format.DateTimeFormatter
////import kotlinx.coroutines.withContext
////import kotlinx.coroutines.Dispatchers
////import java.net.URLEncoder
////import java.net.URL
////import java.net.HttpURLConnection
////import com.example.attendance_android.data.DataStoreManager
////import androidx.compose.runtime.collectAsState
////import androidx.compose.runtime.getValue
////import androidx.compose.runtime.LaunchedEffect
////import androidx.compose.runtime.mutableStateOf
////import androidx.compose.runtime.setValue
////import org.json.JSONObject
////
////import kotlinx.coroutines.withContext
////
////import java.time.Instant
////
////import java.time.ZoneId
////
////// Simple data model for a class record
////data class ClassItem(
////    val id: Int,
////    val subject: String,
////    val teacher: String,
////    val time: String,        // e.g., "10:00 - 10:50"
////    val date: String,        // e.g., "Nov 20, 2025"
////    val attended: Boolean
////)
////
////
////@OptIn(ExperimentalMaterial3Api::class)
////@Composable
////fun StudentScreenContent(
////    navController: NavController? = null,                           // optional navController
////    currentClass: ClassItem? = null,
////    previousClasses: List<ClassItem> = emptyList(),
////    onMarkAttendance: (ClassItem) -> Unit = {}                     // callback for button
////) {
////    // If nothing provided, show sample data
////    val now = LocalDateTime.now()
////    val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")
////
////    val context = LocalContext.current
////    val dataStore = remember { DataStoreManager(context) }
////    val branch by dataStore.branch.collectAsState(initial = "")
////    val section by dataStore.section.collectAsState(initial = "")
////    val year by dataStore.year.collectAsState(initial = "")
////    val backendurl = "https://attendance-app-backend-zr4c.onrender.com"
////
////    var fetchedCurrentClass by remember { mutableStateOf<ClassItem?>(null) }
////    var isLoading by remember { mutableStateOf(false) }
////
////    // Fetch current class when branch, section, year are available
////    LaunchedEffect(branch, section, year) {
////        if (branch.isNotEmpty() && section.isNotEmpty() && year.isNotEmpty()) {
////            isLoading = true
////            fetchedCurrentClass = fetchCurrentClassForStudent(backendurl, branch, section, year.toInt())
////            isLoading = false
////        }
////    }
////
////    // Use fetched class or provided currentClass or default
////    val current = fetchedCurrentClass ?: currentClass ?: remember {
////        val now = LocalDateTime.now()
////        val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")
////        ClassItem(
////            id = 1,
////            subject = "Data Structures",
////            teacher = "Dr. Singh",
////            time = "10:00 - 10:50",
////            date = now.format(formatter),
////            attended = false
////        )
////    }
////
////    val previous = previousClasses.ifEmpty {
////        remember {
////            List(8) { idx ->
////                ClassItem(
////                    id = idx + 2,
////                    subject = listOf("OS", "DBMS", "ML", "Networks", "Compiler", "SE")[idx % 6],
////                    teacher = listOf("Dr. Sharma", "Ms. Gupta", "Prof. Rao", "Mr. Das")[idx % 4],
////                    time = "09:00 - 09:50",
////                    date = now.minusDays((idx + 1).toLong()).format(formatter),
////                    attended = idx % 3 != 0 // some attended / missed
////                )
////            }
////        }
////    }
////
////
////        Column(
////            modifier = Modifier
////                .fillMaxSize()
////
////                .padding(horizontal = 16.dp, vertical = 12.dp)
////        ) {
////            // Current class card
////            Text(
////                text = "Current Class",
////                style = MaterialTheme.typography.titleMedium,
////                fontWeight = FontWeight.SemiBold
////            )
////            Spacer(Modifier.height(8.dp))
////            Card(
////                modifier = Modifier
////                    .fillMaxWidth()
////                    .height(140.dp),
////                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
////            ) {
////                Row(
////                    Modifier
////                        .fillMaxSize()
////                        .padding(16.dp),
////                    verticalAlignment = Alignment.CenterVertically
////                ) {
////                    Column(modifier = Modifier.weight(1f)) {
////                        Text(current.subject, style = MaterialTheme.typography.headlineSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
////                        Spacer(Modifier.height(4.dp))
////                        Text("Teacher: ${current.teacher}", style = MaterialTheme.typography.bodyMedium)
////                        Spacer(Modifier.height(6.dp))
////                        Text("${current.date} • ${current.time}", style = MaterialTheme.typography.bodySmall)
////                    }
////
////                    // Mark attendance button
////                    Column(horizontalAlignment = Alignment.End) {
////                        Button(
////                            onClick = { onMarkAttendance(current) },
////                            modifier = Modifier
////                                .width(140.dp)
////                                .height(44.dp)
////                        ) {
////                            Text(if (current.attended) "Marked" else "Mark Attendance")
////                        }
////                        Spacer(Modifier.height(8.dp))
////                        if (current.attended) {
////                            Text("Status: Present", style = MaterialTheme.typography.bodySmall, color = StatusPresent)
////                        } else {
////                            Text("Status: Not marked", style = MaterialTheme.typography.bodySmall, color = StatusMissed)
////                        }
////                    }
////                }
////            }
////
////            Spacer(Modifier.height(18.dp))
////
////            // Previous classes header
////            Text(
////                text = "Previous Classes",
////                style = MaterialTheme.typography.titleMedium,
////                fontWeight = FontWeight.SemiBold
////            )
////            Spacer(Modifier.height(8.dp))
////
////            // List of previous classes
////            LazyColumn(
////                modifier = Modifier.fillMaxSize(),
////                verticalArrangement = Arrangement.spacedBy(8.dp),
////            ) {
////                items(previous) { cls ->
////                    PreviousClassRow(cls = cls, onClick = {
////                        // default behavior: navigate to details if navController supplied
////                        if (navController != null) {
////                            // Assuming you have a details route, otherwise replace as needed
////                            // navController.navigate("class_details/${cls.id}")
////                        }
////                    })
////                }
////            }
////        }
////    }
////
////
////
////
////@Composable
////fun StudentHomeScreen(
////    navController: NavController? = null,
////    currentClass: ClassItem? = null,
////    previousClasses: List<ClassItem> = emptyList(),
////    onMarkAttendance: (ClassItem) -> Unit = {}
////) {
////    Scaffold(
////        topBar = {
////            // Use your header composable (it will be placed below status bar by Scaffold)
////            HeaderWithProfile(fullname = "Kalyan", collegeName = "GVPCE", onProfileClick = {
////                // optional profile click
////            })
////        },
////        bottomBar = {
////            FooterNavPrimary(
////                onHome = { /* navController?.navigate(NavRoutes.Home.route) */ },
////                onClasses = { /* navController?.navigate(NavRoutes.Classes.route) */ },
////                onSettings = { /* navController?.navigate(NavRoutes.Settings.route) */ },
////                selected = "HOME"
////            )
////        },
////        containerColor = MaterialTheme.colorScheme.background
////    ) { innerPadding ->
////        // IMPORTANT: apply innerPadding so content is not hidden behind bars
////        Box(modifier = Modifier
////            .fillMaxSize()
////            .padding(innerPadding)
////        ) {
////            StudentScreenContent(
////                navController = navController,
////                currentClass = currentClass,
////                previousClasses = previousClasses,
////                onMarkAttendance = onMarkAttendance
////            )
////        }
////    }
////}
////
////
////@Composable
////private fun PreviousClassRow(cls: ClassItem, onClick: () -> Unit) {
////    Card(
////        modifier = Modifier
////            .fillMaxWidth()
////            .height(72.dp)
////            .clickable { onClick() },
////        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
////    ) {
////        Row(modifier = Modifier
////            .fillMaxSize()
////            .padding(horizontal = 12.dp),
////            verticalAlignment = Alignment.CenterVertically
////        ) {
////            Column(modifier = Modifier.weight(1f)) {
////                Text(cls.subject, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
////                Spacer(Modifier.height(2.dp))
////                Text("${cls.date} • ${cls.time}", style = MaterialTheme.typography.bodySmall)
////            }
////
////            // small pill showing status
////            Surface(
////                shape = MaterialTheme.shapes.extraSmall,
////                tonalElevation = 0.dp,
////                modifier = Modifier.padding(start = 8.dp)
////            ) {
////                Box(modifier = Modifier
////                    .padding(horizontal = 12.dp, vertical = 6.dp)
////                ) {
////                    Text(
////                        text = if (cls.attended) "Present" else "Missed",
////                        style = MaterialTheme.typography.bodySmall,
////                        color = if (cls.attended) StatusPresent else StatusMissed
////                    )
////                }
////            }
////        }
////    }
////}
////
////
////
////suspend fun fetchCurrentClassForStudent(
////    backendBaseUrl: String,
////    branch: String,
////    section: String,
////    year: Int
////): ClassItem? = withContext(Dispatchers.IO) {
////    try {
////        val finalUrl = "$backendBaseUrl/api/class/current?branch=${URLEncoder.encode(branch, "utf-8")}&section=${URLEncoder.encode(section, "utf-8")}&year=$year"
////        val url = URL(finalUrl)
////        val conn = (url.openConnection() as HttpURLConnection).apply {
////            requestMethod = "GET"
////            connectTimeout = 10_000
////            readTimeout = 10_000
////        }
////
////        val code = conn.responseCode
////        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
////        val text = stream.bufferedReader().use { it.readText() }
////        conn.disconnect()
////
////        if (code in 200..299 && text.isNotEmpty()) {
////            val root = JSONObject(text)
////
////            // If route responds with exists=false -> no ongoing class
////            val exists = root.optBoolean("exists", false)
////            if (!exists) return@withContext null
////
////            val data = root.optJSONObject("data") ?: return@withContext null
////
////            // Teacher name
////            val teacherName = data.optJSONObject("teacher")?.optString("name", "Unknown Teacher")
////                ?: "Unknown Teacher"
////
////            // Token (useful to display)
////            val token = data.optString("token", "")
////
////            // createdAt -> format into readable date/time
////            val createdAtIso = data.optString("createdAt", null)
////            val dateTimeString = if (!createdAtIso.isNullOrBlank()) {
////                try {
////                    // parse ISO instant -> local time
////                    val instant = Instant.parse(createdAtIso)
////                    val ldt = LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
////                    val fmt = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")
////                    ldt.format(fmt)
////                } catch (e: Exception) {
////                    // fallback
////                    createdAtIso
////                }
////            } else {
////                // fallback to "now"
////                val fmt = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")
////                LocalDateTime.now().format(fmt)
////            }
////
////            // Find the matched branch+section (if present) and confirm it
////            var matchedSectionFound = false
////            val branches = data.optJSONArray("branches")
////            if (branches != null) {
////                for (i in 0 until branches.length()) {
////                    val b = branches.optJSONObject(i) ?: continue
////                    if (b.optString("branchName", "").equals(branch, ignoreCase = true)) {
////                        val secs = b.optJSONArray("sections")
////                        if (secs != null) {
////                            for (j in 0 until secs.length()) {
////                                val s = secs.optJSONObject(j) ?: continue
////                                val sName = s.optString("sectionName", "")
////                                val sYear = s.optInt("year", -1)
////                                if (sName.equals(section, ignoreCase = true) && sYear == year) {
////                                    matchedSectionFound = true
////                                    break
////                                }
////                            }
////                        }
////                    }
////                    if (matchedSectionFound) break
////                }
////            }
////
////            if (!matchedSectionFound) {
////                // No matched section — treat as no ongoing class for this student
////                return@withContext null
////            }
////
////            // Build a ClassItem for UI. You don't have a 'subject' field on server,
////            // choose what to display — here we use "Class - {branch} {section}" as subject.
////            val subjectDisplay = "Class - $branch $section"
////
////            return@withContext ClassItem(
////                id = data.optString("_id", token).hashCode(),
////                subject = subjectDisplay,
////                teacher = teacherName ?: "Unknown Teacher",
////                time = token, // reuse token in the UI 'time' slot or show token separately; adjust as you wish
////                date = dateTimeString,
////                attended = false // you will set based on attendance data later
////            )
////        } else {
////            // server error or no body
////            return@withContext null
////        }
////    } catch (e: Exception) {
////        e.printStackTrace()
////        return@withContext null
////    }
////}
////
////@Preview(showBackground = true)
////@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
////@Composable
////fun StudentHomeScreenPreview() {
////    Attendance_AndroidTheme {
////        // For preview only, pass null navController and use defaults
////        StudentHomeScreen(navController = null, onMarkAttendance = { /* preview */ })
////    }
////}
//
//
//package com.example.attendance_android.components
//
//// --- imports ---
//import android.util.Log
//import androidx.compose.foundation.clickable
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.lazy.LazyColumn
//import androidx.compose.foundation.lazy.items
//import androidx.compose.material3.*
//import androidx.compose.material3.ExperimentalMaterial3Api
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.platform.LocalContext
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.text.style.TextOverflow
//import androidx.compose.ui.tooling.preview.Preview
//import androidx.compose.ui.unit.dp
//import androidx.navigation.NavController
//import com.example.attendance_android.data.DataStoreManager
//import com.example.attendance_android.ui.theme.Attendance_AndroidTheme
//import com.example.attendance_android.ui.theme.StatusMissed
//import com.example.attendance_android.ui.theme.StatusPresent
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.withContext
//import org.json.JSONObject
//import java.net.HttpURLConnection
//import java.net.URL
//import java.net.URLEncoder
//import java.time.Instant
//import java.time.LocalDateTime
//import java.time.ZoneId
//import java.time.format.DateTimeFormatter
//
//// --- data model ---
//data class ClassItem(
//    val id: Int,
//    val subject: String,
//    val teacher: String,
//    val time: String,        // e.g., token or "10:00 - 10:50"
//    val date: String,        // e.g., "dd MMM yyyy, HH:mm"
//    val attended: Boolean
//)
//
//private const val TAG = "StudentHome"
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun StudentScreenContent(
//    navController: NavController? = null,                           // optional navController
//    currentClass: ClassItem? = null,
//    previousClasses: List<ClassItem> = emptyList(),
//    onMarkAttendance: (ClassItem) -> Unit = {}                     // callback for button
//) {
//    val context = LocalContext.current
//    val dataStore = remember { DataStoreManager(context) }
//
//    // DataStore-backed values (strings)
//    val branch by dataStore.branch.collectAsState(initial = "")
//    val section by dataStore.section.collectAsState(initial = "")
//    val yearStr by dataStore.year.collectAsState(initial = "")
//
//    // backend base URL (adjust if needed)
//    val backendurl = "https://attendance-app-backend-zr4c.onrender.com"
//
//    // UI state
//    var fetchedCurrentClass by remember { mutableStateOf<ClassItem?>(null) }
//    var isLoading by remember { mutableStateOf(false) }
//    var lastError by remember { mutableStateOf<String?>(null) }
//
//    // Debug: show DataStore values in logs
//    LaunchedEffect(branch, section, yearStr) {
//        Log.d(TAG, "DataStore values changed: branch='$branch' section='$section' year='$yearStr'")
//    }
//
//    // Helper: safe year parse
//    fun safeYear(s: String): Int? = try { s.toInt() } catch (e: Exception) { null }
//
//    // Fetch when inputs are ready
//    LaunchedEffect(branch, section, yearStr) {
//        Log.d(TAG, "LaunchedEffect triggered for fetching class: branch='$branch' section='$section' year='$yearStr'")
//
//        // Reset states
//        lastError = null
//        fetchedCurrentClass = null
//
//        if (branch.isBlank() || section.isBlank() || yearStr.isBlank()) {
//            Log.d(TAG, "Skipping fetch - missing branch/section/year")
//            return@LaunchedEffect
//        }
//
//        val year = safeYear(yearStr)
//        if (year == null) {
//            Log.w(TAG, "Year parse failed for '$yearStr'")
//            lastError = "Invalid year: $yearStr"
//            return@LaunchedEffect
//        }
//
//        isLoading = true
//        try {
//            val result = fetchCurrentClassForStudent(backendurl, branch, section, year)
//            fetchedCurrentClass = result
//            Log.d(TAG, "fetchCurrentClassForStudent returned: $result")
//        } catch (e: Exception) {
//            Log.e(TAG, "Exception while fetching current class: ${e.message}", e)
//            lastError = e.message ?: "unknown error"
//        } finally {
//            isLoading = false
//        }
//    }
//
//    // Prepare "current" for UI: prefer fetched -> provided -> dummy
//    val now = LocalDateTime.now()
//    val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")
//    val current = fetchedCurrentClass ?: currentClass ?: ClassItem(
//        id = 1,
//        subject = "Data Structures",
//        teacher = "Dr. Singh",
//        time = "10:00 - 10:50",
//        date = now.format(formatter),
//        attended = false
//    )
//
//    val previous = previousClasses.ifEmpty {
//        remember {
//            List(8) { idx ->
//                ClassItem(
//                    id = idx + 2,
//                    subject = listOf("OS", "DBMS", "ML", "Networks", "Compiler", "SE")[idx % 6],
//                    teacher = listOf("Dr. Sharma", "Ms. Gupta", "Prof. Rao", "Mr. Das")[idx % 4],
//                    time = "09:00 - 09:50",
//                    date = now.minusDays((idx + 1).toLong()).format(formatter),
//                    attended = idx % 3 != 0
//                )
//            }
//        }
//    }
//
//    // --- UI ---
//    Column(
//        modifier = Modifier
//            .fillMaxSize()
//            .padding(horizontal = 16.dp, vertical = 12.dp)
//    ) {
//        Text(
//            text = "Current Class",
//            style = MaterialTheme.typography.titleMedium,
//            fontWeight = FontWeight.SemiBold
//        )
//        Spacer(Modifier.height(8.dp))
//
//        // Debug row (temporary) to verify DataStore values on screen
//        Text(
//            text = "DBG: branch='$branch' section='$section' year='$yearStr'",
//            style = MaterialTheme.typography.labelSmall
//        )
//        Spacer(Modifier.height(6.dp))
//
//        // Loading / error / no-class notices
//        when {
//            isLoading -> {
//                Row(verticalAlignment = Alignment.CenterVertically) {
//                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
//                    Spacer(Modifier.width(8.dp))
//                    Text("Checking for ongoing class...")
//                }
//                Spacer(Modifier.height(8.dp))
//            }
//            lastError != null -> {
//                Text("Error: $lastError", color = MaterialTheme.colorScheme.error)
//                Spacer(Modifier.height(8.dp))
//            }
//            fetchedCurrentClass == null && (branch.isNotBlank() && section.isNotBlank() && yearStr.isNotBlank()) -> {
//                Text("No ongoing class found for $branch $section (year $yearStr)")
//                Spacer(Modifier.height(8.dp))
//            }
//        }
//
//        // Card - show current info (either fetched or default)
//        Card(
//            modifier = Modifier
//                .fillMaxWidth()
//                .height(140.dp),
//            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
//        ) {
//            Row(
//                Modifier
//                    .fillMaxSize()
//                    .padding(16.dp),
//                verticalAlignment = Alignment.CenterVertically
//            ) {
//                Column(modifier = Modifier.weight(1f)) {
//                    Text(current.subject, style = MaterialTheme.typography.headlineSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
//                    Spacer(Modifier.height(4.dp))
//                    Text("Teacher: ${current.teacher}", style = MaterialTheme.typography.bodyMedium)
//                    Spacer(Modifier.height(6.dp))
//                    // If fetched class used token in time field, show token label; else show time
//                    if (fetchedCurrentClass != null) {
//                        Text("Started: ${current.date}", style = MaterialTheme.typography.bodySmall)
//                        Spacer(Modifier.height(2.dp))
//                        Text("Token: ${current.time}", style = MaterialTheme.typography.bodySmall)
//                    } else {
//                        Text("${current.date} • ${current.time}", style = MaterialTheme.typography.bodySmall)
//                    }
//                }
//
//                // Mark attendance button
//                Column(horizontalAlignment = Alignment.End) {
//                    Button(
//                        onClick = { onMarkAttendance(current) },
//                        modifier = Modifier
//                            .width(140.dp)
//                            .height(44.dp)
//                    ) {
//                        Text(if (current.attended) "Marked" else "Mark Attendance")
//                    }
//                    Spacer(Modifier.height(8.dp))
//                    if (current.attended) {
//                        Text("Status: Present", style = MaterialTheme.typography.bodySmall, color = StatusPresent)
//                    } else {
//                        Text("Status: Not marked", style = MaterialTheme.typography.bodySmall, color = StatusMissed)
//                    }
//                }
//            }
//        }
//
//        Spacer(Modifier.height(18.dp))
//
//        // Previous classes header
//        Text(
//            text = "Previous Classes",
//            style = MaterialTheme.typography.titleMedium,
//            fontWeight = FontWeight.SemiBold
//        )
//        Spacer(Modifier.height(8.dp))
//
//        // List of previous classes
//        LazyColumn(
//            modifier = Modifier.fillMaxSize(),
//            verticalArrangement = Arrangement.spacedBy(8.dp),
//        ) {
//            items(previous) { cls ->
//                PreviousClassRow(cls = cls, onClick = {
//                    if (navController != null) {
//                        // placeholder
//                    }
//                })
//            }
//        }
//    }
//}
//
//// Reuse your PreviousClassRow (unchanged)
//@Composable
//private fun PreviousClassRow(cls: ClassItem, onClick: () -> Unit) {
//    Card(
//        modifier = Modifier
//            .fillMaxWidth()
//            .height(72.dp)
//            .clickable { onClick() },
//        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
//    ) {
//        Row(
//            modifier = Modifier
//                .fillMaxSize()
//                .padding(horizontal = 12.dp),
//            verticalAlignment = Alignment.CenterVertically
//        ) {
//            Column(modifier = Modifier.weight(1f)) {
//                Text(cls.subject, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
//                Spacer(Modifier.height(2.dp))
//                Text("${cls.date} • ${cls.time}", style = MaterialTheme.typography.bodySmall)
//            }
//
//            Surface(
//                shape = MaterialTheme.shapes.extraSmall,
//                tonalElevation = 0.dp,
//                modifier = Modifier.padding(start = 8.dp)
//            ) {
//                Box(modifier = Modifier
//                    .padding(horizontal = 12.dp, vertical = 6.dp)
//                ) {
//                    Text(
//                        text = if (cls.attended) "Present" else "Missed",
//                        style = MaterialTheme.typography.bodySmall,
//                        color = if (cls.attended) StatusPresent else StatusMissed
//                    )
//                }
//            }
//        }
//    }
//}
//
///**
// * Fetch current class for a branch/section/year.
// * Returns a ClassItem or null.
// * Adds detailed logging (Logcat).
// */
//suspend fun fetchCurrentClassForStudent(
//    backendBaseUrl: String,
//    branch: String,
//    section: String,
//    year: Int
//): ClassItem? = withContext(Dispatchers.IO) {
//    try {
//        val finalUrl = "$backendBaseUrl/api/class/current?branch=${URLEncoder.encode(branch, "utf-8")}&section=${URLEncoder.encode(section, "utf-8")}&year=$year"
//        Log.d(TAG, "GET $finalUrl")
//        val url = URL(finalUrl)
//        val conn = (url.openConnection() as HttpURLConnection).apply {
//            requestMethod = "GET"
//            connectTimeout = 10_000
//            readTimeout = 10_000
//        }
//
//        val code = conn.responseCode
//        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
//        val text = stream.bufferedReader().use { it.readText() }
//        conn.disconnect()
//
//        Log.d(TAG, "Response code=$code, body=$text")
//
//        if (code in 200..299 && text.isNotEmpty()) {
//            val root = JSONObject(text)
//            val exists = root.optBoolean("exists", false)
//            if (!exists) {
//                Log.d(TAG, "Server: exists=false -> no ongoing class")
//                return@withContext null
//            }
//
//            val data = root.optJSONObject("data") ?: run {
//                Log.w(TAG, "Server returned exists=true but data=null")
//                return@withContext null
//            }
//
//            // teacher name
//            val teacherName = data.optJSONObject("teacher")?.optString("name", "Unknown Teacher") ?: "Unknown Teacher"
//            val token = data.optString("token", "")
//            val createdAtIso = data.optString("createdAt", null)
//
//            val dateTimeString = if (!createdAtIso.isNullOrBlank()) {
//                try {
//                    val instant = Instant.parse(createdAtIso)
//                    val ldt = LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
//                    val fmt = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")
//                    ldt.format(fmt)
//                } catch (e: Exception) {
//                    Log.w(TAG, "createdAt parse failed: ${e.message}")
//                    createdAtIso
//                }
//            } else {
//                val fmt = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")
//                LocalDateTime.now().format(fmt)
//            }
//
//            // Confirm section exists in returned object (defensive)
//            var matchedSectionFound = false
//            val branches = data.optJSONArray("branches")
//            if (branches != null) {
//                for (i in 0 until branches.length()) {
//                    val b = branches.optJSONObject(i) ?: continue
//                    if (b.optString("branchName", "").equals(branch, ignoreCase = true)) {
//                        val secs = b.optJSONArray("sections")
//                        if (secs != null) {
//                            for (j in 0 until secs.length()) {
//                                val s = secs.optJSONObject(j) ?: continue
//                                val sName = s.optString("sectionName", "")
//                                val sYear = s.optInt("year", -1)
//                                if (sName.equals(section, ignoreCase = true) && sYear == year) {
//                                    matchedSectionFound = true
//                                    break
//                                }
//                            }
//                        }
//                    }
//                    if (matchedSectionFound) break
//                }
//            }
//
//            if (!matchedSectionFound) {
//                Log.w(TAG, "Returned data did not contain matched section/year - treating as no class")
//                return@withContext null
//            }
//
//            val subjectDisplay = "Class - $branch $section"
//
//            return@withContext ClassItem(
//                id = (data.optString("_id", token)).hashCode(),
//                subject = subjectDisplay,
//                teacher = teacherName,
//                time = token, // token shown in "time" slot; change as needed
//                date = dateTimeString,
//                attended = false
//            )
//        } else {
//            Log.w(TAG, "Server returned non-2xx or empty body: code=$code")
//            return@withContext null
//        }
//    } catch (e: Exception) {
//        Log.e(TAG, "fetchCurrentClassForStudent failed: ${e.message}", e)
//        return@withContext null
//    }
//}
//
//
//@Composable
//fun StudentHomeScreen(
//    navController: NavController? = null,
//    currentClass: ClassItem? = null,
//    previousClasses: List<ClassItem> = emptyList(),
//    onMarkAttendance: (ClassItem) -> Unit = {}
//) {
//    Scaffold(
//        topBar = {
//            // Use your header composable (it will be placed below status bar by Scaffold)
//            HeaderWithProfile(fullname = "Kalyan", collegeName = "GVPCE", onProfileClick = {
//                // optional profile click
//            })
//        },
//        bottomBar = {
//            FooterNavPrimary(
//                onHome = { /* navController?.navigate(NavRoutes.Home.route) */ },
//                onClasses = { /* navController?.navigate(NavRoutes.Classes.route) */ },
//                onSettings = { /* navController?.navigate(NavRoutes.Settings.route) */ },
//                selected = "HOME"
//            )
//        },
//        containerColor = MaterialTheme.colorScheme.background
//    ) { innerPadding ->
//        // IMPORTANT: apply innerPadding so content is not hidden behind bars
//        Box(modifier = Modifier
//            .fillMaxSize()
//            .padding(innerPadding)
//        ) {
//            StudentScreenContent(
//                navController = navController,
//                currentClass = currentClass,
//                previousClasses = previousClasses,
//                onMarkAttendance = onMarkAttendance
//            )
//        }
//    }
//}
//@Preview(showBackground = true)
//@Composable
//fun StudentHomeScreenPreview() {
//    Attendance_AndroidTheme {
//        StudentScreenContent(navController = null, onMarkAttendance = {})
//    }
//}


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
import com.example.attendance_android.ui.theme.Attendance_AndroidTheme
import com.example.attendance_android.ui.theme.StatusMissed
import com.example.attendance_android.ui.theme.StatusPresent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
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
                    if (navController != null) {
                        // placeholder
                    }
                })
            }
        }
    }
}

// Reuse your PreviousClassRow (unchanged)
@Composable
private fun PreviousClassRow(cls: ClassItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(cls.subject, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(2.dp))
                Text("${cls.date} • ${cls.time}", style = MaterialTheme.typography.bodySmall)
            }

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

            val subjectDisplay = "Class - $branch $section"

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