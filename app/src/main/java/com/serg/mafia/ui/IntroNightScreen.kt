package com.serg.mafia.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.serg.mafia.model.GameViewModel
import com.serg.mafia.model.Role

/**
 * Знакомство: фракции открывают глаза по очереди, а ведущий сверяет со списком,
 * что глаза открыли ровно те, кто должен.
 */
@Composable
fun IntroNightScreen(vm: GameViewModel) {
    val s = vm.s
    val steps = s.introSteps
    val step = steps.getOrNull(s.introStepIndex) ?: return
    val actors = s.introActors(step)

    Screen(
        title = t("intro_title"),
        subtitle = t("intro_step", s.introStepIndex + 1, steps.size),
        bottom = {
            BigButton(if (s.introStepIndex == steps.lastIndex) t("city_wakes") else t("next")) {
                vm.introNext()
            }
        },
    ) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            RolePortraitCard(step.role, size = 132, showTitle = false)
            Spacer(Modifier.height(12.dp))
            Text(t(step.titleKey), style = MaterialTheme.typography.headlineMedium, color = Gold)
            Spacer(Modifier.height(6.dp))
            Text(
                t(step.promptKey),
                color = Muted,
                textAlign = TextAlign.Center,
                fontSize = 14.sp,
            )
            Spacer(Modifier.height(14.dp))
            Text(
                if (actors.size > 1) t("opens_eyes_many") else t("opens_eyes_one"),
                color = Moon,
                fontSize = 13.sp,
            )
            Spacer(Modifier.height(6.dp))
            LazyColumn(Modifier.fillMaxSize()) {
                items(actors, key = { it.id }) { p ->
                    PlayerRow(
                        player = p,
                        badge = t(p.role.titleKey),
                        badgeColor = factionColor(p.role.faction),
                        trailing = if (p.role == Role.DON) t("chief") else null,
                    )
                }
            }
        }
    }
}
