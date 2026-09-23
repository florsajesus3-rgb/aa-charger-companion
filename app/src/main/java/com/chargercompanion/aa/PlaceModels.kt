package com.chargercompanion.aa

data class NearbyPlace(
    val name: String,
    val lat: Double,
    val lon: Double,
    val category: String,
    val distanceMeters: Double,
)
