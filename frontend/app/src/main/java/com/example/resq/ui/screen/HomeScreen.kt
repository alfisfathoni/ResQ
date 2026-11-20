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
import androidx.compose.runtime.remember
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
import com.example.resq.ui.components.SlideToCancel
import com.example.resq.ui.components.SosActiveContactsCard
import com.example.resq.ui.components.SosButton
import com.example.resq.ui.model.ResponderItem
import com.example.resq.ui.theme.BackgroundLight
import com.example.resq.ui.theme.ResQTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Full Homepage screen of the RESQ emergency app supporting 3 states:
 *
 * 1. Normal State ("Home"):
 *    - Logo + 2-line instruction subtitle
 *    - Pulsing "HELP" button with "HOLD 3S" badge
 *    - Instant Responders grid (Firefighter, Ambulance, Police, Family)
 *    - Current Address Card
 *    - Curved Bottom Nav Bar with Center SOS FAB
 *
 * 2. Holding Countdown State ("Count"):
 *    - Logo + "RELEASE TO CANCEL" in bold dark text
 *    - Circular countdown progress ring (3s -> 2s -> 1s)
 *    - Release before 3s cancels immediately
 *    - Responders and bottom bar hidden, Current Address Card kept at bottom
 *
 * 3. Active State ("SOS Aktif" - screenshot reference):
 *    - Concentric pulsing rings with "SOS" and green "ON" badge
 *    - 3 overlapping contact avatars
 *    - "Contact sent to 3 emergency contacts at HH:mm" card
 *    - "Slide to cancel" interactive slider
 *    - Current Address Card
 *    - Bottom Nav Bar
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
    var isSosActive by rememberSaveable { mutableStateOf(false) }
    var activationTime by rememberSaveable { mutableStateOf("09:41") }

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
                .padding(bottom = if (isHoldingSos && !isSosActive) 24.dp else 90.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(28.dp))

            // 1. Header Logo
            ResQLogoHeader(
                modifier       = Modifier.padding(horizontal = 24.dp),
                isCountingDown = isHoldingSos,
                showSubtitle   = !isSosActive, // Subtitle hidden in SOS Aktif state
            )

            Spacer(modifier = Modifier.height(if (isSosActive) 14.dp else 24.dp))

            // 2. Central SOS Button (Normal HELP / Countdown Ring / Concentric SOS ON)
            SosButton(
                holdDurationMs  = 3_000L,
                isSosActive     = isSosActive,
                onActivated     = {
                    isSosActive = true
                    try {
                        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                        activationTime = sdf.format(Date())
                    } catch (_: Exception) {
                        activationTime = "09:41"
                    }
                    onSosActivated()
                },
                onHoldingChange = { isHolding, _, _ ->
                    isHoldingSos = isHolding
                },
            )

            when {
                // ── State 3: SOS Aktif ─────────────────────────────────────────
                isSosActive -> {
                    Spacer(modifier = Modifier.height(14.dp))

                    // Contact notification card with 3 overlapping avatars
                    SosActiveContactsCard(
                        sentTime = activationTime,
                        modifier = Modifier.padding(horizontal = 24.dp),
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // "Slide to cancel" slider
                    SlideToCancel(
                        modifier    = Modifier.padding(horizontal = 24.dp),
                        onCancelled = {
                            isSosActive = false
                        },
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // ── State 2: Countdown ─────────────────────────────────────────
                isHoldingSos -> {
                    Spacer(modifier = Modifier.height(60.dp))
                }

                // ── State 1: Normal Home ───────────────────────────────────────
                else -> {
                    Spacer(modifier = Modifier.height(26.dp))

                    // Instant Responders Grid (Firefighter, Ambulance, Police, Family)
                    InstantRespondersSection(
                        modifier         = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp),
                        onResponderClick = onResponderClick,
                    )

                    Spacer(modifier = Modifier.height(18.dp))
                }
            }

            // 4. Current Address Card with status badges
            CurrentAddressCard(
                modifier = Modifier.padding(horizontal = 18.dp),
            )

            Spacer(modifier = Modifier.height(24.dp))
        }

        // ── Pinned Curved Bottom Nav Bar with Center FAB ──────────────────────
        // Hidden only during countdown hold gesture; visible in Normal & SOS Aktif states
        if (!isHoldingSos || isSosActive) {
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
