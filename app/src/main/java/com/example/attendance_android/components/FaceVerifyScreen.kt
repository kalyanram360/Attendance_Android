
//package com.example.attendance_android.components
//
//
//// UI / Compose
//import android.graphics.Bitmap
//import android.graphics.BitmapFactory
//import android.util.Log
//import android.widget.Toast
//import androidx.camera.core.CameraSelector
//import androidx.camera.core.ImageCapture
//import androidx.camera.core.ImageCaptureException
//import androidx.camera.core.ImageProxy
//import androidx.camera.view.LifecycleCameraController
//import androidx.camera.view.PreviewView
//import androidx.compose.foundation.layout.*
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.platform.LocalContext
//import androidx.compose.ui.platform.LocalLifecycleOwner
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.viewinterop.AndroidView
//import androidx.core.content.ContextCompat
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
//import kotlinx.coroutines.delay
//import java.text.SimpleDateFormat
//import java.util.*
//import kotlin.math.sqrt
//import com.google.android.gms.tasks.Task
//import kotlinx.coroutines.suspendCancellableCoroutine
//import java.nio.ByteBuffer
//import kotlin.coroutines.resume
//import kotlin.coroutines.resumeWithException
//import android.graphics.Matrix
//private const val TAG = "FaceVerify"
//fun ImageProxy.toBitmap(): Bitmap {
//    val buffer: ByteBuffer = planes[0].buffer
//    val bytes = ByteArray(buffer.remaining())
//    buffer.get(bytes)
//    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
//
//    // Mirror the bitmap for front camera
//    val matrix = Matrix().apply {
//        postScale(-1f, 1f, bitmap.width / 2f, bitmap.height / 2f)
//    }
//    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
//}
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
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun FaceVerifyScreen(
//    navController: NavController?,
//    token: String? = null,
//    onSuccessNavigateBack: () -> Unit = {}
//) {
//    val ctx = LocalContext.current
//    val lifecycleOwner = LocalLifecycleOwner.current
//    val ds = remember { DataStoreManager(ctx) }
//    val detector = remember { FaceDetection.getClient() }
//    val model = remember { FaceEmbeddingModel(ctx) }
//    val scope = rememberCoroutineScope()
//
//    var loading by remember { mutableStateOf(false) }
//    var resultText by remember { mutableStateOf<String?>(null) }
//    var similarityValue by remember { mutableStateOf<Float?>(null) }
//    var captureTriggered by remember { mutableStateOf(false) }
//
//    val cameraController = remember {
//        LifecycleCameraController(ctx).apply {
//            cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
//        }
//    }
//
//    // Auto-capture after 2 seconds delay (screen stabilization)
//    LaunchedEffect(Unit) {
//        delay(2000)
//        if (!captureTriggered && !loading) {
//            captureTriggered = true
//
//            cameraController.takePicture(
//                ContextCompat.getMainExecutor(ctx),
//                object : ImageCapture.OnImageCapturedCallback() {
//                    override fun onCaptureSuccess(image: ImageProxy) {
//                        val bitmap = image.toBitmap()
//                        image.close()
//
//                        scope.launch {
//                            loading = true
//                            resultText = null
//                            similarityValue = null
//
//                            try {
//                                // 1. detect face & crop
//                                val img = InputImage.fromBitmap(bitmap, 0)
//                                val faces = detector.process(img).awaitResult()
//                                if (faces.isEmpty()) {
//                                    resultText = "No face detected. Try again."
//                                    loading = false
//                                    return@launch
//                                }
//                                val face = faces[0]
//                                val rect = face.boundingBox
//                                val left = rect.left.coerceAtLeast(0)
//                                val top = rect.top.coerceAtLeast(0)
//                                val right = rect.right.coerceAtMost(bitmap.width)
//                                val bottom = rect.bottom.coerceAtMost(bitmap.height)
//                                if (right - left <= 0 || bottom - top <= 0) {
//                                    resultText = "Face crop failed. Try again."
//                                    loading = false
//                                    return@launch
//                                }
//                                val crop = Bitmap.createBitmap(bitmap, left, top, right - left, bottom - top)
//                                val scaled = Bitmap.createScaledBitmap(crop, 112, 112, true)
//
//                                // 2. compute embedding for current image
//                                val currentEmbedding = withContext(Dispatchers.Default) {
//                                    model.getEmbedding(scaled)
//                                }
//
//                                // 3. load stored enrollment embedding
//                                val storedEmbedding = withContext(Dispatchers.IO) {
//                                    ds.loadEmbedding()
//                                }
//
//                                if (storedEmbedding == null) {
//                                    resultText = "No enrollment found. Please enroll first."
//                                    loading = false
//                                    return@launch
//                                }
//
//                                // 4. compare
//                                val sim = cosineSimilarity(storedEmbedding, currentEmbedding)
//                                similarityValue = sim
//                                Log.d(TAG, "similarity=$sim")
//
//                                val threshold = 0.6f
//                                if (sim >= threshold) {
//                                    resultText = "Match ✓ (similarity ${"%.3f".format(sim)})"
//
//                                    // save to local PresentDatabase
//                                    try {
//                                        withContext(Dispatchers.IO) {
//                                            val dao = PresentDatabase.getInstance(ctx).presentDao()
//                                            val subject = token ?: "Class"
//                                            val present = PresentEntity(
//                                                id = 0,
//                                                subject = subject,
//                                                teacher = "Unknown",
//                                                createdAt = System.currentTimeMillis()
//                                            )
//                                            dao.insert(present)
//                                        }
//                                    } catch (e: Exception) {
//                                        Log.e(TAG, "Failed to save present: ${e.message}", e)
//                                    }
//
//                                    loading = false
//                                    delay(1500) // Show success message briefly
//                                    onSuccessNavigateBack()
//                                } else {
//                                    resultText = "No match ✗ (similarity ${"%.3f".format(sim)})"
//                                    loading = false
//                                }
//                            } catch (e: Exception) {
//                                Log.e(TAG, "verification error: ${e.message}", e)
//                                resultText = "Error: ${e.message ?: "unknown"}"
//                                loading = false
//                            }
//                        }
//                    }
//
//                    override fun onError(exception: ImageCaptureException) {
//                        Log.e(TAG, "Capture failed: ${exception.message}", exception)
//                        Toast.makeText(ctx, "Capture failed", Toast.LENGTH_SHORT).show()
//                    }
//                }
//            )
//        }
//    }
//
//    // UI
//    Scaffold(
//        topBar = {
//            CenterAlignedTopAppBar(title = { Text("Verify Face") })
//        }
//    ) { innerPadding ->
//        Box(
//            modifier = Modifier
//                .fillMaxSize()
//                .padding(innerPadding)
//        ) {
//            // Camera preview
//            AndroidView(
//                factory = { context ->
//                    PreviewView(context).apply {
//                        controller = cameraController
//                        cameraController.bindToLifecycle(lifecycleOwner)
//                    }
//                },
//                modifier = Modifier.fillMaxSize()
//            )
//
//            // Status overlay
//            Column(
//                modifier = Modifier
//                    .align(Alignment.BottomCenter)
//                    .fillMaxWidth()
//                    .padding(20.dp),
//                horizontalAlignment = Alignment.CenterHorizontally
//            ) {
//                Card(
//                    colors = CardDefaults.cardColors(
//                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
//                    )
//                ) {
//                    Column(
//                        modifier = Modifier.padding(16.dp),
//                        horizontalAlignment = Alignment.CenterHorizontally
//                    ) {
//                        if (loading) {
//                            CircularProgressIndicator()
//                            Spacer(Modifier.height(8.dp))
//                            Text("Checking face...")
//                        } else if (resultText != null) {
//                            Text(resultText ?: "", style = MaterialTheme.typography.titleMedium)
//                            similarityValue?.let { sim ->
//                                Spacer(Modifier.height(4.dp))
//                                Text("Similarity: ${"%.3f".format(sim)}", style = MaterialTheme.typography.bodySmall)
//                            }
//                        } else {
//                            Text("Position your face in the frame", style = MaterialTheme.typography.bodyMedium)
//                            Text("Capturing...", style = MaterialTheme.typography.bodySmall)
//                        }
//                    }
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
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

enum class VerificationState {
    IDLE, PROCESSING, SUCCESS, FAILURE, NO_FACE, ERROR
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

    var verificationState by remember { mutableStateOf(VerificationState.IDLE) }
    var resultMessage by remember { mutableStateOf("") }
    var similarityValue by remember { mutableStateOf<Float?>(null) }
    var captureTriggered by remember { mutableStateOf(false) }

    val cameraController = remember {
        LifecycleCameraController(ctx).apply {
            cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
        }
    }

    fun captureAndVerify() {
        if (verificationState == VerificationState.PROCESSING) return

        captureTriggered = true
        verificationState = VerificationState.PROCESSING
        resultMessage = ""
        similarityValue = null

        cameraController.takePicture(
            ContextCompat.getMainExecutor(ctx),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    val bitmap = image.toBitmap()
                    image.close()

                    scope.launch {
                        try {
                            // 1. detect face & crop
                            val img = InputImage.fromBitmap(bitmap, 0)
                            val faces = detector.process(img).awaitResult()

                            if (faces.isEmpty()) {
                                verificationState = VerificationState.NO_FACE
                                resultMessage = "No face detected"
                                return@launch
                            }

                            val face = faces[0]
                            val rect = face.boundingBox
                            val left = rect.left.coerceAtLeast(0)
                            val top = rect.top.coerceAtLeast(0)
                            val right = rect.right.coerceAtMost(bitmap.width)
                            val bottom = rect.bottom.coerceAtMost(bitmap.height)

                            if (right - left <= 0 || bottom - top <= 0) {
                                verificationState = VerificationState.ERROR
                                resultMessage = "Face crop failed"
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
                                verificationState = VerificationState.ERROR
                                resultMessage = "No enrollment found"
                                return@launch
                            }

                            // 4. compare
                            val sim = cosineSimilarity(storedEmbedding, currentEmbedding)
                            similarityValue = sim
                            Log.d(TAG, "similarity=$sim")

                            val threshold = 0.6f
                            if (sim >= threshold) {
                                verificationState = VerificationState.SUCCESS
                                resultMessage = "Face matched successfully!"

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

                                delay(2000) // Show success message
                                onSuccessNavigateBack()
                            } else {
                                verificationState = VerificationState.FAILURE
                                resultMessage = "Face doesn't match"
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "verification error: ${e.message}", e)
                            verificationState = VerificationState.ERROR
                            resultMessage = "Verification error occurred"
                        }
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e(TAG, "Capture failed: ${exception.message}", exception)
                    verificationState = VerificationState.ERROR
                    resultMessage = "Camera capture failed"
                }
            }
        )
    }

    // Auto-capture after 2 seconds delay (screen stabilization)
    LaunchedEffect(Unit) {
        delay(2000)
        if (!captureTriggered) {
            captureAndVerify()
        }
    }

    // UI
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .navigationBarsPadding()
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

        // Face frame overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(40.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .border(
                        width = 3.dp,
                        color = when (verificationState) {
                            VerificationState.SUCCESS -> Color(0xFF4CAF50)
                            VerificationState.FAILURE, VerificationState.NO_FACE, VerificationState.ERROR -> Color(0xFFF44336)
                            VerificationState.PROCESSING -> Color(0xFFFFEB3B)
                            else -> Color.White
                        },
                        shape = RoundedCornerShape(24.dp)
                    )
            )
        }

        // Top instruction
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.Black.copy(alpha = 0.7f)
            ) {
                Text(
                    text = when (verificationState) {
                        VerificationState.IDLE -> "Position your face in the frame"
                        VerificationState.PROCESSING -> "Analyzing face..."
                        VerificationState.SUCCESS -> "Verification successful!"
                        VerificationState.FAILURE -> "Face doesn't match"
                        VerificationState.NO_FACE -> "No face detected"
                        VerificationState.ERROR -> "Verification failed"
                    },
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Bottom status card
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AnimatedVisibility(
                visible = verificationState != VerificationState.IDLE,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        when (verificationState) {
                            VerificationState.PROCESSING -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(48.dp),
                                    strokeWidth = 4.dp
                                )
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    "Verifying your face...",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            VerificationState.SUCCESS -> {
                                Surface(
                                    modifier = Modifier.size(64.dp),
                                    shape = CircleShape,
                                    color = Color(0xFF4CAF50).copy(alpha = 0.2f)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = Color(0xFF4CAF50),
                                            modifier = Modifier.size(40.dp)
                                        )
                                    }
                                }
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    resultMessage,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF4CAF50)
                                )
                                similarityValue?.let {
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        "Match: ${(it * 100).toInt()}%",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            VerificationState.FAILURE, VerificationState.NO_FACE, VerificationState.ERROR -> {
                                Surface(
                                    modifier = Modifier.size(64.dp),
                                    shape = CircleShape,
                                    color = Color(0xFFF44336).copy(alpha = 0.2f)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = null,
                                            tint = Color(0xFFF44336),
                                            modifier = Modifier.size(40.dp)
                                        )
                                    }
                                }
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    resultMessage,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFF44336)
                                )

                                similarityValue?.let {
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        "Match: ${(it * 100).toInt()}%",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Spacer(Modifier.height(20.dp))

                                // Retry Button
                                Button(
                                    onClick = {
                                        captureTriggered = false
                                        captureAndVerify()
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "Try Again",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            else -> {}
                        }
                    }
                }
            }
        }
    }
}