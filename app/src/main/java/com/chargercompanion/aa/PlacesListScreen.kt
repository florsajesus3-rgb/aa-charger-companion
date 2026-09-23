package com.chargercompanion.aa

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.CarLocation
import androidx.car.app.model.ItemList
import androidx.car.app.model.Metadata
import androidx.car.app.model.Place
import androidx.car.app.model.PlaceListMapTemplate
import androidx.car.app.model.PlaceMarker
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.Executors

class PlacesListScreen(
    carContext: CarContext,
    private val kind: PlaceKind,
) : Screen(carContext) {
    private val executor = Executors.newSingleThreadExecutor()
    @Volatile private var places: List<NearbyPlace> = emptyList()
    @Volatile private var status: String = "Finding ${kind.label.lowercase()} nearby…"
    @Volatile private var anchor: CarLocation? = null

    init {
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                refresh()
            }
        })
    }

    private fun refresh() {
        executor.execute {
            val loc = lastLocation()
            if (loc == null) {
                status = "Location permission needed — open the phone app once and allow location."
                places = emptyList()
                invalidate()
                return@execute
            }
            anchor = CarLocation.create(loc.latitude, loc.longitude)
            try {
                places = OverpassClient.fetch(loc.latitude, loc.longitude, kind)
                status = if (places.isEmpty()) "No ${kind.label.lowercase()} found nearby." else ""
            } catch (e: Exception) {
                places = emptyList()
                status = "Couldn’t load places. Check data connection."
            }
            invalidate()
        }
    }

    private fun lastLocation(): Location? {
        val fine = ContextCompat.checkSelfPermission(carContext, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(carContext, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (fine != PackageManager.PERMISSION_GRANTED && coarse != PackageManager.PERMISSION_GRANTED) {
            return null
        }
        val lm = carContext.getSystemService(LocationManager::class.java) ?: return null
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
        return providers.mapNotNull { runCatching { lm.getLastKnownLocation(it) }.getOrNull() }
            .maxByOrNull { it.time }
    }

    override fun onGetTemplate(): Template {
        val builder = ItemList.Builder()
        if (status.isNotBlank() && places.isEmpty()) {
            builder.addItem(
                Row.Builder().setTitle(status).build()
            )
        }
        for (p in places) {
            val miles = p.distanceMeters / 1609.34
            val dist = if (miles < 0.1) "${p.distanceMeters.toInt()} m" else String.format("%.1f mi", miles)
            val place = Place.Builder(CarLocation.create(p.lat, p.lon))
                .setMarker(PlaceMarker.Builder().build())
                .build()
            builder.addItem(
                Row.Builder()
                    .setTitle(p.name)
                    .addText(dist)
                    .setMetadata(Metadata.Builder().setPlace(place).build())
                    .build()
            )
        }

        val map = PlaceListMapTemplate.Builder()
            .setTitle(kind.label)
            .setHeaderAction(Action.BACK)
            .setItemList(builder.build())
            .setCurrentLocationEnabled(true)

        anchor?.let { map.setAnchor(Place.Builder(it).build()) }
        return map.build()
    }
}
