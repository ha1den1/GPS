package com.example.gps

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.gps.databinding.ActivityMainBinding
import com.google.android.gms.location.*
import kotlinx.coroutines.*
import android.app.AlertDialog
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private var isRecording: Boolean = false
    private var saveLocally: Boolean = false
    private var saveToServer: Boolean = false
    private var totalDistance: Double = 0.0
    private var startTime: Long = 0
    private var endTime: Long = 0
    private var previousTime: Long = 0
    private val handler = Handler(Looper.getMainLooper())
    private var saveDataRunnable: Runnable? = null
    private var topSpeedInKilometersPerHour = 0.0
    private lateinit var dataManager: DataManager
    private lateinit var networkService: NetworkService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        dataManager = DataManager(this)
        networkService = NetworkService()

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        locationRequest = LocationRequest.create().apply {
            interval = 700
            fastestInterval = 70
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        previousTime = System.currentTimeMillis()

        binding.startButton.setOnClickListener {
            if (!isRecording) {
                startLocationUpdates()
                isRecording = true
                startTime = System.currentTimeMillis()
                Toast.makeText(this, "GPS recording started", Toast.LENGTH_SHORT).show()
            }
        }

        binding.stopButton.setOnClickListener {
            if (isRecording) {
                stopLocationUpdates()
                isRecording = false
                endTime = System.currentTimeMillis()
                Toast.makeText(this, "GPS recording stopped", Toast.LENGTH_SHORT).show()
            }
        }

        binding.saveOptionRadioGroup.setOnCheckedChangeListener { group, checkedId ->
            when (checkedId) {
                binding.localRadioButton.id -> {
                    saveLocally = true
                    saveToServer = false
                }
                binding.serverRadioButton.id -> {
                    saveLocally = false
                    saveToServer = true
                }
                binding.bothRadioButton.id -> {
                    saveLocally = true
                    saveToServer = true
                }
            }
        }

        binding.showLocalDataButton.setOnClickListener {
            displayLocallySavedData()
        }

        binding.deleteLocalDataButton.setOnClickListener {
            deleteLocallySavedData()
        }

        if (!checkLocationPermission()) {
            requestLocationPermission()
        }

        setupSaveDataRunnable()
    }

    private fun setupSaveDataRunnable() {   //periodiskai issaugoti duomenis
        saveDataRunnable = Runnable {
            if (saveLocally && isRecording) {
                if (checkLocationPermission()) {
                    fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                        location?.let {
                            val currentTime = System.currentTimeMillis()
                            dataManager.saveLocationData(it, currentTime)


                            totalDistance += dataManager.calculateDistance(it, previousTime)

                            previousTime = currentTime
                        }
                    }.addOnFailureListener { e ->

                        Toast.makeText(this, "Failed to get last known location: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                } else {

                    requestLocationPermission()
                }
            }
            handler.postDelayed(saveDataRunnable!!, 30000)
        }
    }

    private fun checkLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    private fun displayLocallySavedData() {
        val allEntries = dataManager.getAllData()


        val linearLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        //issaugoti entries
        allEntries.forEach { (key, value) ->
            val textView = TextView(this)
            textView.text = "$key: $value"
            linearLayout.addView(textView)
        }

        // average greitis
        val durationInMillis = endTime - startTime
        val durationInHours = durationInMillis.toDouble() / (1000 * 60 * 60) // ms to h
        val averageSpeed = if (durationInHours != 0.0) maxOf(0.0, totalDistance / durationInHours) else 0.0

        val averageSpeedText = "Average Speed: ${"%.2f".format(averageSpeed)} km/h"
        val averageSpeedTextView = TextView(this).apply {
            text = averageSpeedText
        }
        linearLayout.addView(averageSpeedTextView)

        val scrollView = ScrollView(this).apply {
            addView(linearLayout)
        }


        AlertDialog.Builder(this)
            .setTitle("Locally Saved Data")
            .setView(scrollView)
            .setPositiveButton("Close") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun deleteLocallySavedData() {
        dataManager.clearData()
        Toast.makeText(this, "Locally saved data deleted", Toast.LENGTH_SHORT).show()
    }

    private fun startLocationUpdates() {
        if (!checkLocationPermission()) {
            requestLocationPermission()
            return
        }

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                locationResult ?: return
                for (location in locationResult.locations) {
                    updateSpeed(location)
                    if (saveToServer) {
                        CoroutineScope(Dispatchers.Main).launch {
                            networkService.sendDataToServer(location) //multithreadingas su coroutine naudoja asynchronous
                        }
                    }
                }
            }
        }
    //vietos update'ai yra managinti asinkroniskai
        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
            handler.post(saveDataRunnable!!)
        } catch (e: SecurityException) {
            Toast.makeText(this, "Location permission required", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        handler.removeCallbacks(saveDataRunnable!!)
    }

    private fun updateSpeed(location: Location) {
        val speedInMetersPerSecond = location.speed
        val speedInKilometersPerHour = speedInMetersPerSecond * 3.6
        val formattedSpeedInMetersPerSecond = "%.2f".format(speedInMetersPerSecond)
        val formattedSpeedInKilometersPerHour = "%.2f".format(speedInKilometersPerHour)

        val speedText = "Speed: $formattedSpeedInMetersPerSecond m/s ($formattedSpeedInKilometersPerHour km/h)"
        binding.speedTextView.text = speedText

        // top speedo
        if (speedInKilometersPerHour > topSpeedInKilometersPerHour) {
            topSpeedInKilometersPerHour = speedInKilometersPerHour
            val formattedTopSpeed = "%.2f".format(topSpeedInKilometersPerHour) // 2 skaic po nuliu
            val topSpeedText = "Top Speed: $formattedTopSpeed km/h"
            binding.topSpeedTextView.text = topSpeedText
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    startLocationUpdates()
                } else {
                    Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
                }
            }
            else -> {
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        dataManager.clearData()
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }
}
