package com.example.attendance_android.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "classes")
data class ClassEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val token: String,
    val subject: String?,
    val createdAt: Long = System.currentTimeMillis()
)
