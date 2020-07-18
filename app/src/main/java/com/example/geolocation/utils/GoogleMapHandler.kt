package com.example.geolocation.utils

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.SystemClock
import android.view.animation.LinearInterpolator
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.*
import java.io.IOException
import java.util.*

@SuppressLint("MissingPermission")
class GoogleMapHandler(private val context: Context, private val googleMap: GoogleMap) :
    GoogleMap.OnCameraIdleListener {

    private var targetMarker: Marker? = null
    private var sourceMarker: Marker? = null
    private var sourceLastLocation: LatLng? = null
    private var targetLastLocation: LatLng? = null
    private var targetPolyline: Polyline? = null
    private var focusedMarker = CameraFocus.SOURCE_MARKER

    enum class CameraFocus {
        TARGET_MARKER, SOURCE_MARKER
    }

    init {
        googleMap.isMyLocationEnabled = true
        googleMap.setOnCameraIdleListener(this)
    }


    fun focusCameraOn(focus: CameraFocus) {
        focusedMarker = focus
        val location = if (focusedMarker == CameraFocus.SOURCE_MARKER) {
            sourceLastLocation
        } else {
            targetLastLocation
        }

        location ?: return
        googleMap.animateCamera(
            CameraUpdateFactory.newLatLng(
                location
            ), 800, null
        )
    }

    fun removeTargetMarker() {
        targetMarker?.remove()
        targetPolyline?.remove()
        targetPolyline = null
        targetMarker = null
        sourceMarker?.showInfoWindow()
    }

    fun updateTargetMarker(lat: Double, lng: Double) {
        val latLng = LatLng(lat, lng)
        targetLastLocation = latLng
        if (targetPolyline == null) {
            targetPolyline = googleMap.addPolyline(
                PolylineOptions()
                    .width(8f)
                    .color(Color.GREEN)
                    .geodesic(true)
                    .add(latLng)
            )
        } else {
            val lastPoints = targetPolyline?.points
            lastPoints?.add(latLng)
            targetPolyline?.points = lastPoints
        }

        if (targetMarker != null) {
            targetMarker?.moveToLocation(latLng)
        } else {
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 20f), 1500, null)
            targetMarker = googleMap.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title("Source")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                    .snippet(getAddressFromLatLng(latLng))
            )
            targetMarker?.showInfoWindow()
        }
    }

    fun updateSourceMarker(location: Location) {

        val latLng = LatLng(location.latitude, location.longitude)
        sourceLastLocation = latLng

        if (sourceMarker != null) {
            sourceMarker?.moveToLocation(latLng)
        } else {
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 20f), 1500, null)
            sourceMarker = googleMap.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title("You")
                    .snippet(getAddressFromLatLng(latLng))
            )
            sourceMarker?.showInfoWindow()
        }
    }

    private fun Marker.moveToLocation(destination: LatLng) {
        focusCameraOn(focusedMarker)
        val marker = this
        val startPosition = marker.position
        val latLngInterpolator = LatLngInterpolator.LinearFixed()
        val valueAnimator = ValueAnimator.ofFloat(0f, 1f)
        valueAnimator.duration = 800
        valueAnimator.interpolator = LinearInterpolator()
        valueAnimator.addUpdateListener { animation ->
            try {
                this.position = latLngInterpolator.interpolate(
                    animation.animatedFraction,
                    startPosition,
                    destination
                )
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
        valueAnimator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                super.onAnimationEnd(animation)
                marker.snippet = getAddressFromLatLng(destination)
                marker.showInfoWindow()
            }
        })
        valueAnimator.start()
    }

    @SuppressLint
    fun GoogleMap.repositionMarker(latLng: LatLng) {
        if (!this.projection
                .visibleRegion.latLngBounds
                .contains(latLng)
        ) {
            moveCamera(CameraUpdateFactory.newLatLng(latLng))
        }
    }


    fun getAddressFromLatLng(latLng: LatLng): String? {
        try {
            val addresses: List<Address> = Geocoder(context, Locale.getDefault())
                .getFromLocation(latLng.latitude, latLng.longitude, 1)
            return addresses[0].getAddressLine(0)
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return "Unknown Location"
    }

    private var lastRefresh: Long = 0
    override fun onCameraIdle() {
        if (SystemClock.elapsedRealtime() - lastRefresh > 1200) {
            lastRefresh = SystemClock.elapsedRealtime()
            focusCameraOn(focusedMarker)
        }
    }


}


