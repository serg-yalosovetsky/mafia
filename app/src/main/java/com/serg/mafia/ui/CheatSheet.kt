package com.serg.mafia.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
                if (showLog) "Журнал партии" else "Шпаргалка ведущего",
                color = Gold,
            )
        },
        text = {
            Column {
                if (showLog) {
                    LazyColumn(Modifier.heightIn(max = 460.dp)) {
                        items(s.log.reversed()) { line ->
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
                        "Только для тебя. Не показывай стол.",
                        color = Muted,
                        fontSize = 12.sp,
                    )
                    Spacer(Modifier.height(8.dp))
                    LazyColumn(Modifier.heightIn(max = 440.dp)) {
                        items(s.players, key = { it.id }) { p ->
                            PlayerRow(
                                player = p,
                                badge = p.role.title,
                                badgeColor = factionColor(p.role.faction),
                                dimmed = !p.alive,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Закрыть", color = Gold) }
        },
        dismissButton = {
            TextButton(onClick = { showLog = !showLog }) {
                Text(if (showLog) "Роли" else "Журнал", color = Moon)
            }
        },
    )
}

/** Свои треки: системный выбор файлов, никаких разрешений на хранилище. */
@Composable
fun MusicSettingsDialog(onDismiss: () -> Unit) {
    val music = LocalMusic.current
    var target by remember { mutableStateOf(Track.NIGHT) }
    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments(),
    ) { uris ->
        if (!uris.isNullOrEmpty()) music.addCustomUris(target, uris)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Surface1,
        title = { Text("Музыка", color = Gold) },
        text = {
            Column {
                Text(
                    "Для каждой фазы можно подложить свои файлы с телефона. " +
                        "Пока их нет — играет встроенный трек.",
                    color = Muted,
                    fontSize = 13.sp,
                )
                Spacer(Modifier.height(12.dp))
                Track.entries.forEach { track ->
                    val count = music.customUris(track).size
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(track.title, color = Color(0xFFEDE7F2))
                            Text(
                                if (count == 0) "встроенный трек" else "своих треков: $count",
                                color = Muted,
                                fontSize = 12.sp,
                            )
                        }
                        TextButton(onClick = {
                            target = track
                            picker.launch(arrayOf("audio/*"))
                        }) { Text("Добавить", color = Moon) }
                        if (count > 0) {
                            TextButton(onClick = { music.clearCustom(track) }) {
                                Text("Сброс", color = Blood)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Готово", color = Gold) }
        },
    )
}
