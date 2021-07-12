package com.example.ocr_pjs

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Environment
import android.util.Base64
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.functions.FirebaseFunctions
import com.google.gson.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * 내 컴퓨터 URL. 원래는 도메인이어야 할 것.
 */
val CONST_DESTINATION_URL = "http://192.168.0.10:8080/root/androidDB.jsp"

fun hasPermissions(context : Context, permissions: Array<String>) : Boolean{
    var result : Int
    for (perms in permissions){
        result = ContextCompat.checkSelfPermission(context, perms)
        if(result == PackageManager.PERMISSION_DENIED){
            return false
        }
    }
    return true
}

fun requestOCR(bitmap : Bitmap, completeListener : OnCompleteListener<JsonElement>){
    var _bitmap = scaleBitmapDown(bitmap, 640)
    val base64encoded = convertBitampToBase64(_bitmap)
    val request = createJsonRequest(base64encoded)
    addLangOption(request, "ko")

    callFirebaseFunction("annotateImage", request.toString()).addOnCompleteListener(completeListener)
}

fun scaleBitmapDown(bitmap : Bitmap, maxDimension : Int) : Bitmap{
    val originalWidth = bitmap.width
    val originalHeight = bitmap.height
    var resizedWidth = maxDimension
    var resizedHeight = maxDimension
    if (originalHeight > originalWidth) {
        resizedHeight = maxDimension
        resizedWidth =
            (resizedHeight * originalWidth.toFloat() / originalHeight.toFloat()).toInt()
    } else if (originalWidth > originalHeight) {
        resizedWidth = maxDimension
        resizedHeight =
            (resizedWidth * originalHeight.toFloat() / originalWidth.toFloat()).toInt()
    } else if (originalHeight == originalWidth) {
        resizedHeight = maxDimension
        resizedWidth = maxDimension
    }
    return Bitmap.createScaledBitmap(bitmap, resizedWidth, resizedHeight, false)
}


fun convertBitampToBase64(bitmap : Bitmap) : String{
    val byteArrayOutputStream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
    val imageBytes = byteArrayOutputStream.toByteArray()
    return Base64.encodeToString(imageBytes, Base64.NO_WRAP)
}

/**
 * Create json request to cloud vision
 */
fun createJsonRequest(base64encoded : String) : JsonObject {
    val request = JsonObject()
    // Add image to request
    val image = JsonObject()
    image.add("content", JsonPrimitive(base64encoded))
    request.add("image", image)
    //Add features to the request
    val feature = JsonObject()
    feature.add("type", JsonPrimitive("TEXT_DETECTION"))
    // Alternatively, for DOCUMENT_TEXT_DETECTION:
    // feature.add("type", JsonPrimitive("DOCUMENT_TEXT_DETECTION"))
    val features = JsonArray()
    features.add(feature)
    request.add("features", features)
    return request
}

private fun addLangOption(request : JsonObject, lang : String){
    val imageContext = JsonObject()
    val languageHints = JsonArray()
    languageHints.add(lang)
    imageContext.add("languageHints", languageHints)
    request.add("imageContext", imageContext)
}

private fun callFirebaseFunction(funcName : String, requestJson : String) : Task<JsonElement> {
    return FirebaseFunctions.getInstance()
        .getHttpsCallable(funcName)
        .call(requestJson)
        .continueWith { task ->
            val result = task.result?.data
            JsonParser.parseString(Gson().toJson(result))
        }
}

/**
 * 사용 안함
 */
fun bitmapToByteArray(bitmap: Bitmap) : ByteArray {
    val stream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
    return stream.toByteArray()
}

/**
 * 비트맵을 .jpg 파일로 만들고 그 파일을 반환, 실패한다면 null 반환
 */
fun makeImageFile(bitmap: Bitmap, path: String) : File?{
    val TAG = "TAG_makeImageFile"

    try {
        val file = File(path)
        Log.d(TAG, "file path = " + file.path)
        if(file.exists())
            file.delete()
        file.createNewFile()
        val stream = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
        stream.close()
        return file
    }catch (e: IOException){
        e.printStackTrace()
    }
    return null
}

fun imageFileName(count: Int): String{
    return "img_" + count.toString() + ".jpg"
}

fun imageFilePath(fileName: String): String{
    return Environment.getExternalStorageDirectory().absolutePath + "/" + fileName
}
