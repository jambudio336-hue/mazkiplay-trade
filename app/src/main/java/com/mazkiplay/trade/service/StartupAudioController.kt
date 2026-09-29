package com.mazkiplay.trade.service

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import com.mazkiplay.trade.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Short launch soundtrack: plays once per process launch and releases itself at EOF.
 * The app deliberately exposes mute/skip controls so audio never traps the user.
 */
data class StartupAudioState(val playing: Boolean = false, val muted: Boolean = false)

class StartupAudioController(private val context: Context) {
    private val _state = MutableStateFlow(StartupAudioState())
    val state: StateFlow<StartupAudioState> = _state.asStateFlow()
    private var player: MediaPlayer? = null
    private var focusRequest: AudioFocusRequest? = null
    private val audioManager = context.getSystemService(AudioManager::class.java)

    @Synchronized
    fun playOnce() {
        if (player != null || _state.value.playing) return
        val p = runCatching { MediaPlayer.create(context, R.raw.startup_theme) }.getOrNull() ?: return
        player = p
        p.setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
        p.setOnCompletionListener { release() }
        p.setOnErrorListener { _, _, _ -> release(); true }
        requestFocus()
        p.setVolume(1f, 1f)
        _state.value = StartupAudioState(playing = true, muted = false)
        p.start()
    }

    @Synchronized
    fun toggleMute() {
        val current = _state.value
        val next = !current.muted
        player?.setVolume(if (next) 0f else 1f, if (next) 0f else 1f)
        _state.value = current.copy(muted = next)
    }

    @Synchronized
    fun skip() { release() }

    @Synchronized
    fun release() {
        player?.runCatching { stop() }
        player?.release()
        player = null
        focusRequest?.let { audioManager?.abandonAudioFocusRequest(it) }
        focusRequest = null
        _state.value = StartupAudioState()
    }

    private fun requestFocus() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK).setOnAudioFocusChangeListener { change -> if (change == AudioManager.AUDIOFOCUS_LOSS) skip() }.build()
            focusRequest = request
            audioManager?.requestAudioFocus(request)
        } else {
            @Suppress("DEPRECATION") audioManager?.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
        }
    }
}
