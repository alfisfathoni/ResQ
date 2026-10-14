package com.example.resq.ui.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Data class for a bottom-navigation destination.
 */
data class NavItem(
    val route       : String,
    val label       : String,
    val icon        : ImageVector,
    val selectedIcon: ImageVector = icon,
)

/**
 * Data class for an Instant Responder card.
 * Each responder has its own specific theme tint color as seen in the design image.
 */
data class ResponderItem(
    val label             : String,
    val icon              : ImageVector,
    val tintColor         : Color,
    val contentDescription: String = label,
)
