package com.example.attendance_android.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PresentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(presentEntity: PresentEntity): Long

    @Query("SELECT * FROM present WHERE id = :id")
    fun getById(id: Long): PresentEntity?

    @Query("SELECT * FROM present ORDER BY createdAt DESC")
    fun getAll(): Flow<List<PresentEntity>>

    @Query("DELETE FROM present")
    fun clearAll(): Int
}
