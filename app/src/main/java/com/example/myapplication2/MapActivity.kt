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

class MapActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mapView: MapView
    private lateinit var googleMap: GoogleMap
    private lateinit var btnConfirm: Button
    private lateinit var btnBackToMenu: Button
    private val selectedPoints = mutableListOf<LatLng>()
    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.map_activity)

        // Initialize the map
        mapView = findViewById(R.id.mapView)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        // Initialize the confirm button
        btnConfirm = findViewById(R.id.btnConfirm) // Initialize btnConfirm
        btnBackToMenu = findViewById(R.id.btnBackToMenu)

        // Handle confirm button click
        btnConfirm.setOnClickListener {
            if (selectedPoints.size > 1) {
                drawRoute()
            }
        }
        btnBackToMenu.setOnClickListener {
            finish() // Closes MainActivity and returns to the previous activity (MainScreenActivity)
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
        val optimizedPoints = RouteOptimizer().optimizeRoute(selectedPoints)

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
        val url = DirectionsAPI().buildDirectionsUrl(origin, destination, waypoints)

        // Make the API request
        val request = Request.Builder().url(url).build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@MapActivity, "Failed to fetch directions.", Toast.LENGTH_SHORT).show()
                    e.printStackTrace()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    runOnUiThread {
                        Toast.makeText(this@MapActivity, "Error fetching directions: ${response.message}", Toast.LENGTH_SHORT).show()
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
                        googleMap.addPolyline(PolylineOptions().addAll(DirectionsAPI().decodePolyline(polyline)))
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(this@MapActivity, "No routes found.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
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
