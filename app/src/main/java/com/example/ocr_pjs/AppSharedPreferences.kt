package com.example.ocr_pjs

import android.content.Context
import android.content.SharedPreferences

/**
 * SharedPreferences 는 이것만 사용해서 이용
 */
class AppSharedPreferences private constructor(){

    companion object{
        val FILE_NAME = "AppSharedPreferences"
        val KEY_NUMBER = "Count"

        private var instance: AppSharedPreferences? = null
        private lateinit var application: Context
        private lateinit var sharedPreferences: SharedPreferences
        fun getInstance(_application: Context): AppSharedPreferences{
            return  instance ?: synchronized(this){
                instance ?: AppSharedPreferences().also {
                    application = _application
                    instance = it
                    sharedPreferences = application.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
                }
            }
        }
    }

    fun getInt(key: String, defaultValue: Int): Int{
        return sharedPreferences.getInt(key,defaultValue)
    }

    fun setInt(key: String, value: Int): Boolean{
        return sharedPreferences.edit().putInt(key, value).commit()
    }

}