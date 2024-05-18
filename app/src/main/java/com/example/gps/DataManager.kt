package com.example.gps

import android.content.Context
import android.location.Location
import java.text.SimpleDateFormat
import java.util.*

class DataManager(private val context: Context) {
//naudoja issaugoti ir perskaityti duomenis nebluokojant main threado
    fun saveLocationData(location: Location, currentTime: Long) {
        val sharedPreferences = context.getSharedPreferences("LocationData", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        val keyLatitude = "latitude_$currentTime"
        val keyLongitude = "longitude_$currentTime"
        val keyTimestamp = "timestamp_$currentTime"
        editor.putString(keyLatitude, location.latitude.toString())
        editor.putString(keyLongitude, location.longitude.toString())
        editor.putString(keyTimestamp, SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))

        val speedInKilometersPerHour = location.speed.toDouble() * 3.6 // km/h greitis
        val keySpeed = "speed_$currentTime"
        editor.putString(keySpeed, "%.2f".format(speedInKilometersPerHour))

        editor.apply()
    }

    fun calculateDistance(currentLocation: Location, previousTime: Long): Double {
        val sharedPreferences = context.getSharedPreferences("LocationData", Context.MODE_PRIVATE)

        val previousLocation = Location("")
        sharedPreferences.getString("latitude_$previousTime", "0")?.toDoubleOrNull()?.let { previousLocation.latitude = it }
        sharedPreferences.getString("longitude_$previousTime", "0")?.toDoubleOrNull()?.let { previousLocation.longitude = it }

        return currentLocation.distanceTo(previousLocation).toDouble() / 1000
    }

    fun getAllData(): Map<String, String> {
        val sharedPreferences = context.getSharedPreferences("LocationData", Context.MODE_PRIVATE)
        return sharedPreferences.all.mapValues { it.value.toString() }
    }

    fun clearData() {
        val sharedPreferences = context.getSharedPreferences("LocationData", Context.MODE_PRIVATE)
        sharedPreferences.edit().clear().apply()
    }
}
