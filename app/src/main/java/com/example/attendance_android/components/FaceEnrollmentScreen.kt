package com.example.attendance_android.components


import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.attendance_android.ml.FaceEmbeddingModel
import com.example.attendance_android.data.DataStoreManager
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.Face
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import com.example.attendance_android.utils.await
import com.example.attendance_android.utils.Constants

@Composable
fun FaceEnrollmentScreen(
    onEnrolled: () -> Unit,
) {
    val context = LocalContext.current
    val dataStore = remember { DataStoreManager(context) }
    val embeddingModel = remember { FaceEmbeddingModel(context) }
    val detector = remember { FaceDetection.getClient() }
    val scope = rememberCoroutineScope()

    var loading by remember { mutableStateOf(false) }
    val takePictureLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap: Bitmap? ->
        if (bitmap == null) {
            Toast.makeText(context, "No image captured", Toast.LENGTH_SHORT).show()
            return@rememberLauncherForActivityResult
        }
        // process in coroutine
        scope.launch {
            loading = true
            val result = processEnrollmentBitmap(bitmap, detector, embeddingModel, dataStore)
            loading = false
            if (result) {
                Toast.makeText(context, "Enrollment saved", Toast.LENGTH_SHORT).show()
                onEnrolled()
            } else {
                Toast.makeText(context, "No face detected. Try again.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (loading) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(12.dp))
            Text("Processing...")
        } else {
            Text("Capture a clear front-facing photo for enrollment", modifier = Modifier.padding(12.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = { takePictureLauncher.launch(null) }, modifier = Modifier.height(48.dp)) {
                Text("Capture Enrollment Photo")
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text("Make sure face is centered and well-lit.")
        }
    }
}

// helper suspend function
suspend fun processEnrollmentBitmap(
    bitmap: Bitmap,
    detector: com.google.mlkit.vision.face.FaceDetector,
    model: FaceEmbeddingModel,
    dataStore: DataStoreManager
): Boolean = withContext(Dispatchers.Default) {
    try {
        val image = InputImage.fromBitmap(bitmap, 0)
        val faces = detector.process(image).addOnFailureListener { }.await() // use Task.await extension or use suspendCancellableCoroutine
        if (faces.isEmpty()) return@withContext false
        val face = faces[0]
        val rect = face.boundingBox
        // crop safely
        val safeLeft = rect.left.coerceAtLeast(0)
        val safeTop = rect.top.coerceAtLeast(0)
        val safeRight = rect.right.coerceAtMost(bitmap.width)
        val safeBottom = rect.bottom.coerceAtMost(bitmap.height)
        if (safeRight - safeLeft <= 0 || safeBottom - safeTop <= 0) return@withContext false

        val faceCrop = Bitmap.createBitmap(bitmap, safeLeft, safeTop, safeRight - safeLeft, safeBottom - safeTop)
        val scaled = Bitmap.createScaledBitmap(faceCrop, 112, 112, true)
        val embedding = model.getEmbedding(scaled)
        dataStore.saveEmbedding(embedding, modelVersion = "1")
        return@withContext true
    } catch (e: Exception) {
        e.printStackTrace()
        return@withContext false
    }
}
