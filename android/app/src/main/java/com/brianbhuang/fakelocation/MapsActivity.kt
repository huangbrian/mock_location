package com.brianbhuang.fakelocation

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Button
import androidx.core.app.ActivityCompat
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import java.util.*
import kotlin.concurrent.timer
import com.crashlytics.android.Crashlytics;
import com.google.android.gms.common.api.Status
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.Place.*
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import io.fabric.sdk.android.Fabric;

class MapsActivity : AppCompatActivity(), OnMapReadyCallback,
GoogleMap.OnMapClickListener, LocationListener, PlaceSelectionListener, View.OnClickListener {
    override fun onLocationChanged(p0: Location?) {
        //TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onStatusChanged(p0: String?, p1: Int, p2: Bundle?) {
        //TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onProviderEnabled(p0: String?) {
        //TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onProviderDisabled(p0: String?) {
        //TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onPlaceSelected(p0: Place) {
        val latlng = p0.latLng
        println(p0.toString()+" here----")
        if (latlng != null) {
            placeMarkerOnMap(latlng)
            selected = latlng
            mMap.animateCamera(CameraUpdateFactory.newLatLng(latlng))

            val editor = sharedPreferences.edit()
            editor.putFloat("lat", latlng.latitude.toFloat())
            editor.putFloat("long", latlng.longitude.toFloat())
            editor.apply()
        }
    }

    override fun onError(p0: Status) {
        Log.i(p0.toString()," invalid input")
    }

    override fun onClick(p0: View?) {
        updateLocation(selected)
    }

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var lm: LocationManager
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var location: LatLng
    private lateinit var selected: LatLng
    private var timer: Timer? = null
    private var needsSetup = false

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Fabric.with(this, Crashlytics())
        setContentView(R.layout.activity_maps)
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        // Initialize place API
        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, applicationContext.getString(R.string.api_key));
        }
        val acsupportFragment = supportFragmentManager
                .findFragmentById(R.id.autocomplete_fragment) as AutocompleteSupportFragment
        acsupportFragment.setPlaceFields(listOf(Field.ID, Field.NAME, Field.LAT_LNG))
        acsupportFragment.setOnPlaceSelectedListener(this)
        acsupportFragment.view?.setBackgroundColor(Color.WHITE)

        val button = findViewById<Button>(R.id.update)
        button.setOnClickListener(this)
    }

    private fun setUpMap() {
        if (ActivityCompat.checkSelfPermission(this,
                        android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)

            Handler().post({
                val mainIntent = Intent(this@MapsActivity, Splash::class.java)
                this@MapsActivity.startActivity(mainIntent)
                finish()
            })
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
        selected = latLng
        location = latLng

        timer = timer(period = 2000L) {
            updateLocation(location)
        }
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

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        setUpMap()
    }

    private fun placeMarkerOnMap(location: LatLng) {
        mMap.clear()
        val markerOptions = MarkerOptions().position(location)
        mMap.addMarker(markerOptions)
    }

    private fun updateLocation(p0: LatLng) {
        var fakePoint = Location(LocationManager.GPS_PROVIDER)
        fakePoint.latitude = p0.latitude
        fakePoint.longitude = p0.longitude
        fakePoint.accuracy = 0F
        fakePoint.time = System.currentTimeMillis()
        fakePoint.altitude = 0.0
        fakePoint.elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()

        location = p0
        val editor = sharedPreferences.edit()
        editor.putFloat("lat", p0.latitude.toFloat())
        editor.putFloat("long", p0.longitude.toFloat())
        editor.apply()

        lm.setTestProviderLocation(LocationManager.GPS_PROVIDER, fakePoint)
        fakePoint.provider = LocationManager.NETWORK_PROVIDER
        lm.setTestProviderLocation(LocationManager.NETWORK_PROVIDER, fakePoint)
        fusedLocationClient.setMockLocation(fakePoint)
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
            }
            builder.setCancelable(false)
            val alertDialog = builder.create()
            alertDialog.show()
        }

        mMap.uiSettings.isZoomControlsEnabled = true
        mMap.setPadding(0,150,0,0)
        mMap.setOnMapClickListener(this)
    }

    override fun onMapClick(p0: LatLng?) {
        if (ActivityCompat.checkSelfPermission(this,
                        android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
            Handler().post({
                val mainIntent = Intent(this@MapsActivity, Splash::class.java)
                this@MapsActivity.startActivity(mainIntent)
                finish()
            })
            return
        }
        if (p0 != null) {
            placeMarkerOnMap(p0)
            selected = p0
        }
    }

    override fun onResume() {
        super.onResume()
        if(needsSetup) {
            try {
                setUpMap()
                needsSetup = false
            } catch (exception: java.lang.SecurityException) {
                val builder = AlertDialog.Builder(this)
                builder.setMessage("Please set App as Mock Location App in Developer Settings")
                builder.setNeutralButton("OK") {_,_->
                    needsSetup = true
                    startActivity(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS))
                }
                builder.setCancelable(false)
                val alertDialog = builder.create()
                alertDialog.show()
            }
        }
    }
}