package com.example.attendance_android.data



import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase


@Database(entities = [PresentEntity::class], version = 1, exportSchema = false)
abstract class PresentDatabase : RoomDatabase() {
    abstract fun presentDao(): PresentDao

    companion object {
        @Volatile
        private var INSTANCE: PresentDatabase? = null

        fun getInstance(context: Context): PresentDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PresentDatabase::class.java,
                    "present_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}