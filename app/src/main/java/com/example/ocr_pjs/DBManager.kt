package com.example.ocr_pjs

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.example.ocr_pjs.localDB.AppDatabase
import com.example.ocr_pjs.localDB.Record_OCR
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import java.io.FileInputStream
import java.lang.Exception
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

class DBManager private constructor(){

    val TAG = "TAG_DBManager"

    companion object{
        private var instance: DBManager? = null
        private lateinit var context: Context
        private lateinit var localDB: AppDatabase
        fun getInstance(_context: Context): DBManager{
            return  instance ?: synchronized(this){
                instance ?: DBManager().also {
                    context = _context
                    instance = it
                    localDB = AppDatabase.getInstance(context)!!
                }
            }
        }
    }

    /**
     * ui 스레드가 아닌 다른 스레드에서 돌릴 것
     */
    fun saveResult(bitmap: Bitmap, text: String): SavedLocation{
        var savedLocation = SavedLocation.Fail

        val preferences = AppSharedPreferences.getInstance(context)
        val number = preferences.getInt(AppSharedPreferences.KEY_NUMBER, 1)

        // 여기서 이미지 파일이 만들어진다
        val file = makeImageFile(bitmap, imageFilePath(imageFileName(number)))
        if(file == null){
            Log.d(TAG, "파일 안만들어짐")
            return savedLocation
        }

        val record = Record_OCR(number, text, file.name)
        preferences.setInt(AppSharedPreferences.KEY_NUMBER, number + 1)
        runBlocking {
            CoroutineScope(Dispatchers.IO).launch {
                Log.d(TAG, "코루틴 스코프 시작")

                // 1. 외부 DB에 저장 시도. 성공시 return
                val requestBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("image", file.name, RequestBody.create(MultipartBody.FORM, file))
                    .addFormDataPart("text", text)
                    .build()
                val request = Request.Builder()
                    .url(CONST_DESTINATION_URL)
                    .post(requestBody)
                    .build()
                val client = OkHttpClient.Builder().connectTimeout(3, TimeUnit.SECONDS).build()
                try {
                    var response = client.newCall(request).execute()
                    if(response.isSuccessful){
                        Log.d(TAG, "외부 서버 성공")
                        savedLocation = SavedLocation.ExternalServerDB

                        // 파일이 외부 DB 반영되었으므로 로컬의 파일은 지운다
                        file.delete()
                        return@launch
                    }
                }catch (e: SocketTimeoutException){
                    Log.d(TAG, "외부 서버 응답 시간 초과")
                    e.printStackTrace()
                }
                Log.d(TAG, "외부 서버 실패")

                // 2. 클라우드에 DB 저장 시도. 성공시 return
                val fbFileRef = Firebase.storage.reference.child("imgs/" + record.fileName)
                var fbTaskEndCount = 0
                var fbSuccessCount = 0
                fbFileRef.putStream(FileInputStream(file)).addOnCompleteListener{task ->
                    fbTaskEndCount++
                    if(task.isSuccessful){
                        Log.d(TAG, "파이어베이스 스토리지 성공")
                        fbSuccessCount++
                    }else{
                        task.exception?.printStackTrace()
                    }
                }
                val childRef = Firebase.database.reference.child("ocr")
                childRef.push().setValue(record).addOnCompleteListener{task ->
                    fbTaskEndCount++
                    if(task.isSuccessful){
                        Log.d(TAG, "파이어베이스 데이터베이스 성공")
                        fbSuccessCount++
                    }else{
                        task.exception?.printStackTrace()
                    }
                }
                while(fbTaskEndCount < 2){
                    // 파이어베이스 통신 결과가 나올때까지 기다린다
                }
                Log.d(TAG, "파이어베이스 통신 완료")
                if(fbSuccessCount == 2){ // 파이어베이스 통신 성공
                    Log.d(TAG, "파이어베이스 성공")
                    savedLocation = SavedLocation.CloudServerDB

                    // 파일이 외부 DB 반영되었으므로 로컬의 파일은 지운다
                    file.delete()
                    return@launch
                }
                Log.d(TAG, "파이어베이스 실패")

                // 3. 로컬에 DB 저장
                try{
                    Log.d(TAG, "로컬 디비 이용")
                    localDB.getDAO().insert(record)
                    savedLocation = SavedLocation.LocalDB
                }catch (e: Exception){
                    Log.d(TAG, "로컬 저장에 뭔가 문제가 생김")
                }
            }.join()
        }
        return savedLocation
    }

    enum class SavedLocation{
        ExternalServerDB,
        CloudServerDB,
        LocalDB,
        Fail
    }

}
