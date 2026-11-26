package com.example.resq.ui.screen

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Navigation
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocalPolice
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.resq.data.DummyResponders
import com.example.resq.data.NearbyResponder
import com.example.resq.data.ResponderCategory
import com.example.resq.ui.components.OsmMapView
import com.example.resq.ui.components.ResQBottomNavBar
import com.example.resq.ui.theme.*
import kotlinx.coroutines.delay

private const val USER_LAT = -6.2088
private const val USER_LNG = 106.8456

// ── Category chip meta ─────────────────────────────────────────────────────────
private data class CategoryChipMeta(
    val category    : ResponderCategory,
    val icon        : ImageVector,
    val activeColor : Color,
)

private val categoryChips = listOf(
    CategoryChipMeta(ResponderCategory.HOSPITAL,  Icons.Default.LocalHospital,       Color(0xFFE53935)),
    CategoryChipMeta(ResponderCategory.POLICE,    Icons.Default.LocalPolice,         Color(0xFF1E88E5)),
    CategoryChipMeta(ResponderCategory.AMBULANCE, Icons.Default.MedicalServices,     Color(0xFFE53935)),
    CategoryChipMeta(ResponderCategory.FIRE,      Icons.Default.LocalFireDepartment, Color(0xFFFF6D00)),
    CategoryChipMeta(ResponderCategory.CONTACT,   Icons.Default.Contacts,            Color(0xFF8D6E63)),
)

// Colour per category (for card accents)
private fun categoryColor(cat: ResponderCategory) = when (cat) {
    ResponderCategory.HOSPITAL  -> Color(0xFFE53935)
    ResponderCategory.POLICE    -> Color(0xFF1E88E5)
    ResponderCategory.AMBULANCE -> Color(0xFFE53935)
    ResponderCategory.FIRE      -> Color(0xFFFF6D00)
    ResponderCategory.CONTACT   -> Color(0xFF8D6E63)
}

// Emoji used in photo placeholder circle
private fun categoryEmoji(cat: ResponderCategory) = when (cat) {
    ResponderCategory.HOSPITAL  -> "🏥"
    ResponderCategory.POLICE    -> "🚔"
    ResponderCategory.AMBULANCE -> "🚑"
    ResponderCategory.FIRE      -> "🚒"
    ResponderCategory.CONTACT   -> "📞"
}

// Background tint for the photo placeholder
private fun categoryBg(cat: ResponderCategory) = when (cat) {
    ResponderCategory.HOSPITAL  -> Color(0xFFFFEBEE)
    ResponderCategory.POLICE    -> Color(0xFFE3F2FD)
    ResponderCategory.AMBULANCE -> Color(0xFFFFEBEE)
    ResponderCategory.FIRE      -> Color(0xFFFFF3E0)
    ResponderCategory.CONTACT   -> Color(0xFFEFEBE9)
}

/**
 * Full Map Screen — matches design image:
 *
 * Layout (top → bottom):
 *  ┌─────────────────────────────────────────┐
 *  │  [Search bar            ] [🔧 filter]   │  ← floating over map
 *  │  [Hospital] [Police] [Ambulance] …      │  ← chip row
 *  │                                         │
 *  │       OSMDroid interactive map          │
 *  │   (pin-drop markers + radar rings)      │
 *  │                                         │
 *  ├─────────────────────────────────────────┤
 *  │  ── drag handle ──                      │
 *  │  Hospital                 5 ditemukan   │  ← bottom sheet
 *  │  ┌─────────────────────────────────┐    │
 *  │  │ 🏥  Bhakti Asih General…  [Buka]│    │
 *  │  │     Jl. Raya Kesehatan · 1.2km  │    │
 *  │  └─────────────────────────────────┘    │
 *  ├─────────────────────────────────────────┤
 *  │  [Home] [Map●] [Community] [Rep] [Prof] │  ← bottom nav
 *  └─────────────────────────────────────────┘
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    onNavItemSelected   : (String) -> Unit = {},
    onCommunityFabClick : () -> Unit       = {},
) {
    val context = LocalContext.current

    // ── UI State ───────────────────────────────────────────────────────────────
    var searchQuery       by remember { mutableStateOf("") }
    var activeCategory    by remember { mutableStateOf<ResponderCategory?>(null) }
    var isScanning        by remember { mutableStateOf(false) }
    var visibleResponders by remember { mutableStateOf<List<NearbyResponder>>(emptyList()) }
    var selectedResponder by remember { mutableStateOf<NearbyResponder?>(null) }
    var sheetExpanded     by remember { mutableStateOf(false) }

    // ── Radar pulse (Compose-layer animation while scanning) ───────────────────
    val infiniteTransition = rememberInfiniteTransition(label = "radar")
    val radarScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue  = 1.25f,
        animationSpec = infiniteRepeatable(
            tween(900, easing = EaseInOut),
            RepeatMode.Reverse,
        ),
        label = "radarScale",
    )
    val radarAlpha by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue  = 0.4f,
        animationSpec = infiniteRepeatable(
            tween(900, easing = EaseInOut),
            RepeatMode.Reverse,
        ),
        label = "radarAlpha",
    )

    // ── Rotate animation for radar icon ───────────────────────────────────────
    val radarRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue  = 360f,
        animationSpec = infiniteRepeatable(
            tween(2000, easing = LinearEasing),
            RepeatMode.Restart,
        ),
        label = "radarRot",
    )

    // ── Filtered list (search within active category results) ─────────────────
    val filteredList = remember(visibleResponders, searchQuery) {
        if (searchQuery.isBlank()) visibleResponders
        else visibleResponders.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.address.contains(searchQuery, ignoreCase = true)
        }
    }

    // Trigger scan: start animation, wait 1.5s, populate results
    fun scanCategory(cat: ResponderCategory) {
        activeCategory    = cat
        isScanning        = true
        visibleResponders = emptyList()
        selectedResponder = null
        sheetExpanded     = false
    }

    LaunchedEffect(activeCategory, isScanning) {
        if (activeCategory != null && isScanning) {
            delay(1500)
            visibleResponders = DummyResponders.byCategory(activeCategory!!)
            isScanning        = false
            sheetExpanded     = true
        }
    }

    // ── Bottom sheet scaffold ──────────────────────────────────────────────────
    val sheetState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            initialValue    = SheetValue.Hidden,
            skipHiddenState = false,
        )
    )

    LaunchedEffect(sheetExpanded, filteredList.size) {
        try {
            if (sheetExpanded && filteredList.isNotEmpty()) {
                sheetState.bottomSheetState.partialExpand()
            } else if (!sheetExpanded) {
                sheetState.bottomSheetState.hide()
            }
        } catch (_: Exception) {}
    }

    // ── Root ───────────────────────────────────────────────────────────────────
    Box(modifier = Modifier.fillMaxSize()) {

        BottomSheetScaffold(
            scaffoldState       = sheetState,
            sheetPeekHeight     = 220.dp,
            sheetShape          = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            sheetContainerColor = Color.White,
            sheetShadowElevation = 16.dp,
            sheetContent        = {
                SheetContent(
                    activeCategory    = activeCategory,
                    isScanning        = isScanning,
                    filteredList      = filteredList,
                    selectedResponder = selectedResponder,
                    onCardClick       = { r ->
                        selectedResponder = if (selectedResponder?.id == r.id) null else r
                    },
                    onNavigate        = { r ->
                        val navUri = Uri.parse("google.navigation:q=${r.lat},${r.lng}&mode=d")
                        val navIntent = Intent(Intent.ACTION_VIEW, navUri).apply {
                            setPackage("com.google.android.apps.maps")
                        }
                        if (navIntent.resolveActivity(context.packageManager) != null) {
                            context.startActivity(navIntent)
                        } else {
                            val browserUri = Uri.parse(
                                "https://www.google.com/maps/dir/?api=1" +
                                "&origin=$USER_LAT,$USER_LNG" +
                                "&destination=${r.lat},${r.lng}&travelmode=driving"
                            )
                            context.startActivity(Intent(Intent.ACTION_VIEW, browserUri))
                        }
                    },
                )
            },
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                // ── OSMDroid Map ───────────────────────────────────────────
                OsmMapView(
                    modifier          = Modifier.fillMaxSize(),
                    centerLat         = USER_LAT,
                    centerLng         = USER_LNG,
                    zoomLevel         = 14.0,
                    responders        = visibleResponders,
                    selectedResponder = selectedResponder,
                    showRadar         = isScanning,
                    activeCategory    = activeCategory,
                    onMarkerClick     = { r ->
                        selectedResponder = r
                        sheetExpanded     = true
                    },
                )

                // ── Floating top bar ───────────────────────────────────────
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(top = 10.dp),
                ) {
                    // Search bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .shadow(8.dp, RoundedCornerShape(50.dp))
                            .background(Color.White, RoundedCornerShape(50.dp))
                            .padding(horizontal = 14.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector        = Icons.Default.Search,
                            contentDescription = "Search",
                            tint               = TextSecondary,
                            modifier           = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))

                        // Inline text field (no OutlinedTextField chrome)
                        Box(modifier = Modifier.weight(1f)) {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text      = "Cari rumah sakit, polisi…",
                                    style     = TextStyle(
                                        fontFamily = PoppinsFamily,
                                        fontSize   = 13.sp,
                                        color      = TextSecondary.copy(alpha = 0.55f),
                                    ),
                                )
                            }
                            BasicTextField(
                                value         = searchQuery,
                                onValueChange = { searchQuery = it },
                                singleLine    = true,
                                cursorBrush   = SolidColor(ResQRed),
                                textStyle     = TextStyle(
                                    fontFamily = PoppinsFamily,
                                    fontSize   = 13.sp,
                                    color      = TextPrimary,
                                ),
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }

                        // Clear icon
                        if (searchQuery.isNotBlank()) {
                            Icon(
                                imageVector        = Icons.Default.Close,
                                contentDescription = "Clear",
                                tint               = TextSecondary,
                                modifier           = Modifier
                                    .size(18.dp)
                                    .clickable { searchQuery = "" },
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }

                        // Divider
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(20.dp)
                                .background(Color(0xFFE5E7EB)),
                        )
                        Spacer(modifier = Modifier.width(8.dp))

                        // Filter / sliders icon — matches design
                        Icon(
                            imageVector        = Icons.Default.Tune,
                            contentDescription = "Filter",
                            tint               = ResQRed,
                            modifier           = Modifier
                                .size(20.dp)
                                .clickable { /* TODO: filter sheet */ },
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Category chips
                    LazyRow(
                        contentPadding        = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(categoryChips) { chip ->
                            val isActive = activeCategory == chip.category
                            Row(
                                modifier = Modifier
                                    .shadow(5.dp, RoundedCornerShape(50.dp))
                                    .background(
                                        if (isActive) chip.activeColor else Color.White,
                                        RoundedCornerShape(50.dp),
                                    )
                                    .border(
                                        1.5.dp,
                                        if (isActive) chip.activeColor else Color(0xFFE5E7EB),
                                        RoundedCornerShape(50.dp),
                                    )
                                    .clickable { scanCategory(chip.category) }
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Icon(
                                    imageVector        = chip.icon,
                                    contentDescription = chip.category.label,
                                    tint               = if (isActive) Color.White else chip.activeColor,
                                    modifier           = Modifier.size(16.dp),
                                )
                                Text(
                                    text       = chip.category.label,
                                    fontFamily = PoppinsFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize   = 13.sp,
                                    color      = if (isActive) Color.White else TextPrimary,
                                )
                            }
                        }
                    }
                }

                // ── Radar pulse overlay (Compose layer, shown while scanning) ──
                if (isScanning) {
                    val radarColor = when (activeCategory) {
                        ResponderCategory.POLICE    -> Color(0xFF1E88E5)
                        ResponderCategory.FIRE      -> Color(0xFFFF6D00)
                        ResponderCategory.CONTACT   -> Color(0xFF8D6E63)
                        else                        -> ResQRed
                    }

                    // Outer pulsing ring
                    Box(
                        modifier         = Modifier
                            .align(Alignment.Center)
                            .scale(radarScale)
                            .alpha(radarAlpha)
                            .size(140.dp)
                            .background(radarColor.copy(alpha = 0.12f), CircleShape)
                            .border(2.dp, radarColor.copy(alpha = 0.4f), CircleShape),
                    )
                    // Middle ring
                    Box(
                        modifier         = Modifier
                            .align(Alignment.Center)
                            .scale(radarScale * 0.65f)
                            .alpha(radarAlpha)
                            .size(140.dp)
                            .background(radarColor.copy(alpha = 0.18f), CircleShape)
                            .border(2.dp, radarColor.copy(alpha = 0.5f), CircleShape),
                    )
                    // Inner centre button with rotating radar icon
                    Box(
                        modifier         = Modifier
                            .align(Alignment.Center)
                            .size(60.dp)
                            .background(radarColor, CircleShape)
                            .border(3.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector        = Icons.Default.Radar,
                            contentDescription = "Scanning",
                            tint               = Color.White,
                            modifier           = Modifier.size(32.dp),
                        )
                    }

                    // Scanning label below the radar
                    Box(
                        modifier         = Modifier
                            .align(Alignment.Center)
                            .padding(top = 80.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(50.dp))
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                        ) {
                            Text(
                                text       = "Mencari ${activeCategory?.label} terdekat…",
                                fontFamily = PoppinsFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize   = 12.sp,
                                color      = Color.White,
                            )
                        }
                    }
                }
            }
        }

        // ── Bottom Nav Bar — always on top of everything ───────────────────────
        ResQBottomNavBar(
            selectedRoute  = "map",
            onItemSelected = onNavItemSelected,
            onFabClick     = onCommunityFabClick,
            modifier       = Modifier.align(Alignment.BottomCenter),
        )
    }
}

// ── Bottom sheet content ──────────────────────────────────────────────────────

@Composable
private fun SheetContent(
    activeCategory    : ResponderCategory?,
    isScanning        : Boolean,
    filteredList      : List<NearbyResponder>,
    selectedResponder : NearbyResponder?,
    onCardClick       : (NearbyResponder) -> Unit,
    onNavigate        : (NearbyResponder) -> Unit,
) {
    // Drag handle
    Box(
        modifier         = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(4.dp)
                .clip(CircleShape)
                .background(Color(0xFFDDE1E7)),
        )
    }

    // Sheet header
    if (activeCategory != null) {
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val chipColor = categoryColor(activeCategory)
                Box(
                    modifier         = Modifier
                        .size(32.dp)
                        .background(chipColor.copy(alpha = 0.12f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(categoryEmoji(activeCategory), fontSize = 16.sp)
                }
                Text(
                    text       = activeCategory.label,
                    fontFamily = PoppinsFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize   = 18.sp,
                    color      = TextPrimary,
                )
            }
            if (!isScanning) {
                Text(
                    text       = "${filteredList.size} ditemukan",
                    fontFamily = PoppinsFamily,
                    fontSize   = 12.sp,
                    color      = TextSecondary,
                )
            }
        }
    }

    HorizontalDivider(color = Color(0xFFF0F2F5), thickness = 1.dp,
        modifier = Modifier.padding(horizontal = 16.dp))

    // List or loading
    LazyColumn(
        contentPadding      = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier            = Modifier.fillMaxWidth(),
    ) {
        if (isScanning) {
            item {
                Box(
                    modifier         = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            color       = ResQRed,
                            strokeWidth = 3.dp,
                            modifier    = Modifier.size(36.dp),
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Mencari ${activeCategory?.label} terdekat…",
                            fontFamily = PoppinsFamily,
                            fontSize   = 13.sp,
                            color      = TextSecondary,
                        )
                    }
                }
            }
        } else {
            items(filteredList, key = { it.id }) { responder ->
                ResponderCard(
                    responder    = responder,
                    isSelected   = selectedResponder?.id == responder.id,
                    onClick      = { onCardClick(responder) },
                    onNavigate   = { onNavigate(responder) },
                )
            }
        }
    }
}

// ── Responder card — matches design: photo circle + name + address + badges + Buka ──

@Composable
private fun ResponderCard(
    responder  : NearbyResponder,
    isSelected : Boolean,
    onClick    : () -> Unit,
    onNavigate : () -> Unit,
) {
    val accentColor = categoryColor(responder.category)
    val borderColor = if (isSelected) accentColor else Color(0xFFF0F2F5)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.5.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // ── Photo placeholder circle (matches design thumbnail) ────────────
        Box(
            modifier         = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(categoryBg(responder.category)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text     = categoryEmoji(responder.category),
                fontSize = 26.sp,
            )
        }

        // ── Name + address + distance row ──────────────────────────────────
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text       = responder.name,
                fontFamily = PoppinsFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize   = 13.sp,
                color      = TextPrimary,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text       = responder.address,
                fontFamily = PoppinsFamily,
                fontSize   = 11.sp,
                color      = TextSecondary,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                // Distance badge
                Badge(
                    text  = "${responder.distanceKm} km",
                    color = accentColor,
                )
                // ETA badge
                Badge(
                    text  = "${responder.etaMinutes} menit",
                    color = Color(0xFF64748B),
                )
            }
        }

        // ── Buka button (green, matches design) ────────────────────────────
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(BadgeGreenSolid)
                .clickable(onClick = onNavigate)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    imageVector        = Icons.AutoMirrored.Filled.Navigation,
                    contentDescription = "Navigate",
                    tint               = Color.White,
                    modifier           = Modifier.size(14.dp),
                )
                Text(
                    text       = "Buka",
                    fontFamily = PoppinsFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize   = 13.sp,
                    color      = Color.White,
                )
            }
        }
    }
}

@Composable
private fun Badge(text: String, color: Color) {
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(50.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(
            text       = text,
            fontFamily = PoppinsFamily,
            fontWeight = FontWeight.Medium,
            fontSize   = 10.sp,
            color      = color,
        )
    }
}
