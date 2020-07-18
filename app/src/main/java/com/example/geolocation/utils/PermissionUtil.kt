package com.example.geolocation.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.Context.LOCATION_SERVICE
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.provider.Settings
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.requestPermissions


const val FINE_LOCATION_PERMISSION = android.Manifest.permission.ACCESS_FINE_LOCATION
const val FINE_LOCATION_PERMISSION_REQUEST_CODE = 120

fun AppCompatActivity.checkRequiredPermissions(): Boolean {
    return if (ActivityCompat.checkSelfPermission(
            this,
            FINE_LOCATION_PERMISSION
        ) == PackageManager.PERMISSION_GRANTED
    ) {
        true
    } else {
        if (shouldAskRationale()) {
            showRationaleDialog(
                "Location Permission Required",
                "This permission will allow device to get your GPS location",
                FINE_LOCATION_PERMISSION,
                FINE_LOCATION_PERMISSION_REQUEST_CODE
            )
        } else {
            requestPermissions(
                this,
                arrayOf(FINE_LOCATION_PERMISSION),
                FINE_LOCATION_PERMISSION_REQUEST_CODE
            )
        }
        false
    }
}

fun Context.showPermissionSettings() {
    val builder = AlertDialog.Builder(this)
    builder.setTitle("Permission needed")
        .setMessage("Go to Settings and check the Location Permission inside the Permissions")
        .setPositiveButton("Settings") { dialog, _ ->
            val i = Intent()
            i.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            i.addCategory(Intent.CATEGORY_DEFAULT)
            i.data = Uri.parse(
                "package:" + this.applicationContext.packageName
            )
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            i.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
            i.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
            startActivity(i)
            dialog.dismiss()
        }
        .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
    builder.create().show()
}

fun AppCompatActivity.shouldAskRationale() =
    ActivityCompat.shouldShowRequestPermissionRationale(this, FINE_LOCATION_PERMISSION)

@SuppressLint
fun AppCompatActivity.isGpsEnabled(): Boolean {
    return (getSystemService(LOCATION_SERVICE) as LocationManager)
        .isProviderEnabled(LocationManager.GPS_PROVIDER)
}

private fun AppCompatActivity.showRationaleDialog(
    title: String,
    message: String,
    permission: String,
    requestCode: Int
) {
    val builder: AlertDialog.Builder = AlertDialog.Builder(this)
    builder.setTitle(title)
        .setMessage(message)
        .setPositiveButton("Ok") { _, _ ->
            requestPermissions(this, arrayOf(permission), requestCode)
        }
    builder.create().show()
}