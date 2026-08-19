package com.serg.mafia.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.serg.mafia.model.GameViewModel

@Composable
fun DealScreen(vm: GameViewModel) {
    val s = vm.s
    val player = s.players.getOrNull(s.dealIndex) ?: return
    var held by remember(s.dealIndex) { mutableStateOf(false) }
    var seen by remember(s.dealIndex) { mutableStateOf(false) }

    Screen(
        title = "Раздача ролей",
        subtitle = "Игрок ${s.dealIndex + 1} из ${s.players.size} · телефон идёт по кругу",
        bottom = {
            BigButton(if (seen) "Я запомнил, передаю дальше" else "Сначала посмотри роль", enabled = seen) {
                vm.dealNext()
            }
        },
    ) {
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                player.name,
                style = MaterialTheme.typography.headlineMedium,
                color = Gold,
            )
            Spacer(Modifier.height(10.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (held) Surface2 else Surface1)
                    .pointerInput(s.dealIndex) {
                        detectTapGestures(
                            onPress = {
                                held = true
                                seen = true
                                tryAwaitRelease()
                                held = false
                            },
                        )
                    },
                contentAlignment = Alignment.Center,
            ) {
                Crossfade(targetState = held, label = "role-reveal") { revealed ->
                    if (!revealed) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(16.dp),
                        ) {
                            Text("Роль скрыта", color = Gold, fontSize = 20.sp)
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "Держи палец на карточке,\nчтобы увидеть свою роль",
                                color = Muted,
                                textAlign = TextAlign.Center,
                                fontSize = 16.sp,
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "отпустишь — роль снова скроется",
                                color = Muted.copy(alpha = 0.6f),
                                fontSize = 13.sp,
                            )
                        }
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(16.dp),
                        ) {
                            RolePortraitCard(player.role, size = 200)
                            Spacer(Modifier.height(12.dp))
                            Text(
                                player.role.hint,
                                color = Color(0xFFEDE7F2),
                                textAlign = TextAlign.Center,
                                fontSize = 15.sp,
                            )
                        }
                    }
                }
            }
        }
    }
}
