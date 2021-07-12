package com.example.ocr_pjs.localDB

import androidx.room.Dao
import androidx.room.Insert

@Dao
interface AppDAO {

    @Insert
    fun insert(data: Record_OCR)


}