package com.example.attendance_android.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "present")
data class PresentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val subject: String,
    val teacher: String?,
    val createdAt: Long = System.currentTimeMillis()
)