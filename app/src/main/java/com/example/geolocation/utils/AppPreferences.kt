package com.example.geolocation.utils

import android.content.Context


object AppPreferences {

    private const val KEY_REQUESTING_LOCATION_UPDATES = "requesting_location_updates"
    private const val KEY_TRACKING_USER_LOCATION = "tracking_user_location"
    private const val APP_PREFERENCES = "geolocation_preferences"
    private const val MODE = Context.MODE_PRIVATE

    fun Context.requestingLocationUpdates(): Boolean {
        return getSharedPreferences(APP_PREFERENCES,MODE)
            .getBoolean(KEY_REQUESTING_LOCATION_UPDATES, false)
    }

    fun Context.trackingUserLocation(): Boolean {
        return getSharedPreferences(APP_PREFERENCES,MODE)
            .getBoolean(KEY_TRACKING_USER_LOCATION, false)
    }

    fun Context.setRequestingLocationUpdates(
        isRequesting: Boolean
    ) {
        getSharedPreferences(APP_PREFERENCES, MODE)
            .edit()
            .putBoolean(KEY_REQUESTING_LOCATION_UPDATES, isRequesting)
            .apply()
    }

    fun Context.setTrackingUserLocation(
        isRequesting: Boolean
    ) {
        getSharedPreferences(APP_PREFERENCES, MODE)
            .edit()
            .putBoolean(KEY_TRACKING_USER_LOCATION, isRequesting)
            .apply()
    }
}