package com.brianbhuang.fakelocation

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.SystemClock
import android.provider.Settings
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import java.util.*
import kotlin.concurrent.timer

class MapsActivity : AppCompatActivity(), OnMapReadyCallback,
GoogleMap.OnMapClickListener, LocationListener {
    override fun onLocationChanged(p0: Location?) {
//        println(p0.toString() + " ----- location change")
//        if (p0 != null) {
//            println(p0.isFromMockProvider.toString() + " -------- is mock")
//            println(p0.provider.toString() + " --------- provider")
//        }
    }

    override fun onStatusChanged(p0: String?, p1: Int, p2: Bundle?) {
//        println(p0 + " ----- status change")
    }

    override fun onProviderEnabled(p0: String?) {
        //TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onProviderDisabled(p0: String?) {
        //TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

//    override fun onPause() {
//        super.onPause()
//        if(needsSetup)
//            println("paused for settings")
//    }

    override fun onResume() {
        super.onResume()
        if(needsSetup) {
            setUpMap()
            needsSetup = false
        }
    }

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var lm: LocationManager
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var location: LatLng
    private var timer: Timer? = null
    private var needsSetup = false

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }

    @SuppressLint("MissingPermission")
    override fun onMapClick(p0: LatLng?) {
        if (p0 != null) {
            placeMarkerOnMap(p0)
            updateLocation(p0)
            location = p0

            val editor = sharedPreferences.edit()
            editor.putFloat("lat", p0.latitude.toFloat())
            editor.putFloat("long", p0.longitude.toFloat())
            editor.apply()

            //debugging code
//            println(lm.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)?.toString() + " pas")
//            println(lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)?.toString() + " net")
//            println(lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)?.toString() + " gps")
//            println(lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)?.toString() + " net")
//            println(lm.isProviderEnabled(LocationManager.GPS_PROVIDER)?.toString() + " gps")
//            println("\ncomplete\n")
        }
    }

    private fun updateLocation(p0: LatLng) {
        var fakePoint = Location(LocationManager.GPS_PROVIDER)
        fakePoint.latitude = p0.latitude
        fakePoint.longitude = p0.longitude
        fakePoint.accuracy = 0F
        fakePoint.time = System.currentTimeMillis()
        fakePoint.altitude = 0.0
        fakePoint.elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()

        lm.setTestProviderLocation(LocationManager.GPS_PROVIDER, fakePoint)
            fakePoint.provider = LocationManager.NETWORK_PROVIDER
            lm.setTestProviderLocation(LocationManager.NETWORK_PROVIDER, fakePoint)
        fusedLocationClient.setMockLocation(fakePoint)
    }

    private fun setUpMocks () {
        val provider = lm.getProvider(LocationManager.NETWORK_PROVIDER)
        lm.addTestProvider(provider.name, provider.requiresNetwork(),
                provider.requiresSatellite(), provider.requiresCell(),
                provider.hasMonetaryCost(), provider.supportsAltitude(),
                provider.supportsSpeed(), provider.supportsBearing(),
                provider.powerRequirement, provider.accuracy)
        val providergps = lm.getProvider(LocationManager.GPS_PROVIDER)
        lm.addTestProvider(providergps.name, providergps.requiresNetwork(),
                providergps.requiresSatellite(), providergps.requiresCell(),
                providergps.hasMonetaryCost(), providergps.supportsAltitude(),
                providergps.supportsSpeed(), providergps.supportsBearing(),
                providergps.powerRequirement, providergps.accuracy)
        lm.setTestProviderEnabled(LocationManager.GPS_PROVIDER, true)
        lm.setTestProviderEnabled(LocationManager.NETWORK_PROVIDER, true)
        fusedLocationClient.setMockMode(true)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    private fun setUpMap() {
        if (ActivityCompat.checkSelfPermission(this,
                        android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
            return
        }
        mMap.isMyLocationEnabled = true

        lm.requestLocationUpdates("gps",0,0f,this)
        lm.requestLocationUpdates("network",0,0f,this)
        lm.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER,0,0f,this)

        setUpMocks()

        sharedPreferences = this.getSharedPreferences("mock_location", android.content.Context.MODE_PRIVATE)

        val latLng = LatLng(sharedPreferences.getFloat("lat", 37.422F).toDouble(),
                sharedPreferences.getFloat("long", -122.084F).toDouble())
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 7f))
        placeMarkerOnMap(latLng)
        updateLocation(latLng)
        location = latLng

        timer = timer(period = 5000L) {
            updateLocation(location)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        setUpMap()
    }

    private fun placeMarkerOnMap(location: LatLng) {
        mMap.clear()
        val markerOptions = MarkerOptions().position(location)
        mMap.addMarker(markerOptions)
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        lm = this.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        try {
            setUpMap()

        } catch (exception: java.lang.SecurityException) {
            val builder = AlertDialog.Builder(this)
            builder.setMessage("Please set App as Mock Location App in Developer Settings")
            builder.setNeutralButton("OK") {_,_->
                needsSetup = true
                startActivity(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS))
                //finish()
            }
            builder.setCancelable(false)
            val alertDialog = builder.create()
            alertDialog.show()
            //println("failed")
        }

        mMap.uiSettings.isZoomControlsEnabled = true
        mMap.setOnMapClickListener(this)
    }
}