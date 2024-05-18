package com.example.gps

import android.content.Context
import android.widget.Toast
import java.util.*

object DataDisplayHelper {

    fun displayData(context: Context, data: Map<String, String>, startTime: Long, endTime: Long, totalDistance: Double) {
        val dataString = StringBuilder()
        dataString.append("Locally saved data:\n")
        dataString.append("Start Time: ${Date(startTime)}\n")
        dataString.append("End Time: ${Date(endTime)}\n")
        dataString.append("Total Distance: %.2f km\n".format(totalDistance))
        data.forEach { (key, value) ->
            dataString.append("$key: $value\n")
        }
        Toast.makeText(context, dataString.toString(), Toast.LENGTH_LONG).show()
    }
}
