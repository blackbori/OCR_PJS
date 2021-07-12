package com.example.ocr_pjs

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class FbMessagingService : FirebaseMessagingService() {

    val TAG = "TAG_FbMessagingService"

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "메시지 받았는데??")
        if(remoteMessage.notification != null){
            var title = remoteMessage.notification?.title
            if(title != null)
                title = "제목 없음"
            var body = remoteMessage.notification?.body
            if(body != null)
                body = "내용 없음"

            val intent = Intent(this, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT)
            val channelId = "Channel ID"
            val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val notificationBuilder = NotificationCompat.Builder(this,"")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent)
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){ // 26이상
                val channelName = "Channel Name"
                val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH)
                notificationManager.createNotificationChannel(channel)
            }
            notificationManager.notify(0, notificationBuilder.build())
        }
    }

    override fun onNewToken(p0: String) {

    }
}
