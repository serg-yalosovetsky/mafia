package com.serg.mafia.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.serg.mafia.R
import com.serg.mafia.model.Faction
import com.serg.mafia.model.Role

val Ink = Color(0xFF0E0B10)
val Surface1 = Color(0xFF17131C)
val Surface2 = Color(0xFF221C29)
val Blood = Color(0xFFC2413C)
val Gold = Color(0xFFD8A55B)
val Moon = Color(0xFF8FA6C9)
val RedTeam = Color(0xFFD05A55)
const val BLACK_TEAM_HEX = 0xFF9B8CC4
val BlackTeam = Color(BLACK_TEAM_HEX)
val ManiacTeam = Color(0xFF5FBF8C)
val Muted = Color(0xFF9A93A6)

private val scheme = darkColorScheme(
    primary = Blood,
    onPrimary = Color.White,
    secondary = Gold,
    onSecondary = Ink,
    background = Ink,
    onBackground = Color(0xFFEDE7F2),
    surface = Surface1,
    onSurface = Color(0xFFEDE7F2),
    surfaceVariant = Surface2,
    onSurfaceVariant = Muted,
    error = Blood,
)

private val typography = Typography(
    headlineMedium = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold),
    titleLarge = TextStyle(fontSize = 21.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 16.sp),
    bodyMedium = TextStyle(fontSize = 14.sp),
    labelLarge = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Medium),
)

@Composable
fun MafiaTheme(content: @Composable () -> Unit) {
    @Suppress("UNUSED_EXPRESSION")
    isSystemInDarkTheme() // приложение всегда тёмное: играют вечером
    MaterialTheme(colorScheme = scheme, typography = typography, content = content)
}

fun factionColor(f: Faction): Color = when (f) {
    Faction.RED -> RedTeam
    Faction.BLACK -> BlackTeam
    Faction.MANIAC -> ManiacTeam
}

fun factionLabel(f: Faction): String = when (f) {
    Faction.RED -> "красный"
    Faction.BLACK -> "чёрный"
    Faction.MANIAC -> "маньяк"
}

fun rolePortrait(role: Role): Int = when (role) {
    Role.CIVILIAN -> R.drawable.role_civilian
    Role.DOCTOR -> R.drawable.role_doctor
    Role.SHERIFF -> R.drawable.role_sheriff
    Role.BUTTERFLY -> R.drawable.role_butterfly
    Role.MAFIA -> R.drawable.role_mafia
    Role.DON -> R.drawable.role_don
    Role.MANIAC -> R.drawable.role_maniac
}
