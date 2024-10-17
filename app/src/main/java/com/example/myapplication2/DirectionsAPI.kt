package com.example.myapplication2

import com.google.android.gms.maps.model.LatLng

class DirectionsAPI {
    fun buildDirectionsUrl(
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
//AIzaSyBcWOts8IFU79FQNhPXsptKGE5K0m4IQhE
    fun decodePolyline(encoded: String): List<LatLng> {
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
}