package com.serg.mafia.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.serg.mafia.audio.Sfx
import com.serg.mafia.model.GameViewModel
import com.serg.mafia.model.Phase

@Composable
fun SpeechScreen(vm: GameViewModel, intro: Boolean) {
    val s = vm.s
    val music = LocalMusic.current
    val speaker = s.speakerId?.let { s.player(it) } ?: return
    val seconds = if (intro) s.setup.introSpeechSeconds else s.setup.daySpeechSeconds
    var nominateOpen by remember { mutableStateOf(false) }
    var foulOpen by remember { mutableStateOf(false) }

    Screen(
        title = if (intro) "Первое знакомство" else "День ${s.dayNumber}",
        subtitle = if (intro) {
            "Каждый говорит по $seconds секунд"
        } else {
            "Речь ${seconds} с · можно выставить на голосование"
        },
        bottom = {
            Column {
                if (!intro) {
                    GhostButton("Выставить на голосование", Modifier.fillMaxWidth()) {
                        nominateOpen = true
                    }
                    Spacer(Modifier.height(8.dp))
                }
                // Фол можно выдать в любой круг речей, включая знакомство,
                // и тут же снять, если ведущий нажал лишний раз.
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    GhostButton("+ фол", Modifier.weight(1f)) { vm.addFoul(speaker.id) }
                    GhostButton(
                        if (speaker.fouls > 0) "− снять фол" else "нет фолов",
                        Modifier.weight(1f),
                    ) { if (speaker.fouls > 0) vm.removeFoul(speaker.id) }
                    GhostButton("стол", Modifier.weight(1f)) { foulOpen = true }
                }
                Spacer(Modifier.height(8.dp))
                BigButton("Речь окончена") { vm.finishSpeech() }
            }
        },
    ) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Surface1)
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Говорит", color = Muted, fontSize = 13.sp)
                    Text(
                        "${speaker.id + 1}. ${speaker.name}",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Gold,
                    )
                    if (speaker.fouls > 0) {
                        Text("фолов: ${speaker.fouls}", color = Blood, fontSize = 13.sp)
                    }
                }
            }
            Spacer(Modifier.height(18.dp))
            SpeechTimer(
                totalSeconds = seconds,
                key = "${s.phase}-${s.dayNumber}-${speaker.id}",
                onFinished = { music.sfx(Sfx.TIMEUP) },
            )
            Spacer(Modifier.height(16.dp))
            val fouled = s.players.filter { it.fouls > 0 }
            if (fouled.isNotEmpty()) {
                Text("Фолы:", color = Moon, fontSize = 13.sp)
                Spacer(Modifier.height(4.dp))
                LazyColumn(Modifier.heightIn(max = 160.dp)) {
                    items(fouled, key = { it.id }) { p ->
                        PlayerRow(
                            player = p,
                            badge = "${p.fouls}",
                            badgeColor = Blood,
                            trailing = when {
                                !p.alive -> "выбыл по фолам"
                                p.speechSkipPending -> "пропустит речь"
                                p.fouls == 1 -> "предупреждение"
                                else -> null
                            },
                            onClick = { foulOpen = true },
                            extra = {
                                TextButton(onClick = { vm.removeFoul(p.id) }) {
                                    Text("−", color = Moon, fontSize = 20.sp)
                                }
                            },
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
            }
            if (!intro && s.nominations.isNotEmpty()) {
                Text("На голосовании:", color = Moon, fontSize = 13.sp)
                Spacer(Modifier.height(4.dp))
                LazyColumn(Modifier.heightIn(max = 220.dp)) {
                    items(s.nominations.keys.toList(), key = { it }) { candidateId ->
                        val by = s.nominations[candidateId]!!
                        PlayerRow(
                            player = s.player(candidateId),
                            trailing = "выставил ${s.player(by).name}",
                            onClick = { vm.cancelNomination(candidateId) },
                        )
                    }
                }
            } else if (!intro) {
                Text("Пока никого не выставили", color = Muted, fontSize = 13.sp)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Осталось речей: ${s.speechQueue().size}",
                color = Muted,
                fontSize = 12.sp,
            )
        }
    }

    if (nominateOpen) {
        PickPlayerDialog(
            title = "Кого выставляет ${speaker.name}?",
            players = s.alivePlayers.filter { it.id != speaker.id && it.id !in s.nominations.keys },
            onDismiss = { nominateOpen = false },
            onPick = { id ->
                vm.nominate(speaker.id, id)
                nominateOpen = false
            },
        )
    }
    if (foulOpen) {
        FoulDialog(vm) { foulOpen = false }
    }
}

@Composable
fun PickPlayerDialog(
    title: String,
    players: List<com.serg.mafia.model.Player>,
    onDismiss: () -> Unit,
    onPick: (Int) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Surface1,
        title = { Text(title, color = Color(0xFFEDE7F2)) },
        text = {
            LazyColumn(Modifier.heightIn(max = 400.dp)) {
                items(players, key = { it.id }) { p ->
                    PlayerRow(player = p, onClick = { onPick(p.id) })
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Отмена", color = Muted) }
        },
    )
}

/** Фолы: 1 — предупреждение, 2 — пропуск ближайшей речи, 3 — выбывает. */
@Composable
fun FoulDialog(vm: GameViewModel, onDismiss: () -> Unit) {
    val s = vm.s
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Surface1,
        title = { Text("Фолы", color = Color(0xFFEDE7F2)) },
        text = {
            Column {
                Text(
                    "1 — предупреждение · 2 — пропуск речи · 3 — смерть",
                    color = Muted,
                    fontSize = 12.sp,
                )
                Spacer(Modifier.height(8.dp))
                LazyColumn(Modifier.heightIn(max = 420.dp)) {
                    items(s.players.filter { it.alive }, key = { it.id }) { p ->
                        PlayerRow(
                            player = p,
                            badge = if (p.fouls > 0) "${p.fouls}" else null,
                            badgeColor = Blood,
                            onClick = { vm.addFoul(p.id) },
                            extra = {
                                if (p.fouls > 0) {
                                    TextButton(onClick = { vm.removeFoul(p.id) }) {
                                        Text("−", color = Moon, fontSize = 20.sp)
                                    }
                                }
                            },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Готово", color = Gold) }
        },
    )
}

/** Экран-заглушка на случай, если круг речей закончился в неожиданной фазе. */
@Composable
fun EmptySpeechScreen(vm: GameViewModel) {
    Screen(title = "Круг завершён") {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Все высказались", color = Muted)
            Spacer(Modifier.height(12.dp))
            BigButton(if (vm.s.phase == Phase.DAY) "К голосованию" else "К ночи") { vm.finishSpeech() }
        }
    }
}
