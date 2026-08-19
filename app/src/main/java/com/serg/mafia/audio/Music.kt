package com.serg.mafia.audio

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.serg.mafia.R

/** Фоновая дорожка под фазу партии. */
enum class Track(val title: String, val resId: Int, val prefKey: String) {
    NIGHT("Ночь", R.raw.music_night, "uris_night"),
    DAY("День", R.raw.music_day, "uris_day"),
    VOTE("Голосование", R.raw.music_vote, "uris_vote"),
}

/** Короткие сигналы ведущего. */
enum class Sfx(val resId: Int) {
    GONG(R.raw.sfx_gong),
    MORNING(R.raw.sfx_morning),
    SHOT(R.raw.sfx_shot),
    TIMEUP(R.raw.sfx_timeup),
}

/**
 * Простой плеер на голом MediaPlayer: медиа-сессия и foreground-service здесь не нужны —
 * экран во время партии не гаснет, а вес приложения важнее (см. грабли в spec.mafia).
 */
class MusicController(context: Context) {

    private val app = context.applicationContext
    private val prefs = app.getSharedPreferences("mafia_music", Context.MODE_PRIVATE)

    private var player: MediaPlayer? = null
    private var sfxPlayer: MediaPlayer? = null

    var currentTrack by mutableStateOf<Track?>(null)
        private set
    var isPlaying by mutableStateOf(false)
        private set
    var enabled by mutableStateOf(prefs.getBoolean("enabled", true))
        private set
    var volume by mutableFloatStateOf(prefs.getFloat("volume", 0.6f))
        private set
    var currentTitle by mutableStateOf("")
        private set

    /** Свои треки ведущего для конкретной фазы (пусто — играет встроенный). */
    fun customUris(track: Track): List<Uri> =
        prefs.getString(track.prefKey, "")!!
            .split("|")
            .filter { it.isNotBlank() }
            .map { Uri.parse(it) }

    fun addCustomUris(track: Track, uris: List<Uri>) {
        val existing = customUris(track).map { it.toString() }
        val merged = (existing + uris.map { it.toString() }).distinct()
        prefs.edit().putString(track.prefKey, merged.joinToString("|")).apply()
        uris.forEach { uri ->
            runCatching {
                app.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
        }
        if (currentTrack == track) play(track, restart = true)
    }

    fun clearCustom(track: Track) {
        prefs.edit().remove(track.prefKey).apply()
        if (currentTrack == track) play(track, restart = true)
    }

    private var indexInTrack = 0

    fun play(track: Track, restart: Boolean = false) {
        if (!enabled) {
            currentTrack = track
            return
        }
        if (currentTrack == track && !restart && player != null) {
            resume()
            return
        }
        currentTrack = track
        indexInTrack = 0
        startCurrent()
    }

    private fun startCurrent() {
        val track = currentTrack ?: return
        release()
        val uris = customUris(track)
        val mp = if (uris.isEmpty()) {
            currentTitle = "${track.title} · встроенный"
            MediaPlayer.create(app, track.resId)?.apply { isLooping = true }
        } else {
            val uri = uris[indexInTrack % uris.size]
            currentTitle = "${track.title} · свой трек ${indexInTrack % uris.size + 1}/${uris.size}"
            runCatching {
                MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build(),
                    )
                    setDataSource(app, uri)
                    isLooping = uris.size == 1
                    prepare()
                }
            }.getOrNull()
        } ?: run {
            // Битый или отозванный URI — не роняем партию, откатываемся на встроенный.
            currentTitle = "${track.title} · встроенный"
            MediaPlayer.create(app, track.resId)?.apply { isLooping = true }
        }
        player = mp ?: return
        mp.setVolume(volume, volume)
        mp.setOnCompletionListener { next() }
        mp.start()
        isPlaying = true
    }

    fun next() {
        val track = currentTrack ?: return
        val uris = customUris(track)
        if (uris.isEmpty()) {
            player?.seekTo(0)
            player?.start()
            isPlaying = true
        } else {
            indexInTrack = (indexInTrack + 1) % uris.size
            startCurrent()
        }
    }

    fun pause() {
        runCatching { player?.pause() }
        isPlaying = false
    }

    fun resume() {
        if (!enabled) return
        if (player == null) {
            startCurrent()
        } else {
            runCatching { player?.start() }
            isPlaying = true
        }
    }

    fun toggle() = if (isPlaying) pause() else resume()

    fun enableMusic(value: Boolean) {
        enabled = value
        prefs.edit().putBoolean("enabled", value).apply()
        if (!value) {
            release()
            isPlaying = false
        } else {
            currentTrack?.let { play(it, restart = true) }
        }
    }

    fun changeVolume(v: Float) {
        volume = v.coerceIn(0f, 1f)
        prefs.edit().putFloat("volume", volume).apply()
        runCatching { player?.setVolume(volume, volume) }
    }

    fun sfx(sound: Sfx) {
        if (!enabled) return
        runCatching { sfxPlayer?.release() }
        sfxPlayer = MediaPlayer.create(app, sound.resId)?.apply {
            setVolume(1f, 1f)
            setOnCompletionListener { it.release(); if (sfxPlayer === it) sfxPlayer = null }
            start()
        }
    }

    private fun release() {
        runCatching {
            player?.setOnCompletionListener(null)
            player?.stop()
            player?.release()
        }
        player = null
    }

    fun stop() {
        release()
        isPlaying = false
        currentTrack = null
    }
}
