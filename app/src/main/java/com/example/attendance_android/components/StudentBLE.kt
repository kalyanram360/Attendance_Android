package com.example.attendance_android.components

// ---------- Imports ----------
import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.*
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import androidx.core.content.ContextCompat
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.CircleShape

// ---------- Data classes ----------
data class StudentPresent(val rollNo: String, val name: String)

// ---------- Composable ----------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentBleScreen(
    navController: NavController? = null,
    tokenToMatch: String,                     // token obtained from current class
    studentRollNo: String,                    // student's roll number (from DataStore/login)
    backendBaseUrl: String = "https://attendance-app-backend-zr4c.onrender.com"
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val tag = "StudentBleScreen"

    // BLE objects
    val btAdapter = remember { BluetoothAdapter.getDefaultAdapter() }
    val scanner = remember { btAdapter?.bluetoothLeScanner }

    // UI state
    var scanning by remember { mutableStateOf(false) }
    var scanError by remember { mutableStateOf<String?>(null) }
    var advFoundText by remember { mutableStateOf<String?>(null) }
    var attendanceMarked by remember { mutableStateOf(false) }

    // list of attended students (you can populate this from backend response)
    val attended = remember { mutableStateListOf<StudentPresent>() }

    // pulsing animation
    val infinite = rememberInfiniteTransition()
    val pulse by infinite.animateFloat(
        initialValue = 0.7f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse)
    )

    // Define the service UUID used by teacher advertising (must match teacher's UUID)
    val serviceUuid = ParcelUuid.fromString("0000feed-0000-1000-8000-00805f9b34fb")

    // Mutable reference to the scan callback (allows self-reference inside the callback)
    var scanCallback: ScanCallback? = null

    // Build a ScanCallback that checks service data for the token
    scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            try {
                val record = result.scanRecord ?: return
                // get service data for the UUID
                val data = record.getServiceData(serviceUuid)
                if (data != null) {
                    val stringInAdv = String(data, Charsets.UTF_8)
                    Log.d(tag, "adv serviceData: $stringInAdv from ${result.device.address}")

                    // Compare token (exact match). Trim whitespace to be safe.
                    if (stringInAdv.trim() == tokenToMatch.trim() && !attendanceMarked) {
                        // matched — update UI and call backend to mark attendance
                        advFoundText = "Found teacher device! Marking attendance..."

                        scope.launch {
                            val response = markAttendance(
                                backendBaseUrl = backendBaseUrl,
                                token = tokenToMatch,
                                studentRoll = studentRollNo
                            )

                            if (response != null && response.success) {
                                attendanceMarked = true
                                advFoundText = "✓ Attendance marked successfully!"

                                // Add to attended list from response
                                response.studentData?.let { student ->
                                    attended.add(StudentPresent(student.rollNo, student.name))
                                }

                                // Stop scanning after successful mark
                                try {
                                    // Check BLUETOOTH_CONNECT permission before stopping scan
                                    if (Build.VERSION.SDK_INT >= 31) {
                                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                                            scanner?.stopScan(scanCallback)
                                        }
                                    } else {
                                        scanner?.stopScan(scanCallback)
                                    }
                                    scanning = false
                                } catch (e: Exception) {
                                    Log.e(tag, "Error stopping scan", e)
                                }
                            } else {
                                scanError = response?.message ?: "Failed to mark attendance"
                                advFoundText = null
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(tag, "scan callback error: ${e.message}")
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e(tag, "scan failed code=$errorCode")
            scanError = "Scan failed: $errorCode"
        }
    }

    // Permission check helper
    fun hasScanPermissions(ctx: Context): Boolean {
        val api31 = Build.VERSION.SDK_INT >= 31
        return if (api31) {
            val adv = ContextCompat.checkSelfPermission(ctx, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
            val conn = ContextCompat.checkSelfPermission(ctx, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
            adv && conn
        } else {
            // pre-31 some devices need location permission to scan
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        }
    }

    // Start scanning function
    fun startScan() {
        if (scanner == null) {
            scanError = "Bluetooth LE scanner not available"
            return
        }

        if (!hasScanPermissions(context)) {
            scanError = "Missing BLE scan permissions. Request them from the user."
            return
        }

        try {
            val filters = listOf(
                ScanFilter.Builder()
                    .setServiceUuid(serviceUuid)
                    .build()
            )
            val settings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build()

            scanner.startScan(filters, settings, scanCallback)
            scanning = true
            scanError = null
            attendanceMarked = false
            advFoundText = null
            Log.d(tag, "scanner started")
        } catch (se: SecurityException) {
            scanError = "SecurityException: missing runtime permission"
            Log.e(tag, "startScan SecurityException", se)
        } catch (e: Exception) {
            scanError = "Failed to start scan: ${e.message}"
            Log.e(tag, "startScan", e)
        }
    }

    // Stop scanning
    fun stopScan() {
        try {
            // Check BLUETOOTH_CONNECT permission before stopping scan
            if (Build.VERSION.SDK_INT >= 31) {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                    scanner?.stopScan(scanCallback)
                }
            } else {
                scanner?.stopScan(scanCallback)
            }
        } catch (se: SecurityException) {
            Log.e(tag, "stopScan SecurityException", se)
        } catch (e: Exception) {
            Log.e(tag, "stopScan exception", e)
        } finally {
            scanning = false
        }
    }

    // Clean up when composable leaves composition
    DisposableEffect(Unit) {
        onDispose {
            stopScan()
        }
    }

    // UI
    Scaffold(
        topBar = {
            HeaderWithProfile(fullname = "You", collegeName = "GVPCE", navController = navController)
        },
        bottomBar = {
            FooterNavPrimary(
                onHome = { /* */ },
                onClasses = { /* */ },
                onSettings = { /* */ },
                selected = "HOME"
            )
        }
    ) { innerPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Scan to mark attendance", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))

            // pulsing circle
            Box(
                modifier = Modifier
                    .size((90 * pulse).dp)
                    .background(
                        if (attendanceMarked)
                            Color.Green.copy(alpha = 0.15f)
                        else
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (attendanceMarked) "Marked!"
                    else if (scanning) "Scanning..."
                    else "Not scanning",
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            Spacer(Modifier.height(12.dp))

            if (!hasScanPermissions(context)) {
                Text("BLE scan permission missing — request runtime permissions.", color = Color.Red)
            }

            scanError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            advFoundText?.let { Text(it, color = if (attendanceMarked) Color.Green else MaterialTheme.colorScheme.primary) }

            Spacer(Modifier.height(12.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Button(onClick = { startScan() }, enabled = !scanning && !attendanceMarked) {
                    Text(if (attendanceMarked) "Already Marked" else "Start Scanning")
                }
                Button(onClick = { stopScan() }, enabled = scanning) { Text("Stop") }
            }

            Spacer(Modifier.height(16.dp))

            if (attended.isNotEmpty()) {
                Text("Marked Present", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))

                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(attended) { s ->
                        Card(modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(s.name, fontWeight = FontWeight.SemiBold)
                                    Text(s.rollNo, style = MaterialTheme.typography.bodySmall)
                                }
                                Text("Present", color = Color.Green, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------- Data classes for API response ----------
data class AttendanceResponse(
    val success: Boolean,
    val message: String,
    val studentData: StudentData?
)

data class StudentData(
    val rollNo: String,
    val name: String,
    val present: Boolean,
    val branch: String,
    val section: String,
    val year: Int
)

// ---------- Helper: PATCH request to mark attendance ----------
suspend fun markAttendance(
    backendBaseUrl: String,
    token: String,
    studentRoll: String
): AttendanceResponse? = withContext(Dispatchers.IO) {
    try {
        // URL encode the roll number to handle special characters
        val encodedRoll = URLEncoder.encode(studentRoll, "UTF-8")
        val urlString = "$backendBaseUrl/api/class/$token/mark/$encodedRoll"

        Log.d("markAttendance", "Calling: $urlString")

        val url = URL(urlString)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "PATCH"
            setRequestProperty("Content-Type", "application/json")
            connectTimeout = 10_000
            readTimeout = 10_000
        }

        val code = conn.responseCode
        val text = if (code in 200..299) {
            conn.inputStream.bufferedReader().use { it.readText() }
        } else {
            conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
        }

        conn.disconnect()

        Log.d("markAttendance", "Response code=$code text=$text")

        // Parse the response
        val json = JSONObject(text)
        val success = json.optBoolean("success", false)
        val message = json.optString("message", "Unknown error")

        val studentData = if (success && json.has("data")) {
            val data = json.getJSONObject("data")
            val student = data.getJSONObject("student")
            StudentData(
                rollNo = student.getString("rollNo"),
                name = student.getString("name"),
                present = student.getBoolean("present"),
                branch = student.getString("branch"),
                section = student.getString("section"),
                year = student.getInt("year")
            )
        } else null

        return@withContext AttendanceResponse(success, message, studentData)
    } catch (e: Exception) {
        Log.e("markAttendance", "Error", e)
        return@withContext AttendanceResponse(false, "Network error: ${e.message}", null)
    }
}