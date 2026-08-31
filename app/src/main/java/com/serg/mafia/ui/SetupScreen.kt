package com.serg.mafia.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import com.serg.mafia.model.GameViewModel
import com.serg.mafia.model.Role

@Composable
fun SetupScreen(vm: GameViewModel, onLangChange: (Lang) -> Unit) {
    val setup = vm.s.setup
    var namesOpen by remember { mutableStateOf(false) }

    Screen(
        title = t("setup_title"),
        subtitle = t("setup_sub"),
        bottom = {
            Column {
                setup.validationKey?.let { (key, args) ->
                    Text(
                        t(key, *args.toTypedArray()),
                        color = Blood,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(bottom = 6.dp),
                    )
                }
                BigButton(t("deal_roles"), enabled = setup.isValid) { vm.startDeal() }
            }
        },
    ) {
        LazyColumn {
            item {
                SectionCard(t("players_at_table")) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Stepper(
                            value = setup.playerCount,
                            min = 4,
                            max = 20,
                            onChange = { v -> vm.updateSetup { it.copy(playerCount = v) } },
                        )
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(
                                t("peaceful_count", setup.roleCounts[Role.CIVILIAN] ?: 0),
                                color = RedTeam,
                                fontSize = 14.sp,
                            )
                            Text(t(setup.difficultyKey), color = Gold, fontSize = 13.sp)
                        }
                    }
                }
            }
            item {
                SectionCard(t("blacks_at_table")) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Stepper(
                            value = setup.mafiaTotal,
                            min = 1,
                            max = setup.maxMafia,
                            onChange = { v -> vm.updateSetup { it.copy(mafiaOverride = v) } },
                        )
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(t("mafia_plus_don"), color = BlackTeam, fontSize = 13.sp)
                            Text(
                                if (setup.mafiaIsManual) {
                                    t("manual_usual", setup.autoMafia)
                                } else {
                                    t("usual_number")
                                },
                                color = Muted,
                                fontSize = 12.sp,
                            )
                        }
                    }
                    if (setup.mafiaIsManual) {
                        Spacer(Modifier.height(8.dp))
                        GhostButton(t("return_usual")) {
                            vm.updateSetup { it.copy(mafiaOverride = null) }
                        }
                    }
                }
            }
            item {
                SectionCard(t("roles_in_game")) {
                    RoleToggle(t("role_don"), t("don_hint"), setup.withDon) { v ->
                        vm.updateSetup { it.copy(withDon = v) }
                    }
                    RoleToggle(t("role_sheriff"), t("sheriff_hint"), setup.withSheriff) { v ->
                        vm.updateSetup { it.copy(withSheriff = v) }
                    }
                    RoleToggle(t("role_doctor"), t("doctor_hint"), setup.withDoctor) { v ->
                        vm.updateSetup { it.copy(withDoctor = v) }
                    }
                    RoleToggle(t("role_butterfly"), t("butterfly_hint"), setup.withButterfly) { v ->
                        vm.updateSetup { it.copy(withButterfly = v) }
                    }
                    RoleToggle(t("role_maniac"), t("maniac_hint"), setup.withManiac) { v ->
                        vm.updateSetup { it.copy(withManiac = v) }
                    }
                    RoleToggle(
                        t("first_night_miss"),
                        t("first_night_miss_hint"),
                        setup.firstNightMiss,
                    ) { v -> vm.updateSetup { it.copy(firstNightMiss = v) } }
                }
            }
            item {
                SectionCard(t("composition")) {
                    setup.roleCounts.forEach { (role, count) ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(factionColor(role.faction)),
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(t(role.titleKey), color = Color(0xFFEDE7F2), modifier = Modifier.weight(1f))
                            Text("$count", color = Gold, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        t("roles_total", setup.assignedTotal, setup.playerCount),
                        color = if (setup.isValid) Muted else Blood,
                        fontSize = 13.sp,
                    )
                }
            }
            item {
                SectionCard(t("timing")) {
                    TimeRow(t("speech_intro"), setup.introSpeechSeconds) { v ->
                        vm.updateSetup { it.copy(introSpeechSeconds = v) }
                    }
                    TimeRow(t("speech_day"), setup.daySpeechSeconds) { v ->
                        vm.updateSetup { it.copy(daySpeechSeconds = v) }
                    }
                }
            }
            item {
                SectionCard(t("language")) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Lang.entries.forEach { lang ->
                            val active = setup.lang == lang.code
                            Text(
                                lang.label,
                                color = if (active) Ink else Color(0xFFEDE7F2),
                                fontSize = 14.sp,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (active) Gold else Surface2)
                                    .clickable { onLangChange(lang) }
                                    .padding(vertical = 12.dp),
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
            item {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    GhostButton(if (namesOpen) t("hide_names") else t("set_names")) {
                        namesOpen = !namesOpen
                    }
                }
            }
            if (namesOpen) {
                itemsIndexed(setup.names) { index, name ->
                    OutlinedTextField(
                        value = name,
                        onValueChange = { vm.renamePlayer(index, it) },
                        label = { Text(t("player_n", index + 1)) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Surface1),
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, color = Gold)
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun Stepper(value: Int, min: Int, max: Int, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        StepBtn("−") { if (value > min) onChange(value - 1) }
        Text(
            "$value",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFEDE7F2),
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        StepBtn("+") { if (value < max) onChange(value + 1) }
    }
}

@Composable
private fun StepBtn(label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Surface2)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, fontSize = 24.sp, color = Gold, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun RoleToggle(title: String, hint: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = Color(0xFFEDE7F2), fontWeight = FontWeight.Medium)
            Text(hint, color = Muted, fontSize = 12.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Gold,
                checkedTrackColor = Gold.copy(alpha = 0.35f),
            ),
        )
    }
}

@Composable
private fun TimeRow(title: String, seconds: Int, onChange: (Int) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, color = Color(0xFFEDE7F2), modifier = Modifier.weight(1f))
        StepBtn("−") { if (seconds > 15) onChange(seconds - 15) }
        Text(
            t("seconds_short", seconds),
            color = Gold,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 12.dp),
        )
        StepBtn("+") { if (seconds < 180) onChange(seconds + 15) }
    }
}
