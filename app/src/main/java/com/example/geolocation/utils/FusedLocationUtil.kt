package com.example.geolocation.utils

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import android.util.Log.d
import com.google.android.gms.location.*

@SuppressLint("MissingPermission")
class FusedLocationUtil(private val context :Context) {

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    fun userLocationRequest(onLocationUpdate: (location: Location) -> Unit) {
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
        val locationRequest = createRequest()

          locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                onLocationUpdate(locationResult.lastLocation)
                d(FusedLocationUtil::javaClass.name,"Got fused location")
            }
        }

        fusedLocationProviderClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.myLooper()
        )
        getLastKnowLocation(onLocationUpdate)
    }


    private fun getLastKnowLocation(onLocationUpdate: (location: Location) -> Unit) {
        fusedLocationProviderClient.lastLocation.addOnCompleteListener {
            if (it.isSuccessful && it.result != null) {
                onLocationUpdate(it.result!!)
            }
        }
    }


    private fun createRequest() = LocationRequest.create().apply {
        interval = 10000 // 10 second delay between each request
        fastestInterval = 10000 // 10 seconds fastest time in between each request
        smallestDisplacement = 10f // 10 meters minimum displacement for new location request
        priority = LocationRequest.PRIORITY_HIGH_ACCURACY // GPS high accuracy location requests
    }

    fun unregister(){
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
    }


}




