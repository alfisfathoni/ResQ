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
 * Matches both states in the reference screenshot:
 * 1. Normal state ("Home"):
 *    - Logo + 2-line instruction subtitle
 *    - Pulsing "HELP" button with "HOLD 4S" badge
 *    - Instant Responders grid (Firefighter, Ambulance, Police, Family)
 *    - Current Address Card
 *    - Curved Bottom Nav Bar with Center SOS FAB
 * 2. Holding countdown state ("Count"):
 *    - Logo + "RELEASE TO CANCEL" in bold dark text
 *    - Circular countdown progress ring (4s -> 3s -> 2s -> 1s)
 *    - Release before 4s cancels immediately
 *    - Bottom nav and responders hidden, Current Address Card kept at bottom
 */
@Composable
fun HomeScreen(
    onSosActivated      : () -> Unit              = {},
    onResponderClick    : (ResponderItem) -> Unit = {},
    onNavItemSelected   : (String) -> Unit        = {},
    onCommunityFabClick : () -> Unit              = {},
) {
    var selectedRoute by rememberSaveable { mutableStateOf("home") }
    var isHoldingSos by remember { mutableStateOf(false) }

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
                .padding(bottom = if (isHoldingSos) 24.dp else 90.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(28.dp))

            // 1. Header: Logo RESQ + Pin + Subtitle ("RELEASE TO CANCEL" when holding)
            ResQLogoHeader(
                modifier       = Modifier.padding(horizontal = 24.dp),
                isCountingDown = isHoldingSos,
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 2. Main SOS Button (Switches to Circular Countdown Ring on hold)
            SosButton(
                holdDurationMs  = 4_000L,
                onActivated     = onSosActivated,
                onHoldingChange = { isHolding, _, _ ->
                    isHoldingSos = isHolding
                },
            )

            Spacer(modifier = Modifier.height(26.dp))

            if (!isHoldingSos) {
                // 3. Instant Responders Grid (Only shown in normal state)
                InstantRespondersSection(
                    modifier         = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp),
                    onResponderClick = onResponderClick,
                )

                Spacer(modifier = Modifier.height(18.dp))
            } else {
                // Spacing in countdown mode so Address Card is nicely placed below the ring
                Spacer(modifier = Modifier.height(60.dp))
            }

            // 4. Current Address Card with status badges
            CurrentAddressCard(
                modifier = Modifier.padding(horizontal = 18.dp),
            )

            Spacer(modifier = Modifier.height(24.dp))
        }

        // ── Pinned Curved Bottom Nav Bar with Center FAB (Hidden in countdown mode) ──
        if (!isHoldingSos) {
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
}

@Preview(showBackground = true, showSystemUi = true, name = "HomeScreen")
@Composable
private fun HomeScreenPreview() {
    ResQTheme {
        HomeScreen()
    }
}
