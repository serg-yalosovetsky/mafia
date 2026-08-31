package com.serg.mafia.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.serg.mafia.audio.PhaseMode
import com.serg.mafia.audio.Track
import com.serg.mafia.model.GameViewModel

/** Шпаргалка ведущего: кто есть кто и что уже произошло. */
@Composable
fun CheatSheetDialog(vm: GameViewModel, onDismiss: () -> Unit) {
    val s = vm.s
    var showLog by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Surface1,
        title = {
            Text(
                if (showLog) t("journal_title") else t("cheatsheet"),
                color = Gold,
            )
        },
        text = {
            Column {
                if (showLog) {
                    LazyColumn(Modifier.heightIn(max = 460.dp)) {
                        items(s.log.reversed()) { entry ->
                            val line = t(entry.key, *entry.args.toTypedArray())
                            Text(
                                line,
                                color = if (line.startsWith("—")) Gold else Color(0xFFEDE7F2),
                                fontSize = 13.sp,
                                modifier = Modifier.padding(vertical = 3.dp),
                            )
                        }
                    }
                } else {
                    Text(
                        t("cheatsheet_hint"),
                        color = Muted,
                        fontSize = 12.sp,
                    )
                    Spacer(Modifier.height(8.dp))
                    LazyColumn(Modifier.heightIn(max = 440.dp)) {
                        items(s.players, key = { it.id }) { p ->
                            PlayerRow(
                                player = p,
                                badge = t(p.role.titleKey),
                                badgeColor = factionColor(p.role.faction),
                                dimmed = !p.alive,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(t("close"), color = Gold) }
        },
        dismissButton = {
            TextButton(onClick = { showLog = !showLog }) {
                Text(if (showLog) t("roles") else t("journal"), color = Moon)
            }
        },
    )
}

/** Библиотека ведущего и назначение музыки на фазы. */
@Composable
fun MusicSettingsDialog(onDismiss: () -> Unit) {
    val music = LocalMusic.current
    var phasePicker by remember { mutableStateOf<Track?>(null) }

    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments(),
    ) { uris -> if (!uris.isNullOrEmpty()) music.addFiles(uris) }

    val folderPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri -> uri?.let { music.addFolder(it) } }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Surface1,
        title = { Text(t("music"), color = Gold) },
        text = {
            Column {
                Text(t("music_default_hint"), color = Muted, fontSize = 12.sp)
                Spacer(Modifier.height(10.dp))

                Text(t("phase_music"), color = Moon, fontSize = 13.sp)
                Track.entries.forEach { track ->
                    val mode = music.mode(track)
                    val chosen = music.phaseTracks(track)
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Surface2)
                            .clickable { phasePicker = track }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(t(track.titleKey), color = Color(0xFFEDE7F2), modifier = Modifier.weight(1f))
                        Text(
                            when (mode) {
                                PhaseMode.SILENCE -> t("mode_silence")
                                PhaseMode.BUILTIN -> t("mode_builtin")
                                PhaseMode.LIBRARY -> t("mode_library", chosen.size)
                            },
                            color = if (mode == PhaseMode.SILENCE) Muted else Gold,
                            fontSize = 13.sp,
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))
                Text(t("music_library"), color = Moon, fontSize = 13.sp)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GhostButton(t("add_files"), Modifier.weight(1f)) {
                        filePicker.launch(arrayOf("audio/*"))
                    }
                    GhostButton(t("add_folder"), Modifier.weight(1f)) { folderPicker.launch(null) }
                }
                if (music.library.isEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(t("music_lib_empty"), color = Muted, fontSize = 12.sp)
                } else {
                    LazyColumn(Modifier.heightIn(max = 220.dp)) {
                        items(music.library, key = { it.uri }) { item ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    item.name,
                                    color = Color(0xFFEDE7F2),
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f),
                                )
                                TextButton(onClick = { music.removeFromLibrary(item) }) {
                                    Text("×", color = Blood, fontSize = 18.sp)
                                }
                            }
                        }
                    }
                    TextButton(onClick = { music.clearLibrary() }) {
                        Text(t("clear_library"), color = Blood, fontSize = 13.sp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(t("ok"), color = Gold) }
        },
    )

    phasePicker?.let { track ->
        PhaseMusicDialog(track) { phasePicker = null }
    }
}

/** Что играет в конкретной фазе: тишина, встроенный трек или отмеченные свои. */
@Composable
private fun PhaseMusicDialog(track: Track, onDismiss: () -> Unit) {
    val music = LocalMusic.current
    val mode = music.mode(track)
    val selected = music.selection(track)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Surface1,
        title = { Text(t("choose_for", t(track.titleKey)), color = Gold) },
        text = {
            Column {
                ModeRow(t("pick_silence"), mode == PhaseMode.SILENCE) {
                    music.setMode(track, PhaseMode.SILENCE)
                }
                ModeRow(t("pick_builtin"), mode == PhaseMode.BUILTIN) {
                    music.setMode(track, PhaseMode.BUILTIN)
                }
                if (music.library.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Text(t("from_library"), color = Moon, fontSize = 13.sp)
                    LazyColumn(Modifier.heightIn(max = 300.dp)) {
                        items(music.library, key = { it.uri }) { item ->
                            val on = item.uri in selected
                            ModeRow(item.name, on && mode == PhaseMode.LIBRARY) {
                                music.toggleForPhase(track, item.uri)
                            }
                        }
                    }
                } else {
                    Spacer(Modifier.height(8.dp))
                    Text(t("music_lib_empty"), color = Muted, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(t("ok"), color = Gold) }
        },
    )
}

@Composable
private fun ModeRow(label: String, active: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (active) Gold.copy(alpha = 0.18f) else Surface2)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            color = if (active) Gold else Color(0xFFEDE7F2),
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (active) Text("✓", color = Gold, fontSize = 15.sp)
    }
}
