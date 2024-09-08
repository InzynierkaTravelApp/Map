package com.example.myapplication

// Represents the overall response from the Directions API
data class DirectionsResponse(
    val routes: List<Route>
)

// Represents a route in the response
data class Route(
    val overview_polyline: OverviewPolyline
)

// Represents the polyline object that contains the encoded points string
data class OverviewPolyline(
    val points: String
)