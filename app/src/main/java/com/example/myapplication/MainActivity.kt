package com.example.myapplication

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import com.example.myapplication.ui.theme.MapScreen
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.example.myapplication.ui.theme.RetrofitInstance
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : ComponentActivity(), OnMapReadyCallback {

    private lateinit var mapView: MapView
    private lateinit var googleMap: GoogleMap
    private val apiKey = "AIzaSyBcWOts8IFU79FQNhPXsptKGE5K0m4IQhE"

    // Store all selected points
    private var selectedPoints = mutableListOf<LatLng>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize the MapView
        mapView = MapView(this)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this) // Set up the map asynchronously

        setContent {
            MyApplicationTheme {
                MapScreen(mapView, ::onConfirmPoints) // Pass callback for confirming points
            }
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap.setOnMapClickListener { latLng ->
            handleMapClick(latLng)
        }

        // Move camera to a default position
        val initialPosition = LatLng(40.748817, -73.985428) // Example position
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(initialPosition, 12f))
    }

    private fun handleMapClick(latLng: LatLng) {
        selectedPoints.add(latLng)
        googleMap.addMarker(MarkerOptions().position(latLng).title("Point ${selectedPoints.size}"))
    }

    private fun onConfirmPoints() {
        // Handle the confirmation of selected points
        if (selectedPoints.size > 1) {
            getDirections(selectedPoints)
        } else {
            Toast.makeText(this, "Please select at least two points.", Toast.LENGTH_SHORT).show()
        }
    }

    // Update getDirections to handle multiple points
    private fun getDirections(points: List<LatLng>) {
        val origin = points.first()
        val destination = points.last()

        val waypoints = points.subList(1, points.size - 1).joinToString("|") { "${it.latitude},${it.longitude}" }

        val originStr = "${origin.latitude},${origin.longitude}"
        val destinationStr = "${destination.latitude},${destination.longitude}"

        // Call Directions API with waypoints
        val call = RetrofitInstance.directionsApiService.getDirections(
            originStr,
            destinationStr,
            waypoints,
            apiKey
        )
        call.enqueue(object : Callback<DirectionsResponse> {
            override fun onResponse(call: Call<DirectionsResponse>, response: Response<DirectionsResponse>) {
                if (response.isSuccessful) {
                    val directionsResponse = response.body()
                    val polylinePoints = directionsResponse?.routes?.get(0)?.overview_polyline?.points
                    polylinePoints?.let {
                        drawRouteOnMap(it)
                    }
                } else {
                    Toast.makeText(this@MainActivity, "Error fetching directions", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<DirectionsResponse>, t: Throwable) {
                Toast.makeText(this@MainActivity, "Request failed: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
    private fun decodePoly(encoded: String): List<LatLng> {
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

            val latLng = LatLng(
                lat / 1E5,
                lng / 1E5
            )
            poly.add(latLng)
        }

        return poly
    }


    private fun drawRouteOnMap(encodedPoints: String) {
        val polylineOptions = PolylineOptions()
            .addAll(decodePoly(encodedPoints))
            .width(10f)
            .color(android.graphics.Color.BLUE)

        googleMap.clear() // Clear previous markers and polylines
        googleMap.addPolyline(polylineOptions)

        // Adjust camera to fit the route
        val boundsBuilder = LatLngBounds.Builder()
        decodePoly(encodedPoints).forEach { boundsBuilder.include(it) }
        val bounds = boundsBuilder.build()
        googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100))
    }

}
