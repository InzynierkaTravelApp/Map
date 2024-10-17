package com.example.myapplication2

import com.google.android.gms.maps.model.LatLng

class RouteOptimizer {
    fun optimizeRoute(points: List<LatLng>): List<LatLng> {
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
}