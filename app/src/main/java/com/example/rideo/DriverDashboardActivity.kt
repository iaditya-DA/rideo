package com.example.rideo

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.media.MediaPlayer
import android.os.*
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import org.osmdroid.bonuspack.routing.OSRMRoadManager
import org.osmdroid.bonuspack.routing.RoadManager
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable

class DriverDashboardActivity : AppCompatActivity(), LocationListener {

    private lateinit var mapView: MapView
    private lateinit var statusMainText: TextView
    private lateinit var statusSubText: TextView
    private lateinit var rideActionButtons: LinearLayout
    private lateinit var rideProgressButtons: LinearLayout
    private lateinit var btnAcceptRide: Button
    private lateinit var btnRejectRide: Button
    private lateinit var btnArrived: Button
    private lateinit var btnComplete: Button

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var rideListener: ListenerRegistration? = null
    private var currentRideId: String? = null

    private lateinit var locationManager: LocationManager
    private var driverMarker: Marker? = null
    private var pickupLocation: GeoPoint? = null
    private var destinationLocation: GeoPoint? = null
    private var routePolyline: Polyline? = null

    private var mediaPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activtity_driver_dashboard)

        mapView = findViewById(R.id.driver_map_view)
        statusMainText = findViewById(R.id.status_main_text)
        statusSubText = findViewById(R.id.status_sub_text)
        rideActionButtons = findViewById(R.id.ride_action_buttons)
        rideProgressButtons = findViewById(R.id.ride_progress_buttons)
        btnAcceptRide = findViewById(R.id.btn_accept_ride)
        btnRejectRide = findViewById(R.id.btn_reject_ride)
        btnArrived = findViewById(R.id.btn_arrived)
        btnComplete = findViewById(R.id.btn_complete)

        setupMap()
        setDriverAlwaysOnline()
        setupButtonListeners()
    }

    private fun setupMap() {
        Configuration.getInstance().load(applicationContext, getSharedPreferences("osmdroid", MODE_PRIVATE))
        mapView.setMultiTouchControls(true)
        mapView.controller.setZoom(18.0)

        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        checkAndStartLocationUpdates()
    }

    private fun checkAndStartLocationUpdates() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
        } else {
            startLocationUpdates()
        }
    }

    private fun startLocationUpdates() {
        try {
            val isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

            if (!isGPSEnabled && !isNetworkEnabled) {
                Toast.makeText(this, "Please enable GPS or Internet", Toast.LENGTH_LONG).show()
                startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                return
            }

            if (isGPSEnabled)
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000L, 2f, this)
            if (isNetworkEnabled)
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 2000L, 2f, this)

        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    override fun onLocationChanged(location: Location) {

        val newPoint = GeoPoint(location.latitude, location.longitude)

        if (driverMarker == null) {
            initDriverMarker(newPoint)
        } else {
            animateMarkerTo(driverMarker!!, newPoint)
        }

        mapView.controller.animateTo(newPoint)

        val driverId = auth.currentUser?.uid ?: return
        db.collection("Driver").document(driverId)
            .set(
                mapOf(
                    "current_location" to mapOf(
                        "lat" to location.latitude,
                        "lng" to location.longitude,
                        "timestamp" to FieldValue.serverTimestamp()
                    )
                ), SetOptions.merge()
            )

        // Safe route drawing
        val target = destinationLocation ?: pickupLocation
        if (driverMarker != null && target != null) {
            drawLiveRoute(driverMarker!!.position, target)
        }
    }

    private fun initDriverMarker(point: GeoPoint) {

        val widthPx = (20 * resources.displayMetrics.density).toInt()
        val heightPx = (20 * resources.displayMetrics.density).toInt()

        val carDrawable = ContextCompat.getDrawable(this, R.drawable.car_top)

        val bitmap = (carDrawable as? BitmapDrawable)?.bitmap
        bitmap?.let {

            val scaledBitmap = Bitmap.createScaledBitmap(it, widthPx, heightPx, false)
            val scaledDrawable = BitmapDrawable(resources, scaledBitmap)

            driverMarker = Marker(mapView).apply {
                title = "You (Driver)"
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                position = point
                icon = scaledDrawable
                mapView.overlays.add(this)
            }

            mapView.controller.setCenter(point)
            mapView.invalidate()
        }
    }

    private fun animateMarkerTo(marker: Marker, toPosition: GeoPoint) {

        val start = SystemClock.uptimeMillis()
        val duration = 1000L
        val startLat = marker.position.latitude
        val startLng = marker.position.longitude
        val deltaLat = toPosition.latitude - startLat
        val deltaLng = toPosition.longitude - startLng
        val interpolator = LinearInterpolator()

        mapView.post(object : Runnable {
            override fun run() {
                val elapsed = SystemClock.uptimeMillis() - start
                val t = (elapsed.toFloat() / duration).coerceAtMost(1f)
                val v = interpolator.getInterpolation(t)

                marker.position = GeoPoint(
                    startLat + deltaLat * v,
                    startLng + deltaLng * v
                )

                mapView.invalidate()
                if (t < 1f) mapView.postDelayed(this, 16)
            }
        })
    }

    private fun drawLiveRoute(start: GeoPoint, end: GeoPoint) {

        Thread {
            val roadManager = OSRMRoadManager(this, "RIDEO_APP")
            val road = roadManager.getRoad(arrayListOf(start, end))

            runOnUiThread {
                routePolyline?.let { mapView.overlays.remove(it) }

                routePolyline = RoadManager.buildRoadOverlay(road)
                mapView.overlays.add(routePolyline)
                mapView.invalidate()
            }

        }.start()
    }

    private fun setDriverAlwaysOnline() {

        val driverId = auth.currentUser?.uid ?: return

        db.collection("Driver").document(driverId)
            .set(mapOf("status" to "online"), SetOptions.merge())

        listenForRideRequests()
    }

    private fun listenForRideRequests() {

        rideListener?.remove()

        rideListener = db.collection("ride_requests")
            .whereEqualTo("status", "pending")
            .addSnapshotListener { snapshot, _ ->

                if (snapshot == null || snapshot.isEmpty) return@addSnapshotListener

                val doc = snapshot.documents.first()
                currentRideId = doc.id

                pickupLocation = GeoPoint(
                    doc.getDouble("pickup_lat") ?: 0.0,
                    doc.getDouble("pickup_lng") ?: 0.0
                )

                destinationLocation = GeoPoint(
                    doc.getDouble("destination_lat") ?: 0.0,
                    doc.getDouble("destination_lng") ?: 0.0
                )

                rideActionButtons.visibility = View.VISIBLE
                rideProgressButtons.visibility = View.GONE
            }
    }

    private fun setupButtonListeners() {

        btnAcceptRide.setOnClickListener {

            val driverId = auth.currentUser?.uid ?: return@setOnClickListener
            val rideId = currentRideId ?: return@setOnClickListener

            db.collection("ride_requests").document(rideId)
                .update(
                    mapOf(
                        "status" to "accepted",
                        "driver_id" to driverId
                    )
                )

            rideActionButtons.visibility = View.GONE
            rideProgressButtons.visibility = View.VISIBLE

            driverMarker?.position?.let { driverPos ->
                pickupLocation?.let { pickup ->
                    drawLiveRoute(driverPos, pickup)
                }
            }
        }

        btnRejectRide.setOnClickListener {

            val rideId = currentRideId ?: return@setOnClickListener

            db.collection("ride_requests").document(rideId)
                .update("status", "rejected")

            resetUI()
        }
    }

    private fun resetUI() {
        rideActionButtons.visibility = View.GONE
        rideProgressButtons.visibility = View.GONE
        currentRideId = null
        pickupLocation = null
        destinationLocation = null
        routePolyline?.let { mapView.overlays.remove(it) }
        mapView.invalidate()
    }

    override fun onDestroy() {
        super.onDestroy()
        rideListener?.remove()
        locationManager.removeUpdates(this)
    }
}
