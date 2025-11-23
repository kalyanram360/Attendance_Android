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
import androidx.compose.foundation.clickable
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
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.*
import android.R.attr.tag
import android.content.pm.PackageManager

import androidx.core.content.ContextCompat


// Simple data class for attended student list (dummy)
data class AttendedStudent(val rollNo: String, val name: String)

// AdvertisingScreen composable
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvertisingScreen(
    navController: NavController? = null,
    year: String,
    branch: String,
    section: String,
    teacherEmail: String,               // pass teacher's college email (required by backend)
    backendBaseUrl: String = "https://attendance-app-backend-zr4c.onrender.com"
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val tag = "AdvertisingScreen"

    // UI state
    var token by remember { mutableStateOf<String?>(null) }
    var posting by remember { mutableStateOf(false) }
    var postingError by remember { mutableStateOf<String?>(null) }
    var advertising by remember { mutableStateOf(false) }
    var advError by remember { mutableStateOf<String?>(null) }

    // Dummy attended list (replace with real incoming attendance)
    val attended = remember { mutableStateListOf(
        AttendedStudent("18CS1001","Akhil"),
        AttendedStudent("18CS1005","Meena"),
        AttendedStudent("18CS1010","Ravi")
    ) }

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

            // Use a simple service UUID for identification; you can change to your own UUID
            val serviceUuid = ParcelUuid(UUID.fromString("0000feed-0000-1000-8000-00805f9b34fb"))

            // Put token as service data (UTF-8 bytes)
            val data = AdvertiseData.Builder()
                .addServiceData(serviceUuid, tokenValue.toByteArray(Charsets.UTF_8))
                // optionally include service UUID so scanners can filter
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
//    fun stopAdvertising() {
//        try {
//            advertiser?.stopAdvertising(advCallback)
//            advertising = false
//        } catch (e: Exception) {
//            e.printStackTrace()
//        }
//    }
    fun stopAdvertising() {
        try {
            // On Android 12+ we need BLUETOOTH_CONNECT to stop/start advertising
            val needConnectPerm = android.os.Build.VERSION.SDK_INT >= 31
            if (needConnectPerm) {
                val granted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) == PackageManager.PERMISSION_GRANTED

                if (!granted) {
                    // Not permitted — avoid calling the API (prevents SecurityException)
                    Log.w("AdvertisingScreen", "Missing BLUETOOTH_CONNECT permission; cannot stop advertising.")
                    advError = "Missing BLUETOOTH_CONNECT permission; cannot stop advertising."
                    // Optionally inform the user / trigger a permission request flow
                    return
                }
            }

            // Safe to call the advertiser API
            advertiser?.stopAdvertising(advCallback)
            advertising = false
        } catch (se: SecurityException) {
            // Defensive: handle a SecurityException if it still occurs
            Log.e("AdvertisingScreen", "SecurityException while stopping advertising: ${se.message}")
            se.printStackTrace()
            advError = "Permission required to stop advertising."
        } catch (e: Exception) {
            Log.e("AdvertisingScreen", "Error stopping advertising: ${e.message}")
            e.printStackTrace()
            advError = "Failed to stop advertising: ${e.message}"
        }
    }


    // Post class creation to backend (synchronous inside coroutine)
    // Post class creation to backend (synchronous inside coroutine)
    suspend fun postCreateClass(
        teacherEmail: String,
        branch: String,
        section: String,
        year: String,
        token: String
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

            val changedYear = romanToInt(year)
            Log.d(tag, "Converted year: $year -> $changedYear")

            val body = JSONObject().apply {
                put("teacherEmail", teacherEmail.trim())
                put("branch", branch.trim())
                put("section", section.trim())
                put("year", changedYear)  // Send as integer
                put("token", token.trim())
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



    // Permissions check (very basic). For Android 12+ you need BLUETOOTH_ADVERTISE and BLUETOOTH_CONNECT
    fun hasBluetoothAdvertisePermission(): Boolean {
        val ctx = context
        val api31 = android.os.Build.VERSION.SDK_INT >= 31
        return if (api31) {
            ctx.checkSelfPermission(Manifest.permission.BLUETOOTH_ADVERTISE) == android.content.pm.PackageManager.PERMISSION_GRANTED
                    && ctx.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            // Pre-31: coarse location might be required on some devices
            true
        }
    }

    // UI layout
    Scaffold(
        topBar = {
            HeaderWithProfile(fullname = teacherEmail.split("@").firstOrNull() ?: "T", collegeName = "GVPCE")
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Advertising Class", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))

            // Show selected class details
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Year: $year • Branch: $branch • Section: $section")
                    Text("Teacher: $teacherEmail", style = MaterialTheme.typography.bodySmall)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Advertising status & animation
            Box(modifier = Modifier
                .size(120.dp)
                .background(Color.Transparent),
                contentAlignment = Alignment.Center
            ) {
                // pulsing circle
                Box(
                    modifier = Modifier
                        .size((80 * pulse).dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f), shape = androidx.compose.foundation.shape.CircleShape)
                )
                if (advertising) {
                    Text("Advertising...", style = MaterialTheme.typography.bodyLarge)
                } else {
                    Text("Not Advertising", style = MaterialTheme.typography.bodyLarge)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Token display and actions
            token?.let {
                Text(text = "Token: $it", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (postingError != null) {
                Text("Post error: $postingError", color = MaterialTheme.colorScheme.error)
            }
            if (advError != null) {
                Text("Advertise error: $advError", color = MaterialTheme.colorScheme.error)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Button(
                    onClick = {
                        // Generate token and POST + start advertise
                        scope.launch {
                            if (!hasBluetoothAdvertisePermission()) {
                                advError = "Missing BLUETOOTH_ADVERTISE/CONNECT permission (Android 12+). Request runtime permissions."
                                return@launch
                            }

                            posting = true
                            postingError = null
                            advError = null

                            val t = makeToken()
                            token = t

                            val (ok, err) = postCreateClass(teacherEmail, branch, section, year, t)
                            posting = false
                            if (!ok) {
                                postingError = err ?: "Unknown error"
                                token = null
                                return@launch
                            }

                            // start advertising
                            val started = startBleAdvertising(t)
                            advertising = started
                            if (!started) advError = advError ?: "Failed to start advertising"
                        }
                    },
                    enabled = !advertising && !posting
                ) {
                    Text("Start Advertising")
                }

                Button(
                    onClick = {
                        stopAdvertising()
                    },
                    enabled = advertising
                ) {
                    Text("Stop")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Dummy attended list
            Text("Attended students", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(attended) { s ->
                    Card(modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)) {
                        Row(modifier = Modifier
                            .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(s.name, fontWeight = FontWeight.SemiBold)
                                Text(s.rollNo, style = MaterialTheme.typography.bodySmall)
                            }
                            Text("Present", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        } // Column
    } // Scaffold

    // Clean up advertising when composable leaves the composition
    DisposableEffect(Unit) {
        onDispose {
            try {
                advertiser?.stopAdvertising(advCallback)
            } catch (e: Exception) {
                Log.e(tag, "Error stopping advertising: ${e.message}")
            }
        }
    }
}
fun romanToInt(roman: String): Int {
    return when (roman.uppercase().trim()) {
        "I" -> {
            1
        }
        "II" -> {
            2
        }
        "III" -> {
            3
        }
        "IV" -> {
            4
        }
        else -> {

            0
        }
    }
}

