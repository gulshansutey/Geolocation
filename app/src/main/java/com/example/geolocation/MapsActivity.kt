package com.example.geolocation

import android.content.*
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.IBinder
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.geolocation.models.LocationModel
import com.example.geolocation.utils.AppPreferences.requestingLocationUpdates
import com.example.geolocation.utils.FINE_LOCATION_PERMISSION_REQUEST_CODE
import com.example.geolocation.utils.GoogleMapHandler
import com.example.geolocation.utils.checkRequiredPermissions
import com.example.geolocation.utils.showPermissionSettings
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.firebase.database.*
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_maps.*


class MapsActivity : AppCompatActivity(), OnMapReadyCallback, ServiceConnection {

    private var isBound = false
    private var locationUpdateService: LocationUpdateService? = null
    private lateinit var mapFragment: SupportMapFragment
    private lateinit var myReceiver: MyReceiver
    private lateinit var firebaseDatabaseReference: DatabaseReference
    private lateinit var googleMapHandler: GoogleMapHandler

    private var hasPermissionGranted = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        init()
    }

    private fun init() {
        hasPermissionGranted = checkRequiredPermissions()
        firebaseDatabaseReference = FirebaseDatabase.getInstance().getReference("locations")
        myReceiver = MyReceiver()

        mapFragment = supportFragmentManager
            .findFragmentById(map.id) as SupportMapFragment
        mapFragment.getMapAsync(this)

        share_location_switch.setOnCheckedChangeListener { btn, state ->
            if (hasPermissionGranted)
                changeShareSwitchState(state)
            else { btn.isChecked = false
                showPermissionSettings()}
        }

        track_location_switch.setOnCheckedChangeListener { btn, state ->
            if (hasPermissionGranted)
                changeTrackingSwitchState(state)
            else { btn.isChecked = false
                showPermissionSettings()}
        }
    }

    private fun changeTrackingSwitchState(state: Boolean) {
        if (state) {
            googleMapHandler.focusCameraOn(GoogleMapHandler.CameraFocus.TARGET_MARKER)
            share_location_switch.isChecked = !state
            track_location_switch.text = getString(R.string.start_tracking_switch_txt)
            firebaseDatabaseReference.addValueEventListener(firebaseEventListener)
        } else {
            googleMapHandler.focusCameraOn(GoogleMapHandler.CameraFocus.SOURCE_MARKER)
            googleMapHandler.removeTargetMarker()
            track_location_switch.text = getString(R.string.stop_tracking_switch_txt)
            firebaseDatabaseReference.removeEventListener(firebaseEventListener)
        }
    }

    private fun changeShareSwitchState(state: Boolean) {
        if (state) {
            track_location_switch.isChecked = !state
            locationUpdateService?.requestLocationUpdates()
            share_location_switch.text = getString(R.string.start_sharing_location_switch_txt)
        } else {
            locationUpdateService?.removeLocationUpdates()
            share_location_switch.text = getString(R.string.stop_sharing_location_switch_txt)
        }
    }

    private lateinit var googleMap: GoogleMap
    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        if (!hasPermissionGranted) return
        initMap()
    }
    private fun initMap() {
        googleMapHandler = GoogleMapHandler(this, googleMap)
    }

    private val firebaseEventListener = object : ValueEventListener {
        override fun onCancelled(de: DatabaseError) {

        }

        override fun onDataChange(dataSnapshot: DataSnapshot) {
            if (dataSnapshot.value != null) {
                val locationModel =
                    Gson().fromJson(dataSnapshot.value.toString(), LocationModel::class.java)
                googleMapHandler.updateTargetMarker(locationModel.lat, locationModel.lng)
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == FINE_LOCATION_PERMISSION_REQUEST_CODE)
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(
                    this@MapsActivity,
                    "Location permission required to work this app properly",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                initMap()
                bindLocationService()
                hasPermissionGranted = true
            }
    }


    override fun onResume() {
        super.onResume()
        mapFragment.onResume()
        LocalBroadcastManager.getInstance(this).registerReceiver(
            myReceiver,
            IntentFilter(LocationUpdateService.ACTION_BROADCAST)
        )
    }

    override fun onStart() {
        super.onStart()

        if (!hasPermissionGranted) return

        bindLocationService()
    }

    private fun bindLocationService(){
        share_location_switch.isChecked = requestingLocationUpdates()
        bindService(
            Intent(this, LocationUpdateService::class.java),
            this@MapsActivity,
            Context.BIND_AUTO_CREATE
        )
    }

    override fun onStop() {
        if (isBound) {
            unbindService(this)
            isBound = false
        }
        super.onStop()
    }

    override fun onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(myReceiver)
        mapFragment.onPause()
        super.onPause()
    }


    override fun onServiceDisconnected(component: ComponentName?) {
        isBound = false
        locationUpdateService = null
    }

    override fun onServiceConnected(component: ComponentName?, service: IBinder?) {
        val binder = service as LocationUpdateService.LocalBinder
        locationUpdateService = binder.service
        isBound = true
    }

    inner class MyReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            val location =
                intent.getParcelableExtra<Location>(LocationUpdateService.EXTRA_LOCATION)
            if (location != null) {
                if (::googleMapHandler.isInitialized && hasPermissionGranted)
                    googleMapHandler.updateSourceMarker(location)
            }
        }
    }


}