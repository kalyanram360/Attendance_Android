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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.attendance_android.data.PresentDatabase
import com.example.attendance_android.data.PresentEntity
import androidx.compose.ui.tooling.preview.Preview

// ---------- Data classes ----------
data class StudentPresent(val rollNo: String, val name: String)

// ---------- Composable ----------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentBleScreen(
    navController: NavController? = null,
    tokenToMatch: String,
    studentRollNo: String,
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

    val attended = remember { mutableStateListOf<StudentPresent>() }

    // Enhanced animations
    val infinite = rememberInfiniteTransition(label = "pulse")
    val pulse by infinite.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val rotation by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val serviceUuid = ParcelUuid.fromString("0000feed-0000-1000-8000-00805f9b34fb")
    var scanCallback: ScanCallback? = null

    scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            try {
                val record = result.scanRecord ?: return
                val data = record.getServiceData(serviceUuid)
                if (data != null) {
                    val stringInAdv = String(data, Charsets.UTF_8)
                    Log.d(tag, "adv serviceData: $stringInAdv from ${result.device.address}")

                    if (stringInAdv.trim() == tokenToMatch.trim() && !attendanceMarked) {
                        attendanceMarked = true

                        try {
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

                        advFoundText = "Found teacher device! Marking attendance..."

                        scope.launch {
                            val response = markAttendance(
                                backendBaseUrl = backendBaseUrl,
                                token = tokenToMatch,
                                studentRoll = studentRollNo
                            )

                            if (response != null && response.success) {
                                advFoundText = "✓ Attendance marked successfully!"

                                response.studentData?.let { student ->
                                    attended.add(StudentPresent(student.rollNo, student.name))
                                }

                                scope.launch {
                                    try {
                                        withContext(Dispatchers.IO) {
                                            val db = PresentDatabase.getInstance(context)
                                            val presentEntity = PresentEntity(
                                                subject = response.studentData?.Subject ?: "Unknown",
                                                teacher = response.studentData?.Teacher ?: "Unknown",
                                                createdAt = System.currentTimeMillis()
                                            )
                                            db.presentDao().insert(presentEntity)
                                            Log.d(tag, "Attendance record saved to local DB")
                                        }
                                    } catch (e: Exception) {
                                        Log.e(tag, "Failed to save attendance to local DB: ${e.message}")
                                    }
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

    fun hasScanPermissions(ctx: Context): Boolean {
        val api31 = Build.VERSION.SDK_INT >= 31
        return if (api31) {
            val adv = ContextCompat.checkSelfPermission(ctx, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
            val conn = ContextCompat.checkSelfPermission(ctx, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
            adv && conn
        } else {
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        }
    }

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

    fun stopScan() {
        try {
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    )
                )
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                contentPadding = PaddingValues(16.dp)
            ) {
                item {
                    Spacer(Modifier.height(8.dp))

                    // Header Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bluetooth,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Mark Your Attendance",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Scan to find nearby teacher device",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }

                    // Scanning Status Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 20.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Animated scanning indicator
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(vertical = 16.dp)
                            ) {
                                // Outer pulsing ring
                                Box(
                                    modifier = Modifier
                                        .size((140 * pulse).dp)
                                        .background(
                                            when {
                                                attendanceMarked -> Color(0xFF4CAF50).copy(alpha = 0.1f)
                                                scanning -> MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                                else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                                            },
                                            shape = CircleShape
                                        )
                                )

                                // Middle ring
                                Box(
                                    modifier = Modifier
                                        .size(100.dp)
                                        .background(
                                            when {
                                                attendanceMarked -> Color(0xFF4CAF50).copy(alpha = 0.2f)
                                                scanning -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                                else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                                            },
                                            shape = CircleShape
                                        )
                                )

                                // Inner circle with icon
                                Surface(
                                    modifier = Modifier.size(70.dp),
                                    shape = CircleShape,
                                    color = when {
                                        attendanceMarked -> Color(0xFF4CAF50)
                                        scanning -> MaterialTheme.colorScheme.primary
                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                    },
                                    shadowElevation = 8.dp
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = when {
                                                attendanceMarked -> Icons.Default.CheckCircle
                                                scanning -> Icons.Default.Bluetooth
                                                else -> Icons.Default.BluetoothDisabled
                                            },
                                            contentDescription = null,
                                            modifier = Modifier.size(36.dp),
                                            tint = when {
                                                attendanceMarked -> Color.White
                                                scanning -> Color.White
                                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                                            }
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.height(16.dp))

                            // Status text
                            Text(
                                text = when {
                                    attendanceMarked -> "Attendance Marked!"
                                    scanning -> "Scanning for devices..."
                                    else -> "Ready to scan"
                                },
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    attendanceMarked -> Color(0xFF4CAF50)
                                    scanning -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.onSurface
                                }
                            )

                            // Permission warning
                            if (!hasScanPermissions(context)) {
                                Spacer(Modifier.height(16.dp))
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            "BLE permissions required",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    }
                                }
                            }

                            // Error message
                            scanError?.let { error ->
                                Spacer(Modifier.height(12.dp))
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Error,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            error,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    }
                                }
                            }

                            // Success message
                            advFoundText?.let { text ->
                                Spacer(Modifier.height(12.dp))
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (attendanceMarked)
                                            Color(0xFF4CAF50).copy(alpha = 0.1f)
                                        else
                                            MaterialTheme.colorScheme.primaryContainer
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            if (attendanceMarked) Icons.Default.CheckCircle else Icons.Default.Info,
                                            contentDescription = null,
                                            tint = if (attendanceMarked) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (attendanceMarked) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onPrimaryContainer,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.height(20.dp))

                            // Action buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                FilledTonalButton(
                                    onClick = { startScan() },
                                    enabled = !scanning && !attendanceMarked,
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(vertical = 16.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.BluetoothSearching,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        if (attendanceMarked) "Marked" else "Start Scan",
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                OutlinedButton(
                                    onClick = { stopScan() },
                                    enabled = scanning,
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(vertical = 16.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Stop,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text("Stop", fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }

                    // Attended students list
                    if (attended.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Attendance Recorded",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }
                }

                items(attended) { student ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF4CAF50).copy(alpha = 0.05f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                modifier = Modifier.size(48.dp),
                                shape = CircleShape,
                                color = Color(0xFF4CAF50).copy(alpha = 0.15f)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Person,
                                        contentDescription = null,
                                        tint = Color(0xFF4CAF50),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }

                            Spacer(Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    student.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    student.rollNo,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF4CAF50)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        "Present",
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
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
    val year: Int,
    val Subject: String,
    val Teacher: String
)

// ---------- Helper: PATCH request to mark attendance ----------
suspend fun markAttendance(
    backendBaseUrl: String,
    token: String,
    studentRoll: String
): AttendanceResponse? = withContext(Dispatchers.IO) {
    try {
        val encodedRoll = URLEncoder.encode(studentRoll, "UTF-8")
        val urlString = "$backendBaseUrl/api/class/mark/$token/$encodedRoll"

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
                year = student.getInt("year"),
                Subject = student.getString("Subject"),
                Teacher = student.getString("Teacher")
            )
        } else null

        return@withContext AttendanceResponse(success, message, studentData)
    } catch (e: Exception) {
        Log.e("markAttendance", "Error", e)
        return@withContext AttendanceResponse(false, "Network error: ${e.message}", null)
    }
}

@Composable
@Preview(showBackground = true)
fun StudentBleScreenPreview() {
    StudentBleScreen(
        navController = null,
        tokenToMatch = "sample-token-123",
        studentRollNo = "21A91A05"
    )
}