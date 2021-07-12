package com.example.ocr_pjs.localDB

import androidx.room.*

@Entity
data class Record_OCR (

    /**
     * 순서
     */
    @ColumnInfo
    @PrimaryKey(autoGenerate = false)
    var number: Int,

    /**
     * 인식된 텍스트
     */
    @ColumnInfo
    var text: String,

    /**
     * 파일의 이름 ex)img_1.jpg
     */
    @ColumnInfo
    var fileName: String
)