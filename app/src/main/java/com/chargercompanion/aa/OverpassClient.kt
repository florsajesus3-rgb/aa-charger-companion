package com.chargercompanion.aa

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object OverpassClient {
    private val http = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    fun fetch(lat: Double, lon: Double, kind: PlaceKind): List<NearbyPlace> {
        val amenity = when (kind) {
            PlaceKind.GAS -> "fuel"
            PlaceKind.FOOD -> "fast_food|restaurant|cafe"
            PlaceKind.PARKING -> "parking"
        }
        val radius = 3500
        val query = """
            [out:json][timeout:25];
            (
              node["amenity"~"$amenity"](around:$radius,$lat,$lon);
              way["amenity"~"$amenity"](around:$radius,$lat,$lon);
            );
            out center 25;
        """.trimIndent()

        val url = "https://overpass-api.de/api/interpreter?data=" +
            java.net.URLEncoder.encode(query, Charsets.UTF_8.name())
        val req = Request.Builder().url(url).header("User-Agent", "ChargerCompanion/1.0").build()
        http.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return emptyList()
            val body = resp.body?.string() ?: return emptyList()
            val elements = JSONObject(body).optJSONArray("elements") ?: return emptyList()
            val out = ArrayList<NearbyPlace>()
            for (i in 0 until elements.length()) {
                val el = elements.getJSONObject(i)
                val tags = el.optJSONObject("tags") ?: continue
                val name = tags.optString("name").ifBlank { kind.label }
                val plat: Double
                val plon: Double
                if (el.has("lat") && el.has("lon")) {
                    plat = el.getDouble("lat")
                    plon = el.getDouble("lon")
                } else {
                    val center = el.optJSONObject("center") ?: continue
                    plat = center.getDouble("lat")
                    plon = center.getDouble("lon")
                }
                out += NearbyPlace(
                    name = name,
                    lat = plat,
                    lon = plon,
                    category = kind.label,
                    distanceMeters = haversine(lat, lon, plat, plon),
                )
            }
            return out.sortedBy { it.distanceMeters }.take(12)
        }
    }

    private fun haversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2) * sin(dLon / 2)
        return 2 * r * atan2(sqrt(a), sqrt(1 - a))
    }
}

enum class PlaceKind(val label: String) {
    GAS("Gas"),
    FOOD("Food"),
    PARKING("Parking"),
}
