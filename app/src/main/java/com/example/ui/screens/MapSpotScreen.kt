package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.model.CarSpot
import com.example.model.RadarSighting
import com.example.model.RarityTier
import com.example.ui.CarSpotterViewModel
import com.example.ui.components.CarSpotImage
import com.example.ui.theme.CobaltAccent
import com.example.ui.theme.CobaltSoft
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.HairlineStrong
import com.example.ui.theme.InkMuted
import com.example.ui.theme.InkPrimary
import com.example.ui.theme.InkSecondary
import com.example.ui.theme.MistSurface
import com.example.ui.theme.OutfitDisplayFont
import com.example.ui.theme.PorcelainSurface
import com.example.ui.theme.PureWhite
import com.example.ui.theme.RacingRed
import com.example.ui.theme.WarmGold
import com.example.util.LocationHelper
import com.example.util.SpotCoordinates
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sinh
import kotlin.math.tan

private enum class SightingScopeFilter(val label: String) {
    ALL("All Sightings"),
    YOURS("Yours"),
    OTHERS("Others")
}

private const val DEFAULT_LAT = 34.0522
private const val DEFAULT_LNG = -118.2437
private const val TILE_BASE_DP = 256.0
private const val MIN_MAP_ZOOM = 3.0
private const val MAX_MAP_ZOOM = 18.0

@Composable
fun MapSpotScreen(
    viewModel: CarSpotterViewModel
) {
    val context = LocalContext.current
    val allSightings by viewModel.radarSightings.collectAsState()
    val userLocation by viewModel.currentUserLocation.collectAsState()

    var isStreetMapMode by remember { mutableStateOf(true) }
    var scopeFilter by remember { mutableStateOf(SightingScopeFilter.ALL) }
    var selectedTierFilter by remember { mutableStateOf<RarityTier?>(null) }
    var selectedSightingId by remember { mutableStateOf<String?>(null) }

    var hasLocationPermission by remember {
        mutableStateOf(LocationHelper.hasLocationPermission(context))
    }

    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    var showPermissionBanner by remember {
        mutableStateOf(!hasLocationPermission || !hasNotificationPermission)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val fine = perms[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarse = perms[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        val locGranted = fine || coarse || LocationHelper.hasLocationPermission(context)
        val notifGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms[Manifest.permission.POST_NOTIFICATIONS] ?: hasNotificationPermission
        } else {
            true
        }
        hasLocationPermission = locGranted
        hasNotificationPermission = notifGranted
        if (locGranted) {
            viewModel.refreshUserLocation()
        }
        if (locGranted && notifGranted) {
            showPermissionBanner = false
        }
    }

    LaunchedEffect(Unit) {
        if (hasLocationPermission) {
            viewModel.refreshUserLocation()
        }
    }

    val filteredSightings = remember(allSightings, scopeFilter, selectedTierFilter) {
        allSightings.filter { sighting ->
            val matchesScope = when (scopeFilter) {
                SightingScopeFilter.ALL -> true
                SightingScopeFilter.YOURS -> sighting.isOwnSpot
                SightingScopeFilter.OTHERS -> !sighting.isOwnSpot
            }
            val matchesTier = selectedTierFilter == null || sighting.carSpot.rarityTier == selectedTierFilter
            matchesScope && matchesTier
        }
    }

    val mappedSightings = remember(filteredSightings) {
        filteredSightings.filter { hasValidCoordinates(it.carSpot) }
    }

    val selectedSighting = remember(allSightings, filteredSightings, selectedSightingId) {
        selectedSightingId?.let { id ->
            filteredSightings.firstOrNull { it.carSpot.id == id }
                ?: allSightings.firstOrNull { it.carSpot.id == id }
        }
    }

    // Camera viewport state for the interactive Slippy-Tile Map
    var centerLat by remember { mutableDoubleStateOf(DEFAULT_LAT) }
    var centerLng by remember { mutableDoubleStateOf(DEFAULT_LNG) }
    var zoomLevel by remember { mutableDoubleStateOf(12.0) }
    var hasInitializedCamera by remember { mutableStateOf(false) }

    LaunchedEffect(userLocation, mappedSightings.size) {
        if (!hasInitializedCamera) {
            when {
                userLocation != null -> {
                    centerLat = userLocation!!.latitude
                    centerLng = userLocation!!.longitude
                    zoomLevel = 12.5
                    hasInitializedCamera = true
                }
                mappedSightings.isNotEmpty() -> {
                    val latest = mappedSightings.first().carSpot
                    centerLat = latest.latitude
                    centerLng = latest.longitude
                    zoomLevel = 12.0
                    hasInitializedCamera = true
                }
            }
        }
    }

    val focusCameraOn: (Double, Double, Double) -> Unit = { lat, lng, zoom ->
        centerLat = lat.coerceIn(-85.0, 85.0)
        centerLng = normalizeLongitude(lng)
        zoomLevel = zoom.coerceIn(MIN_MAP_ZOOM, MAX_MAP_ZOOM)
        hasInitializedCamera = true
    }

    val fitAllSightings: () -> Unit = {
        val points = buildList {
            userLocation?.let { add(it.latitude to it.longitude) }
            mappedSightings.forEach { add(it.carSpot.latitude to it.carSpot.longitude) }
        }
        if (points.isNotEmpty()) {
            val minLat = points.minOf { it.first }
            val maxLat = points.maxOf { it.first }
            val minLng = points.minOf { it.second }
            val maxLng = points.maxOf { it.second }
            centerLat = (minLat + maxLat) / 2.0
            centerLng = (minLng + maxLng) / 2.0
            val span = maxOf(abs(maxLat - minLat), abs(maxLng - minLng))
            zoomLevel = when {
                span < 0.015 -> 13.5
                span < 0.06 -> 12.0
                span < 0.25 -> 10.5
                span < 1.2 -> 8.5
                span < 6.0 -> 6.0
                else -> 4.0
            }
            hasInitializedCamera = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PureWhite)
            .testTag("map_spot_screen")
    ) {
        if (isStreetMapMode) {
            InteractiveStreetTileMap(
                sightings = mappedSightings,
                userLocation = userLocation,
                selectedSighting = selectedSighting,
                centerLat = centerLat,
                centerLng = centerLng,
                zoomLevel = zoomLevel,
                onCameraMoved = { newLat, newLng, newZoom ->
                    centerLat = newLat
                    centerLng = newLng
                    zoomLevel = newZoom
                    hasInitializedCamera = true
                },
                onSightingSelected = { sighting ->
                    selectedSightingId = sighting.carSpot.id
                    if (hasValidCoordinates(sighting.carSpot)) {
                        focusCameraOn(
                            sighting.carSpot.latitude,
                            sighting.carSpot.longitude,
                            maxOf(zoomLevel, 13.0)
                        )
                    }
                }
            )
        } else {
            TacticalRadarMapCanvas(
                sightings = mappedSightings,
                userLocation = userLocation,
                selectedSighting = selectedSighting,
                onSightingClick = { sighting ->
                    selectedSightingId = sighting.carSpot.id
                }
            )
        }

        // Top Floating Header, Permission Prompt, Scope & Tier Filters
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            val ownCount = filteredSightings.count { it.isOwnSpot }
            val otherCount = filteredSightings.size - ownCount

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(PureWhite.copy(alpha = 0.96f))
                    .border(1.dp, HairlineBorder, RoundedCornerShape(18.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(CobaltSoft),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isStreetMapMode) Icons.Default.Map else Icons.Default.Radar,
                            contentDescription = null,
                            tint = CobaltAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (isStreetMapMode) "SPOTTER MAP" else "LIVE RADAR",
                            color = CobaltAccent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        )
                        Text(
                            text = "${mappedSightings.size} Pins • $ownCount Yours • $otherCount Others",
                            fontFamily = OutfitDisplayFont,
                            color = InkPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = { isStreetMapMode = !isStreetMapMode },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = InkPrimary,
                        contentColor = PureWhite
                    ),
                    shape = RoundedCornerShape(999.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("toggle_map_view_mode")
                ) {
                    Icon(
                        imageVector = if (isStreetMapMode) Icons.Default.Radar else Icons.Default.Map,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = if (isStreetMapMode) "Radar" else "Map",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (showPermissionBanner) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("notification_permission_banner"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = PureWhite.copy(alpha = 0.97f)),
                    border = BorderStroke(1.dp, HairlineBorder),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(CobaltSoft),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = null,
                                    tint = CobaltAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Enable Live GPS & Spotter Radar",
                                    fontFamily = OutfitDisplayFont,
                                    color = InkPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Pin your sightings accurately & see distance to nearby spots",
                                    color = InkMuted,
                                    fontSize = 10.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = {
                                val perms = mutableListOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    perms.add(Manifest.permission.POST_NOTIFICATIONS)
                                }
                                permissionLauncher.launch(perms.toTypedArray())
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CobaltAccent,
                                contentColor = PureWhite
                            ),
                            shape = RoundedCornerShape(999.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("Enable", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Scope filter row (All Sightings / Yours / Others)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SightingScopeFilter.entries.forEach { scope ->
                    val isSelected = scopeFilter == scope
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(if (isSelected) CobaltAccent else PureWhite.copy(alpha = 0.96f))
                            .border(
                                1.dp,
                                if (isSelected) CobaltAccent else HairlineBorder,
                                RoundedCornerShape(999.dp)
                            )
                            .clickable { scopeFilter = scope }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .testTag("map_scope_${scope.name.lowercase()}")
                    ) {
                        Text(
                            text = scope.label,
                            color = if (isSelected) PureWhite else InkPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Rarity filter pills
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val isAll = selectedTierFilter == null
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (isAll) InkPrimary else PureWhite.copy(alpha = 0.95f))
                        .border(1.dp, if (isAll) InkPrimary else HairlineBorder, RoundedCornerShape(999.dp))
                        .clickable { selectedTierFilter = null }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .testTag("map_filter_all")
                ) {
                    Text(
                        text = "All Tiers",
                        color = if (isAll) PureWhite else InkSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                RarityTier.entries.forEach { tier ->
                    val isSelected = selectedTierFilter == tier
                    val tierColor = Color(tier.colorHex)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(if (isSelected) tierColor else PureWhite.copy(alpha = 0.95f))
                            .border(1.dp, if (isSelected) tierColor else HairlineBorder, RoundedCornerShape(999.dp))
                            .clickable { selectedTierFilter = tier }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .testTag("map_filter_${tier.name}")
                    ) {
                        Text(
                            text = tier.label,
                            color = if (isSelected) PureWhite else tierColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // Right-side Street Map Controls (Zoom +, -, Center on GPS, Fit All)
        if (isStreetMapMode) {
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 14.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(PureWhite.copy(alpha = 0.96f))
                    .border(1.dp, HairlineBorder, RoundedCornerShape(16.dp)),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                IconButton(
                    onClick = { zoomLevel = (zoomLevel + 1.0).coerceIn(MIN_MAP_ZOOM, MAX_MAP_ZOOM) },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("map_zoom_in")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Zoom in",
                        tint = InkPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                HorizontalDivider(modifier = Modifier.width(32.dp), thickness = 1.dp, color = HairlineBorder)
                IconButton(
                    onClick = { zoomLevel = (zoomLevel - 1.0).coerceIn(MIN_MAP_ZOOM, MAX_MAP_ZOOM) },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("map_zoom_out")
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Zoom out",
                        tint = InkPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                HorizontalDivider(modifier = Modifier.width(32.dp), thickness = 1.dp, color = HairlineBorder)
                IconButton(
                    onClick = {
                        viewModel.refreshUserLocation()
                        val loc = userLocation
                        if (loc != null) {
                            focusCameraOn(loc.latitude, loc.longitude, maxOf(zoomLevel, 13.5))
                        } else if (!hasLocationPermission) {
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("map_center_me")
                ) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = "Center on my location",
                        tint = if (userLocation != null) CobaltAccent else InkSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                if (mappedSightings.isNotEmpty()) {
                    HorizontalDivider(modifier = Modifier.width(32.dp), thickness = 1.dp, color = HairlineBorder)
                    IconButton(
                        onClick = fitAllSightings,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("map_fit_all")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CenterFocusStrong,
                            contentDescription = "Fit all sightings",
                            tint = InkPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // Bottom Dock: Recent Sightings Quick-Jump Strip + Selected Pin Preview Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (filteredSightings.isEmpty()) {
                EmptyRadarNotice(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    scopeFilter = scopeFilter
                )
            } else {
                RecentSightingsStrip(
                    sightings = filteredSightings.take(25),
                    userLocation = userLocation,
                    selectedId = selectedSightingId,
                    onChipClick = { sighting ->
                        selectedSightingId = sighting.carSpot.id
                        if (hasValidCoordinates(sighting.carSpot)) {
                            focusCameraOn(
                                sighting.carSpot.latitude,
                                sighting.carSpot.longitude,
                                maxOf(zoomLevel, 13.5)
                            )
                        }
                    }
                )
            }

            AnimatedVisibility(
                visible = selectedSighting != null,
                modifier = Modifier.padding(horizontal = 16.dp),
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                val sighting = selectedSighting
                if (sighting != null) {
                    SelectedSightingPreviewCard(
                        sighting = sighting,
                        userLocation = userLocation,
                        onInspect = { viewModel.inspectSighting(sighting) },
                        onOpenGoogleMaps = {
                            val spot = sighting.carSpot
                            val queryLabel = Uri.encode("${spot.make} ${spot.model} (${sighting.spotterName})")
                            val geoIntent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("geo:${spot.latitude},${spot.longitude}?q=${spot.latitude},${spot.longitude}($queryLabel)")
                            )
                            val fallbackIntent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://www.google.com/maps/search/?api=1&query=${spot.latitude},${spot.longitude}")
                            )
                            runCatching { context.startActivity(geoIntent) }
                                .onFailure { runCatching { context.startActivity(fallbackIntent) } }
                        },
                        onClose = { selectedSightingId = null }
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyRadarNotice(
    modifier: Modifier = Modifier,
    scopeFilter: SightingScopeFilter
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhite.copy(alpha = 0.96f)),
        border = BorderStroke(1.dp, HairlineBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(PorcelainSurface)
                    .border(1.dp, HairlineBorder, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.DirectionsCar,
                    contentDescription = null,
                    tint = InkSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = when (scopeFilter) {
                        SightingScopeFilter.ALL -> "No Live Sightings on Radar Yet"
                        SightingScopeFilter.YOURS -> "No Personal Sightings Match Filters"
                        SightingScopeFilter.OTHERS -> "No Other Spotters Nearby Yet"
                    },
                    fontFamily = OutfitDisplayFont,
                    color = InkPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Snap a car from the Spot tab to drop the next live sighting pin.",
                    color = InkMuted,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun RecentSightingsStrip(
    sightings: List<RadarSighting>,
    userLocation: SpotCoordinates?,
    selectedId: String?,
    onChipClick: (RadarSighting) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("radar_recent_sightings_strip")
    ) {
        items(sightings, key = { it.carSpot.id }) { sighting ->
            val spot = sighting.carSpot
            val isSelected = spot.id == selectedId
            val tierColor = Color(spot.rarityTier.colorHex)
            val distText = if (userLocation != null && hasValidCoordinates(spot)) {
                val mi = LocationHelper.distanceMiles(
                    userLocation.latitude,
                    userLocation.longitude,
                    spot.latitude,
                    spot.longitude
                )
                " • ${formatDistanceMiles(mi)}"
            } else {
                ""
            }

            Card(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onChipClick(sighting) }
                    .testTag("radar_strip_item_${spot.id}"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) InkPrimary else PureWhite.copy(alpha = 0.96f)
                ),
                border = BorderStroke(
                    width = if (isSelected) 1.5.dp else 1.dp,
                    color = if (isSelected) InkPrimary else HairlineBorder
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 1.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(tierColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "${spot.make} ${spot.model}",
                            fontFamily = OutfitDisplayFont,
                            color = if (isSelected) PureWhite else InkPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Text(
                            text = "${if (sighting.isOwnSpot) "You" else sighting.spotterHandle} • ${formatSightingAgo(spot.timestamp)}$distText",
                            color = if (isSelected) PureWhite.copy(alpha = 0.78f) else InkMuted,
                            fontSize = 10.sp,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectedSightingPreviewCard(
    sighting: RadarSighting,
    userLocation: SpotCoordinates?,
    onInspect: () -> Unit,
    onOpenGoogleMaps: () -> Unit,
    onClose: () -> Unit
) {
    val spot = sighting.carSpot
    val tierColor = Color(spot.rarityTier.colorHex)
    val hasCoords = hasValidCoordinates(spot)
    val distanceLabel = if (userLocation != null && hasCoords) {
        val mi = LocationHelper.distanceMiles(
            userLocation.latitude,
            userLocation.longitude,
            spot.latitude,
            spot.longitude
        )
        " • ${formatDistanceMiles(mi)} away"
    } else {
        ""
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("map_pin_preview_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        border = BorderStroke(1.dp, HairlineBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(PorcelainSurface)
                        .border(1.dp, HairlineBorder, RoundedCornerShape(14.dp))
                ) {
                    CarSpotImage(
                        car = spot,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(tierColor.copy(alpha = 0.12f))
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = spot.rarityTier.label.uppercase(),
                                color = tierColor,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (sighting.isOwnSpot) CobaltSoft else MistSurface)
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (sighting.isOwnSpot) "YOUR SPOT" else sighting.spotterName.uppercase(),
                                color = if (sighting.isOwnSpot) CobaltAccent else InkSecondary,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                        Text(
                            text = formatSightingAgo(spot.timestamp),
                            color = InkMuted,
                            fontSize = 10.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "${spot.year} ${spot.make} ${spot.model}",
                        fontFamily = OutfitDisplayFont,
                        color = InkPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = "${spot.horsepower} HP • ${spot.topSpeedMph} MPH • +${spot.pointsAwarded} pts",
                        color = InkSecondary,
                        fontSize = 11.sp,
                        maxLines = 1
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = CobaltAccent,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "${spot.locationName}$distanceLabel",
                            color = CobaltAccent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close preview",
                        tint = InkMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onInspect,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = InkPrimary,
                        contentColor = PureWhite
                    ),
                    shape = RoundedCornerShape(999.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("map_inspect_button")
                ) {
                    Text("Inspect Specs", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                if (hasCoords) {
                    OutlinedButton(
                        onClick = onOpenGoogleMaps,
                        shape = RoundedCornerShape(999.dp),
                        border = BorderStroke(1.dp, HairlineBorder),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = PureWhite,
                            contentColor = InkPrimary
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("map_open_google_maps_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = null,
                            tint = CobaltAccent,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Google Maps", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

/**
 * Pure Jetpack Compose interactive Slippy-Tile street map powered by CartoDB Voyager light-mode raster tiles
 * over a crisp #FFFFFF architectural canvas grid, with zero WebView renderer dependencies.
 */
@Composable
private fun InteractiveStreetTileMap(
    sightings: List<RadarSighting>,
    userLocation: SpotCoordinates?,
    selectedSighting: RadarSighting?,
    centerLat: Double,
    centerLng: Double,
    zoomLevel: Double,
    onCameraMoved: (Double, Double, Double) -> Unit,
    onSightingSelected: (RadarSighting) -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current

    val tileZoom = floor(zoomLevel).toInt().coerceIn(MIN_MAP_ZOOM.toInt(), MAX_MAP_ZOOM.toInt())
    val scaleFactor = 2.0.pow(zoomLevel - tileZoom)
    val renderedTileDp = TILE_BASE_DP * scaleFactor
    val n = 1 shl tileZoom

    val currentLat by androidx.compose.runtime.rememberUpdatedState(centerLat)
    val currentLng by androidx.compose.runtime.rememberUpdatedState(centerLng)
    val currentZoom by androidx.compose.runtime.rememberUpdatedState(zoomLevel)

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(PureWhite)
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val nextZoom = if (zoom != 1f) {
                        (currentZoom + (ln(zoom.toDouble()) / ln(2.0))).coerceIn(MIN_MAP_ZOOM, MAX_MAP_ZOOM)
                    } else {
                        currentZoom
                    }
                    val activeTileZoom = floor(nextZoom).toInt().coerceIn(MIN_MAP_ZOOM.toInt(), MAX_MAP_ZOOM.toInt())
                    val activeTileDp = TILE_BASE_DP * 2.0.pow(nextZoom - activeTileZoom)
                    val activeN = (1 shl activeTileZoom).toDouble()
                    val (cx, cy) = latLngToTileCoords(currentLat, currentLng, activeTileZoom)
                    val panXdp = pan.x / density.density
                    val panYdp = pan.y / density.density
                    val newX = cx - (panXdp / activeTileDp)
                    val newY = (cy - (panYdp / activeTileDp)).coerceIn(0.05, activeN - 0.05)
                    val (newLat, newLng) = tileCoordsToLatLng(newX, newY, activeTileZoom)
                    onCameraMoved(newLat, normalizeLongitude(newLng), nextZoom)
                }
            }
            .testTag("interactive_street_tile_map")
    ) {
        val viewportW = maxWidth
        val viewportH = maxHeight
        val (centerTileX, centerTileY) = latLngToTileCoords(centerLat, centerLng, tileZoom)

        // Architectural #FFFFFF canvas grid under street tiles for instant visual continuity while panning/offline
        Canvas(modifier = Modifier.fillMaxSize()) {
            val step = 48.dp.toPx()
            var x = step
            while (x < size.width) {
                drawLine(
                    color = HairlineBorder.copy(alpha = 0.5f),
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = 1.dp.toPx()
                )
                x += step
            }
            var y = step
            while (y < size.height) {
                drawLine(
                    color = HairlineBorder.copy(alpha = 0.5f),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1.dp.toPx()
                )
                y += step
            }
        }

        // Visible street map tiles
        val halfTilesW = (viewportW.value / 2.0) / renderedTileDp
        val halfTilesH = (viewportH.value / 2.0) / renderedTileDp
        val minTx = floor(centerTileX - halfTilesW).toInt()
        val maxTx = ceil(centerTileX + halfTilesW).toInt()
        val minTy = floor(centerTileY - halfTilesH).toInt().coerceIn(0, n - 1)
        val maxTy = ceil(centerTileY + halfTilesH).toInt().coerceIn(0, n - 1)

        for (tx in minTx..maxTx) {
            val wrappedTx = ((tx % n) + n) % n
            for (ty in minTy..maxTy) {
                val screenX = viewportW / 2 + ((tx - centerTileX) * renderedTileDp).dp
                val screenY = viewportH / 2 + ((ty - centerTileY) * renderedTileDp).dp
                val sub = when (abs(wrappedTx + ty) % 3) {
                    0 -> "a"
                    1 -> "b"
                    else -> "c"
                }
                val tileUrl = "https://$sub.basemaps.cartocdn.com/rastertiles/voyager/$tileZoom/$wrappedTx/$ty@2x.png"
                val request = remember(tileUrl) {
                    ImageRequest.Builder(context)
                        .data(tileUrl)
                        .addHeader("User-Agent", "CarSpotter/20.0 (Android)")
                        .crossfade(false)
                        .build()
                }
                AsyncImage(
                    model = request,
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier
                        .offset(x = screenX, y = screenY)
                        .size((renderedTileDp + 1.0).dp)
                )
            }
        }

        // Current User Live GPS Beacon
        if (userLocation != null) {
            val (ux, uy) = latLngToTileCoords(userLocation.latitude, userLocation.longitude, tileZoom)
            val userScreenX = viewportW / 2 + ((ux - centerTileX) * renderedTileDp).dp
            val userScreenY = viewportH / 2 + ((uy - centerTileY) * renderedTileDp).dp
            if (userScreenX in (-40).dp..(viewportW + 40.dp) &&
                userScreenY in (-40).dp..(viewportH + 40.dp)
            ) {
                Box(
                    modifier = Modifier
                        .offset(x = userScreenX - 18.dp, y = userScreenY - 18.dp)
                        .size(36.dp)
                        .testTag("map_user_gps_beacon"),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(CobaltAccent.copy(alpha = 0.18f))
                    )
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(CobaltAccent)
                            .border(2.5.dp, PureWhite, CircleShape)
                    )
                }
            }
        }

        // Sightings pins (draw non-selected first, selected on top)
        val orderedSightings = sightings.sortedBy { it.carSpot.id == selectedSighting?.carSpot?.id }
        val clusterCounters = HashMap<String, Int>()

        orderedSightings.forEach { sighting ->
            val spot = sighting.carSpot
            val bucketKey = "${(spot.latitude * 10_000).toInt()}_${(spot.longitude * 10_000).toInt()}"
            val overlapIndex = clusterCounters.getOrDefault(bucketKey, 0)
            clusterCounters[bucketKey] = overlapIndex + 1

            val (px, py) = latLngToTileCoords(spot.latitude, spot.longitude, tileZoom)
            val jitterAngle = overlapIndex * 1.1
            val jitterRadiusDp = if (overlapIndex == 0) 0.0 else 14.0
            val pinX = viewportW / 2 + ((px - centerTileX) * renderedTileDp + cos(jitterAngle) * jitterRadiusDp).dp
            val pinY = viewportH / 2 + ((py - centerTileY) * renderedTileDp + sin(jitterAngle) * jitterRadiusDp).dp

            if (pinX in (-48).dp..(viewportW + 48.dp) && pinY in (-48).dp..(viewportH + 48.dp)) {
                val isSelected = selectedSighting?.carSpot?.id == spot.id
                val tierColor = Color(spot.rarityTier.colorHex)
                val pinSize = if (isSelected) 44.dp else 36.dp

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .offset(x = pinX - pinSize / 2, y = pinY - pinSize / 2)
                        .clickable { onSightingSelected(sighting) }
                        .testTag("map_pin_${spot.id}")
                ) {
                    Box(
                        modifier = Modifier
                            .size(pinSize)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isSelected -> InkPrimary
                                    sighting.isOwnSpot -> tierColor
                                    else -> PureWhite
                                }
                            )
                            .border(
                                width = if (sighting.isOwnSpot) 2.5.dp else 2.5.dp,
                                color = if (sighting.isOwnSpot) PureWhite else tierColor,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (sighting.isOwnSpot) Icons.Default.DirectionsCar else Icons.Default.Person,
                            contentDescription = "${spot.make} ${spot.model} by ${sighting.spotterName}",
                            tint = if (isSelected || sighting.isOwnSpot) PureWhite else tierColor,
                            modifier = Modifier.size(if (isSelected) 20.dp else 16.dp)
                        )
                    }
                    if (isSelected || zoomLevel >= 13.5) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(PureWhite.copy(alpha = 0.94f))
                                .border(1.dp, HairlineBorder, RoundedCornerShape(6.dp))
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = spot.model,
                                color = InkPrimary,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * True Haversine Geographic Radar plotting sightings around the user's real GPS position
 * (or most recent sighting center) at actual compass bearings and log-scaled mile rings.
 */
@Composable
private fun TacticalRadarMapCanvas(
    sightings: List<RadarSighting>,
    userLocation: SpotCoordinates?,
    selectedSighting: RadarSighting?,
    onSightingClick: (RadarSighting) -> Unit
) {
    val refLat = userLocation?.latitude ?: sightings.firstOrNull()?.carSpot?.latitude ?: DEFAULT_LAT
    val refLng = userLocation?.longitude ?: sightings.firstOrNull()?.carSpot?.longitude ?: DEFAULT_LNG

    val sweepTransition = rememberInfiniteTransition(label = "radar_sweep")
    val sweepAngleDeg by sweepTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweep_angle"
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(PureWhite)
            .testTag("tactical_radar_canvas")
    ) {
        val radarMaxRadiusDp = minOf(maxWidth, maxHeight) * 0.42f

        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val maxRadius = minOf(size.width, size.height) * 0.42f

            // Animated sweep line
            val sweepRad = Math.toRadians((sweepAngleDeg - 90f).toDouble())
            drawLine(
                brush = Brush.linearGradient(
                    colors = listOf(CobaltAccent.copy(alpha = 0.35f), CobaltAccent.copy(alpha = 0.04f)),
                    start = center,
                    end = Offset(
                        x = center.x + (cos(sweepRad) * maxRadius).toFloat(),
                        y = center.y + (sin(sweepRad) * maxRadius).toFloat()
                    )
                ),
                start = center,
                end = Offset(
                    x = center.x + (cos(sweepRad) * maxRadius).toFloat(),
                    y = center.y + (sin(sweepRad) * maxRadius).toFloat()
                ),
                strokeWidth = 2.5.dp.toPx()
            )

            // Concentric distance rings
            val ringCount = 3
            for (i in 1..ringCount) {
                val radius = (maxRadius / ringCount) * i
                drawCircle(
                    color = if (i == ringCount) HairlineStrong else HairlineBorder,
                    radius = radius,
                    center = center,
                    style = Stroke(width = 1.2.dp.toPx())
                )
            }

            // Crosshairs
            drawLine(
                color = HairlineBorder,
                start = Offset(center.x, center.y - maxRadius),
                end = Offset(center.x, center.y + maxRadius),
                strokeWidth = 1.dp.toPx()
            )
            drawLine(
                color = HairlineBorder,
                start = Offset(center.x - maxRadius, center.y),
                end = Offset(center.x + maxRadius, center.y),
                strokeWidth = 1.dp.toPx()
            )

            // Center User GPS pulse
            drawCircle(
                color = CobaltAccent.copy(alpha = 0.16f),
                radius = 18.dp.toPx(),
                center = center
            )
            drawCircle(
                color = CobaltAccent,
                radius = 6.dp.toPx(),
                center = center
            )
        }

        // Compass & ring labels
        Text(
            text = "N",
            color = InkMuted,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = -radarMaxRadiusDp - 10.dp)
        )
        Text(
            text = "5 mi",
            color = InkMuted,
            fontSize = 9.sp,
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = -(radarMaxRadiusDp * 0.34f) + 6.dp)
        )
        Text(
            text = "25 mi",
            color = InkMuted,
            fontSize = 9.sp,
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = -(radarMaxRadiusDp * 0.67f) + 6.dp)
        )
        Text(
            text = "100+ mi",
            color = InkMuted,
            fontSize = 9.sp,
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = -radarMaxRadiusDp + 8.dp)
        )

        // Sightings positioned using true compass bearing + scaled Haversine distance
        val ordered = sightings.sortedBy { it.carSpot.id == selectedSighting?.carSpot?.id }
        ordered.forEachIndexed { index, sighting ->
            val spot = sighting.carSpot
            val distMiles = LocationHelper.distanceMiles(refLat, refLng, spot.latitude, spot.longitude)
            val normalizedR = if (distMiles < 0.05) {
                // Distinct inner ring offsets for sightings right next to center
                0.14f + (index % 4) * 0.03f
            } else {
                (ln(1.0 + distMiles) / ln(1.0 + 150.0)).toFloat().coerceIn(0.18f, 0.94f)
            }
            val bearing = if (distMiles < 0.05) {
                (index * 65.0) * (PI / 180.0)
            } else {
                LocationHelper.bearingRadians(refLat, refLng, spot.latitude, spot.longitude)
            }

            // bearingRadians: 0 = North (up = negative Y), PI/2 = East (right = positive X)
            val offsetX = (sin(bearing) * radarMaxRadiusDp.value * normalizedR).dp
            val offsetY = (-cos(bearing) * radarMaxRadiusDp.value * normalizedR).dp

            val isSelected = selectedSighting?.carSpot?.id == spot.id
            val tierColor = Color(spot.rarityTier.colorHex)

            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(x = offsetX, y = offsetY)
                    .size(if (isSelected) 40.dp else 32.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isSelected -> InkPrimary
                            sighting.isOwnSpot -> tierColor
                            else -> PureWhite
                        }
                    )
                    .border(
                        width = 2.dp,
                        color = if (sighting.isOwnSpot) PureWhite else tierColor,
                        shape = CircleShape
                    )
                    .clickable { onSightingClick(sighting) }
                    .testTag("radar_spot_${spot.id}"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (sighting.isOwnSpot) Icons.Default.DirectionsCar else Icons.Default.Person,
                    contentDescription = "${spot.make} ${spot.model}",
                    tint = if (isSelected || sighting.isOwnSpot) PureWhite else tierColor,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

private fun hasValidCoordinates(spot: CarSpot): Boolean {
    return (spot.latitude != 0.0 || spot.longitude != 0.0) &&
        spot.latitude in -90.0..90.0 &&
        spot.longitude in -180.0..180.0
}

private fun latLngToTileCoords(lat: Double, lng: Double, zoom: Int): Pair<Double, Double> {
    val clampedLat = lat.coerceIn(-85.05112878, 85.05112878)
    val latRad = Math.toRadians(clampedLat)
    val n = (1 shl zoom).toDouble()
    val x = ((lng + 180.0) / 360.0) * n
    val y = (1.0 - ln(tan(latRad) + 1.0 / cos(latRad)) / PI) / 2.0 * n
    return x to y
}

private fun tileCoordsToLatLng(x: Double, y: Double, zoom: Int): Pair<Double, Double> {
    val n = (1 shl zoom).toDouble()
    val lng = (x / n) * 360.0 - 180.0
    val latRad = atan(sinh(PI * (1.0 - 2.0 * y / n)))
    val lat = Math.toDegrees(latRad).coerceIn(-85.0, 85.0)
    return lat to lng
}

private fun normalizeLongitude(lng: Double): Double {
    var result = (lng + 180.0) % 360.0
    if (result < 0.0) result += 360.0
    return result - 180.0
}

private fun formatDistanceMiles(miles: Double): String {
    return when {
        miles < 0.2 -> "Nearby"
        miles < 10.0 -> String.format(Locale.US, "%.1f mi", miles)
        else -> String.format(Locale.US, "%.0f mi", miles)
    }
}

private fun formatSightingAgo(timestampMs: Long): String {
    if (timestampMs <= 0L) return "Recent"
    val diff = (System.currentTimeMillis() - timestampMs).coerceAtLeast(0L)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
    val hours = TimeUnit.MILLISECONDS.toHours(diff)
    val days = TimeUnit.MILLISECONDS.toDays(diff)
    return when {
        minutes < 1 -> "Just now"
        minutes < 60 -> "${minutes}m ago"
        hours < 24 -> "${hours}h ago"
        days < 7 -> "${days}d ago"
        else -> "${days / 7}w ago"
    }
}
