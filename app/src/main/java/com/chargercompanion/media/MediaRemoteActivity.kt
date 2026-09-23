package com.chargercompanion.media

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Bundle
import android.provider.Settings
import android.view.KeyEvent
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.chargercompanion.R
import com.chargercompanion.databinding.ActivityMediaRemoteBinding

class MediaRemoteActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMediaRemoteBinding
    private lateinit var audioManager: AudioManager
    private var controller: MediaController? = null

    private val sessionListener = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) = refreshLabel()
        override fun onMetadataChanged(metadata: android.media.MediaMetadata?) = refreshLabel()
    }

    private val activeSessionsListener =
        MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
            bindController(pickBest(controllers))
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMediaRemoteBinding.inflate(layoutInflater)
        setContentView(binding.root)
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        binding.btnPlayPause.setOnClickListener { sendMediaKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) }
        binding.btnPrev.setOnClickListener { sendMediaKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS) }
        binding.btnNext.setOnClickListener { sendMediaKey(KeyEvent.KEYCODE_MEDIA_NEXT) }
        binding.btnVolDown.setOnClickListener {
            audioManager.adjustStreamVolume(
                AudioManager.STREAM_MUSIC,
                AudioManager.ADJUST_LOWER,
                AudioManager.FLAG_SHOW_UI,
            )
        }
        binding.btnVolUp.setOnClickListener {
            audioManager.adjustStreamVolume(
                AudioManager.STREAM_MUSIC,
                AudioManager.ADJUST_RAISE,
                AudioManager.FLAG_SHOW_UI,
            )
        }
        binding.nowPlaying.setOnClickListener { openNotificationAccess() }
    }

    override fun onStart() {
        super.onStart()
        if (!hasNotificationAccess()) {
            binding.nowPlaying.text =
                "Tap here to enable Notification access — required for play/pause to control Netflix/YouTube/Spotify."
            return
        }
        attachSessions()
    }

    override fun onStop() {
        super.onStop()
        controller?.unregisterCallback(sessionListener)
        val msm = getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
        runCatching { msm.removeOnActiveSessionsChangedListener(activeSessionsListener) }
    }

    private fun attachSessions() {
        val msm = getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
        val cn = ComponentName(this, MediaNotificationListener::class.java)
        try {
            msm.addOnActiveSessionsChangedListener(activeSessionsListener, cn)
            bindController(pickBest(msm.getActiveSessions(cn)))
        } catch (e: SecurityException) {
            binding.nowPlaying.text = "Notification access blocked. Tap here to enable it."
        }
    }

    private fun pickBest(controllers: List<MediaController>?): MediaController? {
        if (controllers.isNullOrEmpty()) return null
        return controllers.firstOrNull {
            val s = it.playbackState?.state
            s == PlaybackState.STATE_PLAYING || s == PlaybackState.STATE_BUFFERING
        } ?: controllers.first()
    }

    private fun bindController(next: MediaController?) {
        controller?.unregisterCallback(sessionListener)
        controller = next
        next?.registerCallback(sessionListener)
        refreshLabel()
    }

    private fun refreshLabel() {
        val meta = controller?.metadata
        val title = meta?.getString(android.media.MediaMetadata.METADATA_KEY_TITLE)
        val artist = meta?.getString(android.media.MediaMetadata.METADATA_KEY_ARTIST)
        val pkg = controller?.packageName
        binding.nowPlaying.text = when {
            !title.isNullOrBlank() && !artist.isNullOrBlank() -> "$title — $artist"
            !title.isNullOrBlank() -> title
            pkg != null -> "Controlling: $pkg"
            else -> getString(R.string.no_session)
        }
    }

    private fun sendMediaKey(keyCode: Int) {
        val c = controller
        if (c != null) {
            when (keyCode) {
                KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                    val state = c.playbackState?.state
                    if (state == PlaybackState.STATE_PLAYING) c.transportControls.pause()
                    else c.transportControls.play()
                }
                KeyEvent.KEYCODE_MEDIA_NEXT -> c.transportControls.skipToNext()
                KeyEvent.KEYCODE_MEDIA_PREVIOUS -> c.transportControls.skipToPrevious()
            }
            return
        }
        // Fallback media keys
        audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
        audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
        Toast.makeText(this, "No active media session — start playback first", Toast.LENGTH_SHORT).show()
    }

    private fun hasNotificationAccess(): Boolean {
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners") ?: return false
        val cn = ComponentName(this, MediaNotificationListener::class.java).flattenToString()
        return flat.split(':').any { it.equals(cn, ignoreCase = true) || it.contains(packageName) }
    }

    private fun openNotificationAccess() {
        startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        Toast.makeText(this, "Enable Charger Companion, then return", Toast.LENGTH_LONG).show()
    }
}
