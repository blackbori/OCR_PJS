package com.example.ocr_pjs.localDB

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Record_OCR::class], version = 1, exportSchema = false)
abstract class AppDatabase: RoomDatabase(){

    abstract fun getDAO(): AppDAO

    companion object {
        private var instance: AppDatabase? = null

        @Synchronized
        fun getInstance(context: Context): AppDatabase? {
            if (instance == null) {
                instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
            }
            return instance
        }
    }

}