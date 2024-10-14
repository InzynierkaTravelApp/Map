package com.example.myapplication2

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.gms.maps.MapsInitializer
import okhttp3.*
import org.json.JSONObject
import java.io.IOException

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mapView: MapView
    private lateinit var googleMap: GoogleMap
    private lateinit var btnConfirm: Button
    private val selectedPoints = mutableListOf<LatLng>()
    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize the map
        mapView = findViewById(R.id.mapView)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        // Initialize the confirm button
        btnConfirm = findViewById(R.id.btnConfirm) // Initialize btnConfirm

        // Handle confirm button click
        btnConfirm.setOnClickListener {
            if (selectedPoints.size > 1) {
                drawRoute()
            }
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        MapsInitializer.initialize(this)

        googleMap.setOnMapClickListener { latLng ->
            // Add a marker to the selected location
            googleMap.addMarker(MarkerOptions().position(latLng))
            selectedPoints.add(latLng)
            drawRoute()
        }

        // Initial map camera position
        val initialLocation = LatLng(54.35, 18.64)
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(initialLocation, 10f))
    }


    private fun drawRoute() {
        if (selectedPoints.size < 2) return

        // Optimize the selected points order
        val optimizedPoints = optimizeRoute(selectedPoints)

        // Extract the origin, destination, and waypoints from optimized points
        val origin = optimizedPoints.first()
        val destination = optimizedPoints.last()
        val waypoints = optimizedPoints.subList(1, optimizedPoints.size - 1)

        // Clear previous polylines and markers before drawing a new one
        googleMap.clear()

        // Re-add markers for all points in the optimized route
        optimizedPoints.forEach { point ->
            googleMap.addMarker(MarkerOptions().position(point))
        }

        // Build the Directions API URL
        val url = buildDirectionsUrl(origin, destination, waypoints)

        // Make the API request
        val request = Request.Builder().url(url).build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Failed to fetch directions.", Toast.LENGTH_SHORT).show()
                    e.printStackTrace()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "Error fetching directions: ${response.message}", Toast.LENGTH_SHORT).show()
                    }
                    return
                }

                val data = response.body?.string() ?: return
                val jsonResponse = JSONObject(data)
                val routes = jsonResponse.getJSONArray("routes")

                if (routes.length() > 0) {
                    val polyline = routes.getJSONObject(0)
                        .getJSONObject("overview_polyline")
                        .getString("points")
                    runOnUiThread {
                        googleMap.addPolyline(PolylineOptions().addAll(decodePolyline(polyline)))
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "No routes found.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }


    private fun optimizeRoute(points: List<LatLng>): List<LatLng> {
        if (points.isEmpty()) return emptyList()

        val optimizedRoute = mutableListOf<LatLng>()
        val unvisited = points.toMutableList()

        // Start from the first point
        var currentPoint = unvisited.removeAt(0)
        optimizedRoute.add(currentPoint)

        while (unvisited.isNotEmpty()) {
            var nearestPoint: LatLng? = null
            var nearestDistance = Double.MAX_VALUE

            // Find the nearest unvisited point
            for (point in unvisited) {
                val distance = haversine(currentPoint, point)
                if (distance < nearestDistance) {
                    nearestDistance = distance
                    nearestPoint = point
                }
            }

            // Move to the nearest point
            if (nearestPoint != null) {
                optimizedRoute.add(nearestPoint)
                unvisited.remove(nearestPoint)
                currentPoint = nearestPoint
            }
        }

        return optimizedRoute
    }

    // Haversine formula to calculate distance between two LatLng points
    private fun haversine(point1: LatLng, point2: LatLng): Double {
        val R = 6371e3 // Earth radius in meters
        val lat1 = Math.toRadians(point1.latitude)
        val lat2 = Math.toRadians(point2.latitude)
        val deltaLat = Math.toRadians(point2.latitude - point1.latitude)
        val deltaLng = Math.toRadians(point2.longitude - point1.longitude)

        val a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) +
                Math.cos(lat1) * Math.cos(lat2) *
                Math.sin(deltaLng / 2) * Math.sin(deltaLng / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))

        return R * c // Distance in meters
    }




    private fun buildDirectionsUrl(
        origin: LatLng,
        destination: LatLng,
        waypoints: List<LatLng>
    ): String {
        val apiKey = "AIzaSyBcWOts8IFU79FQNhPXsptKGE5K0m4IQhE"
        val originParam = "origin=${origin.latitude},${origin.longitude}"
        val destinationParam = "destination=${destination.latitude},${destination.longitude}"
        val waypointsParam = if (waypoints.isNotEmpty()) {
            "waypoints=" + waypoints.joinToString("|") { "${it.latitude},${it.longitude}" }
        } else ""

        return "https://maps.googleapis.com/maps/api/directions/json?$originParam&$destinationParam&$waypointsParam&key=$apiKey"
    }

    private fun decodePolyline(encoded: String): List<LatLng> {
        val poly = ArrayList<LatLng>()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0

        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lat += dlat

            shift = 0
            result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lng += dlng

            val latLng = LatLng(lat.toDouble() / 1E5, lng.toDouble() / 1E5)
            poly.add(latLng)
        }

        return poly
    }

    // Override lifecycle methods for MapView
    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }
}
