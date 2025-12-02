package com.example.attendance_android.utils



object Constants {
    const val MODEL_FILE = "output_model.tflite"    // place model in app/src/main/assets/
    const val EMBEDDING_LENGTH = 128                 // change if your model outputs different length
    const val EMBEDDING_PREF_KEY = "embedding_v1"    // DataStore key name
}
