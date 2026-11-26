package com.example.resq.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.example.resq.data.NearbyResponder
import com.example.resq.data.ResponderCategory
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

// ── Category colours ───────────────────────────────────────────────────────────
private fun categoryColor(cat: ResponderCategory): Int = when (cat) {
    ResponderCategory.HOSPITAL  -> AndroidColor.parseColor("#E53935")
    ResponderCategory.POLICE    -> AndroidColor.parseColor("#1E88E5")
    ResponderCategory.AMBULANCE -> AndroidColor.parseColor("#E53935")
    ResponderCategory.FIRE      -> AndroidColor.parseColor("#FF6D00")
    ResponderCategory.CONTACT   -> AndroidColor.parseColor("#8D6E63")
}

private fun categoryEmoji(cat: ResponderCategory): String = when (cat) {
    ResponderCategory.HOSPITAL  -> "🏥"
    ResponderCategory.POLICE    -> "🚔"
    ResponderCategory.AMBULANCE -> "🚑"
    ResponderCategory.FIRE      -> "🚒"
    ResponderCategory.CONTACT   -> "👤"
}

/**
 * Pin-drop shaped marker drawable (teardrop) with coloured fill + white emoji inside.
 * Looks like a standard Google Maps-style pin.
 *
 *  ╭───╮
 *  │ 🏥│   ← circle head with emoji
 *  │   │
 *   ╲ ╱
 *    ▼       ← pointed tail at bottom
 */
private fun createPinBitmap(color: Int, label: String, selected: Boolean): Bitmap {
    val w = 96
    val h = 128
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)

    val headR   = w / 2f - 4f
    val cx      = w / 2f
    val headCy  = headR + 4f
    val tipY    = h.toFloat() - 4f

    // Shadow
    val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = AndroidColor.parseColor("#40000000")
    }
    canvas.drawOval(RectF(cx - headR + 4, tipY - 8, cx + headR - 4, tipY + 4), shadowPaint)

    // Pin body colour — slightly darker if selected
    val pinColor = if (selected) AndroidColor.parseColor("#22C55E") else color

    // ── Teardrop path ──────────────────────────────────────────────────────
    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = pinColor }
    val path = Path().apply {
        // Left side of tear
        moveTo(cx, tipY)
        cubicTo(cx - headR * 0.6f, tipY - 20f, cx - headR, headCy + headR * 0.9f, cx - headR, headCy)
        // Top arc
        arcTo(RectF(cx - headR, headCy - headR, cx + headR, headCy + headR), 180f, 180f, false)
        // Right side down to tip
        cubicTo(cx + headR, headCy + headR * 0.9f, cx + headR * 0.6f, tipY - 20f, cx, tipY)
        close()
    }
    canvas.drawPath(path, fillPaint)

    // White border on head circle
    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = AndroidColor.WHITE
        style = Paint.Style.STROKE
        strokeWidth = if (selected) 7f else 5f
    }
    canvas.drawCircle(cx, headCy, headR, strokePaint)

    // Emoji text in the circle
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize  = 36f
        textAlign = Paint.Align.CENTER
    }
    // Center text vertically in circle head
    val textY = headCy - (textPaint.ascent() + textPaint.descent()) / 2f
    canvas.drawText(label, cx, textY, textPaint)

    return bmp
}

/** Radar pulse concentric circle drawn as a Polygon ring. */
private fun radarCircle(center: GeoPoint, radiusMeter: Double, color: Int): Polygon {
    val polygon = Polygon()
    polygon.points = Polygon.pointsAsCircle(center, radiusMeter)
    // Semi-transparent fill + visible outline
    val fillAlpha  = (0x20).shl(24)
    polygon.fillPaint.color    = (color and 0x00FFFFFF) or fillAlpha
    polygon.outlinePaint.color = color
    polygon.outlinePaint.strokeWidth = 4f
    polygon.outlinePaint.style = Paint.Style.STROKE
    return polygon
}

@Composable
fun OsmMapView(
    modifier          : Modifier                  = Modifier,
    centerLat         : Double                    = -6.2088,
    centerLng         : Double                    = 106.8456,
    zoomLevel         : Double                    = 15.0,
    responders        : List<NearbyResponder>     = emptyList(),
    selectedResponder : NearbyResponder?          = null,
    showRadar         : Boolean                   = false,
    activeCategory    : ResponderCategory?        = null,
    onMarkerClick     : (NearbyResponder) -> Unit = {},
) {
    val context = LocalContext.current

    // OSMDroid user-agent (required)
    LaunchedEffect(Unit) {
        Configuration.getInstance().load(
            context,
            context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE),
        )
        Configuration.getInstance().userAgentValue = context.packageName
    }

    AndroidView(
        modifier = modifier,
        factory  = { ctx ->
            MapView(ctx).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                controller.setZoom(zoomLevel)
                controller.setCenter(GeoPoint(centerLat, centerLng))

                // Blue dot for user's own location
                val locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(ctx), this)
                locationOverlay.enableMyLocation()
                overlays.add(locationOverlay)
            }
        },
        update = { mapView ->
            // Remove all dynamic overlays (keep MyLocationNewOverlay)
            mapView.overlays.removeAll { it is Marker || it is Polygon }

            val userPoint = GeoPoint(centerLat, centerLng)

            // ── Radar rings (shown while scanning) ────────────────────────
            val scanColor = when (activeCategory) {
                ResponderCategory.POLICE    -> AndroidColor.parseColor("#1E88E5")
                ResponderCategory.FIRE      -> AndroidColor.parseColor("#FF6D00")
                ResponderCategory.CONTACT   -> AndroidColor.parseColor("#8D6E63")
                else                        -> AndroidColor.parseColor("#E53935")
            }

            if (showRadar) {
                // 3 concentric rings at 400m, 800m, 1200m
                listOf(400.0, 800.0, 1200.0).forEach { radius ->
                    mapView.overlays.add(radarCircle(userPoint, radius, scanColor))
                }
            } else if (responders.isNotEmpty()) {
                // After scan: show softer coverage ring up to furthest result
                val maxDist = responders.maxOf { it.distanceKm } * 1100
                mapView.overlays.add(radarCircle(userPoint, maxDist, scanColor))
            }

            // ── Responder pin markers ──────────────────────────────────────
            responders.forEach { responder ->
                val isSelected = selectedResponder?.id == responder.id
                val pinBmp     = createPinBitmap(
                    color    = categoryColor(responder.category),
                    label    = categoryEmoji(responder.category),
                    selected = isSelected,
                )
                val marker = Marker(mapView).apply {
                    position    = GeoPoint(responder.lat, responder.lng)
                    title       = responder.name
                    snippet     = "${responder.distanceKm} km · ${responder.etaMinutes} menit"
                    icon        = BitmapDrawable(mapView.resources, pinBmp)
                    // Anchor at the tip of the pin (bottom center)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    setOnMarkerClickListener { _, _ ->
                        onMarkerClick(responder)
                        true
                    }
                }
                mapView.overlays.add(marker)
            }

            // ── Zoom / pan ─────────────────────────────────────────────────
            when {
                selectedResponder != null ->
                    mapView.controller.animateTo(
                        GeoPoint(selectedResponder.lat, selectedResponder.lng),
                        17.0, 800L,
                    )
                responders.isNotEmpty() ->
                    mapView.controller.animateTo(userPoint, 14.0, 600L)
                else ->
                    mapView.controller.animateTo(userPoint, zoomLevel, 400L)
            }

            mapView.invalidate()
        },
    )
}
