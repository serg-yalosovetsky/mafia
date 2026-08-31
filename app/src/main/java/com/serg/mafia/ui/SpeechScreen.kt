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
    var nomTableOpen by remember { mutableStateOf(false) }
    var foulOpen by remember { mutableStateOf(false) }

    Screen(
        title = if (intro) t("intro_day_title") else t("day_title", s.dayNumber),
        subtitle = if (intro) t("intro_day_sub", seconds) else t("day_sub", seconds),
        bottom = {
            Column {
                if (!intro) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        GhostButton(t("nominate"), Modifier.weight(1f)) { nominateOpen = true }
                        GhostButton(t("who_nominated_short"), Modifier.weight(1f)) {
                            nomTableOpen = true
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
                // Фол можно выдать в любой круг речей, включая знакомство,
                // и тут же снять, если ведущий нажал лишний раз.
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    GhostButton(t("foul_plus"), Modifier.weight(1f)) { vm.addFoul(speaker.id) }
                    GhostButton(
                        if (speaker.fouls > 0) t("foul_minus") else t("no_fouls"),
                        Modifier.weight(1f),
                    ) { if (speaker.fouls > 0) vm.removeFoul(speaker.id) }
                    GhostButton(t("all_fouls"), Modifier.weight(1f)) { foulOpen = true }
                }
                Spacer(Modifier.height(8.dp))
                BigButton(t("speech_done")) { vm.finishSpeech() }
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
                    Text(t("speaks"), color = Muted, fontSize = 13.sp)
                    Text(
                        "${speaker.id + 1}. ${speaker.name}",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Gold,
                    )
                    if (speaker.fouls > 0) {
                        Text(t("fouls_of", speaker.fouls), color = Blood, fontSize = 13.sp)
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
                Text(t("fouls_label"), color = Moon, fontSize = 13.sp)
                Spacer(Modifier.height(4.dp))
                LazyColumn(Modifier.heightIn(max = 160.dp)) {
                    items(fouled, key = { it.id }) { p ->
                        PlayerRow(
                            player = p,
                            badge = "${p.fouls}",
                            badgeColor = Blood,
                            trailing = when {
                                !p.alive -> t("foul_out")
                                p.speechSkipPending -> t("foul_skip")
                                p.fouls == 1 -> t("foul_warning")
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
            if (!intro && s.candidates.isNotEmpty()) {
                Text(t("on_vote"), color = Moon, fontSize = 13.sp)
                Spacer(Modifier.height(4.dp))
                LazyColumn(Modifier.heightIn(max = 220.dp)) {
                    items(s.candidates, key = { it }) { candidateId ->
                        val by = s.nominatedBy(candidateId)
                        PlayerRow(
                            player = s.player(candidateId),
                            badge = "${by.size}",
                            badgeColor = Blood,
                            trailing = t("nominated_by", by.joinToString { s.player(it).name }),
                            onClick = { vm.cancelNomination(candidateId) },
                        )
                    }
                }
            } else if (!intro) {
                Text(t("nobody_nominated"), color = Muted, fontSize = 13.sp)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                t("speeches_left", s.speechQueue().size),
                color = Muted,
                fontSize = 12.sp,
            )
        }
    }

    if (nominateOpen) {
        PickPlayerDialog(
            title = t("who_nominates", speaker.name),
            players = s.alivePlayers.filter { it.id != speaker.id },
            selectedId = s.nominationsBy[speaker.id],
            onClear = if (s.nominationsBy.containsKey(speaker.id)) {
                { vm.clearNomination(speaker.id); nominateOpen = false }
            } else {
                null
            },
            onDismiss = { nominateOpen = false },
            onPick = { id ->
                vm.nominate(speaker.id, id)
                nominateOpen = false
            },
        )
    }
    if (nomTableOpen) {
        NominationTableDialog(vm) { nomTableOpen = false }
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
    selectedId: Int? = null,
    onClear: (() -> Unit)? = null,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Surface1,
        title = { Text(title, color = Color(0xFFEDE7F2)) },
        text = {
            LazyColumn(Modifier.heightIn(max = 400.dp)) {
                items(players, key = { it.id }) { p ->
                    PlayerRow(
                        player = p,
                        selected = p.id == selectedId,
                        onClick = { onPick(p.id) },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(t("cancel"), color = Muted) }
        },
        dismissButton = onClear?.let { clear ->
            { TextButton(onClick = clear) { Text(t("remove"), color = Blood) } }
        },
    )
}

/** Таблица «кто кого выставил»: у каждого игрока за столом свой записанный выбор. */
@Composable
fun NominationTableDialog(vm: GameViewModel, onDismiss: () -> Unit) {
    val s = vm.s
    var picking by remember { mutableStateOf<Int?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Surface1,
        title = { Text(t("nomination_table"), color = Gold) },
        text = {
            Column {
                Text(
                    t("nomination_table_hint"),
                    color = Muted,
                    fontSize = 12.sp,
                )
                Spacer(Modifier.height(8.dp))
                LazyColumn(Modifier.heightIn(max = 420.dp)) {
                    items(s.alivePlayers, key = { it.id }) { p ->
                        val target = s.nominationsBy[p.id]
                        PlayerRow(
                            player = p,
                            trailing = target?.let { t("arrow_to", s.player(it).name) } ?: t("nobody"),
                            trailingColor = if (target != null) Gold else Muted,
                            onClick = { picking = p.id },
                            extra = {
                                if (target != null) {
                                    TextButton(onClick = { vm.clearNomination(p.id) }) {
                                        Text("×", color = Moon, fontSize = 18.sp)
                                    }
                                }
                            },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(t("ok"), color = Gold) }
        },
    )

    picking?.let { byId ->
        PickPlayerDialog(
            title = t("who_nominates", s.player(byId).name),
            players = s.alivePlayers.filter { it.id != byId },
            selectedId = s.nominationsBy[byId],
            onClear = if (s.nominationsBy.containsKey(byId)) {
                { vm.clearNomination(byId); picking = null }
            } else {
                null
            },
            onDismiss = { picking = null },
            onPick = { id ->
                vm.nominate(byId, id)
                picking = null
            },
        )
    }
}

/** Фолы: 1 — предупреждение, 2 — пропуск ближайшей речи, 3 — выбывает. */
@Composable
fun FoulDialog(vm: GameViewModel, onDismiss: () -> Unit) {
    val s = vm.s
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Surface1,
        title = { Text(t("fouls_title"), color = Color(0xFFEDE7F2)) },
        text = {
            Column {
                Text(
                    t("fouls_rule"),
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
            TextButton(onClick = onDismiss) { Text(t("ok"), color = Gold) }
        },
    )
}

/** Экран-заглушка на случай, если круг речей закончился в неожиданной фазе. */
@Composable
fun EmptySpeechScreen(vm: GameViewModel) {
    Screen(title = t("round_done")) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(t("all_spoke"), color = Muted)
            Spacer(Modifier.height(12.dp))
            BigButton(if (vm.s.phase == Phase.DAY) t("to_vote") else t("to_night")) {
                vm.finishSpeech()
            }
        }
    }
}
