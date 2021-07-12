package com.example.ocr_pjs

import android.Manifest
import android.annotation.TargetApi
import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.firebase.ui.auth.AuthUI
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.gson.*
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {

    //<editor-fold desc="변수">
    val TAG = "Tag_MainActivity"

    private val PERMISSIONS = arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE)

    // 코드 넘버 ////////////////////////////////////////////////////////////////////////
    private val CODE_PERMISSION_REQUEST = 1000
    private val CODE_TAKE_PICTURE = 1001
    private val CODE_SIGN_IN = 1002
    /////////////////////////////////////////////////////////////////////////////////////

    // 뷰 ////////////////////////////////////////////////////////////////////////////////
    private lateinit var iv_photo : ImageView
    private lateinit var tv_annotation : TextView
    private lateinit var btn_push : Button
    //////////////////////////////////////////////////////////////////////////////////////

    // UI 결정 변수들 /////////////////////////////////////////////////////////////////////
    /**로그인 했는지*/
    private var ui_var_login : LoginState = LoginState.No
    /**사진 찍었는지*/
    private var ui_var_photo : PhotoState = PhotoState.No
    /**권한 획득 했는지*/
    private var ui_var_permission : _PermissionState = _PermissionState.NoPermission
    ////////////////////////////////////////////////////////////////////////////////////////

    private var print_state_number = 1
    //</editor-fold>

    //<editor-fold desc="라이프 사이클">
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        iv_photo = findViewById(R.id.iv_photo)
        tv_annotation = findViewById(R.id.tv_annotation)
        btn_push = findViewById(R.id.btn_push)
        btn_push.setOnClickListener(onClickListener)

        initializeUI()
    }
    //</editor-fold>

    //<editor-fold desc="이벤트처리">
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when(requestCode){
            CODE_TAKE_PICTURE -> {
                try{
                    // 사진을 화면에 표시한다
                    var bitmap = data?.extras?.get("data") as Bitmap
                    iv_photo.setImageBitmap(bitmap)

                    // cloud vision api 에 ocr 요청한다
                    requestOCR(bitmap, object: OnCompleteListener<JsonElement>{
                        override fun onComplete(task: Task<JsonElement>) {
                            if(task.isSuccessful){
                                try {
                                    // 인식된 텍스트 화면에 표시
                                    val annotation = task.result!!.asJsonArray[0].asJsonObject["fullTextAnnotation"].asJsonObject
                                    val text = annotation["text"].asString
                                    tv_annotation.text = text

                                    // 비동기로 DB에 저장
                                    CoroutineScope(Dispatchers.IO).async {
                                        // 어디에 저장되었는지 표시한다
                                        val savedLocation = DBManager.getInstance(this@MainActivity).saveResult(bitmap, text)
                                        when(savedLocation){
                                            DBManager.SavedLocation.ExternalServerDB -> {
                                                Log.d(TAG, "외부 디비에 저장 성공")
                                            }
                                            DBManager.SavedLocation.CloudServerDB -> {
                                                Log.d(TAG, "클라우드 디비에 저장 성공")
                                            }
                                            DBManager.SavedLocation.LocalDB -> {
                                                Log.d(TAG, "로컬 디비에 저장??")
                                            }
                                            DBManager.SavedLocation.Fail -> {
                                                Log.d(TAG, "저장 실패")
                                            }
                                        }
                                    }
                                    return
                                }catch (e : Exception){
                                    e.printStackTrace()
                                }
                            }
                            // ocr 작업이 성공적으로 수행됐다면 return 이기에
                            // 여기까지 왔다면 실패이다
                            tv_annotation.text = "인식 실패"
                            Toast.makeText(this@MainActivity, "텍스트 인식에 실패했습니다", Toast.LENGTH_SHORT).show()
                        }
                    })
                    // 사진 화면에 표시함
                    set_ui_var_photo(PhotoState.Ok)
                }catch (e: Exception){
                    e.printStackTrace()
                    // 사진 화면에 표시 실패
                    set_ui_var_photo(PhotoState.Fail)
                }
            }
            CODE_SIGN_IN -> { // 구글 로그인
                if(resultCode == Activity.RESULT_OK){
                    set_ui_var_login(LoginState.Ok)
                }else{
                    set_ui_var_login(LoginState.Fail)
                }
            }
        }
    }
    @TargetApi(Build.VERSION_CODES.M)
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when(requestCode){
            CODE_PERMISSION_REQUEST -> {
                if(grantResults.size > 0){
                    val cameraPermissionAccepted = grantResults.get(0) == PackageManager.PERMISSION_GRANTED
                    val diskPermissionAccepted = grantResults.get(1) == PackageManager.PERMISSION_GRANTED
                    val accepted = (cameraPermissionAccepted || diskPermissionAccepted)

                    if(accepted){
                        set_ui_var_permission(_PermissionState.OK)
                    }else{
                        set_ui_var_permission(_PermissionState.Fail)
                    }
                }
            }
        }
    }
    val onClickListener = object: View.OnClickListener{
        override fun onClick(view: View?) {
            if(view == null)
                return

            when (view.id){
                R.id.btn_push -> {
                    Toast.makeText(this@MainActivity, "구현중", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    //</editor-fold>

    //<editor-fold desc="다이얼로그">
    @TargetApi(Build.VERSION_CODES.M)
    private fun showDialogForPermission(msg : String){
        val builder = AlertDialog.Builder(this)
        builder.setTitle("제목")
        builder.setMessage(msg)
        builder.setCancelable(false)
        builder.setPositiveButton("예", object : DialogInterface.OnClickListener{
            override fun onClick(p0: DialogInterface?, p1: Int) {
                set_ui_var_permission(_PermissionState.Request)
                p0?.dismiss()
            }
        })
        builder.setNegativeButton("아니오", object : DialogInterface.OnClickListener{
            override fun onClick(p0: DialogInterface?, p1: Int) {
                p0?.dismiss()
            }
        })
        builder.create().show()
    }
    //</editor-fold>

    private fun showLoginUI(){
        val providers = arrayListOf(
            AuthUI.IdpConfig.GoogleBuilder().build())

        startActivityForResult(
            AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(providers)
                .build(),
            CODE_SIGN_IN)
    }

    /**
     * ui 결정 변수들을 통해 화면에 표시할 내용을 결정하여 보여준다
     */
    private fun setUI(){
        print_ui_state()
        when(ui_var_permission){
            _PermissionState.NoPermission -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    requestPermissions(PERMISSIONS, CODE_PERMISSION_REQUEST)
                    set_ui_var_permission(_PermissionState.Request)
                }
                return
            }
            _PermissionState.Request -> {
                return
            }
            _PermissionState.Fail -> {
                showDialogForPermission("기능을 사용하기 위한 권한이 필요합니다")
            }
            _PermissionState.OK -> {
                // 리턴 노노
            }
        }
        when(ui_var_login){
            LoginState.No -> {
                showLoginUI()
                set_ui_var_login(LoginState.Request)
                return
            }
            LoginState.Request -> {
                return
            }
            LoginState.Fail -> {
                set_ui_var_login(LoginState.No)
                return
            }
            LoginState.Ok -> {
                // 리턴 노노
            }
        }
        when(ui_var_photo){
            PhotoState.No -> {
                set_ui_var_photo(PhotoState.Request)
                return
            }
            PhotoState.Request -> {
                startActivityForResult(Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE), CODE_TAKE_PICTURE)
                return
            }
            PhotoState.Fail -> {
                set_ui_var_photo(PhotoState.No)
            }
            PhotoState.Ok -> {
                // 리턴 노노
            }
        }
    }

    /**
     * ui 초기화
     */
    private fun initializeUI(){
        if(hasPermissions(this, PERMISSIONS)){
            ui_var_permission = _PermissionState.OK
        }
        if(Firebase.auth.currentUser != null){
            ui_var_login = LoginState.Ok
        }
        setUI()
    }

    //<editor-fold desc="set_ui_variables">
    private fun set_ui_var_login(login : LoginState){
        if(ui_var_login != login){
            ui_var_login = login
            setUI()
        }
    }
    private fun set_ui_var_photo(photo : PhotoState){
        if(ui_var_photo != photo){
            ui_var_photo = photo
            setUI()
        }
    }
    private fun set_ui_var_permission(permission : _PermissionState){
        if(ui_var_permission != permission){
            ui_var_permission = permission
            setUI()
        }
    }
    //</editor-fold>

    enum class LoginState{
        /**로그인 안됨*/
        No,
        /**로그인 요청*/
        Request,
        /**로그인 실패*/
        Fail,
        /**로그인 되어있음*/
        Ok
    }
    enum class PhotoState{
        /**사진 없음*/
        No,
        /**사진 요청*/
        Request,
        /**사진 실패*/
        Fail,
        /**사진 있음*/
        Ok
    }
    enum class _PermissionState{
        /**권한 없음*/
        NoPermission,
        /**권한 신청*/
        Request,
        /**권한 신청했으나 실패*/
        Fail,
        /**권한 획득함*/
        OK
    }

    fun print_ui_state(){
        Log.d(TAG, "print state${print_state_number++}\nui_var_permission : $ui_var_permission\nui_var_login : $ui_var_login\nui_var_photo : $ui_var_photo")
    }


}