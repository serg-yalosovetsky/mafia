package com.serg.mafia

import android.content.Context
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.serg.mafia.audio.MusicController
import com.serg.mafia.audio.Sfx
import com.serg.mafia.audio.Track
import com.serg.mafia.model.GameViewModel
import com.serg.mafia.model.Phase
import com.serg.mafia.ui.CheatSheetDialog
import com.serg.mafia.ui.DealScreen
import com.serg.mafia.ui.EmptySpeechScreen
import com.serg.mafia.ui.GameOverScreen
import androidx.compose.ui.graphics.Color
import com.serg.mafia.ui.Blood
import com.serg.mafia.ui.Gold
import com.serg.mafia.ui.Lang
import com.serg.mafia.ui.LocalLang
import com.serg.mafia.ui.IntroNightScreen
import com.serg.mafia.ui.LocalMusic
import com.serg.mafia.ui.MafiaTheme
import com.serg.mafia.ui.MorningScreen
import com.serg.mafia.ui.Muted
import com.serg.mafia.ui.MusicSettingsDialog
import com.serg.mafia.ui.NightScreen
import com.serg.mafia.ui.SetupScreen
import com.serg.mafia.ui.SpeechScreen
import com.serg.mafia.ui.rawString
import com.serg.mafia.ui.t
import com.serg.mafia.ui.tr
import com.serg.mafia.ui.Surface1
import com.serg.mafia.ui.VoteScreen

class MainActivity : ComponentActivity() {

    private val vm: GameViewModel by viewModels()
    private lateinit var music: MusicController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Партия идёт с телефоном в руках у ведущего — гасить экран нельзя.
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        music = MusicController(this)

        val prefs = getSharedPreferences("mafia", Context.MODE_PRIVATE)
        // Язык интерфейса переживает перезапуск: по умолчанию украинский.
        val saved = Lang.of(prefs.getString("lang", vm.s.setup.lang))
        applyLang(saved)

        setContent {
            val lang = Lang.of(vm.s.setup.lang)
            music.builtinTitle = { track -> tr(lang, "music_builtin", tr(lang, track.titleKey)) }
            MafiaTheme {
                CompositionLocalProvider(LocalMusic provides music, LocalLang provides lang) {
                    Root(vm) { chosen ->
                        prefs.edit().putString("lang", chosen.code).apply()
                        applyLang(chosen)
                    }
                }
            }
        }
    }

    /** Смена языка меняет и шаблон имён по умолчанию («Гравець 1» / «Игрок 1» / «Player 1»). */
    private fun applyLang(lang: Lang) {
        vm.setLang(lang.code, rawString(lang, "player_n"))
    }

    override fun onStop() {
        super.onStop()
        music.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        music.stop()
    }
}

@Composable
private fun Root(vm: GameViewModel, onLangChange: (Lang) -> Unit) {
    val music = LocalMusic.current
    val s = vm.s
    var cheatOpen by remember { mutableStateOf(false) }
    var musicOpen by remember { mutableStateOf(false) }
    var exitOpen by remember { mutableStateOf(false) }

    // Музыка и сигналы едут за фазой партии.
    LaunchedEffect(s.phase, s.nightNumber, s.dayNumber) {
        when (s.phase) {
            Phase.SETUP -> music.stop()
            Phase.DEAL -> music.play(Track.NIGHT)
            Phase.INTRO_NIGHT -> {
                music.play(Track.NIGHT)
                music.sfx(Sfx.GONG)
            }
            Phase.NIGHT -> {
                music.play(Track.NIGHT)
                music.sfx(Sfx.GONG)
            }
            Phase.MORNING -> {
                music.play(Track.DAY)
                if (s.lastNight.killed.isEmpty()) music.sfx(Sfx.MORNING) else music.sfx(Sfx.SHOT)
            }
            Phase.INTRO_DAY, Phase.DAY -> music.play(Track.DAY)
            Phase.VOTE -> music.play(Track.VOTE)
            Phase.GAME_OVER -> music.play(Track.DAY)
        }
    }

    Box(Modifier.fillMaxSize()) {
        when (s.phase) {
            Phase.SETUP -> SetupScreen(vm, onLangChange)
            Phase.DEAL -> DealScreen(vm)
            Phase.INTRO_NIGHT -> IntroNightScreen(vm)
            Phase.INTRO_DAY -> if (s.speakerId != null) SpeechScreen(vm, intro = true) else EmptySpeechScreen(vm)
            Phase.NIGHT -> NightScreen(vm)
            Phase.MORNING -> MorningScreen(vm)
            Phase.DAY -> if (s.speakerId != null) SpeechScreen(vm, intro = false) else EmptySpeechScreen(vm)
            Phase.VOTE -> VoteScreen(vm)
            Phase.GAME_OVER -> GameOverScreen(vm)
        }

        // Быстрый доступ ведущего: шпаргалка с ролями и настройки музыки.
        Box(Modifier.align(Alignment.TopEnd).padding(top = 24.dp, end = 4.dp)) {
            androidx.compose.foundation.layout.Row {
                if (s.phase != Phase.SETUP) {
                    IconButton(onClick = { exitOpen = true }) {
                        Icon(Icons.Filled.Home, contentDescription = t("to_setup"), tint = Gold)
                    }
                }
                if (s.phase != Phase.SETUP && s.phase != Phase.DEAL) {
                    IconButton(onClick = { cheatOpen = true }) {
                        Icon(Icons.Filled.List, contentDescription = t("cheatsheet"), tint = Gold)
                    }
                }
                IconButton(onClick = { musicOpen = true }) {
                    Icon(Icons.Filled.Tune, contentDescription = t("music"), tint = Gold)
                }
            }
        }
    }

    if (cheatOpen) CheatSheetDialog(vm) { cheatOpen = false }
    if (musicOpen) MusicSettingsDialog { musicOpen = false }
    if (exitOpen) {
        // Нажал «старт» случайно — из партии всегда можно выйти обратно к столу.
        AlertDialog(
            onDismissRequest = { exitOpen = false },
            containerColor = Surface1,
            title = { Text(t("exit_title"), color = Color(0xFFEDE7F2)) },
            text = { Text(t("exit_body"), color = Muted) },
            confirmButton = {
                TextButton(onClick = {
                    vm.backToSetup()
                    exitOpen = false
                }) { Text(t("exit_yes"), color = Blood) }
            },
            dismissButton = {
                TextButton(onClick = { exitOpen = false }) { Text(t("exit_no"), color = Gold) }
            },
        )
    }
}
