package com.chargercompanion.media

import android.content.Context
import android.media.AudioManager
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Bundle
import android.view.KeyEvent
import androidx.appcompat.app.AppCompatActivity
import com.chargercompanion.R
import com.chargercompanion.databinding.ActivityMediaRemoteBinding

/**
 * Big-button remote for whatever is playing on the phone.
 * Route Netflix/YouTube/Spotify audio to the Charger over Bluetooth,
 * then use these controls without hunting small on-screen buttons.
 */
class MediaRemoteActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMediaRemoteBinding
    private lateinit var audioManager: AudioManager
    private var controller: MediaController? = null

    private val sessionListener = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) {
            refreshLabel()
        }

        override fun onMetadataChanged(metadata: android.media.MediaMetadata?) {
            refreshLabel()
        }
    }

    private val activeSessionsListener =
        MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
            bindController(controllers?.firstOrNull())
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
            audioManager.adjustVolume(AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
        }
        binding.btnVolUp.setOnClickListener {
            audioManager.adjustVolume(AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
        }
    }

    override fun onStart() {
        super.onStart()
        val msm = getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
        try {
            msm.addOnActiveSessionsChangedListener(activeSessionsListener, null)
            bindController(msm.getActiveSessions(null).firstOrNull())
        } catch (_: SecurityException) {
            // Notification listener not granted — media keys still work via AudioManager
            binding.nowPlaying.setText(R.string.no_session)
        }
    }

    override fun onStop() {
        super.onStop()
        controller?.unregisterCallback(sessionListener)
        val msm = getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
        try {
            msm.removeOnActiveSessionsChangedListener(activeSessionsListener)
        } catch (_: Exception) {
        }
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
        binding.nowPlaying.text = when {
            !title.isNullOrBlank() && !artist.isNullOrBlank() -> "$title — $artist"
            !title.isNullOrBlank() -> title
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
        // Fallback: inject media key events so Bluetooth AVRCP still responds
        audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
        audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
    }
}
