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
        title = t("deal_title"),
        subtitle = t("deal_sub", s.dealIndex + 1, s.players.size),
        bottom = {
            BigButton(if (seen) t("memorized") else t("look_first"), enabled = seen) {
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
                            Text(t("role_hidden"), color = Gold, fontSize = 20.sp)
                            Spacer(Modifier.height(12.dp))
                            Text(
                                t("hold_to_see"),
                                color = Muted,
                                textAlign = TextAlign.Center,
                                fontSize = 16.sp,
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                t("release_hides"),
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
                                t(player.role.hintKey),
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
