package com.serg.mafia.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.serg.mafia.model.Faction
import com.serg.mafia.model.GameViewModel
import com.serg.mafia.model.winnerKey

@Composable
fun MorningScreen(vm: GameViewModel) {
    val s = vm.s
    val killed = s.lastNight.killed

    Screen(
        title = t("morning_title"),
        subtitle = t("morning_sub", s.nightNumber),
        bottom = { BigButton(t("start_day")) { vm.startDay() } },
    ) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            if (killed.isEmpty()) {
                Spacer(Modifier.height(24.dp))
                Text(t("all_survived"), style = MaterialTheme.typography.headlineMedium, color = Gold)
                if (s.lastNight.savedByDoctor != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        t("doctor_saved"),
                        color = Muted,
                        fontSize = 14.sp,
                    )
                }
            } else {
                Text(t("city_lost"), color = Muted, fontSize = 14.sp)
                Spacer(Modifier.height(10.dp))
                killed.forEach { id ->
                    val p = s.player(id)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        RolePortraitCard(p.role, size = 150, showTitle = false)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "${p.id + 1}. ${p.name}",
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color(0xFFEDE7F2),
                        )
                        Text(t(p.role.titleKey), color = factionColor(p.role.faction), fontSize = 16.sp)
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                t("alive_count", s.alivePlayers.size),
                color = Muted,
                fontSize = 13.sp,
            )
        }
    }
}

@Composable
fun VoteScreen(vm: GameViewModel) {
    val s = vm.s
    val leaders = vm.voteLeaders
    // Кандидаты — те, кого выставили, плюс добавленные ведущим вручную.
    val candidates = s.votes.keys.toList().ifEmpty { s.candidates }
    var addOpen by remember { mutableStateOf(false) }

    Screen(
        title = t("vote_title"),
        subtitle = t("vote_sub", s.dayNumber, s.alivePlayers.size),
        bottom = {
            Column {
                when {
                    leaders.size == 1 -> BigButton(t("expel", s.player(leaders[0]).name)) {
                        vm.eliminate(leaders[0])
                    }
                    leaders.size > 1 -> Text(
                        t("tie") + leaders.joinToString { s.player(it).name } + t("tie_hint"),
                        color = Gold,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                    else -> Text(
                        t("put_votes"),
                        color = Muted,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
                Spacer(Modifier.height(6.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    GhostButton(t("add_player"), Modifier.weight(1f)) { addOpen = true }
                    GhostButton(t("nobody_out"), Modifier.weight(1f)) { vm.skipVote() }
                }
            }
        },
    ) {
        LazyColumn(Modifier.fillMaxSize()) {
            items(candidates, key = { it }) { id ->
                val p = s.player(id)
                val votes = s.votes[id] ?: 0
                val isLeader = id in leaders
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isLeader) Blood.copy(alpha = 0.18f) else Surface1)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "${p.id + 1}. ${p.name}",
                            color = Color(0xFFEDE7F2),
                            fontWeight = FontWeight.Medium,
                        )
                        val by = s.nominatedBy(id)
                        Text(
                            if (by.isEmpty()) {
                                t("added_by_host")
                            } else {
                                t("nominated_by", by.joinToString { s.player(it).name })
                            },
                            color = Muted,
                            fontSize = 12.sp,
                        )
                    }
                    VoteStep("−") { vm.setVote(id, votes - 1) }
                    Text(
                        "$votes",
                        color = if (isLeader) Blood else Gold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        modifier = Modifier
                            .width(46.dp)
                            .padding(horizontal = 6.dp),
                        textAlign = TextAlign.Center,
                    )
                    VoteStep("+") { vm.setVote(id, votes + 1) }
                }
                if (leaders.size > 1 && isLeader) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        GhostButton(t("expel", p.name)) { vm.eliminate(id) }
                    }
                }
            }
        }
    }

    if (addOpen) {
        AddVoteCandidateDialog(vm) { addOpen = false }
    }
}

@Composable
private fun AddVoteCandidateDialog(vm: GameViewModel, onDismiss: () -> Unit) {
    val s = vm.s
    PickPlayerDialog(
        title = t("who_to_add"),
        players = s.alivePlayers.filter { it.id !in s.votes.keys },
        onDismiss = onDismiss,
        onPick = { id ->
            vm.addVoteCandidate(id)
            onDismiss()
        },
    )
}

@Composable
private fun VoteStep(label: String, onClick: () -> Unit) {
    Text(
        label,
        color = Gold,
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .size(42.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Surface2)
            .clickable(onClick = onClick)
            .padding(top = 6.dp),
    )
}

@Composable
fun GameOverScreen(vm: GameViewModel) {
    val s = vm.s
    val winner = s.winner ?: Faction.RED

    Screen(
        title = t(winnerKey(winner)),
        subtitle = t("game_over"),
        bottom = {
            Column {
                BigButton(t("new_game_same")) { vm.newGameSameTable() }
                Spacer(Modifier.height(8.dp))
                GhostButton(t("change_setup"), Modifier.fillMaxWidth()) { vm.reset() }
            }
        },
    ) {
        LazyColumn(Modifier.fillMaxSize()) {
            items(s.players, key = { it.id }) { p ->
                PlayerRow(
                    player = p,
                    badge = t(p.role.titleKey),
                    badgeColor = factionColor(p.role.faction),
                    trailing = if (p.alive) t("survived") else null,
                )
            }
        }
    }
}
