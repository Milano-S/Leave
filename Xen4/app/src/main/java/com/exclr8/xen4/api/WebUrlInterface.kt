package com.exclr8.xen4.api

import com.exclr8.xen4.model.UrlData
import com.exclr8.xen4.url.Urls
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers

interface WebUrlInterface {
    @GET(Urls.webUrl)
    fun getUrls(
        @Header("APP_TOKEN_KEY") appTokenKey: String
    ): Call<UrlData>
}