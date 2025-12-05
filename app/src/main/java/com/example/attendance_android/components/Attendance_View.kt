package com.example.attendance_android.components

import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.*

data class StudentAttendance(
    val rollNumber: String,
    val attendanceMap: Map<String, Boolean>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceViewScreen(
    navController: NavController? = null,
    teacherName: String = "Teacher",
    backendBaseUrl: String = "https://attendance-app-backend-zr4c.onrender.com"
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Form state
    var year by remember { mutableStateOf("") }
    var branch by remember { mutableStateOf("") }
    var section by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }
    var filterType by remember { mutableStateOf("all") } // "all", "single", "range"
    var singleDate by remember { mutableStateOf("") }
    var fromDate by remember { mutableStateOf("") }
    var toDate by remember { mutableStateOf("") }

    // Data state
    var students by remember { mutableStateOf<List<StudentAttendance>>(emptyList()) }
    var dates by remember { mutableStateOf<List<String>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    // Response metadata
    var responseYear by remember { mutableStateOf("") }
    var responseBranch by remember { mutableStateOf("") }
    var responseSection by remember { mutableStateOf("") }
    var responseSubject by remember { mutableStateOf("") }

    // Dropdowns
    val years = listOf("I", "II", "III", "IV")
    val branches = listOf("CSE", "ECE", "ME", "CE")
    val sections = listOf("A", "B", "C")
    val subjects = listOf("OS", "DS", "DBMS")
    val filterTypes = listOf("All Dates" to "all", "Single Date" to "single", "Date Range" to "range")

    var yearExpanded by remember { mutableStateOf(false) }
    var branchExpanded by remember { mutableStateOf(false) }
    var sectionExpanded by remember { mutableStateOf(false) }
    var subjectExpanded by remember { mutableStateOf(false) }
    var filterExpanded by remember { mutableStateOf(false) }

    suspend fun fetchAttendance() = withContext(Dispatchers.IO) {
        try {
            val yearInt = romanToInt(year)
            var url = "$backendBaseUrl/api/attendance?year=$yearInt&branch=${URLEncoder.encode(branch, "UTF-8")}&section=${URLEncoder.encode(section, "UTF-8")}&subject=${URLEncoder.encode(subject, "UTF-8")}"

            when (filterType) {
                "single" -> if (singleDate.isNotEmpty()) url += "&date=${URLEncoder.encode(singleDate, "UTF-8")}"
                "range" -> if (fromDate.isNotEmpty() && toDate.isNotEmpty()) {
                    url += "&from=${URLEncoder.encode(fromDate, "UTF-8")}&to=${URLEncoder.encode(toDate, "UTF-8")}"
                }
            }

            Log.d("AttendanceView", "URL: $url")

            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 15000
                readTimeout = 15000
            }

            val code = conn.responseCode
            val text = if (code in 200..299) {
                conn.inputStream.bufferedReader().use { it.readText() }
            } else {
                conn.errorStream?.bufferedReader()?.use { it.readText() } ?: "Error"
            }
            conn.disconnect()

            Log.d("AttendanceView", "Response: $text")

            if (code !in 200..299) {
                return@withContext Pair(false, text)
            }

            val json = JSONObject(text)
            val studentsArray = json.getJSONArray("students")
            val studentsList = mutableListOf<StudentAttendance>()
            val allDates = mutableSetOf<String>()

            for (i in 0 until studentsArray.length()) {
                val studentObj = studentsArray.getJSONObject(i)
                val rollNo = studentObj.getString("rollNumber")
                val attendanceArray = studentObj.getJSONArray("attendance")
                val attendanceMap = mutableMapOf<String, Boolean>()

                for (j in 0 until attendanceArray.length()) {
                    val attObj = attendanceArray.getJSONObject(j)
                    val date = attObj.getString("date")
                    val present = attObj.getBoolean("present")
                    attendanceMap[date] = present
                    allDates.add(date)
                }

                studentsList.add(StudentAttendance(rollNo, attendanceMap))
            }

            val metadata = listOf(
                json.getString("year"),
                json.getString("branch"),
                json.getString("section"),
                json.getString("subject")
            )

            return@withContext Pair(true, Triple(studentsList, allDates.sorted(), metadata))
        } catch (e: Exception) {
            Log.e("AttendanceView", "Error: ${e.message}", e)
            return@withContext Pair(false, e.message ?: "Error")
        }
    }

    fun formatDate(dateStr: String): String {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
            val date = sdf.parse(dateStr)
            SimpleDateFormat("MMM dd", Locale.US).format(date ?: Date())
        } catch (e: Exception) {
            dateStr.take(10)
        }
    }

    fun downloadPDF() {
        scope.launch(Dispatchers.IO) {
            try {
                val pdfDocument = PdfDocument()
                val pageInfo = PdfDocument.PageInfo.Builder(842, 595, 1).create()
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas
                val paint = Paint()

                var yPos = 50f
                paint.textSize = 20f
                paint.isFakeBoldText = true
                canvas.drawText("Attendance Report", 50f, yPos, paint)
                yPos += 30f

                paint.textSize = 14f
                paint.isFakeBoldText = false
                canvas.drawText("Subject: $responseSubject | Year: $responseYear | Branch: $responseBranch | Section: $responseSection", 50f, yPos, paint)
                yPos += 40f

                paint.textSize = 12f
                paint.isFakeBoldText = true
                canvas.drawText("Roll No", 50f, yPos, paint)
                var xPos = 150f
                dates.take(10).forEach { date ->
                    canvas.drawText(formatDate(date), xPos, yPos, paint)
                    xPos += 70f
                }
                yPos += 25f

                paint.isFakeBoldText = false
                students.forEach { student ->
                    canvas.drawText(student.rollNumber, 50f, yPos, paint)
                    xPos = 150f
                    dates.take(10).forEach { date ->
                        val present = student.attendanceMap[date]
                        val mark = when (present) {
                            true -> "✓"
                            false -> "✗"
                            else -> "-"
                        }
                        canvas.drawText(mark, xPos, yPos, paint)
                        xPos += 70f
                    }
                    yPos += 25f
                    if (yPos > 550) return@forEach
                }

                pdfDocument.finishPage(page)

                val file = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    "Attendance_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.pdf"
                )
                pdfDocument.writeTo(FileOutputStream(file))
                pdfDocument.close()

                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "PDF saved: ${file.absolutePath}", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Log.e("AttendanceView", "PDF Error: ${e.message}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Failed to create PDF", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun downloadCSV() {
        scope.launch(Dispatchers.IO) {
            try {
                val csv = StringBuilder()
                csv.append("Roll Number,${dates.joinToString(",") { formatDate(it) }}\n")

                students.forEach { student ->
                    csv.append(student.rollNumber)
                    dates.forEach { date ->
                        val present = student.attendanceMap[date]
                        val mark = when (present) {
                            true -> "Present"
                            false -> "Absent"
                            else -> "-"
                        }
                        csv.append(",$mark")
                    }
                    csv.append("\n")
                }

                val file = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    "Attendance_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.csv"
                )
                file.writeText(csv.toString())

                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "CSV saved: ${file.absolutePath}", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Log.e("AttendanceView", "CSV Error: ${e.message}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Failed to create CSV", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            HeaderWithProfile(
                fullname = teacherName,
                collegeName = "GVPCE",
                navController = navController
            )
        },
        bottomBar = {
            FooterNavPrimary(
                onHome = { navController?.navigate("teacher_home") },
                onClasses = { },
                onSettings = { },
                selected = "CLASSES"
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 20.dp)
        ) {
            // Header
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Attendance Records",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "View and download attendance reports",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Filters Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Outlined.FilterList,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "Filters",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Year
                            ExposedDropdownMenuBox(
                                expanded = yearExpanded,
                                onExpandedChange = { yearExpanded = it },
                                modifier = Modifier.weight(1f)
                            ) {
                                OutlinedTextField(
                                    value = year,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Year *") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(yearExpanded) },
                                    modifier = Modifier.fillMaxWidth().menuAnchor()
                                )
                                ExposedDropdownMenu(
                                    expanded = yearExpanded,
                                    onDismissRequest = { yearExpanded = false }
                                ) {
                                    years.forEach { y ->
                                        DropdownMenuItem(
                                            text = { Text(y) },
                                            onClick = { year = y; yearExpanded = false }
                                        )
                                    }
                                }
                            }

                            // Branch
                            ExposedDropdownMenuBox(
                                expanded = branchExpanded,
                                onExpandedChange = { branchExpanded = it },
                                modifier = Modifier.weight(1f)
                            ) {
                                OutlinedTextField(
                                    value = branch,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Branch *") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(branchExpanded) },
                                    modifier = Modifier.fillMaxWidth().menuAnchor()
                                )
                                ExposedDropdownMenu(
                                    expanded = branchExpanded,
                                    onDismissRequest = { branchExpanded = false }
                                ) {
                                    branches.forEach { b ->
                                        DropdownMenuItem(
                                            text = { Text(b) },
                                            onClick = { branch = b; branchExpanded = false }
                                        )
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Section
                            ExposedDropdownMenuBox(
                                expanded = sectionExpanded,
                                onExpandedChange = { sectionExpanded = it },
                                modifier = Modifier.weight(1f)
                            ) {
                                OutlinedTextField(
                                    value = section,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Section *") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(sectionExpanded) },
                                    modifier = Modifier.fillMaxWidth().menuAnchor()
                                )
                                ExposedDropdownMenu(
                                    expanded = sectionExpanded,
                                    onDismissRequest = { sectionExpanded = false }
                                ) {
                                    sections.forEach { s ->
                                        DropdownMenuItem(
                                            text = { Text(s) },
                                            onClick = { section = s; sectionExpanded = false }
                                        )
                                    }
                                }
                            }

                            // Subject
                            ExposedDropdownMenuBox(
                                expanded = subjectExpanded,
                                onExpandedChange = { subjectExpanded = it },
                                modifier = Modifier.weight(1f)
                            ) {
                                OutlinedTextField(
                                    value = subject,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Subject *") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(subjectExpanded) },
                                    modifier = Modifier.fillMaxWidth().menuAnchor()
                                )
                                ExposedDropdownMenu(
                                    expanded = subjectExpanded,
                                    onDismissRequest = { subjectExpanded = false }
                                ) {
                                    subjects.forEach { s ->
                                        DropdownMenuItem(
                                            text = { Text(s) },
                                            onClick = { subject = s; subjectExpanded = false }
                                        )
                                    }
                                }
                            }
                        }

                        Divider()

                        // Filter Type
                        ExposedDropdownMenuBox(
                            expanded = filterExpanded,
                            onExpandedChange = { filterExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = filterTypes.find { it.second == filterType }?.first ?: "All Dates",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Date Filter") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(filterExpanded) },
                                modifier = Modifier.fillMaxWidth().menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = filterExpanded,
                                onDismissRequest = { filterExpanded = false }
                            ) {
                                filterTypes.forEach { (label, value) ->
                                    DropdownMenuItem(
                                        text = { Text(label) },
                                        onClick = {
                                            filterType = value
                                            filterExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // Date inputs based on filter type
                        when (filterType) {
                            "single" -> {
                                OutlinedTextField(
                                    value = singleDate,
                                    onValueChange = { singleDate = it },
                                    label = { Text("Date (YYYY-MM-DD)") },
                                    placeholder = { Text("2024-01-15") },
                                    modifier = Modifier.fillMaxWidth(),
                                    leadingIcon = { Icon(Icons.Outlined.CalendarToday, null) }
                                )
                            }
                            "range" -> {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    OutlinedTextField(
                                        value = fromDate,
                                        onValueChange = { fromDate = it },
                                        label = { Text("From") },
                                        placeholder = { Text("2024-01-01") },
                                        modifier = Modifier.weight(1f),
                                        leadingIcon = { Icon(Icons.Outlined.CalendarToday, null) }
                                    )
                                    OutlinedTextField(
                                        value = toDate,
                                        onValueChange = { toDate = it },
                                        label = { Text("To") },
                                        placeholder = { Text("2024-01-31") },
                                        modifier = Modifier.weight(1f),
                                        leadingIcon = { Icon(Icons.Outlined.CalendarToday, null) }
                                    )
                                }
                            }
                        }

                        // Fetch Button
                        Button(
                            onClick = {
                                if (year.isEmpty() || branch.isEmpty() || section.isEmpty() || subject.isEmpty()) {
                                    error = "Please fill all required fields"
                                    return@Button
                                }
                                scope.launch {
                                    loading = true
                                    error = null
                                    val result = fetchAttendance()
                                    loading = false

                                    if (result.first && result.second is Triple<*, *, *>) {
                                        val data = result.second as Triple<List<StudentAttendance>, List<String>, List<String>>
                                        students = data.first
                                        dates = data.second
                                        val metadata = data.third
                                        responseYear = metadata[0]
                                        responseBranch = metadata[1]
                                        responseSection = metadata[2]
                                        responseSubject = metadata[3]
                                    } else {
                                        error = result.second as? String ?: "Failed to fetch"
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            enabled = !loading,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            if (loading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.Outlined.Search, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("View Attendance", style = MaterialTheme.typography.titleMedium)
                            }
                        }

                        if (error != null) {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer
                                )
                            ) {
                                Text(
                                    text = "⚠️ $error",
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.padding(12.dp),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }

            // Stats & Download
            if (students.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Outlined.People, null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "${students.size}",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text("Students", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Outlined.CalendarToday, null, tint = MaterialTheme.colorScheme.secondary)
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "${dates.size}",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text("Classes", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }

                // Download Buttons
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { downloadPDF() },
                            modifier = Modifier.weight(1f).height(56.dp)
                        ) {
                            Icon(Icons.Outlined.PictureAsPdf, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("PDF")
                        }
                        OutlinedButton(
                            onClick = { downloadCSV() },
                            modifier = Modifier.weight(1f).height(56.dp)
                        ) {
                            Icon(Icons.Outlined.Description, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("CSV")
                        }
                    }
                }
            }

            // Attendance Table
            if (students.isNotEmpty() && dates.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "$responseSubject - Year $responseYear $responseBranch $responseSection",
                                    modifier = Modifier.padding(16.dp),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }

                            Box(modifier = Modifier.fillMaxWidth()) {
                                // Roll Number Column - Fixed
                                Column(
                                    modifier = Modifier
                                        .width(150.dp)
                                        .background(MaterialTheme.colorScheme.surface)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp)
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                            .border(0.5.dp, MaterialTheme.colorScheme.outline)
                                            .padding(8.dp),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        Text("Roll Number", fontWeight = FontWeight.Bold)
                                    }
                                    students.forEach { student ->
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(48.dp)
                                                .border(0.5.dp, MaterialTheme.colorScheme.outline)
                                                .padding(8.dp),
                                            contentAlignment = Alignment.CenterStart
                                        ) {
                                            Text(student.rollNumber, fontWeight = FontWeight.Medium)
                                        }
                                    }
                                }

                                // Dates Columns - Scrollable
                                Row(
                                    modifier = Modifier
                                        .padding(start = 150.dp)
                                        .horizontalScroll(rememberScrollState())
                                ) {
                                    dates.forEach { date ->
                                        Column(modifier = Modifier.width(80.dp)) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(48.dp)
                                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                                    .border(0.5.dp, MaterialTheme.colorScheme.outline)
                                                    .padding(8.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    formatDate(date),
                                                    fontWeight = FontWeight.Bold,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    textAlign = TextAlign.Center
                                                )
                                            }
                                            students.forEach { student ->
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(48.dp)
                                                        .border(0.5.dp, MaterialTheme.colorScheme.outline)
                                                        .padding(8.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    val present = student.attendanceMap[date]
                                                    when (present) {
                                                        true -> Text(
                                                            "✓",
                                                            color = Color(0xFF4CAF50),
                                                            style = MaterialTheme.typography.titleLarge,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                        false -> Text(
                                                            "✗",
                                                            color = Color(0xFFF44336),
                                                            style = MaterialTheme.typography.titleLarge,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                        null -> Text("-", color = Color.Gray)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}