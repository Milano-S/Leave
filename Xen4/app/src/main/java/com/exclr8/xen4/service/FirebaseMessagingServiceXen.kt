package com.exclr8.xen4.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Build
import android.util.Log
import androidx.core.content.ContentProviderCompat.requireContext
import com.exclr8.xen4.R
import com.exclr8.xen4.activity.MainActivity
import com.exclr8.xen4.api.FCMRegistrationInterface
import com.exclr8.xen4.model.FCMResponse
import com.exclr8.xen4.model.PushNotificationRequest
import com.exclr8.xen4.sharedPref.SharedPreference
import com.exclr8.xen4.url.Urls
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

private const val TAG = "FirebaseService"

class FirebaseMessagingServiceXen : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {

        Log.i(TAG, "Remote Message Data : " + remoteMessage.data["url"].toString())
        val notificationManager: NotificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        lateinit var notificationChannel: NotificationChannel
        lateinit var builder: Notification.Builder
        val channelId = "i.apps.notifications"
        val description = "Test notification"

        val url = remoteMessage.data["url"].toString()
        //val url = "fakeUrl"
        Log.i(TAG, "Url Data : $url")

        SharedPreference(applicationContext).save("url", url)

        val intent = Intent(this, MainActivity::class.java)
        if (remoteMessage.data["url"] != null) {
            intent.putExtra("url", url)
        }

        val pendingIntent =
            PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationChannel =
                NotificationChannel(channelId, description, NotificationManager.IMPORTANCE_HIGH)
            notificationChannel.enableLights(true)
            notificationChannel.lightColor = Color.GREEN
            notificationChannel.enableVibration(false)
            notificationManager.createNotificationChannel(notificationChannel)

            builder = Notification.Builder(this, channelId)
                .setContentTitle("Xen4")
                .setContentText(url)
                .setSmallIcon(R.mipmap.ic_launcher_foreground)
                .setLargeIcon(
                    BitmapFactory.decodeResource(
                        resources,
                        R.mipmap.ic_launcher_foreground
                    )
                )
                .setContentIntent(pendingIntent)
                .setShowWhen(true)
                .setAutoCancel(true)
        } else {
            builder = Notification.Builder(this)
                .setContentTitle("Xen4")
                .setContentText(url)
                .setSmallIcon(R.mipmap.ic_launcher_foreground)
                .setLargeIcon(
                    BitmapFactory.decodeResource(
                        resources,
                        R.mipmap.ic_launcher_foreground
                    )
                )
                .setContentIntent(pendingIntent)
                .setShowWhen(true)
                .setAutoCancel(true)
        }
        notificationManager.notify(1234, builder.build())
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.i(TAG, "FCM Token : $token")
    }

    fun sendRegistrationToServer(
        pushNotificationRequest: PushNotificationRequest,
        userTokenKey: String,
        url: String
    ) {
        val request = Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create())
            .baseUrl(url)
            .build()
            .create(FCMRegistrationInterface::class.java)

        val retrofitData = request.registerToken(
            pushNotificationRequest, userTokenKey
        )
        retrofitData.enqueue(object : Callback<FCMResponse> {
            override fun onResponse(call: Call<FCMResponse>, response: Response<FCMResponse>) {
                Log.d(TAG, "FCM: " + response.body().toString())
            }

            override fun onFailure(call: Call<FCMResponse>, t: Throwable) {
                Log.d(TAG, t.message.toString())
            }
        })
    }
}