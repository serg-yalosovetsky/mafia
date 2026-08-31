package com.serg.mafia.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
fun NightScreen(vm: GameViewModel) {
    val s = vm.s
    val steps = s.nightSteps
    val step = steps.getOrNull(s.nightStepIndex) ?: return

    val selected = when (step.role) {
        Role.BUTTERFLY -> s.night.butterflyTarget
        Role.MAFIA -> s.night.mafiaTarget
        Role.DON -> s.night.donCheck
        Role.DOCTOR -> s.night.doctorTarget
        Role.SHERIFF -> s.night.sheriffCheck
        Role.MANIAC -> s.night.maniacTarget
        else -> null
    }

    // Носитель роли на этом шаге (для мафии — вся чёрная команда).
    val actorBlocked = when (step.role) {
        Role.MAFIA -> s.aliveBlack.size == 1 && vm.isBlocked(s.aliveBlack.firstOrNull()?.id)
        else -> vm.isBlocked(s.aliveHolders(step.role).firstOrNull()?.id)
    }

    val offerMiss = step.role == Role.MAFIA && s.isFirstNight &&
        s.setup.suggestsFirstNightMiss && !s.firstNightMissDecided

    Screen(
        title = t("night_title", s.nightNumber),
        subtitle = t("night_step", s.nightStepIndex + 1, steps.size),
        bottom = {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (s.nightStepIndex > 0) {
                    GhostButton(t("back"), Modifier.weight(1f)) { vm.nightBack() }
                }
                BigButton(
                    if (s.nightStepIndex == steps.lastIndex) t("city_wakes") else t("next"),
                    Modifier.weight(2f),
                ) { vm.nightNext() }
            }
        },
    ) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RolePortraitCard(step.role, size = 92, showTitle = false)
                Spacer(Modifier.height(8.dp))
                Column(Modifier.padding(start = 14.dp)) {
                    Text(t(step.titleKey), style = MaterialTheme.typography.titleLarge, color = Gold)
                    Text(t(step.promptKey), color = Muted, fontSize = 13.sp)
                    if (step.role == Role.MAFIA && s.aliveBlack.size > 1) {
                        Text(
                            t("blacks_alive", s.aliveBlack.size),
                            color = BlackTeam,
                            fontSize = 12.sp,
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))

            if (actorBlocked) {
                Banner(t("blocked_banner"), Blood)
            }

            if (offerMiss) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Surface2)
                        .padding(12.dp),
                ) {
                    Text(
                        t("miss_offer_head", s.setup.mafiaTotal, s.setup.playerCount),
                        color = Gold,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                    )
                    Text(
                        t("miss_offer_body"),
                        color = Muted,
                        fontSize = 13.sp,
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        BigButton(t("miss_yes"), Modifier.weight(1f), color = Moon.copy(alpha = 0.7f)) {
                            vm.setFirstNightMiss(true)
                        }
                        BigButton(t("miss_no"), Modifier.weight(1f)) {
                            vm.setFirstNightMiss(false)
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
            }

            if (s.night.mafiaMissed && step.role == Role.MAFIA) {
                Banner(t("miss_banner"), Moon)
            }

            // Результат проверки комиссара — только ведущему и только тут.
            if (step.role == Role.SHERIFF && selected != null && !actorBlocked) {
                val target = s.player(selected)
                Banner(
                    t("check_result", target.name, t(factionKey(target.role.checksAs))),
                    factionColor(target.role.checksAs),
                )
            }
            if (step.role == Role.DON && selected != null && !actorBlocked) {
                val target = s.player(selected)
                Banner(
                    if (target.role == Role.SHERIFF) t("is_sheriff") else t("not_sheriff"),
                    if (target.role == Role.SHERIFF) Gold else Muted,
                )
            }

            Spacer(Modifier.height(6.dp))
            PlayerPicker(
                players = s.alivePlayers,
                selectedId = selected,
                badgeFor = { p ->
                    when {
                        // Шаг комиссара: ведущий сразу видит принадлежность каждого.
                        step.revealFactions ->
                            t(factionKey(p.role.checksAs)) to factionColor(p.role.checksAs)
                        step.revealSheriff && p.role == Role.SHERIFF -> t("badge_sheriff") to Gold
                        step.role == Role.MAFIA && p.role.isBlack -> t("badge_own") to BlackTeam
                        step.role == Role.BUTTERFLY && p.id == s.night.butterflyTarget ->
                            t("badge_block") to Blood
                        else -> null
                    }
                },
                onSelect = { id ->
                    vm.chooseNightTarget(step.role, if (selected == id) null else id)
                },
            )
        }
    }
}

@Composable
private fun Banner(text: String, color: Color) {
    Text(
        text,
        color = color,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.14f))
            .padding(12.dp),
    )
}
