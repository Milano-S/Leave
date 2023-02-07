package com.exclr8.xen4.api

import com.exclr8.xen4.model.LoginOrEmail
import com.exclr8.xen4.model.PasswordResponse
import com.exclr8.xen4.url.Urls
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST

interface ResetPasswordInterface {
    @Headers("Content-Type: application/json; charset=utf-8")
    @POST(Urls.forgotPasswordUrl)
    fun resetPassword(
        @Body LoginOrEmail : LoginOrEmail,
        @Header("APP_TOKEN_KEY") appTokenKey: String
    ): Call<PasswordResponse>
}