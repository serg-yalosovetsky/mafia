package com.serg.mafia.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.MusicOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.serg.mafia.audio.MusicController
import com.serg.mafia.model.Player
import com.serg.mafia.model.Role
import kotlinx.coroutines.delay

val LocalMusic = staticCompositionLocalOf<MusicController> { error("MusicController не задан") }

@Composable
fun Screen(
    title: String,
    subtitle: String? = null,
    bottom: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Ink)
            .padding(top = 36.dp, start = 16.dp, end = 16.dp, bottom = 8.dp),
    ) {
        Text(title, style = MaterialTheme.typography.headlineMedium, color = Color(0xFFEDE7F2))
        if (subtitle != null) {
            Spacer(Modifier.height(4.dp))
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = Muted)
        }
        Spacer(Modifier.height(14.dp))
        Box(Modifier.weight(1f)) { content() }
        if (bottom != null) {
            Spacer(Modifier.height(8.dp))
            bottom()
        }
        MiniPlayer()
    }
}

@Composable
fun BigButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color = Blood,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = Color.White),
    ) {
        Text(text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun GhostButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(14.dp),
    ) {
        Text(text, color = Muted)
    }
}

@Composable
fun RolePortraitCard(role: Role, size: Int = 180, showTitle: Boolean = true) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Image(
            painter = painterResource(rolePortrait(role)),
            contentDescription = role.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(size.dp)
                .clip(RoundedCornerShape(20.dp))
                .border(2.dp, factionColor(role.faction).copy(alpha = 0.5f), RoundedCornerShape(20.dp)),
        )
        if (showTitle) {
            Spacer(Modifier.height(10.dp))
            Text(
                role.title,
                style = MaterialTheme.typography.titleLarge,
                color = factionColor(role.faction),
            )
        }
    }
}

/** Строка игрока в списке: номер, имя, метки фолов/ролей, отметка выбора. */
@Composable
fun PlayerRow(
    player: Player,
    selected: Boolean = false,
    trailing: String? = null,
    trailingColor: Color = Muted,
    badge: String? = null,
    badgeColor: Color = Gold,
    dimmed: Boolean = false,
    onClick: (() -> Unit)? = null,
    extra: @Composable (() -> Unit)? = null,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) Blood.copy(alpha = 0.22f) else Surface1,
        ),
        border = if (selected) androidx.compose.foundation.BorderStroke(1.5.dp, Blood) else null,
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(if (player.alive) Surface2 else Color(0xFF2A1D1D)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "${player.id + 1}",
                    color = if (player.alive) Gold else Muted,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    player.name,
                    color = if (dimmed || !player.alive) Muted else Color(0xFFEDE7F2),
                    fontWeight = FontWeight.Medium,
                )
                val marks = buildList {
                    if (!player.alive) add(player.deathReason ?: "выбыл")
                    if (player.fouls > 0) add("фолы: ${player.fouls}")
                    if (player.speechSkipPending) add("пропуск речи")
                }
                if (marks.isNotEmpty()) {
                    Text(marks.joinToString(" · "), color = Muted, fontSize = 12.sp)
                }
            }
            if (badge != null) {
                Text(
                    badge,
                    color = badgeColor,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(badgeColor.copy(alpha = 0.14f))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                )
                Spacer(Modifier.width(8.dp))
            }
            if (trailing != null) {
                Text(trailing, color = trailingColor, fontSize = 13.sp)
            }
            extra?.invoke()
        }
    }
}

/** Список для выбора цели: живые сверху, мёртвые не показываем. */
@Composable
fun PlayerPicker(
    players: List<Player>,
    selectedId: Int?,
    modifier: Modifier = Modifier,
    badgeFor: (Player) -> Pair<String, Color>? = { null },
    onSelect: (Int) -> Unit,
) {
    LazyColumn(modifier.fillMaxSize()) {
        items(players, key = { it.id }) { p ->
            val badge = badgeFor(p)
            PlayerRow(
                player = p,
                selected = p.id == selectedId,
                badge = badge?.first,
                badgeColor = badge?.second ?: Gold,
                onClick = { onSelect(p.id) },
            )
        }
    }
}

/**
 * Таймер речи: старт / пауза / сброс. Сам не тикает в фоне —
 * ведущий держит телефон в руках и экран не гаснет.
 */
@Composable
fun SpeechTimer(
    totalSeconds: Int,
    key: Any,
    autoStart: Boolean = false,
    onFinished: () -> Unit = {},
) {
    var remaining by rememberSaveable(key) { mutableIntStateOf(totalSeconds) }
    var running by rememberSaveable(key) { mutableStateOf(autoStart) }
    var fired by rememberSaveable(key) { mutableStateOf(false) }

    LaunchedEffect(running, key) {
        while (running && remaining > 0) {
            delay(1000)
            remaining -= 1
        }
        if (remaining == 0 && !fired) {
            fired = true
            running = false
            onFinished()
        }
    }

    val progress = if (totalSeconds == 0) 0f else remaining / totalSeconds.toFloat()
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text(
            "%d:%02d".format(remaining / 60, remaining % 60),
            fontSize = 56.sp,
            fontWeight = FontWeight.Bold,
            color = if (remaining <= 10) Blood else Color(0xFFEDE7F2),
        )
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = if (remaining <= 10) Blood else Gold,
            trackColor = Surface2,
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = { running = !running },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Surface2),
            ) {
                Icon(
                    if (running) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (running) "Пауза" else "Старт",
                    tint = Gold,
                )
                Spacer(Modifier.width(6.dp))
                Text(if (running) "Пауза" else "Старт", color = Color(0xFFEDE7F2))
            }
            Button(
                onClick = {
                    running = false
                    remaining = totalSeconds
                    fired = false
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Surface2),
            ) {
                Icon(Icons.Filled.Refresh, contentDescription = "Сброс", tint = Moon)
                Spacer(Modifier.width(6.dp))
                Text("Сброс", color = Color(0xFFEDE7F2))
            }
        }
    }
}

/** Мини-плеер: живёт внизу каждого экрана. */
@Composable
fun MiniPlayer() {
    val music = LocalMusic.current
    var showVolume by remember { mutableStateOf(false) }
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Surface1)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { music.enableMusic(!music.enabled) }) {
                Icon(
                    if (music.enabled) Icons.Filled.MusicNote else Icons.Filled.MusicOff,
                    contentDescription = "Музыка",
                    tint = if (music.enabled) Gold else Muted,
                )
            }
            Text(
                if (music.enabled) music.currentTitle.ifBlank { "музыка" } else "музыка выключена",
                color = Muted,
                fontSize = 12.sp,
                maxLines = 1,
                modifier = Modifier
                    .weight(1f)
                    .clickable { showVolume = !showVolume },
                textAlign = TextAlign.Center,
            )
            IconButton(onClick = { music.toggle() }, enabled = music.enabled) {
                Icon(
                    if (music.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = "Пауза",
                    tint = if (music.enabled) Moon else Muted,
                )
            }
            IconButton(onClick = { music.next() }, enabled = music.enabled) {
                Icon(Icons.Filled.SkipNext, contentDescription = "Следующий", tint = if (music.enabled) Moon else Muted)
            }
        }
        if (showVolume) {
            Slider(
                value = music.volume,
                onValueChange = { music.changeVolume(it) },
                modifier = Modifier.padding(horizontal = 12.dp),
            )
        }
    }
}

@Suppress("unused")
val systemBarsInsets: WindowInsets
    @Composable get() = WindowInsets.systemBars
