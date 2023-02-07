package com.exclr8.xen4.api

import com.exclr8.xen4.model.UserDetailsResponse
import com.exclr8.xen4.url.Urls
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Header

interface UserDetailsInterface {
    @GET(Urls.userDetailsUrl)
    fun getUserDetails(
        @Header("USER_TOKEN_KEY") userTokenKey: String
    ): Call<UserDetailsResponse>
}