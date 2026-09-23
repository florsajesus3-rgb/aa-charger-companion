package com.chargercompanion.ui

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.chargercompanion.databinding.ActivityHomeBinding
import com.chargercompanion.media.MediaNotificationListener
import com.chargercompanion.media.MediaRemoteActivity

class HomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHomeBinding

    private val locationPermission = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        refreshHints()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnRemote.setOnClickListener {
            startActivity(Intent(this, MediaRemoteActivity::class.java))
        }
        binding.btnNotifAccess.setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            Toast.makeText(this, "Turn on Charger Companion, then come back", Toast.LENGTH_LONG).show()
        }

        ensureLocationPermission()
        refreshHints()
    }

    override fun onResume() {
        super.onResume()
        refreshHints()
    }

    private fun refreshHints() {
        val locOk =
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        val notifOk = hasNotificationAccess()
        binding.setupHints.text = buildString {
            append("Setup checklist\n\n")
            append(if (locOk) "✓ Location allowed\n" else "○ Allow location (prompt or App info)\n")
            append(if (notifOk) "✓ Notification access on (media remote)\n" else "○ Enable notification access (button above)\n")
            append("○ Android Auto phone app → tap App icon 10+ times → Developer settings → Unknown sources ON\n")
            append("○ USB to Uconnect, open Charger Companion on the 8.4 for Gas/Food/Parking\n")
            append("\nMedia remote works from the phone even if AA places don’t show yet.")
        }
    }

    private fun hasNotificationAccess(): Boolean {
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners") ?: return false
        val cn = ComponentName(this, MediaNotificationListener::class.java).flattenToString()
        return flat.split(':').any { it.equals(cn, ignoreCase = true) || it.contains(packageName) }
    }

    private fun ensureLocationPermission() {
        val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (fine != PackageManager.PERMISSION_GRANTED || coarse != PackageManager.PERMISSION_GRANTED) {
            locationPermission.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                )
            )
        }
    }
}
