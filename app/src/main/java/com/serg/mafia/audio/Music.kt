package com.serg.mafia.audio

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.serg.mafia.R

/** Фоновая дорожка под фазу партии. */
enum class Track(val titleKey: String, val resId: Int, val prefKey: String) {
    NIGHT("track_night", R.raw.music_night, "night"),
    DAY("track_day", R.raw.music_day, "day"),
    VOTE("track_vote", R.raw.music_vote, "vote"),
}

/** Что звучит в фазе: тишина, встроенный трек или выбранные из библиотеки. */
enum class PhaseMode { SILENCE, BUILTIN, LIBRARY }

/** Трек из библиотеки ведущего: файл на телефоне и его читаемое имя. */
data class LibraryTrack(val uri: String, val name: String)

/** Короткие сигналы ведущего. */
enum class Sfx(val resId: Int) {
    GONG(R.raw.sfx_gong),
    MORNING(R.raw.sfx_morning),
    SHOT(R.raw.sfx_shot),
    TIMEUP(R.raw.sfx_timeup),
}

/**
 * Плеер на голом MediaPlayer: медиа-сессия и foreground-service не нужны — экран во
 * время партии не гаснет, а вес приложения важнее (см. грабли в spec.mafia).
 *
 * Музыка устроена как библиотека + назначение: ведущий один раз добавляет файлы или
 * целую папку, а потом на каждую фазу выбирает треки из готового списка. По умолчанию
 * звучит только ночь — днём и на голосовании музыка мешает разговору за столом.
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

    /** Библиотека ведущего — общий список, из которого музыка назначается фазам. */
    val library = mutableStateListOf<LibraryTrack>()

    /** Как подписать встроенный трек в мини-плеере — подставляет UI на языке интерфейса. */
    var builtinTitle: (Track) -> String = { it.name }

    init {
        library.addAll(loadLibrary())
    }

    // ── библиотека ───────────────────────────────────────────────────────────
    private fun loadLibrary(): List<LibraryTrack> =
        prefs.getString("library", "")!!
            .split("\n")
            .filter { it.isNotBlank() }
            .mapNotNull { line ->
                val parts = line.split(SEP)
                if (parts.size == 2) LibraryTrack(parts[0], parts[1]) else null
            }

    private fun saveLibrary() {
        prefs.edit()
            .putString("library", library.joinToString("\n") { it.uri + SEP + it.name })
            .apply()
    }

    private fun displayName(uri: Uri): String {
        val fromResolver = runCatching {
            app.contentResolver.query(uri, null, null, null, null)?.use { c ->
                val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0 && c.moveToFirst()) c.getString(idx) else null
            }
        }.getOrNull()
        return (fromResolver ?: uri.lastPathSegment ?: "track").substringAfterLast('/')
    }

    private fun persist(uri: Uri) {
        runCatching {
            app.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }
    }

    /** Добавить отдельные файлы, выбранные системным диалогом. */
    fun addFiles(uris: List<Uri>) {
        uris.forEach { uri ->
            persist(uri)
            val item = LibraryTrack(uri.toString(), displayName(uri))
            if (library.none { it.uri == item.uri }) library += item
        }
        saveLibrary()
    }

    /** Добавить всю папку разом: из неё забираются аудиофайлы. */
    fun addFolder(treeUri: Uri) {
        persist(treeUri)
        val docId = runCatching { DocumentsContract.getTreeDocumentId(treeUri) }.getOrNull() ?: return
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, docId)
        runCatching {
            app.contentResolver.query(
                children,
                arrayOf(
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    DocumentsContract.Document.COLUMN_MIME_TYPE,
                ),
                null,
                null,
                null,
            )?.use { c ->
                while (c.moveToNext()) {
                    val id = c.getString(0)
                    val name = c.getString(1) ?: id
                    val mime = c.getString(2) ?: ""
                    val isAudio = mime.startsWith("audio/") ||
                        name.substringAfterLast('.', "").lowercase() in AUDIO_EXT
                    if (!isAudio) continue
                    val uri = DocumentsContract.buildDocumentUriUsingTree(treeUri, id).toString()
                    if (library.none { it.uri == uri }) library += LibraryTrack(uri, name)
                }
            }
        }
        saveLibrary()
    }

    fun removeFromLibrary(track: LibraryTrack) {
        library.remove(track)
        Track.entries.forEach { phase ->
            saveSelection(phase, selection(phase).filter { it != track.uri })
        }
        saveLibrary()
        currentTrack?.let { play(it, restart = true) }
    }

    fun clearLibrary() {
        library.clear()
        Track.entries.forEach { saveSelection(it, emptyList()) }
        saveLibrary()
        stop()
    }

    // ── назначение музыки фазам ──────────────────────────────────────────────
    /** По умолчанию звучит только ночь: день и голосование идут в тишине. */
    private fun defaultMode(track: Track) =
        if (track == Track.NIGHT) PhaseMode.BUILTIN else PhaseMode.SILENCE

    fun mode(track: Track): PhaseMode = runCatching {
        PhaseMode.valueOf(prefs.getString("mode_${track.prefKey}", null) ?: defaultMode(track).name)
    }.getOrDefault(defaultMode(track))

    fun setMode(track: Track, mode: PhaseMode) {
        prefs.edit().putString("mode_${track.prefKey}", mode.name).apply()
        if (currentTrack == track) play(track, restart = true)
    }

    fun selection(track: Track): List<String> =
        prefs.getString("sel_${track.prefKey}", "")!!.split("\n").filter { it.isNotBlank() }

    private fun saveSelection(track: Track, uris: List<String>) {
        prefs.edit().putString("sel_${track.prefKey}", uris.joinToString("\n")).apply()
    }

    /** Отметить или снять трек библиотеки для фазы. Первый выбор включает режим «свои». */
    fun toggleForPhase(track: Track, uri: String) {
        val cur = selection(track).toMutableList()
        if (!cur.remove(uri)) cur += uri
        saveSelection(track, cur)
        when {
            cur.isNotEmpty() -> setMode(track, PhaseMode.LIBRARY)
            mode(track) == PhaseMode.LIBRARY -> setMode(track, PhaseMode.SILENCE)
            currentTrack == track -> play(track, restart = true)
        }
    }

    fun phaseTracks(track: Track): List<LibraryTrack> =
        selection(track).mapNotNull { uri -> library.firstOrNull { it.uri == uri } }

    // ── воспроизведение ──────────────────────────────────────────────────────
    private var indexInTrack = 0

    fun play(track: Track, restart: Boolean = false) {
        val sameTrack = currentTrack == track
        currentTrack = track
        if (!enabled || mode(track) == PhaseMode.SILENCE) {
            release()
            isPlaying = false
            currentTitle = ""
            return
        }
        if (!restart && sameTrack && player != null && isPlaying) return
        indexInTrack = 0
        startCurrent()
    }

    private fun startCurrent() {
        val track = currentTrack ?: return
        release()
        val chosen = if (mode(track) == PhaseMode.LIBRARY) phaseTracks(track) else emptyList()
        val mp = if (chosen.isEmpty()) {
            currentTitle = builtinTitle(track)
            MediaPlayer.create(app, track.resId)?.apply { isLooping = true }
        } else {
            val item = chosen[indexInTrack % chosen.size]
            currentTitle = item.name
            runCatching {
                MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build(),
                    )
                    setDataSource(app, Uri.parse(item.uri))
                    isLooping = chosen.size == 1
                    prepare()
                }
            }.getOrNull()
        } ?: run {
            // Битый или отозванный URI — не роняем партию, откатываемся на встроенный.
            currentTitle = builtinTitle(track)
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
        val chosen = if (mode(track) == PhaseMode.LIBRARY) phaseTracks(track) else emptyList()
        if (chosen.isEmpty()) {
            player?.seekTo(0)
            player?.start()
            isPlaying = player != null
        } else {
            indexInTrack = (indexInTrack + 1) % chosen.size
            startCurrent()
        }
    }

    fun pause() {
        runCatching { player?.pause() }
        isPlaying = false
    }

    fun resume() {
        if (!enabled) return
        val track = currentTrack ?: return
        if (mode(track) == PhaseMode.SILENCE) return
        if (player == null) startCurrent() else runCatching { player?.start() }
        isPlaying = player != null
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
        currentTitle = ""
    }

    private companion object {
        /** Разделитель «uri‖имя» в prefs: в URI и именах файлов такого символа не бывает. */
        const val SEP = "‖"
        val AUDIO_EXT = setOf("mp3", "ogg", "m4a", "aac", "wav", "flac", "opus", "oga")
    }
}
