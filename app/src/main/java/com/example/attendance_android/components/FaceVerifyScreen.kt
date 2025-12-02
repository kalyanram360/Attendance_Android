//package com.example.attendance_android.components
//
//
//// UI / Compose
//import android.graphics.Bitmap
//import android.util.Log
//import android.widget.Toast
//import androidx.activity.compose.rememberLauncherForActivityResult
//import androidx.activity.result.contract.ActivityResultContracts
//import androidx.compose.foundation.layout.*
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.platform.LocalContext
//import androidx.compose.ui.unit.dp
//import androidx.navigation.NavController
//import com.example.attendance_android.data.DataStoreManager
//import com.example.attendance_android.data.PresentDatabase
//import com.example.attendance_android.data.PresentEntity
//import com.example.attendance_android.ml.FaceEmbeddingModel
//import com.google.mlkit.vision.common.InputImage
//import com.google.mlkit.vision.face.FaceDetection
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.withContext
//import kotlinx.coroutines.launch
//import java.text.SimpleDateFormat
//import java.util.*
//import kotlin.math.sqrt
//import com.google.android.gms.tasks.Task
//import kotlinx.coroutines.suspendCancellableCoroutine
//import kotlin.coroutines.resume
//import kotlin.coroutines.resumeWithException
//
//private const val TAG = "FaceVerify"
//
//suspend fun <T> Task<T>.awaitResult(): T = suspendCancellableCoroutine { cont ->
//    addOnSuccessListener { r -> if (!cont.isCancelled) cont.resume(r) }
//    addOnFailureListener { e -> if (!cont.isCancelled) cont.resumeWithException(e) }
//    addOnCanceledListener { if (!cont.isCancelled) cont.cancel() }
//}
//
//fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
//    var dot = 0f
//    var magA = 0f
//    var magB = 0f
//    val n = minOf(a.size, b.size)
//    for (i in 0 until n) {
//        dot += a[i] * b[i]
//        magA += a[i] * a[i]
//        magB += b[i] * b[i]
//    }
//    return dot / (sqrt(magA) * sqrt(magB) + 1e-10f)
//}
//
///**
// * FaceVerifyScreen
// *
// * Route param: expects token (class token) as String argument if you want to pass it.
// *
// * Usage: navController.navigate("face_verify/$token")
// */
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun FaceVerifyScreen(
//    navController: NavController?,
//    token: String? = null,                // optional: class token to send to backend
//    onSuccessNavigateBack: () -> Unit = {}
//) {
//    val ctx = LocalContext.current
//    val ds = remember { DataStoreManager(ctx) }
//    val detector = remember { FaceDetection.getClient() }
//    val model = remember { FaceEmbeddingModel(ctx) }
//    val scope = rememberCoroutineScope()
//
//    var loading by remember { mutableStateOf(false) }
//    var resultText by remember { mutableStateOf<String?>(null) }
//    var similarityValue by remember { mutableStateOf<Float?>(null) }
//
//    // capture preview launcher
//    val takePreviewLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap: Bitmap? ->
//        if (bitmap == null) {
//            Toast.makeText(ctx, "Capture failed", Toast.LENGTH_SHORT).show()
//            return@rememberLauncherForActivityResult
//        }
//
//        scope.launch {
//            loading = true
//            resultText = null
//            similarityValue = null
//
//            try {
//                // 1. detect face & crop
//                val img = InputImage.fromBitmap(bitmap, 0)
//                val faces = detector.process(img).awaitResult()
//                if (faces.isEmpty()) {
//                    resultText = "No face detected. Try again."
//                    loading = false
//                    return@launch
//                }
//                val face = faces[0]
//                val rect = face.boundingBox
//                val left = rect.left.coerceAtLeast(0)
//                val top = rect.top.coerceAtLeast(0)
//                val right = rect.right.coerceAtMost(bitmap.width)
//                val bottom = rect.bottom.coerceAtMost(bitmap.height)
//                if (right - left <= 0 || bottom - top <= 0) {
//                    resultText = "Face crop failed. Try again."
//                    loading = false
//                    return@launch
//                }
//                val crop = Bitmap.createBitmap(bitmap, left, top, right - left, bottom - top)
//                val scaled = Bitmap.createScaledBitmap(crop, 112, 112, true)
//
//                // 2. compute embedding for current image
//                val currentEmbedding = withContext(Dispatchers.Default) {
//                    model.getEmbedding(scaled)
//                }
//
//                // 3. load stored enrollment embedding (suspend)
//                val storedEmbedding = withContext(Dispatchers.IO) {
//                    ds.loadEmbedding()
//                }
//
//                if (storedEmbedding == null) {
//                    resultText = "No enrollment found. Please enroll first."
//                    loading = false
//                    return@launch
//                }
//
//                // 4. compare
//                val sim = cosineSimilarity(storedEmbedding, currentEmbedding)
//                similarityValue = sim
//                Log.d(TAG, "similarity=$sim")
//
//                // threshold — tune if needed (your Constants.THRESHOLD)
//                val threshold = 0.6f
//                if (sim >= threshold) {
//                    // success: mark attendance locally + optionally call backend
//                    resultText = "Match ✓ (similarity ${"%.3f".format(sim)})"
//
//                    // save to local PresentDatabase
//                    try {
//                        withContext(Dispatchers.IO) {
//                            val dao = PresentDatabase.getInstance(ctx).presentDao()
//                            val now = System.currentTimeMillis()
//                            val fmt = SimpleDateFormat("dd MMM yyyy • HH:mm", Locale.getDefault())
//                            val subject = token ?: "Class"
//                            val present = PresentEntity(
//                                id = 0, // auto-generate if using @PrimaryKey(autoGenerate = true)
//                                subject = subject,
//                                teacher = "Unknown",
//                                createdAt = now
//                            )
//                            dao.insert(present)
//                        }
//                    } catch (e: Exception) {
//                        Log.e(TAG, "Failed to save present: ${e.message}", e)
//                    }
//
//                    // OPTIONAL: do backend mark attendance call here using token, student email/roll etc.
//                    // Example: launch background coroutine to call your existing endpoint.
//
//                    // navigate back or show success
//                    loading = false
//                    // small delay or immediate navigation - choose UX you want
//                    onSuccessNavigateBack()
//                } else {
//                    resultText = "No match ✗ (similarity ${"%.3f".format(sim)})"
//                    loading = false
//                }
//            } catch (e: Exception) {
//                Log.e(TAG, "verification error: ${e.message}", e)
//                resultText = "Error: ${e.message ?: "unknown"}"
//                loading = false
//            }
//        }
//    }
//
//    // UI
//    Scaffold(
//        topBar = {
//            CenterAlignedTopAppBar(title = { Text("Verify Face") })
//        }
//    ) { innerPadding ->
//        Column(
//            modifier = Modifier
//                .fillMaxSize()
//                .padding(innerPadding)
//                .padding(20.dp),
//            horizontalAlignment = Alignment.CenterHorizontally,
//            verticalArrangement = Arrangement.Center
//        ) {
//            if (loading) {
//                CircularProgressIndicator()
//                Spacer(Modifier.height(12.dp))
//                Text("Checking face...")
//            } else {
//                Button(onClick = { takePreviewLauncher.launch(null) }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
//                    Text("Capture & Verify")
//                }
//                Spacer(Modifier.height(12.dp))
//
//                if (resultText != null) {
//                    Text(resultText ?: "", style = MaterialTheme.typography.titleMedium)
//                    Spacer(Modifier.height(8.dp))
//                    similarityValue?.let { sim ->
//                        Text("Similarity: ${"%.3f".format(sim)}", style = MaterialTheme.typography.bodySmall)
//                    }
//                } else {
//                    Text("Tap capture to take a photo and verify against your enrolled face.", style = MaterialTheme.typography.bodyMedium, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
//                }
//            }
//        }
//    }
//}
package com.example.attendance_android.components


// UI / Compose
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.example.attendance_android.data.DataStoreManager
import com.example.attendance_android.data.PresentDatabase
import com.example.attendance_android.data.PresentEntity
import com.example.attendance_android.ml.FaceEmbeddingModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.sqrt
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.suspendCancellableCoroutine
import java.nio.ByteBuffer
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import android.graphics.Matrix
private const val TAG = "FaceVerify"
fun ImageProxy.toBitmap(): Bitmap {
    val buffer: ByteBuffer = planes[0].buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

    // Mirror the bitmap for front camera
    val matrix = Matrix().apply {
        postScale(-1f, 1f, bitmap.width / 2f, bitmap.height / 2f)
    }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}

suspend fun <T> Task<T>.awaitResult(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { r -> if (!cont.isCancelled) cont.resume(r) }
    addOnFailureListener { e -> if (!cont.isCancelled) cont.resumeWithException(e) }
    addOnCanceledListener { if (!cont.isCancelled) cont.cancel() }
}

fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
    var dot = 0f
    var magA = 0f
    var magB = 0f
    val n = minOf(a.size, b.size)
    for (i in 0 until n) {
        dot += a[i] * b[i]
        magA += a[i] * a[i]
        magB += b[i] * b[i]
    }
    return dot / (sqrt(magA) * sqrt(magB) + 1e-10f)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FaceVerifyScreen(
    navController: NavController?,
    token: String? = null,
    onSuccessNavigateBack: () -> Unit = {}
) {
    val ctx = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val ds = remember { DataStoreManager(ctx) }
    val detector = remember { FaceDetection.getClient() }
    val model = remember { FaceEmbeddingModel(ctx) }
    val scope = rememberCoroutineScope()

    var loading by remember { mutableStateOf(false) }
    var resultText by remember { mutableStateOf<String?>(null) }
    var similarityValue by remember { mutableStateOf<Float?>(null) }
    var captureTriggered by remember { mutableStateOf(false) }

    val cameraController = remember {
        LifecycleCameraController(ctx).apply {
            cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
        }
    }

    // Auto-capture after 2 seconds delay (screen stabilization)
    LaunchedEffect(Unit) {
        delay(2000)
        if (!captureTriggered && !loading) {
            captureTriggered = true

            cameraController.takePicture(
                ContextCompat.getMainExecutor(ctx),
                object : ImageCapture.OnImageCapturedCallback() {
                    override fun onCaptureSuccess(image: ImageProxy) {
                        val bitmap = image.toBitmap()
                        image.close()

                        scope.launch {
                            loading = true
                            resultText = null
                            similarityValue = null

                            try {
                                // 1. detect face & crop
                                val img = InputImage.fromBitmap(bitmap, 0)
                                val faces = detector.process(img).awaitResult()
                                if (faces.isEmpty()) {
                                    resultText = "No face detected. Try again."
                                    loading = false
                                    return@launch
                                }
                                val face = faces[0]
                                val rect = face.boundingBox
                                val left = rect.left.coerceAtLeast(0)
                                val top = rect.top.coerceAtLeast(0)
                                val right = rect.right.coerceAtMost(bitmap.width)
                                val bottom = rect.bottom.coerceAtMost(bitmap.height)
                                if (right - left <= 0 || bottom - top <= 0) {
                                    resultText = "Face crop failed. Try again."
                                    loading = false
                                    return@launch
                                }
                                val crop = Bitmap.createBitmap(bitmap, left, top, right - left, bottom - top)
                                val scaled = Bitmap.createScaledBitmap(crop, 112, 112, true)

                                // 2. compute embedding for current image
                                val currentEmbedding = withContext(Dispatchers.Default) {
                                    model.getEmbedding(scaled)
                                }

                                // 3. load stored enrollment embedding
                                val storedEmbedding = withContext(Dispatchers.IO) {
                                    ds.loadEmbedding()
                                }

                                if (storedEmbedding == null) {
                                    resultText = "No enrollment found. Please enroll first."
                                    loading = false
                                    return@launch
                                }

                                // 4. compare
                                val sim = cosineSimilarity(storedEmbedding, currentEmbedding)
                                similarityValue = sim
                                Log.d(TAG, "similarity=$sim")

                                val threshold = 0.6f
                                if (sim >= threshold) {
                                    resultText = "Match ✓ (similarity ${"%.3f".format(sim)})"

                                    // save to local PresentDatabase
                                    try {
                                        withContext(Dispatchers.IO) {
                                            val dao = PresentDatabase.getInstance(ctx).presentDao()
                                            val subject = token ?: "Class"
                                            val present = PresentEntity(
                                                id = 0,
                                                subject = subject,
                                                teacher = "Unknown",
                                                createdAt = System.currentTimeMillis()
                                            )
                                            dao.insert(present)
                                        }
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Failed to save present: ${e.message}", e)
                                    }

                                    loading = false
                                    delay(1500) // Show success message briefly
                                    onSuccessNavigateBack()
                                } else {
                                    resultText = "No match ✗ (similarity ${"%.3f".format(sim)})"
                                    loading = false
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "verification error: ${e.message}", e)
                                resultText = "Error: ${e.message ?: "unknown"}"
                                loading = false
                            }
                        }
                    }

                    override fun onError(exception: ImageCaptureException) {
                        Log.e(TAG, "Capture failed: ${exception.message}", exception)
                        Toast.makeText(ctx, "Capture failed", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
    }

    // UI
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(title = { Text("Verify Face") })
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Camera preview
            AndroidView(
                factory = { context ->
                    PreviewView(context).apply {
                        controller = cameraController
                        cameraController.bindToLifecycle(lifecycleOwner)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // Status overlay
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (loading) {
                            CircularProgressIndicator()
                            Spacer(Modifier.height(8.dp))
                            Text("Checking face...")
                        } else if (resultText != null) {
                            Text(resultText ?: "", style = MaterialTheme.typography.titleMedium)
                            similarityValue?.let { sim ->
                                Spacer(Modifier.height(4.dp))
                                Text("Similarity: ${"%.3f".format(sim)}", style = MaterialTheme.typography.bodySmall)
                            }
                        } else {
                            Text("Position your face in the frame", style = MaterialTheme.typography.bodyMedium)
                            Text("Capturing...", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}