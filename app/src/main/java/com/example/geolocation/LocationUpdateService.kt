package com.example.geolocation

import android.app.*
import android.content.Intent
import android.content.res.Configuration
import android.location.Location
import android.os.*
import android.util.Log.e
import android.util.Log.i
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.geolocation.models.LocationModel
import com.example.geolocation.utils.AppPreferences.requestingLocationUpdates
import com.example.geolocation.utils.AppPreferences.setRequestingLocationUpdates
import com.example.geolocation.utils.FusedLocationUtil
import com.google.firebase.database.FirebaseDatabase


class LocationUpdateService : Service() {
    private val binder: IBinder = LocalBinder()
    companion object{
        private val PACKAGE_NAME = {LocationUpdateService::class.java.name}
        val ACTION_BROADCAST = "$PACKAGE_NAME.broadcast"
        val EXTRA_LOCATION = "$PACKAGE_NAME.location"
        val EXTRA_STARTED_FROM_NOTIFICATION = "$PACKAGE_NAME.started_from_notification"
    }

    private val NOTIFICATION_CHANNEL_ID = "location_service_notification"
    private val TAG: String = this::class.java.simpleName
    private val NOTIFICATION_ID = 132132


    private lateinit var fusedLocationUtil: FusedLocationUtil
    override fun onBind(intent: Intent): IBinder {
        i(TAG, "in onBind()")
        stopForeground(true)
        configurationChanged = false
        return binder
    }

    override fun onRebind(intent: Intent?) {
        i(TAG, "in onRebind()")
        stopForeground(true)
        configurationChanged = false
        super.onRebind(intent)
    }

    override fun onCreate() {
        i(TAG, " onCreate")
        val handlerThread = HandlerThread(TAG)
        handlerThread.start()
        serviceHandler = Handler(handlerThread.looper)

        fusedLocationUtil = FusedLocationUtil(this)

        fusedLocationUtil.also {
            try {
                fusedLocationUtil.userLocationRequest {
                    onNewLocation(it)
                }
            } catch (se: SecurityException) {
                setRequestingLocationUpdates(false)
                e(TAG, "Lost location permission. Could not request updates. $se")
            }
        }
    }

    private fun onNewLocation(location: Location) {
        i(TAG, "New location \n \n : $location")
        this.location = location
        if (requestingLocationUpdates())
            shareLocationToFireBase(location)

        val intent = Intent(ACTION_BROADCAST)
        intent.putExtra(EXTRA_LOCATION, location)
        LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intent)
    }


    private var configurationChanged = false
    private var serviceHandler: Handler? = null
    private var location: Location? = null

    override fun onDestroy() {
        serviceHandler?.removeCallbacksAndMessages(null)
        fusedLocationUtil.unregister()
        i(TAG, " onDestroy")
        super.onDestroy()
    }

    fun requestLocationUpdates() {
        i(TAG, "Requesting location updates")
        setRequestingLocationUpdates(true)
        startService(Intent(applicationContext, this::class.java))
        /*fusedLocationUtil.also {
            try {
                fusedLocationUtil.userLocationRequest {
                    onNewLocation(it)
                }
            } catch (se: SecurityException) {
                setRequestingLocationUpdates(false)
                e(TAG, "Lost location permission. Could not request updates. $se")
            }
        }*/
    }

    fun removeLocationUpdates() {
        i(TAG, "Removing location updates")
        try {
            //fusedLocationUtil.unregister()
            setRequestingLocationUpdates(false)
            stopSelf()
        } catch (unlikely: SecurityException) {
            setRequestingLocationUpdates(true)
            e(TAG, "Lost location permission. Could not remove updates. $unlikely")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        i(TAG, " onStartCommand")

        val startedFromNotification = intent!!.getBooleanExtra(
            EXTRA_STARTED_FROM_NOTIFICATION,
            false
        )
        if (startedFromNotification) {
            removeLocationUpdates()
            stopSelf()
        }
        return START_NOT_STICKY
    }

    override fun onUnbind(intent: Intent?): Boolean {
        i(TAG, "onUnbind")
        if (!configurationChanged && requestingLocationUpdates()) {
            i(TAG, "Starting foreground service")
            startForeground(NOTIFICATION_ID, buildNotification())
        }
        return true
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        configurationChanged = true
    }

    private fun buildNotification(): Notification {
        val intent = Intent(this, this::class.java)
        // Extra to figure out if we arrived in onStartCommand via the notification or not.
        intent.putExtra(EXTRA_STARTED_FROM_NOTIFICATION, true)
        // PendingIntent that leads to a call to onStartCommand() in this service.
        val servicePendingIntent = PendingIntent.getService(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
        // PendingIntent to launch activity.
        val activityPendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MapsActivity::class.java),
            0
        )


        val builder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID).apply {
            addAction(R.drawable.common_full_open_on_phone, "Launch", activityPendingIntent)
            addAction(R.drawable.baseline_highlight_off_black_24, "Cancel", servicePendingIntent)
            setContentTitle("Tracking Location")
            setContentText("Your location is being shared to firebase")
            setOngoing(true)
            setSmallIcon(R.mipmap.ic_launcher_round)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            val name: CharSequence = getString(R.string.app_name)
            val channel =
                NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    name,
                    NotificationManager.IMPORTANCE_DEFAULT
                )
            notificationManager.createNotificationChannel(channel)
            builder.setChannelId(NOTIFICATION_CHANNEL_ID)
        }
        return builder.build()
    }

    inner class LocalBinder : Binder() {
        val service: LocationUpdateService
            get() = this@LocationUpdateService
    }

    private fun shareLocationToFireBase(location: Location) {
        val database = FirebaseDatabase.getInstance()
        val dataBaseReference = database.getReference("locations")
        dataBaseReference.setValue(
            LocationModel(
                location.latitude,
                location.longitude,
                location.time
            )
        )
    }

}
