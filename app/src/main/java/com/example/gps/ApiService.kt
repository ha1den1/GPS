package com.example.gps

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {

    @POST("/location")
    fun saveLocation(@Body params: Map<String, String>): Call<Void>
}
