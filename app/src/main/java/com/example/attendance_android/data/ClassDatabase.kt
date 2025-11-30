package com.example.attendance_android.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [ClassEntity::class], version = 1, exportSchema = false)
abstract class ClassDatabase : RoomDatabase() {
    abstract fun classDao(): ClassDao

    companion object {
        @Volatile
        private var INSTANCE: ClassDatabase? = null

        fun getInstance(context: Context): ClassDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ClassDatabase::class.java,
                    "classes_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
