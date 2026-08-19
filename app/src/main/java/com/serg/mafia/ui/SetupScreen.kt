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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.serg.mafia.model.GameViewModel
import com.serg.mafia.model.Role

@Composable
fun SetupScreen(vm: GameViewModel) {
    val setup = vm.s.setup
    var namesOpen by remember { mutableStateOf(false) }

    Screen(
        title = "Новая партия",
        subtitle = "Собери стол и раздай роли",
        bottom = {
            Column {
                setup.validationMessage?.let {
                    Text(it, color = Blood, fontSize = 13.sp, modifier = Modifier.padding(bottom = 6.dp))
                }
                BigButton("Раздать роли", enabled = setup.isValid) { vm.startDeal() }
            }
        },
    ) {
        LazyColumn {
            item {
                SectionCard("Игроков за столом") {
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
                                "Мирных: ${setup.roleCounts[Role.CIVILIAN] ?: 0}",
                                color = RedTeam,
                                fontSize = 14.sp,
                            )
                            Text(setup.difficultyLabel, color = Gold, fontSize = 13.sp)
                        }
                    }
                }
            }
            item {
                SectionCard("Чёрных за столом") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Stepper(
                            value = setup.mafiaTotal,
                            min = 1,
                            max = setup.maxMafia,
                            onChange = { v -> vm.updateSetup { it.copy(mafiaOverride = v) } },
                        )
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text("мафия + дон", color = BlackTeam, fontSize = 13.sp)
                            Text(
                                if (setup.mafiaIsManual) "вручную · обычно ${setup.autoMafia}" else "обычное число",
                                color = Muted,
                                fontSize = 12.sp,
                            )
                        }
                    }
                    if (setup.mafiaIsManual) {
                        Spacer(Modifier.height(8.dp))
                        GhostButton("Вернуть обычное число") {
                            vm.updateSetup { it.copy(mafiaOverride = null) }
                        }
                    }
                }
            }
            item {
                SectionCard("Роли в игре") {
                    RoleToggle("Дон", "Ночью ищет комиссара", setup.withDon) { v ->
                        vm.updateSetup { it.copy(withDon = v) }
                    }
                    RoleToggle("Комиссар", "Ночью проверяет игрока", setup.withSheriff) { v ->
                        vm.updateSetup { it.copy(withSheriff = v) }
                    }
                    RoleToggle("Врач", "Ночью лечит одного", setup.withDoctor) { v ->
                        vm.updateSetup { it.copy(withDoctor = v) }
                    }
                    RoleToggle("Бабочка", "Блокирует ночное действие", setup.withButterfly) { v ->
                        vm.updateSetup { it.copy(withButterfly = v) }
                    }
                    RoleToggle("Маньяк", "Играет сам за себя", setup.withManiac) { v ->
                        vm.updateSetup { it.copy(withManiac = v) }
                    }
                    RoleToggle(
                        "Первая ночь — в воздух",
                        "Мафия промахивается первым выстрелом: красным легче",
                        setup.firstNightMiss,
                    ) { v -> vm.updateSetup { it.copy(firstNightMiss = v) } }
                }
            }
            item {
                SectionCard("Состав") {
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
                            Text(role.title, color = Color(0xFFEDE7F2), modifier = Modifier.weight(1f))
                            Text("$count", color = Gold, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Всего ролей: ${setup.assignedTotal} из ${setup.playerCount}",
                        color = if (setup.isValid) Muted else Blood,
                        fontSize = 13.sp,
                    )
                }
            }
            item {
                SectionCard("Хронометраж") {
                    TimeRow("Речь на знакомстве", setup.introSpeechSeconds) { v ->
                        vm.updateSetup { it.copy(introSpeechSeconds = v) }
                    }
                    TimeRow("Речь днём", setup.daySpeechSeconds) { v ->
                        vm.updateSetup { it.copy(daySpeechSeconds = v) }
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
                    GhostButton(if (namesOpen) "Свернуть имена" else "Задать имена игроков") {
                        namesOpen = !namesOpen
                    }
                }
            }
            if (namesOpen) {
                itemsIndexed(setup.names) { index, name ->
                    OutlinedTextField(
                        value = name,
                        onValueChange = { vm.renamePlayer(index, it) },
                        label = { Text("Игрок ${index + 1}") },
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
            "${seconds} с",
            color = Gold,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 12.dp),
        )
        StepBtn("+") { if (seconds < 180) onChange(seconds + 15) }
    }
}
