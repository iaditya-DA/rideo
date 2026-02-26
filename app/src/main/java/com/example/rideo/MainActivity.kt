package com.example.rideo

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatSpinner
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import org.osmdroid.bonuspack.routing.OSRMRoadManager
import org.osmdroid.bonuspack.routing.RoadManager
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

class   MainActivity : AppCompatActivity(), LocationListener {

    private lateinit var mapView: MapView
    private lateinit var pickupSpinner: AppCompatSpinner
    private lateinit var dropoffSpinner: AppCompatSpinner
    private lateinit var requestView: LinearLayout
    private lateinit var statusView: ConstraintLayout
    private lateinit var statusFrom: TextView
    private lateinit var statusTo: TextView
    private lateinit var statusMessage: TextView
    private lateinit var btnRequestRide: Button

    private val db = FirebaseFirestore.getInstance()
    private var rideListener: ListenerRegistration? = null
    private var driverListener: ListenerRegistration? = null
    private var currentRideId: String? = null
    private var currentDriverId: String? = null

    private lateinit var locationManager: LocationManager
    private var riderMarker: Marker? = null

    // Driver visuals & smoothing
    private var driverMarker: Marker? = null
    private var lastDriverLocation: GeoPoint? = null
    private var lastDriverBearing: Float = 0f
    private var driverRoute: Polyline? = null

    // Pickup/drop visuals
    private var pickupMarker: Marker? = null
    private var dropMarker: Marker? = null
    private var staticRoute: Polyline? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize Views
        mapView = findViewById(R.id.map_view_osm)
        pickupSpinner = findViewById(R.id.input_pickup_location)
        dropoffSpinner = findViewById(R.id.input_dropoff_location)
        requestView = findViewById(R.id.ride_request_view)
        statusView = findViewById(R.id.ride_status_view)
        statusFrom = findViewById(R.id.status_from)
        statusTo = findViewById(R.id.status_to)
        statusMessage = findViewById(R.id.status_message)
        btnRequestRide = findViewById(R.id.btn_request_ride)

        setupMap()
        setupLocationSpinners()
        checkActiveRide()
        setupRideRequestLogic()
    }

    private fun setupMap() {
        Configuration.getInstance().load(applicationContext, getSharedPreferences("osmdroid", MODE_PRIVATE))
        mapView.setMultiTouchControls(true)
        mapView.controller.setZoom(18.0)
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        mapView.controller.setCenter(GeoPoint(23.251392, 77.524738)) // Default center
        checkLocationPermission()
    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
        } else startLocationUpdates()
    }

    private fun startLocationUpdates() {
        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 3000L, 2f, this)
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 3000L, 2f, this)
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    private fun setupLocationSpinners() {
        val locations = getLnctLocations().keys.toTypedArray()
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, locations)
        pickupSpinner.adapter = adapter
        dropoffSpinner.adapter = adapter
    }

    private fun setupRideRequestLogic() {
        val lnctLocations = getLnctLocations()
        btnRequestRide.setOnClickListener {
            val fromName = pickupSpinner.selectedItem.toString()
            val toName = dropoffSpinner.selectedItem.toString()
            if (fromName == toName) {
                Toast.makeText(this, "Pickup and Dropoff cannot be same", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val fromPoint = lnctLocations[fromName] ?: return@setOnClickListener
            val toPoint = lnctLocations[toName] ?: return@setOnClickListener

            val rideRequest = hashMapOf(
                "from" to fromName,
                "to" to toName,
                "pickup_lat" to fromPoint.latitude,
                "pickup_lng" to fromPoint.longitude,
                "destination_lat" to toPoint.latitude,
                "destination_lng" to toPoint.longitude,
                "status" to "pending",
                "rider_id" to "test_rider_001", // add dummy rider id
                "timestamp" to FieldValue.serverTimestamp()
            )

            db.collection("ride_requests").add(rideRequest).addOnSuccessListener { docRef ->
                currentRideId = docRef.id
                updateUIForRide(fromName, toName)
                showPickupDestinationRoute(fromPoint, toPoint)
                listenToRideStatus()
            }
        }
    }

    private fun updateUIForRide(fromName: String, toName: String) {
        requestView.visibility = View.GONE
        statusView.visibility = View.VISIBLE
        statusFrom.text = "From: $fromName"
        statusTo.text = "To: $toName"
        statusMessage.text = "Waiting for driver..."
    }

    override fun onLocationChanged(location: Location) {
        updateRiderMarker(GeoPoint(location.latitude, location.longitude))
    }

    private fun updateRiderMarker(point: GeoPoint) {
        if (riderMarker == null) {
            riderMarker = Marker(mapView).apply {
                title = "You (Rider)"
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                mapView.overlays.add(this)
            }
        }
        riderMarker!!.position = point
        mapView.invalidate()
    }

    private fun listenToRideStatus() {
        currentRideId?.let { rideId ->
            rideListener?.remove()
            rideListener = db.collection("ride_requests").document(rideId)
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot == null || !snapshot.exists()) return@addSnapshotListener
                    val status = snapshot.getString("status") ?: "pending"
                    val driverId = snapshot.getString("driver_id")

                    when (status) {
                        "accepted" -> statusMessage.text = "Driver is on the way..."
                        "driver_arrived" -> statusMessage.text = "Driver has arrived!"
                        "completed" -> {
                            statusMessage.text = "Ride completed"
                            Toast.makeText(this, "Ride completed successfully!", Toast.LENGTH_SHORT).show()
                            mapView.postDelayed({ resetRideUI() }, 3000)
                        }
                    }

                    driverId?.let {
                        currentDriverId = it
                        listenToDriverLocation(it)
                    }
                }
        }
    }

    private fun listenToDriverLocation(driverId: String) {
        driverListener?.remove()
        driverListener = db.collection("Driver").document(driverId)
            .addSnapshotListener { snapshot, _ ->
                val snap = snapshot ?: return@addSnapshotListener
                val loc = snap.get("current_location") as? Map<*, *> ?: return@addSnapshotListener
                val lat = (loc["lat"] as? Number)?.toDouble() ?: return@addSnapshotListener
                val lng = (loc["lng"] as? Number)?.toDouble() ?: return@addSnapshotListener
                val driverPoint = GeoPoint(lat, lng)

                // compute bearing if possible: prefer last saved bearing from doc if you saved it,
                // otherwise compute from lastDriverLocation -> driverPoint
                val computedBearing = computeBearing(lastDriverLocation, driverPoint)

                runOnUiThread {
                    // 1️⃣ Update or create driver marker (with scaled 20dp icon)
                    if (driverMarker == null) {
                        driverMarker = Marker(mapView).apply {
                            title = "Driver"
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER) // center anchor so rotation looks natural
                            position = driverPoint
                            icon = getScaledCarDrawable(20) // 20dp icon
                            mapView.overlays.add(this)
                        }
                        lastDriverLocation = driverPoint
                    } else {
                        // Smoothly animate to new position and rotate
                        animateDriverMarker(driverMarker!!, driverPoint, computedBearing)
                    }

                    // 2️⃣ Update moving route (driver → destination)
                    val pickupLat = pickupMarker?.position?.latitude
                    val pickupLng = pickupMarker?.position?.longitude
                    val dropLat = dropMarker?.position?.latitude
                    val dropLng = dropMarker?.position?.longitude

                    if (pickupLat != null && pickupLng != null && dropLat != null && dropLng != null) {
                        val dropPoint = GeoPoint(dropLat, dropLng)

                        // Build route dynamically from driver current position to drop in background thread
                        Thread {
                            try {
                                val roadManager = OSRMRoadManager(this, "RIDEO_APP")
                                val road = roadManager.getRoad(arrayListOf(driverPoint, dropPoint))
                                runOnUiThread {
                                    driverRoute?.let { mapView.overlays.remove(it) }
                                    driverRoute = RoadManager.buildRoadOverlay(road).apply {
                                        outlinePaint.color = 0xFFFF9800.toInt() // Orange
                                        outlinePaint.strokeWidth = 8f
                                    }
                                    mapView.overlays.add(driverRoute)
                                    mapView.invalidate()
                                }
                            } catch (ignored: Exception) {
                            }
                        }.start()
                    }

                    mapView.invalidate()
                }
            }
    }

    private fun showPickupDestinationRoute(fromPoint: GeoPoint, toPoint: GeoPoint) {
        Thread {
            val roadManager = OSRMRoadManager(this, "RIDEO_APP")
            val road = roadManager.getRoad(arrayListOf(fromPoint, toPoint))
            runOnUiThread {
                staticRoute = RoadManager.buildRoadOverlay(road).apply {
                    outlinePaint.color = 0xFF1E88E5.toInt()
                    outlinePaint.strokeWidth = 10f
                }
                pickupMarker = Marker(mapView).apply {
                    position = fromPoint
                    title = "Pickup"
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                }
                dropMarker = Marker(mapView).apply {
                    position = toPoint
                    title = "Drop"
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                }
                mapView.overlays.add(staticRoute)
                mapView.overlays.add(pickupMarker)
                mapView.overlays.add(dropMarker)
                mapView.controller.animateTo(fromPoint)
                mapView.invalidate()
            }
        }.start()
    }

    // Animate driver marker from its current position to newPoint with rotation; ~60fps
    private fun animateDriverMarker(marker: Marker, newPoint: GeoPoint, targetBearing: Float) {
        val startPoint = lastDriverLocation ?: marker.position
        val startLat = startPoint.latitude
        val startLng = startPoint.longitude
        val endLat = newPoint.latitude
        val endLng = newPoint.longitude

        val startBearing = lastDriverBearing
        val endBearing = targetBearing

        val handler = Handler(Looper.getMainLooper())
        val animStart = SystemClock.uptimeMillis()
        val duration = 1000L // 1 second; tweak as needed for smoothness
        val interpolator = LinearInterpolator()

        handler.post(object : Runnable {
            override fun run() {
                val elapsed = SystemClock.uptimeMillis() - animStart
                val fraction = (elapsed.toFloat() / duration).coerceAtMost(1f)
                val t = interpolator.getInterpolation(fraction)

                val lat = startLat + (endLat - startLat) * t
                val lng = startLng + (endLng - startLng) * t
                marker.position = GeoPoint(lat, lng)

                // smooth bearing interpolation (shortest angle)
                val smoothedBearing = interpolateAngle(startBearing, endBearing, t)
                marker.rotation = smoothedBearing

                mapView.invalidate()

                if (fraction < 1f) {
                    handler.postDelayed(this, 16) // ~60fps
                } else {
                    // final snapshot
                    marker.position = newPoint
                    marker.rotation = endBearing
                    lastDriverLocation = newPoint
                    lastDriverBearing = endBearing
                    mapView.invalidate()
                }
            }
        })
    }

    // Helper: compute bearing from prev->curr; returns 0f if prev is null or identical
    private fun computeBearing(prev: GeoPoint?, curr: GeoPoint): Float {
        if (prev == null) return 0f
        val lat1 = Math.toRadians(prev.latitude)
        val lon1 = Math.toRadians(prev.longitude)
        val lat2 = Math.toRadians(curr.latitude)
        val lon2 = Math.toRadians(curr.longitude)
        val dLon = lon2 - lon1
        val y = Math.sin(dLon) * Math.cos(lat2)
        val x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1) * Math.cos(lat2) * Math.cos(dLon)
        val brng = Math.toDegrees(Math.atan2(y, x))
        return ((brng + 360.0) % 360.0).toFloat()
    }

    // Interpolate between two angles (degrees) along shortest path
    private fun interpolateAngle(start: Float, end: Float, t: Float): Float {
        var delta = (end - start + 540f) % 360f - 180f
        return (start + delta * t + 360f) % 360f
    }

    // Return a scaled drawable (dp -> px) for car_top resource
    private fun getScaledCarDrawable(dpSize: Int): BitmapDrawable? {
        val drawable = ContextCompat.getDrawable(this, R.drawable.car_top) ?: return null
        val bmp = (drawable as? BitmapDrawable)?.bitmap ?: run {
            // fallback: try drawing to bitmap if drawable is VectorDrawable (rare)
            return null
        }
        val px = (dpSize * resources.displayMetrics.density).toInt()
        val scaled = Bitmap.createScaledBitmap(bmp, px, px, true)
        return BitmapDrawable(resources, scaled)
    }

    private fun getLnctLocations() = mapOf(
        "LNCT MAIN" to GeoPoint(23.251392, 77.524738),
        "LNCT ELCELLENCE" to GeoPoint(23.250054, 77.522280),
        "LNCT OLD SCIENCE" to GeoPoint(23.250308, 77.526012),
        "LNCT NEW SCIENCE" to GeoPoint(23.249722, 77.527774),
        "LNCT AGRICULTURE" to GeoPoint(23.249808786262093, 77.5287256858025),
        "LNCT MCA (RAMNATH GUHA BLOCK)" to GeoPoint(23.249845599663054, 77.52836010443491),
        "LNCT CME BLOCK" to GeoPoint(23.248891364583024, 77.52499289160696),
        "CV RAMAN BLOCK" to GeoPoint(23.25049935729951, 77.52495981112644),
        "LNCT PHARMACY BLOCK" to GeoPoint(23.249087439542055, 77.5276501958869),
        "SHRI HANUMAN TEMPLE" to GeoPoint(23.251487326420854, 77.52370906237275),
        "CENTRAL LIBRARY" to GeoPoint(23.25005843201248, 77.52566156337996),
        "LNCT CAFE 9" to GeoPoint(23.250304537241863, 77.52669405741315),
        "HIDDEN CAFE" to GeoPoint(23.250597835169412, 77.52654508687048),
        "SUTO CAFE" to GeoPoint(23.25048920638257, 77.52641503322211),
        "LNCT BASKETBALL GROUND" to GeoPoint(23.249548218416667, 77.52231532180012),
        "LNCT FOOTBALL GROUND" to GeoPoint(23.249846045486503, 77.52352755490435),
        "REFFTO DRONE LAB" to GeoPoint(23.2493065803438, 77.52275232564051),
        "LNCT CENTRAL WORKSHOP" to GeoPoint(23.250017206294984, 77.52526479115247),
        "LNCT IDEA LAB" to GeoPoint(23.250202012408238, 77.52562219522491),
        "LNCT BUS STAND" to GeoPoint(23.251277994630925, 77.52315104615973),
        "LNCT ARYABHATT AUDITORIUM" to GeoPoint(23.250865446863394, 77.52325109842376),
        "RATANPUR BOYS HOSTEL" to GeoPoint(23.250455139927954, 77.52354393431897),
        "RAIPUR BOYS HOSTEL" to GeoPoint(23.250475318986606, 77.52314860586107),
        "KALYANI GIRLS HOSTEL" to GeoPoint(23.250450655692262, 77.52398074786194),
        "LNCT DANCE, MUISC AND GYM CLUB" to GeoPoint(23.250480972582967, 77.52694507531348)
    )

    private fun checkActiveRide() {
        db.collection("ride_requests").whereEqualTo("status", "pending").limit(1).get()
            .addOnSuccessListener { snapshots ->
                if (!snapshots.isEmpty) {
                    val doc = snapshots.documents.first()
                    currentRideId = doc.id
                    val from = doc.getString("from") ?: ""
                    val to = doc.getString("to") ?: ""
                    updateUIForRide(from, to)
                    listenToRideStatus()
                } else resetRideUI()
            }
    }

    private fun resetRideUI() {
        currentRideId = null
        currentDriverId = null
        statusView.visibility = View.GONE
        requestView.visibility = View.VISIBLE
        mapView.overlays.clear()
        pickupMarker = null
        dropMarker = null
        driverMarker = null
        staticRoute = null
        driverRoute = null
        lastDriverLocation = null
        lastDriverBearing = 0f
        mapView.controller.setCenter(GeoPoint(23.251392, 77.524738))
    }

    override fun onResume() { super.onResume(); mapView.onResume() }
    override fun onPause() { super.onPause(); mapView.onPause() }
    override fun onDestroy() {
        super.onDestroy()
        rideListener?.remove()
        driverListener?.remove()
        try { locationManager.removeUpdates(this) } catch (ignored: Exception) {}
    }
}
