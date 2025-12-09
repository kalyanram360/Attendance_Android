
package com.example.attendance_android.components

// imports
import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.os.ParcelUuid
import android.util.Log
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.json.JSONArray
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL
import java.util.*
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.example.attendance_android.data.ClassDatabase
import com.example.attendance_android.data.ClassEntity
import com.example.attendance_android.NavRoutes
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape

// Simple data class for attended student list
data class AttendedStudent(val rollNo: String, val name: String)

// Data models for class details
data class ClassStudent(val rollNo: String, val name: String, var present: Boolean)
data class ClassSection(val sectionName: String, val year: Int, val students: MutableList<ClassStudent>)
data class ClassBranch(val branchName: String, val sections: MutableList<ClassSection>)

// AdvertisingScreen composable
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvertisingScreen(
    navController: NavController? = null,
    sectionsJson: String,          // Changed: JSON string of sections
    subject: String,                // Changed: subject separate
    teacherEmail: String,
    backendBaseUrl: String = "https://attendance-app-backend-zr4c.onrender.com"
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val tag = "AdvertisingScreen"

    // Parse sections JSON
    val sectionsList = remember {
        try {
            val jsonArray = JSONArray(sectionsJson)
            val list = mutableListOf<Triple<String, String, String>>() // (year, branch, section)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    Triple(
                        obj.getString("year"),
                        obj.getString("branch"),
                        obj.getString("section")
                    )
                )
            }
            list
        } catch (e: Exception) {
            Log.e(tag, "Error parsing sections: ${e.message}")
            emptyList()
        }
    }

    // UI state
    var token by remember { mutableStateOf<String?>(null) }
    var posting by remember { mutableStateOf(false) }
    var postingError by remember { mutableStateOf<String?>(null) }
    var advertising by remember { mutableStateOf(false) }
    var advError by remember { mutableStateOf<String?>(null) }

    // Attended list (populated dynamically from backend)
    val attended = remember { mutableStateListOf<AttendedStudent>() }

    // BLE objects (remember across recompositions)
    val btAdapter = remember { BluetoothAdapter.getDefaultAdapter() }
    val advertiser: BluetoothLeAdvertiser? = remember { btAdapter?.bluetoothLeAdvertiser }

    // Pulse animation for "advertising" indicator
    val pulseAnim = rememberInfiniteTransition()
    val pulse by pulseAnim.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse)
    )

    // Helper: create a short unique token
    fun makeToken(): String = UUID.randomUUID().toString().replace("-", "").take(10)

    // AdvertiseCallback implementation
    val advCallback = remember {
        object : AdvertiseCallback() {
            override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
                super.onStartSuccess(settingsInEffect)
                Log.d(tag, "Advertising started")
            }

            override fun onStartFailure(errorCode: Int) {
                super.onStartFailure(errorCode)
                Log.e(tag, "Advertising failed: $errorCode")
            }
        }
    }

    // Start advertising using Android BLE APIs
    suspend fun startBleAdvertising(tokenValue: String): Boolean = withContext(Dispatchers.Default) {
        if (advertiser == null) {
            advError = "BLE advertiser not available on this device"
            return@withContext false
        }

        try {
            val settings = AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                .setConnectable(false)
                .build()

            val serviceUuid = ParcelUuid(UUID.fromString("0000feed-0000-1000-8000-00805f9b34fb"))

            val data = AdvertiseData.Builder()
                .addServiceData(serviceUuid, tokenValue.toByteArray(Charsets.UTF_8))
                .addServiceUuid(serviceUuid)
                .setIncludeDeviceName(false)
                .build()

            advertiser.startAdvertising(settings, data, advCallback)
            return@withContext true
        } catch (e: Exception) {
            e.printStackTrace()
            advError = "Advertise start failed: ${e.message}"
            return@withContext false
        }
    }

    // Stop advertising
    fun stopAdvertising() {
        try {
            val needConnectPerm = android.os.Build.VERSION.SDK_INT >= 31
            if (needConnectPerm) {
                val granted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) == PackageManager.PERMISSION_GRANTED

                if (!granted) {
                    Log.w("AdvertisingScreen", "Missing BLUETOOTH_CONNECT permission; cannot stop advertising.")
                    advError = "Missing BLUETOOTH_CONNECT permission; cannot stop advertising."
                    return
                }
            }

            advertiser?.stopAdvertising(advCallback)
            advertising = false
        } catch (se: SecurityException) {
            Log.e("AdvertisingScreen", "SecurityException while stopping advertising: ${se.message}")
            se.printStackTrace()
            advError = "Permission required to stop advertising."
        } catch (e: Exception) {
            Log.e("AdvertisingScreen", "Error stopping advertising: ${e.message}")
            e.printStackTrace()
            advError = "Failed to stop advertising: ${e.message}"
        }
    }

    // Fetch full class branches/details (sections+students with present flag)
    suspend fun fetchFullClassDetails(token: String): List<ClassBranch> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$backendBaseUrl/api/class/branches/${URLEncoder.encode(token, "UTF-8")}")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10_000
                readTimeout = 10_000
            }

            val code = conn.responseCode
            val text = if (code in 200..299) conn.inputStream.bufferedReader().use { it.readText() } else conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
            conn.disconnect()

            if (code !in 200..299) return@withContext emptyList()

            val json = JSONObject(text)
            if (!json.optBoolean("success", false)) return@withContext emptyList()

            val data = json.optJSONObject("data") ?: return@withContext emptyList()
            val branchesArr = data.optJSONArray("branches") ?: return@withContext emptyList()

            val branches = mutableListOf<ClassBranch>()
            for (i in 0 until branchesArr.length()) {
                val bObj = branchesArr.getJSONObject(i)
                val branchName = bObj.optString("branchName", "")
                val sectionsArr = bObj.optJSONArray("sections") ?: continue
                val sections = mutableListOf<ClassSection>()
                for (j in 0 until sectionsArr.length()) {
                    val sObj = sectionsArr.getJSONObject(j)
                    val sectionName = sObj.optString("sectionName", "")
                    val year = sObj.optInt("year", 0)
                    val studentsArr = sObj.optJSONArray("students") ?: continue
                    val students = mutableListOf<ClassStudent>()
                    for (k in 0 until studentsArr.length()) {
                        val st = studentsArr.getJSONObject(k)
                        students.add(ClassStudent(st.optString("rollNo", ""), st.optString("name", ""), st.optBoolean("present", false)))
                    }
                    sections.add(ClassSection(sectionName, year, students))
                }
                branches.add(ClassBranch(branchName, sections))
            }

            return@withContext branches
        } catch (e: Exception) {
            Log.e(tag, "fetchFullClassDetails error: ${e.message}")
            return@withContext emptyList()
        }
    }

    // Archive class by sending the full class object to backend
    suspend fun archiveClassOnServer(classObject: JSONObject): Pair<Boolean, String?> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$backendBaseUrl/api/class/archive")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                connectTimeout = 15_000
                readTimeout = 15_000
            }

            val body = JSONObject().apply { put("classObject", classObject) }
            conn.outputStream.use { os -> OutputStreamWriter(os, "UTF-8").use { it.write(body.toString()); it.flush() } }

            val code = conn.responseCode
            val resp = if (code in 200..299) conn.inputStream.bufferedReader().use { it.readText() } else conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
            conn.disconnect()
            return@withContext if (code in 200..299) Pair(true, null) else Pair(false, "Server $code: $resp")
        } catch (e: Exception) {
            Log.e(tag, "archiveClassOnServer error: ${e.message}")
            return@withContext Pair(false, e.message)
        }
    }

    // Post class creation to backend
    suspend fun postCreateClass(
        teacherEmail: String,
        subject: String,
        token: String,
        sections: List<Triple<String, String, String>>
    ): Pair<Boolean, String?> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$backendBaseUrl/api/class/create")
            Log.d(tag, "Posting to: $url")

            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "application/json")
                connectTimeout = 15_000
                readTimeout = 15_000
            }

            val body = JSONObject().apply {
                put("teacherEmail", teacherEmail.trim())
                put("subject", subject.trim())
                put("token", token.trim())

                // Build sections array
                val sectionsArray = JSONArray()
                sections.forEach { (year, branch, section) ->
                    val sectionObj = JSONObject().apply {
                        put("year", romanToInt(year))
                        put("branch", branch.trim())
                        put("section", section.trim())
                    }
                    sectionsArray.put(sectionObj)
                }
                put("sections", sectionsArray)
            }

            Log.d(tag, "Request body: ${body.toString()}")

            conn.outputStream.use { os ->
                OutputStreamWriter(os, "UTF-8").use {
                    it.write(body.toString())
                    it.flush()
                }
            }

            val responseCode = conn.responseCode
            Log.d(tag, "Response code: $responseCode")

            val responseText = if (responseCode in 200..299) {
                conn.inputStream.bufferedReader().use { it.readText() }
            } else {
                conn.errorStream?.bufferedReader()?.use { it.readText() } ?: "No error message"
            }

            Log.d(tag, "Response: $responseText")
            conn.disconnect()

            if (responseCode in 200..299) {
                return@withContext Pair(true, null)
            } else {
                return@withContext Pair(false, "Server returned $responseCode: $responseText")
            }
        } catch (e: Exception) {
            Log.e(tag, "Exception during POST: ${e.message}", e)
            e.printStackTrace()
            return@withContext Pair(false, "Network error: ${e.message}")
        }
    }

    // Fetch branches/students for token
    suspend fun fetchClassBranches(token: String): List<AttendedStudent> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$backendBaseUrl/api/class/branches/${URLEncoder.encode(token, "UTF-8")}")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10_000
                readTimeout = 10_000
            }

            val code = conn.responseCode
            val text = if (code in 200..299) conn.inputStream.bufferedReader().use { it.readText() } else conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
            conn.disconnect()

            if (code !in 200..299) return@withContext emptyList()

            val json = JSONObject(text)
            if (!json.optBoolean("success", false)) return@withContext emptyList()

            val data = json.optJSONObject("data") ?: return@withContext emptyList()
            val branches = data.optJSONArray("branches") ?: return@withContext emptyList()

            val presentStudents = mutableListOf<AttendedStudent>()
            for (i in 0 until branches.length()) {
                val branchObj = branches.getJSONObject(i)
                val sections = branchObj.optJSONArray("sections") ?: continue
                for (j in 0 until sections.length()) {
                    val sectionObj = sections.getJSONObject(j)
                    val students = sectionObj.optJSONArray("students") ?: continue
                    for (k in 0 until students.length()) {
                        val stu = students.getJSONObject(k)
                        if (stu.optBoolean("present", false)) {
                            presentStudents.add(AttendedStudent(stu.optString("rollNo", ""), stu.optString("name", "")))
                        }
                    }
                }
            }

            return@withContext presentStudents
        } catch (e: Exception) {
            Log.e(tag, "fetchClassBranches error: ${e.message}")
            return@withContext emptyList()
        }
    }

    // Delete class on server
    suspend fun deleteClassFromServer(token: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL("$backendBaseUrl/api/class/delete/${URLEncoder.encode(token, "UTF-8")}")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "DELETE"
                connectTimeout = 10_000
                readTimeout = 10_000
            }

            val code = conn.responseCode
            val responseText = if (code in 200..299) {
                conn.inputStream.bufferedReader().use { it.readText() }
            } else {
                conn.errorStream?.bufferedReader()?.use { it.readText() } ?: "Unknown error"
            }
            conn.disconnect()

            Log.d(tag, "Delete class response code: $code")
            return@withContext code in 200..299
        } catch (e: Exception) {
            Log.e(tag, "deleteClassFromServer error: ${e.message}")
            return@withContext false
        }
    }

    // Permissions check
    fun hasBluetoothAdvertisePermission(): Boolean {
        val ctx = context
        val api31 = android.os.Build.VERSION.SDK_INT >= 31
        return if (api31) {
            ctx.checkSelfPermission(Manifest.permission.BLUETOOTH_ADVERTISE) == PackageManager.PERMISSION_GRANTED
                    && ctx.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    // UI layout
    val outerPulse by pulseAnim.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "outerPulse"
    )

    Scaffold(
        topBar = {
            HeaderWithProfile(
                fullname = teacherEmail.split("@").firstOrNull() ?: "T",
                collegeName = "GVPCE",
                navController = navController
            )
        }
    ) { innerPadding ->
        LaunchedEffect(Unit) {
            if (token == null) {
                posting = true
                val t = makeToken()
                token = t
                val (ok, err) = postCreateClass(teacherEmail, subject, t, sectionsList)
                posting = false
                if (!ok) {
                    postingError = err ?: "Failed to create class on server"
                }
            }

            while (true) {
                val curToken = token
                if (!curToken.isNullOrBlank()) {
                    val list = fetchClassBranches(curToken)
                    attended.clear()
                    attended.addAll(list)
                }
                kotlinx.coroutines.delay(5000)
            }
        }

        var classDetails by remember { mutableStateOf<List<ClassBranch>?>(null) }
        var classStopped by remember { mutableStateOf(false) }
        val presentMap = remember { mutableStateMapOf<String, Boolean>() }

        // Get students from ALL selected sections
        val studentsForAllSections = remember(classDetails, sectionsList) {
            val allStudents = mutableListOf<ClassStudent>()

            classDetails?.forEach { branch ->
                branch.sections.forEach { section ->
                    // Check if this section matches any of our selected sections
                    val matches = sectionsList.any { (year, branchName, sectionName) ->
                        branch.branchName.equals(branchName, true) &&
                                section.sectionName.equals(sectionName, true) &&
                                section.year == romanToInt(year)
                    }

                    if (matches) {
                        allStudents.addAll(section.students)
                    }
                }
            }

            allStudents
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val groupedStudents = remember(studentsForAllSections, classDetails, sectionsList) {
                val groups = mutableMapOf<String, MutableList<ClassStudent>>()

                sectionsList.forEach { (year, branch, section) ->
                    val key = "$year - $branch - $section"
                    val studentsForThisSection = studentsForAllSections.filter { student ->
                        classDetails?.firstOrNull { it.branchName.equals(branch, true) }
                            ?.sections
                            ?.firstOrNull {
                                it.sectionName.equals(section, true) &&
                                        it.year == romanToInt(year)
                            }
                            ?.students
                            ?.any { it.rollNo == student.rollNo }
                            ?: false
                    }
                    groups[key] = studentsForThisSection.toMutableList()
                }

                groups
            }
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                contentPadding = PaddingValues(vertical = 20.dp)
            ) {
                // Header Section
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Class Session",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Class Info Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.extraLarge,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Subject
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Class,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = subject,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }

                            Divider(
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f),
                                thickness = 1.dp
                            )

                            // Display all sections
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Sections (${sectionsList.size})",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                )

                                sectionsList.forEach { (year, branch, section) ->
                                    Surface(
                                        shape = MaterialTheme.shapes.small,
                                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(8.dp),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Text(
                                                text = "Year $year",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(text = "•", style = MaterialTheme.typography.bodySmall)
                                            Text(
                                                text = branch,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(text = "•", style = MaterialTheme.typography.bodySmall)
                                            Text(
                                                text = "Section $section",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }

                            Divider(
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f),
                                thickness = 1.dp
                            )

                            // Teacher
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = teacherEmail,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }

                // Advertising Status Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.extraLarge,
                        colors = CardDefaults.cardColors(
                            containerColor = if (advertising)
                                MaterialTheme.colorScheme.tertiaryContainer
                            else
                                MaterialTheme.colorScheme.surfaceVariant
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Pulsing Animation
                            Box(
                                modifier = Modifier.size(160.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (advertising) {
                                    Box(
                                        modifier = Modifier
                                            .size((140 * outerPulse).dp)
                                            .background(
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                                shape = CircleShape
                                            )
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .size((100 * pulse).dp)
                                        .background(
                                            if (advertising)
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                                            else
                                                MaterialTheme.colorScheme.surfaceVariant,
                                            shape = CircleShape
                                        )
                                )

                                Box(
                                    modifier = Modifier
                                        .size(80.dp)
                                        .background(
                                            if (advertising)
                                                MaterialTheme.colorScheme.primary
                                            else
                                                MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (advertising) Icons.Outlined.Wifi else Icons.Outlined.WifiOff,
                                        contentDescription = null,
                                        tint = if (advertising) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                            }

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = if (advertising) "Broadcasting Active" else "Ready to Start",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = if (advertising)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                token?.let {
                                    Surface(
                                        shape = MaterialTheme.shapes.medium,
                                        color = MaterialTheme.colorScheme.surface,
                                        modifier = Modifier.padding(top = 8.dp)
                                    ) {
                                        Text(
                                            text = "Token: $it",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Error Messages
                if (postingError != null || advError != null) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            shape = MaterialTheme.shapes.large
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (postingError != null) {
                                    Text(
                                        text = "⚠️ $postingError",
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                if (advError != null) {
                                    Text(
                                        text = "⚠️ $advError",
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                }

                // Control Buttons
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                scope.launch {
                                    if (!hasBluetoothAdvertisePermission()) {
                                        advError = "Missing BLUETOOTH permissions"
                                        return@launch
                                    }

                                    val curToken = token
                                    if (curToken.isNullOrBlank()) {
                                        postingError = "Token not ready"
                                        return@launch
                                    }

                                    advError = null
                                    val started = startBleAdvertising(curToken)
                                    advertising = started
                                    if (!started) advError = advError ?: "Failed to start"
                                }
                            },
                            enabled = !advertising && !posting,
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            shape = MaterialTheme.shapes.large
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Start", style = MaterialTheme.typography.titleMedium)
                        }

                        Button(
                            onClick = {
                                stopAdvertising()
                                classStopped = true
                                scope.launch {
                                    val curToken = token
                                    if (!curToken.isNullOrBlank()) {
                                        val details = fetchFullClassDetails(curToken)
                                        classDetails = details
                                        presentMap.clear()
                                        // Populate presentMap for all students in selected sections
                                        details.forEach { branch ->
                                            branch.sections.forEach { section ->
                                                val matches = sectionsList.any { (year, branchName, sectionName) ->
                                                    branch.branchName.equals(branchName, true) &&
                                                            section.sectionName.equals(sectionName, true) &&
                                                            section.year == romanToInt(year)
                                                }
                                                if (matches) {
                                                    section.students.forEach { st ->
                                                        presentMap[st.rollNo] = st.present
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            },
                            enabled = advertising,
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            shape = MaterialTheme.shapes.large,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Stop,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Stop", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }

                // Attendance Section
                if (!classStopped) {
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Present Students",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        text = "${attended.size}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    }

                    if (attended.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(150.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ),
                                shape = MaterialTheme.shapes.large
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
                                            imageVector = Icons.Outlined.People,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                            modifier = Modifier.size(48.dp)
                                        )
                                        Text(
                                            text = "Waiting for students...",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        items(attended) { student ->
                            AttendanceCard(student = student)
                        }
                    }
                } else {

                    // Edit Mode
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            ),
                            shape = MaterialTheme.shapes.large
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Edit,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Review Attendance",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Text(
                                        text = "Make final adjustments before archiving",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }
                    // After the "Review Attendance" card, replace the flat items() with grouped sections

// Add this above the items() call:

// Show each section with header and students
                    groupedStudents.forEach { (sectionKey, students) ->
                        // Section header
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = sectionKey,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primary
                                    ) {
                                        Text(
                                            text = "${students.count { presentMap[it.rollNo] ?: false }}/${students.size}",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Students in this section
                        items(students) { student ->
                            EditableAttendanceCard(
                                student = student,
                                checked = presentMap[student.rollNo] ?: false,
                                onCheckedChange = { presentMap[student.rollNo] = it }
                            )
                        }

                        // Spacer between sections
                        item {
                            Spacer(Modifier.height(8.dp))
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            FilledTonalButton(
                                onClick = {
                                    classStopped = false
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp),
                                shape = MaterialTheme.shapes.large
                            ) {
                                Icon(Icons.Outlined.Close, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Cancel")
                            }

                            Button(
                                onClick = {
                                    scope.launch {
                                        posting = true
                                        val curToken = token ?: return@launch

                                        // Post attendance for each section separately
                                        var allAttendanceSuccess = true
                                        var attendanceError: String? = null

                                        for ((year, branch, section) in sectionsList) {
                                            val studentsForThisSection = studentsForAllSections.filter { student ->
                                                // Match students to this specific section
                                                classDetails?.firstOrNull { it.branchName.equals(branch, true) }
                                                    ?.sections
                                                    ?.firstOrNull {
                                                        it.sectionName.equals(section, true) &&
                                                                it.year == romanToInt(year)
                                                    }
                                                    ?.students
                                                    ?.any { it.rollNo == student.rollNo }
                                                    ?: false
                                            }

                                            val attendanceData = studentsForThisSection.map { student ->
                                                Pair(student.rollNo, presentMap[student.rollNo] ?: false)
                                            }

                                            val (ok, err) = postAttendanceToServer(
                                                year = year,
                                                branch = branch,
                                                section = section,
                                                subject = subject,
                                                attendanceData = attendanceData,
                                                backendBaseUrl = backendBaseUrl
                                            )

                                            if (!ok) {
                                                allAttendanceSuccess = false
                                                attendanceError = err
                                                break
                                            }
                                        }

                                        if (!allAttendanceSuccess) {
                                            postingError = attendanceError ?: "Failed to post attendance"
                                            posting = false
                                            return@launch
                                        }

                                        // Archive class
                                        val classJson = JSONObject()
                                        classJson.put("token", curToken)
                                        val teacherObj = JSONObject()
                                        teacherObj.put("name", teacherEmail.split("@").firstOrNull() ?: "Teacher")
                                        teacherObj.put("email", teacherEmail)
                                        classJson.put("teacher", teacherObj)
                                        classJson.put("subject", subject)

                                        val branchesJson = JSONArray()
                                        classDetails?.forEach { b ->
                                            val bObj = JSONObject()
                                            bObj.put("branchName", b.branchName)
                                            val secs = JSONArray()
                                            b.sections.forEach { s ->
                                                val sObj = JSONObject()
                                                sObj.put("sectionName", s.sectionName)
                                                sObj.put("year", s.year)
                                                val studs = JSONArray()
                                                s.students.forEach { st ->
                                                    val stuObj = JSONObject()
                                                    stuObj.put("rollNo", st.rollNo)
                                                    stuObj.put("name", st.name)
                                                    stuObj.put("present", presentMap[st.rollNo] ?: st.present)
                                                    studs.put(stuObj)
                                                }
                                                sObj.put("students", studs)
                                                secs.put(sObj)
                                            }
                                            bObj.put("sections", secs)
                                            branchesJson.put(bObj)
                                        }
                                        classJson.put("branches", branchesJson)

                                        val (archiveOk, archiveErr) = archiveClassOnServer(classJson)

                                        posting = false

                                        if (archiveOk) {
                                            postingError = null
                                            advError = null
                                            try {
                                                withContext(Dispatchers.IO) {
                                                    val db = ClassDatabase.getInstance(context)
                                                    db.classDao().insert(
                                                        ClassEntity(
                                                            token = curToken,
                                                            subject = subject,
                                                            createdAt = System.currentTimeMillis()
                                                        )
                                                    )
                                                }
                                            } catch (e: Exception) {
                                                Log.e(tag, "Failed to save: ${e.message}")
                                            }

                                            navController?.navigate(NavRoutes.TeacherHome.route) {
                                                popUpTo(0)
                                            }
                                        } else {
                                            postingError = archiveErr ?: "Failed to archive class"
                                        }
                                    }
                                },
                                enabled = !posting,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp),
                                shape = MaterialTheme.shapes.large
                            ) {
                                if (posting) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(Icons.Outlined.Archive, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Archive")
                                }
                            }
                        }
                    }
                }

                // Bottom spacer
                item {
                    Spacer(Modifier.height(20.dp))
                }
            }
        }
    }

    // Clean up advertising when composable leaves the composition
    DisposableEffect(token) {
        onDispose {
            try {
                advertiser?.stopAdvertising(advCallback)
            } catch (e: Exception) {
                Log.e(tag, "Error stopping advertising: ${e.message}")
            }

            // Delete class from server when user navigates back or app is removed from recents
            if (!token.isNullOrBlank()) {
                // Run on a background thread to avoid NetworkOnMainThreadException
                Thread {
                    try {
                        Log.d(tag, "Attempting to delete class with token: $token")
                        val url = URL("$backendBaseUrl/api/class/delete/${URLEncoder.encode(token, "UTF-8")}")
                        val conn = (url.openConnection() as HttpURLConnection).apply {
                            requestMethod = "DELETE"
                            connectTimeout = 10_000
                            readTimeout = 10_000
                        }
                        val code = conn.responseCode
                        val responseBody = try {
                            if (code in 200..299) {
                                conn.inputStream.bufferedReader().use { it.readText() }
                            } else {
                                conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                            }
                        } catch (e: Exception) {
                            ""
                        }
                        conn.disconnect()
                        
                        Log.d(tag, "Delete request completed - Code: $code, Response: $responseBody")
                        if (code in 200..299) {
                            Log.d(tag, "Class deleted from server successfully on exit (code: $code)")
                        } else {
                            Log.w(tag, "Failed to delete class from server on exit (code: $code)")
                        }
                    } catch (e: Exception) {
                        Log.e(tag, "Error deleting class on exit: ${e.message}", e)
                    }
                }.start()
            }
        }
    }
}

fun romanToInt(roman: String): Int {
    return when (roman.uppercase().trim()) {
        "I" -> 1
        "II" -> 2
        "III" -> 3
        "IV" -> 4
        else -> 0
    }
}

@Composable
fun InfoChip(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
fun AttendanceCard(student: AttendedStudent) {
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
                    imageVector = Icons.Filled.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = student.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = student.rollNo,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun EditableAttendanceCard(
    student: ClassStudent,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = if (checked)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary
                )
            )

            Spacer(Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        if (checked)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.surfaceVariant,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = null,
                    tint = if (checked)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = student.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (checked)
                        MaterialTheme.colorScheme.onSurface
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = student.rollNo,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

suspend fun postAttendanceToServer(
    year: String,
    branch: String,
    section: String,
    subject: String,
    attendanceData: List<Pair<String, Boolean>>,
    backendBaseUrl: String
): Pair<Boolean, String?> = withContext(Dispatchers.IO) {
    try {
        val url = URL("$backendBaseUrl/api/attendance")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            connectTimeout = 15_000
            readTimeout = 15_000
        }

        val bodyJson = JSONObject().apply {
            put("year", romanToInt(year))
            put("branch", branch)
            put("section", section)
            put("subject", subject)
            put("date", java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(Date()))

            val attendanceArray = JSONArray()
            attendanceData.forEach { (rollNo, isPresent) ->
                val studentAttendance = JSONObject().apply {
                    put("rollNumber", rollNo)
                    put("present", isPresent)
                }
                attendanceArray.put(studentAttendance)
            }
            put("attendance", attendanceArray)
        }

        conn.outputStream.use { os ->
            OutputStreamWriter(os, "UTF-8").use { writer ->
                writer.write(bodyJson.toString())
                writer.flush()
            }
        }

        val code = conn.responseCode
        val responseText = if (code in 200..299) {
            conn.inputStream.bufferedReader().use { it.readText() }
        } else {
            conn.errorStream?.bufferedReader()?.use { it.readText() } ?: "Unknown error"
        }
        conn.disconnect()

        return@withContext if (code in 200..299) {
            Pair(true, null)
        } else {
            Pair(false, "Server $code: $responseText")
        }

    } catch (e: Exception) {
        Log.e("AdvertisingScreen", "postAttendanceToServer error: ${e.message}")
        return@withContext Pair(false, "Client-side error: ${e.message}")
    }
}

@Preview(showBackground = true)
@Composable
fun AdvertisingScreenPreview() {
    val sectionsJson = """[
        {"year":"I","branch":"CSE","section":"A"},
        {"year":"I","branch":"CSE","section":"B"}
    ]"""

    AdvertisingScreen(
        navController = null,
        sectionsJson = sectionsJson,
        subject = "Data Structures",
        teacherEmail = "teacher@gvpce.ac.in"
    )
}