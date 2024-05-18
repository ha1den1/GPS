package com.example.gps

import android.location.Location
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.*

class NetworkService {

    private val retrofit = Retrofit.Builder()
        .baseUrl("http://192.168.1.103:3000")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val service = retrofit.create(ApiService::class.java)

    suspend fun sendDataToServer(location: Location) {
        withContext(Dispatchers.IO) {   //coroutines del asynkroniskumo
            try {
                val params = mapOf(
                    "latitude" to location.latitude.toString(),
                    "longitude" to location.longitude.toString(),
                    "timestamp" to SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()),
                    "speed" to "%.2f".format(location.speed * 3.6) // greitis i km/h
                )
                val response = service.saveLocation(params).execute()
                if (!response.isSuccessful) {

                }
            } catch (e: Exception) {

            }
        }
    }
}
