package com.example.resq.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.resq.ui.components.CurrentAddressCard
import com.example.resq.ui.components.InstantRespondersSection
import com.example.resq.ui.components.ResQBottomNavBar
import com.example.resq.ui.components.ResQLogoHeader
import com.example.resq.ui.components.SosButton
import com.example.resq.ui.model.ResponderItem
import com.example.resq.ui.theme.BackgroundLight
import com.example.resq.ui.theme.ResQTheme

/**
 * Full Homepage screen of the RESQ emergency app.
 * Matches the reference layout, spacing, and visual elements exactly.
 */
@Composable
fun HomeScreen(
    onSosActivated      : () -> Unit              = {},
    onResponderClick    : (ResponderItem) -> Unit = {},
    onNavItemSelected   : (String) -> Unit        = {},
    onCommunityFabClick : () -> Unit              = {},
) {
    var selectedRoute by rememberSaveable { mutableStateOf("home") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight),
    ) {
        // ── Scrollable Body Content ───────────────────────────────────────────
        Column(
            modifier            = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 90.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(28.dp))

            // 1. Header: Logo RESQ + Pin + Subtitle
            ResQLogoHeader(
                modifier = Modifier.padding(horizontal = 24.dp),
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 2. Main SOS "HELP" Button
            SosButton(
                holdDurationMs = 4_000L,
                onActivated    = onSosActivated,
            )

            Spacer(modifier = Modifier.height(26.dp))

            // 3. Instant Responders Grid (Firefighter, Ambulance, Police, Family)
            InstantRespondersSection(
                modifier         = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp),
                onResponderClick = onResponderClick,
            )

            Spacer(modifier = Modifier.height(18.dp))

            // 4. Current Address Card with status badges
            CurrentAddressCard(
                modifier = Modifier.padding(horizontal = 18.dp),
            )

            Spacer(modifier = Modifier.height(24.dp))
        }

        // ── Pinned Curved Bottom Nav Bar with Center FAB ──────────────────────
        ResQBottomNavBar(
            selectedRoute  = selectedRoute,
            onItemSelected = { route ->
                selectedRoute = route
                onNavItemSelected(route)
            },
            onFabClick = onCommunityFabClick,
            modifier   = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "HomeScreen")
@Composable
private fun HomeScreenPreview() {
    ResQTheme {
        HomeScreen()
    }
}
