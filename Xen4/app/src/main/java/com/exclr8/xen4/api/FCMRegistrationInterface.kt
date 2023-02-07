package com.exclr8.xen4.api

import com.exclr8.xen4.model.FCMResponse
import com.exclr8.xen4.model.PushNotificationRequest
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface FCMRegistrationInterface {
    @POST("api/core/PushNotification/RegisterToken")
    fun registerToken(
        @Body pushNotificationRequest: PushNotificationRequest,
        @Header("USER_TOKEN_KEY") userTokenKey: String
    ): Call<FCMResponse>
}