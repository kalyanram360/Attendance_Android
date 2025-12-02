package com.example.attendance_android.ml


import android.content.Context
import android.graphics.Bitmap
import com.example.attendance_android.utils.Constants
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sqrt
import java.io.FileInputStream
import java.nio.channels.FileChannel

class FaceEmbeddingModel(private val context: Context) {
    private val interpreter: Interpreter
    private val inputSize = 112  // model input size (change if your model requires different)
    private val embeddingLen = Constants.EMBEDDING_LENGTH

    init {
        val modelBuffer = loadModelFile(context, Constants.MODEL_FILE)
        val options = Interpreter.Options()
        // options.setNumThreads(4) // tune if needed
        interpreter = Interpreter(modelBuffer, options)
    }

    private fun loadModelFile(context: Context, modelPath: String): ByteBuffer {
        val assetFd = context.assets.openFd(modelPath)
        val inputStream = FileInputStream(assetFd.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = assetFd.startOffset
        val declaredLength = assetFd.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    /** Convert a face-cropped Bitmap (112x112) into FloatArray embedding (L2-normalized) */
    fun getEmbedding(faceBitmap: Bitmap): FloatArray {
        val input = convertBitmapToInputBuffer(faceBitmap)
        val output = Array(1) { FloatArray(embeddingLen) }
        interpreter.run(input, output)
        return l2Normalize(output[0])
    }

    private fun convertBitmapToInputBuffer(bitmap: Bitmap): ByteBuffer {
        val img = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, true)
        val byteBuffer = ByteBuffer.allocateDirect(1 * inputSize * inputSize * 3 * 4)
            .order(ByteOrder.nativeOrder())
        val intValues = IntArray(inputSize * inputSize)
        img.getPixels(intValues, 0, img.width, 0, 0, img.width, img.height)
        var i = 0
        for (y in 0 until inputSize) {
            for (x in 0 until inputSize) {
                val v = intValues[i++]
                val r = ((v shr 16) and 0xFF) / 255.0f
                val g = ((v shr 8) and 0xFF) / 255.0f
                val b = (v and 0xFF) / 255.0f
                // normalize depending on your model expectation; many MobileFaceNet use (r-0.5)*2
                byteBuffer.putFloat((r - 0.5f) * 2.0f)
                byteBuffer.putFloat((g - 0.5f) * 2.0f)
                byteBuffer.putFloat((b - 0.5f) * 2.0f)
            }
        }
        byteBuffer.rewind()
        return byteBuffer
    }

    private fun l2Normalize(vector: FloatArray): FloatArray {
        var sum = 0f
        for (v in vector) sum += v * v
        val norm = sqrt(sum)
        return vector.map { it / (norm + 1e-10f) }.toFloatArray()
    }

    fun close() {
        interpreter.close()
    }
}
