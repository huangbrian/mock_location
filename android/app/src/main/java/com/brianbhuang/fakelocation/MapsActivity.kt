package com.brianbhuang.fakelocation

import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.os.SystemClock
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

class MapsActivity : AppCompatActivity(), OnMapReadyCallback,
GoogleMap.OnMapClickListener {

    class FakeLocation : LocationSource {

        private var listener: LocationSource.OnLocationChangedListener? = null
        private var fakePoint: Location

        constructor(loc: LatLng) {
            fakePoint = Location("fake")
            fakePoint.latitude = loc.latitude
            fakePoint.longitude = loc.longitude
            fakePoint.accuracy = 100F
            listener?.onLocationChanged(fakePoint)
        }

        override fun activate(p0: LocationSource.OnLocationChangedListener?) {
            if (p0 != null) {
                this.listener = p0
            }
        }

        fun move(point: LatLng) {
            fakePoint = Location("fake")
            fakePoint.latitude = point.latitude
            fakePoint.longitude = point.longitude
            fakePoint.accuracy = 100F
            listener?.onLocationChanged(fakePoint)
        }

        override fun deactivate() {
            this.listener = null
        }
    }

    private lateinit var mMap: GoogleMap
    //private lateinit var lastLocation: Location
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var source: FakeLocation
    //private lateinit var locationCallback: LocationCallback
    private lateinit var lm: LocationManager

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }

    override fun onMapClick(p0: LatLng?) {
        if (p0 != null) {
            placeMarkerOnMap(p0)
            source.move(p0)
            var fakePoint = Location("gps")
            fakePoint.latitude = p0.latitude
            fakePoint.longitude = p0.longitude
            fakePoint.accuracy = 0F
            fakePoint.time = System.currentTimeMillis()
            fakePoint.altitude = 0.0
            fakePoint.elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
            fusedLocationClient.setMockLocation(fakePoint)

            lm.setTestProviderLocation(LocationManager.GPS_PROVIDER, fakePoint)
            fakePoint.provider = "network"
            lm.setTestProviderLocation(LocationManager.NETWORK_PROVIDER, fakePoint)
//            lm.setTestProviderEnabled(LocationManager.PASSIVE_PROVIDER, true)
            for (provide in lm.allProviders) {
                println(provide)
            }
            println(lm.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)?.toString() + " pas")
            println(lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)?.toString() + " net")
            println(lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)?.toString() + " gps")
            println(lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)?.toString() + " net")
            println(lm.isProviderEnabled(LocationManager.GPS_PROVIDER)?.toString() + " gps")
            println(fakePoint.toString() + "fakeloc")
            println("\ncomplete\n")
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
        lm.addTestProvider(providergps.name, provider.requiresNetwork(),
                providergps.requiresSatellite(), providergps.requiresCell(),
                providergps.hasMonetaryCost(), providergps.supportsAltitude(),
                providergps.supportsSpeed(), providergps.supportsBearing(),
                providergps.powerRequirement, providergps.accuracy)
        //val providerps = lm.getProvider(LocationManager.PASSIVE_PROVIDER)
//            /lm.addTestProvider(providerps.name, providerps.requiresNetwork(),
//                    providerps.requiresSatellite(), providerps.requiresCell(),
//                    providerps.hasMonetaryCost(), providerps.supportsAltitude(),
//                    providerps.supportsSpeed(), providerps.supportsBearing(),
//                    providerps.powerRequirement, providerps.accuracy)
//            //lm.setTestProviderLocation("fake", fakePoint)
        lm.setTestProviderEnabled(LocationManager.GPS_PROVIDER, true)
        lm.setTestProviderEnabled(LocationManager.NETWORK_PROVIDER, true)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
//        locationCallback = object : LocationCallback() {
//            override fun onLocationResult(locationResult: LocationResult?) {
//                locationResult ?: return
//                for (location in locationResult.locations){
//                    // Update UI with location data
//                    // ...
//                }
//            }
//        }
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }

//    override fun onResume() {
//        super.onResume()
//        startLocationUpdates()
//    }
//
//    private fun startLocationUpdates() {
//        fusedLocationClient.requestLocationUpdates(LocationRequest.create(), locationCallback, null)
//    }
//
//    override fun onPause() {
//        super.onPause()
//        stopLocationUpdates()
//    }
//
//    private fun stopLocationUpdates() {
//        fusedLocationClient.removeLocationUpdates(locationCallback)
//    }

    private fun setUpMap() {
        if (ActivityCompat.checkSelfPermission(this,
                        android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
            return
        }
        mMap.isMyLocationEnabled = true

        fusedLocationClient.lastLocation.addOnSuccessListener(this) { location ->

            if (location != null) {
                val currentLatLng = LatLng(location.latitude, location.longitude)
                placeMarkerOnMap(currentLatLng)
                source = FakeLocation(currentLatLng)
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 12f))
                mMap.setLocationSource(source)
                println("done\n\n\n\n\n\n")
            }
            else {
                val nolatlng = LatLng(37.422, -122.084)
                placeMarkerOnMap(nolatlng)
                source = FakeLocation(nolatlng)
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(nolatlng, 12f))
                mMap.setLocationSource(source)
            }
            fusedLocationClient.setMockMode(true)
            setUpMocks()
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

        setUpMap()
        //setUpMocks()
        mMap.uiSettings.isZoomControlsEnabled = true
        mMap.setOnMapClickListener(this)
    }
}