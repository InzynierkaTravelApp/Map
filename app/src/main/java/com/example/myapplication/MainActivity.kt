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

    private val apiKey = "AIzaSyBcWOts8IFU79FQNhPXsptKGE5K0m4IQhE"

    private lateinit var mapView: MapView
    private lateinit var googleMap: GoogleMap

    private var origin: LatLng? = null
    private var destination: LatLng? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize the MapView
        mapView = MapView(this)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this) // Set up the map asynchronously
        setContent {
            MyApplicationTheme {
                MapScreen(mapView)
            }
        }
    }

    @Composable
    fun MapScreen(mapView: MapView) {
        AndroidView(factory = {
            mapView
        }, modifier = Modifier.fillMaxSize())
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
        if (origin == null) {
            origin = latLng
            googleMap.addMarker(MarkerOptions().position(latLng).title("Origin"))
        } else if (destination == null) {
            destination = latLng
            googleMap.addMarker(MarkerOptions().position(latLng).title("Destination"))
            getDirections(origin!!, destination!!)
        } else {
            // Reset taps if both points are set
            origin = latLng
            destination = null
            googleMap.clear()
            googleMap.addMarker(MarkerOptions().position(latLng).title("Origin"))
        }
    }

    private fun getDirections(origin: LatLng, destination: LatLng) {
        val originStr = "${origin.latitude},${origin.longitude}"
        val destinationStr = "${destination.latitude},${destination.longitude}"

        val call = RetrofitInstance.directionsApiService.getDirections(
            originStr,
            destinationStr,
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



    private fun decodePoly(encoded: String): List<LatLng> {
        val poly = mutableListOf<LatLng>()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0

        while (index < len) {
            // Decode latitude
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encoded[index++].code - 63
                result = result or ((b and 0x1f) shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = if (result and 1 != 0) result shr 1 else -(result shr 1)
            lat += dlat

            // Decode longitude
            shift = 0
            result = 0
            do {
                b = encoded[index++].code - 63
                result = result or ((b and 0x1f) shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = if (result and 1 != 0) result shr 1 else -(result shr 1)
            lng += dlng

            // Convert to LatLng and add to list
            val p = LatLng(
                (lat / 1E5).toDouble(),
                (lng / 1E5).toDouble()
            )
            Log.d("Decoded LatLng", "LatLng: ${p.latitude}, ${p.longitude}")
            poly.add(p)
        }

        return poly
    }





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

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MyApplicationTheme {
        Greeting("Android")
    }
}
