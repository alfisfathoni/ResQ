package com.example.resq.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.resq.ui.theme.PoppinsFamily

/**
 * Circular avatar that shows the first letter(s) of a name.
 * Color is deterministically derived from the name so the same contact
 * always gets the same color across the app.
 */

private val AvatarPalette = listOf(
    Color(0xFF5C6BC0), // indigo
    Color(0xFF26A69A), // teal
    Color(0xFFEF5350), // red
    Color(0xFFAB47BC), // purple
    Color(0xFF42A5F5), // blue
    Color(0xFFFF7043), // deep orange
    Color(0xFF66BB6A), // green
    Color(0xFFEC407A), // pink
    Color(0xFF8D6E63), // brown
    Color(0xFF26C6DA), // cyan
)

private fun avatarColorFor(name: String): Color {
    val index = name.trim().firstOrNull()?.lowercaseChar()?.code ?: 0
    return AvatarPalette[index % AvatarPalette.size]
}

private fun initialsOf(name: String): String {
    val parts = name.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }
    return when {
        parts.isEmpty() -> "?"
        parts.size == 1 -> parts[0].first().uppercase()
        else            -> "${parts[0].first().uppercase()}${parts[1].first().uppercase()}"
    }
}

@Composable
fun InitialAvatar(
    name     : String,
    size     : Dp       = 44.dp,
    fontSize : TextUnit = 16.sp,
    modifier : Modifier = Modifier,
) {
    val color    = remember(name) { avatarColorFor(name) }
    val initials = remember(name) { initialsOf(name) }

    Box(
        contentAlignment = Alignment.Center,
        modifier         = modifier
            .size(size)
            .clip(CircleShape)
            .background(color),
    ) {
        Text(
            text       = initials,
            color      = Color.White,
            fontFamily = PoppinsFamily,
            fontWeight = FontWeight.Bold,
            fontSize   = fontSize,
        )
    }
}
