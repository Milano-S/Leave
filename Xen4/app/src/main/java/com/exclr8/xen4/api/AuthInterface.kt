package com.exclr8.xen4.api

import com.exclr8.xen4.model.AuthRequest
import com.exclr8.xen4.model.AuthResponse
import com.exclr8.xen4.url.Urls
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface AuthInterface {
    @Headers("Content-Type: application/json; charset=utf-8")
    @POST(Urls.authenticationUrl)
    fun authorizeApp(
        @Body authModel: AuthRequest
    ): Call<AuthResponse>
}