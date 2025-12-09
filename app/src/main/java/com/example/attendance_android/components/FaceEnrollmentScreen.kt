//package com.example.attendance_android.components
//
//
//import android.graphics.Bitmap
//import android.widget.Toast
//import androidx.activity.compose.rememberLauncherForActivityResult
//import androidx.activity.result.contract.ActivityResultContracts
//import androidx.compose.foundation.layout.*
//import androidx.compose.material3.Button
//import androidx.compose.material3.Text
//import androidx.compose.material3.CircularProgressIndicator
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.platform.LocalContext
//import androidx.compose.ui.unit.dp
//import com.example.attendance_android.ml.FaceEmbeddingModel
//import com.example.attendance_android.data.DataStoreManager
//import com.google.mlkit.vision.common.InputImage
//import com.google.mlkit.vision.face.FaceDetection
//import com.google.mlkit.vision.face.Face
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.withContext
//import kotlinx.coroutines.launch
//import com.example.attendance_android.utils.await
//import com.example.attendance_android.utils.Constants
//
//@Composable
//fun FaceEnrollmentScreen(
//    onEnrolled: () -> Unit,
//) {
//    val context = LocalContext.current
//    val dataStore = remember { DataStoreManager(context) }
//    val embeddingModel = remember { FaceEmbeddingModel(context) }
//    val detector = remember { FaceDetection.getClient() }
//    val scope = rememberCoroutineScope()
//
//    var loading by remember { mutableStateOf(false) }
//    val takePictureLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap: Bitmap? ->
//        if (bitmap == null) {
//            Toast.makeText(context, "No image captured", Toast.LENGTH_SHORT).show()
//            return@rememberLauncherForActivityResult
//        }
//        // process in coroutine
//        scope.launch {
//            loading = true
//            val result = processEnrollmentBitmap(bitmap, detector, embeddingModel, dataStore)
//            loading = false
//            if (result) {
//                Toast.makeText(context, "Enrollment saved", Toast.LENGTH_SHORT).show()
//                onEnrolled()
//            } else {
//                Toast.makeText(context, "No face detected. Try again.", Toast.LENGTH_SHORT).show()
//            }
//        }
//    }
//
//    Column(
//        modifier = Modifier.fillMaxSize().padding(24.dp),
//        horizontalAlignment = Alignment.CenterHorizontally,
//        verticalArrangement = Arrangement.Center
//    ) {
//        if (loading) {
//            CircularProgressIndicator()
//            Spacer(modifier = Modifier.height(12.dp))
//            Text("Processing...")
//        } else {
//            Text("Capture a clear front-facing photo for enrollment", modifier = Modifier.padding(12.dp))
//            Spacer(modifier = Modifier.height(12.dp))
//            Button(onClick = { takePictureLauncher.launch(null) }, modifier = Modifier.height(48.dp)) {
//                Text("Capture Enrollment Photo")
//            }
//            Spacer(modifier = Modifier.height(12.dp))
//            Text("Make sure face is centered and well-lit.")
//        }
//    }
//}
//
//// helper suspend function
//suspend fun processEnrollmentBitmap(
//    bitmap: Bitmap,
//    detector: com.google.mlkit.vision.face.FaceDetector,
//    model: FaceEmbeddingModel,
//    dataStore: DataStoreManager
//): Boolean = withContext(Dispatchers.Default) {
//    try {
//        val image = InputImage.fromBitmap(bitmap, 0)
//        val faces = detector.process(image).addOnFailureListener { }.await() // use Task.await extension or use suspendCancellableCoroutine
//        if (faces.isEmpty()) return@withContext false
//        val face = faces[0]
//        val rect = face.boundingBox
//        // crop safely
//        val safeLeft = rect.left.coerceAtLeast(0)
//        val safeTop = rect.top.coerceAtLeast(0)
//        val safeRight = rect.right.coerceAtMost(bitmap.width)
//        val safeBottom = rect.bottom.coerceAtMost(bitmap.height)
//        if (safeRight - safeLeft <= 0 || safeBottom - safeTop <= 0) return@withContext false
//
//        val faceCrop = Bitmap.createBitmap(bitmap, safeLeft, safeTop, safeRight - safeLeft, safeBottom - safeTop)
//        val scaled = Bitmap.createScaledBitmap(faceCrop, 112, 112, true)
//        val embedding = model.getEmbedding(scaled)
//        dataStore.saveEmbedding(embedding, modelVersion = "1")
//        return@withContext true
//    } catch (e: Exception) {
//        e.printStackTrace()
//        return@withContext false
//    }
//}

package com.example.attendance_android.components

import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    val takePictureLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap == null) {
            Toast.makeText(context, "No image captured", Toast.LENGTH_SHORT).show()
            return@rememberLauncherForActivityResult
        }
        scope.launch {
            loading = true
            val result = processEnrollmentBitmap(bitmap, detector, embeddingModel, dataStore)
            loading = false
            if (result) {
                Toast.makeText(context, "Face enrolled successfully!", Toast.LENGTH_SHORT).show()
                onEnrolled()
            } else {
                Toast.makeText(context, "No face detected. Please try again.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (loading) {
                // Loading State
                Surface(
                    modifier = Modifier.size(120.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shadowElevation = 8.dp
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(60.dp),
                            strokeWidth = 4.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Processing Your Face",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Please wait while we analyze your photo...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            } else {
                // Main Content

                // Face Icon with Border
                Box(
                    modifier = Modifier.size(140.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shadowElevation = 8.dp
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Face,
                                contentDescription = null,
                                modifier = Modifier.size(70.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Decorative border
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .border(
                                width = 3.dp,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                shape = CircleShape
                            )
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = "Face Enrollment",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "We need to register your face for attendance verification",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Instructions Card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shadowElevation = 2.dp
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lightbulb,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text(
                                    text = "Tips for Best Results",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }

                        InstructionItem("Face the camera directly")
                        InstructionItem("Ensure good lighting")
                        InstructionItem("Remove glasses if possible")
                        InstructionItem("Keep a neutral expression")
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Capture Button
                Button(
                    onClick = { takePictureLauncher.launch(null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 4.dp,
                        pressedElevation = 8.dp
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Capture Photo",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Privacy Note
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Your face data is securely stored on your device only",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InstructionItem(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

// Helper suspend function
suspend fun processEnrollmentBitmap(
    bitmap: Bitmap,
    detector: com.google.mlkit.vision.face.FaceDetector,
    model: FaceEmbeddingModel,
    dataStore: DataStoreManager
): Boolean = withContext(Dispatchers.Default) {
    try {
        val image = InputImage.fromBitmap(bitmap, 0)
        val faces = detector.process(image).addOnFailureListener { }.await()
        if (faces.isEmpty()) return@withContext false

        val face = faces[0]
        val rect = face.boundingBox

        // Crop safely
        val safeLeft = rect.left.coerceAtLeast(0)
        val safeTop = rect.top.coerceAtLeast(0)
        val safeRight = rect.right.coerceAtMost(bitmap.width)
        val safeBottom = rect.bottom.coerceAtMost(bitmap.height)

        if (safeRight - safeLeft <= 0 || safeBottom - safeTop <= 0) {
            return@withContext false
        }

        val faceCrop = Bitmap.createBitmap(
            bitmap,
            safeLeft,
            safeTop,
            safeRight - safeLeft,
            safeBottom - safeTop
        )
        val scaled = Bitmap.createScaledBitmap(faceCrop, 112, 112, true)
        val embedding = model.getEmbedding(scaled)
        dataStore.saveEmbedding(embedding, modelVersion = "1")

        return@withContext true
    } catch (e: Exception) {
        e.printStackTrace()
        return@withContext false
    }
}