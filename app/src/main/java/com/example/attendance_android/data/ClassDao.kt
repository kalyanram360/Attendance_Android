package com.example.attendance_android.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ClassDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(classEntity: ClassEntity): Long

    @Query("SELECT * FROM classes WHERE id = :id")
    fun getById(id: Long): ClassEntity?

    @Query("SELECT * FROM classes WHERE token = :token LIMIT 1")
    fun getByToken(token: String): ClassEntity?

    @Query("SELECT * FROM classes ORDER BY id DESC")
    fun getAll(): Flow<List<ClassEntity>>

    @Query("DELETE FROM classes WHERE id = :id")
    fun deleteById(id: Long): Int

    @Query("DELETE FROM classes")
    fun clearAll(): Int
}
