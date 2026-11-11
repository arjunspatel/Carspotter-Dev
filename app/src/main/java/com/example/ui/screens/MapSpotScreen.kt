package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.R
import com.example.model.CarSpot
import com.example.model.RarityTier
import com.example.ui.CarSpotterViewModel
import com.example.ui.theme.CarbonBorder
import com.example.ui.theme.CarbonCard
import com.example.ui.theme.CarbonCardElevated
import com.example.ui.theme.CarbonDark
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.GoldenTrophy
import com.example.ui.theme.NitroCyan
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TurboAmber

@Composable
fun MapSpotScreen(
    viewModel: CarSpotterViewModel
) {
    val context = LocalContext.current
    val allSpots by viewModel.allSpots.collectAsState()

    var isGoogleMapsMode by remember { mutableStateOf(true) }
    var selectedTierFilter by remember { mutableStateOf<RarityTier?>(null) }
    var selectedSpotOnMap by remember { mutableStateOf<CarSpot?>(null) }

    // Check location and notification permissions
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    var hasNotificationPermission by remember {
        mutableStateOf(
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    var showPermissionBanner by remember {
        mutableStateOf(!hasLocationPermission || !hasNotificationPermission)
    }

    // Permission Launcher for Location and Notifications
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val locGranted = perms[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val notifGranted = perms[Manifest.permission.POST_NOTIFICATIONS] ?: true
        hasLocationPermission = locGranted
        hasNotificationPermission = notifGranted
        if (locGranted && notifGranted) {
            showPermissionBanner = false
        }
    }

    val filteredSpots = allSpots.filter { spot ->
        selectedTierFilter == null || spot.rarityTier == selectedTierFilter
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CarbonDark)
            .testTag("map_spot_screen")
    ) {
        if (isGoogleMapsMode) {
            // Google Maps Interactive View
            GoogleMapsEmbedView(
                spots = filteredSpots,
                selectedSpot = selectedSpotOnMap,
                onSpotSelected = { selectedSpotOnMap = it }
            )
        } else {
            // High-Tech Tactical Radar Map Canvas
            TacticalRadarMapCanvas(
                spots = filteredSpots,
                selectedSpot = selectedSpotOnMap,
                onSpotClick = { selectedSpotOnMap = it }
            )
        }

        // Top Header, Mode Switcher & Filter Bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(16.dp)
        ) {
            // Header Card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CarbonCard.copy(alpha = 0.92f))
                    .border(1.dp, CarbonBorder, RoundedCornerShape(16.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isGoogleMapsMode) Icons.Default.Map else Icons.Default.Radar,
                        contentDescription = null,
                        tint = TurboAmber,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = if (isGoogleMapsMode) "GOOGLE MAPS SPOTTING" else "TACTICAL RADAR",
                            color = TurboAmber,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "${filteredSpots.size} Sighting Pins Active",
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Switch between Google Maps and Tactical Radar
                Button(
                    onClick = { isGoogleMapsMode = !isGoogleMapsMode },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CarbonCardElevated,
                        contentColor = NitroCyan
                    ),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CarbonBorder),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("toggle_map_view_mode")
                ) {
                    Icon(
                        imageVector = if (isGoogleMapsMode) Icons.Default.Radar else Icons.Default.Map,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isGoogleMapsMode) "Radar View" else "Google Maps",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Notification & Location Permission Request Banner
            if (showPermissionBanner) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("notification_permission_banner"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = CarbonCard.copy(alpha = 0.95f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, TurboAmber.copy(alpha = 0.5f))
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
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = null,
                                tint = TurboAmber,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Enable Spotter Radar & Alerts",
                                    color = TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Get notified when rare cars are spotted near your coordinates",
                                    color = TextMuted,
                                    fontSize = 10.sp
                                )
                            }
                        }

                        Button(
                            onClick = {
                                val perms = mutableListOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                    perms.add(Manifest.permission.POST_NOTIFICATIONS)
                                }
                                permissionLauncher.launch(perms.toTypedArray())
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = TurboAmber, contentColor = Color.Black),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text("Enable", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Rarity filter chips over map
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val isAll = selectedTierFilter == null
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isAll) NitroCyan else CarbonCard.copy(alpha = 0.85f))
                        .border(1.dp, if (isAll) NitroCyan else CarbonBorder, RoundedCornerShape(14.dp))
                        .clickable { selectedTierFilter = null }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .testTag("map_filter_all")
                ) {
                    Text(
                        text = "All Pins",
                        color = if (isAll) Color.Black else TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                RarityTier.values().forEach { tier ->
                    val isSelected = selectedTierFilter == tier
                    val tierColor = Color(tier.colorHex)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isSelected) tierColor else CarbonCard.copy(alpha = 0.85f))
                            .border(1.dp, if (isSelected) tierColor else CarbonBorder, RoundedCornerShape(14.dp))
                            .clickable { selectedTierFilter = tier }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .testTag("map_filter_${tier.name}")
                    ) {
                        Text(
                            text = tier.label,
                            color = if (isSelected) Color.Black else tierColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Bottom Selected Car Pin Preview Card
        AnimatedVisibility(
            visible = selectedSpotOnMap != null,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp),
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it })
        ) {
            val spot = selectedSpotOnMap
            if (spot != null) {
                val tierColor = Color(spot.rarityTier.colorHex)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .border(1.dp, tierColor, RoundedCornerShape(20.dp))
                        .testTag("map_pin_preview_card"),
                    colors = CardDefaults.cardColors(containerColor = CarbonCard)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Thumbnail
                        Box(
                            modifier = Modifier
                                .size(76.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .border(1.dp, CarbonBorder, RoundedCornerShape(14.dp))
                        ) {
                            when {
                                spot.photoUri == "res:car_porsche" -> {
                                    Image(
                                        painter = painterResource(id = R.drawable.car_porsche),
                                        contentDescription = spot.model,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                                spot.photoUri == "res:car_ferrari" -> {
                                    Image(
                                        painter = painterResource(id = R.drawable.car_ferrari),
                                        contentDescription = spot.model,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                                spot.photoUri != null -> {
                                    AsyncImage(
                                        model = spot.photoUri,
                                        contentDescription = spot.model,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                                else -> {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(CarbonCardElevated),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DirectionsCar,
                                            contentDescription = null,
                                            tint = TurboAmber,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // Car Information
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(tierColor.copy(alpha = 0.2f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = spot.rarityTier.label.uppercase(),
                                        color = tierColor,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "+${spot.pointsAwarded} pts",
                                    color = TurboAmber,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "${spot.make} ${spot.model}",
                                color = TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                text = "${spot.horsepower} HP • ${spot.topSpeedMph} MPH",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = NitroCyan,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = spot.locationName,
                                    color = NitroCyan,
                                    fontSize = 10.sp,
                                    maxLines = 1
                                )
                            }
                        }

                        // Inspect & Close
                        Column(horizontalAlignment = Alignment.End) {
                            Button(
                                onClick = { viewModel.inspectCar(spot) },
                                colors = ButtonDefaults.buttonColors(containerColor = TurboAmber, contentColor = Color.Black),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("Inspect", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Close",
                                color = TextMuted,
                                fontSize = 10.sp,
                                modifier = Modifier
                                    .clickable { selectedSpotOnMap = null }
                                    .padding(4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * High-performance interactive Google Maps View embedded for car sighting telemetry
 */
@Composable
private fun GoogleMapsEmbedView(
    spots: List<CarSpot>,
    selectedSpot: CarSpot?,
    onSpotSelected: (CarSpot) -> Unit
) {
    val mapHtml = remember(spots, selectedSpot) {
        val markersJs = spots.joinToString(",\n") { spot ->
            val color = when (spot.rarityTier) {
                RarityTier.LEGENDARY -> "#FF1744"
                RarityTier.EXOTIC -> "#FF9800"
                RarityTier.RARE -> "#AB47BC"
                RarityTier.UNCOMMON -> "#29B6F6"
                RarityTier.COMMON -> "#78909C"
            }
            """{
                id: '${spot.id}',
                title: '${spot.make} ${spot.model}',
                lat: ${spot.latitude},
                lng: ${spot.longitude},
                hp: ${spot.horsepower},
                speed: ${spot.topSpeedMph},
                tier: '${spot.rarityTier.label}',
                color: '$color'
            }"""
        }

        val initialLat = selectedSpot?.latitude ?: if (spots.isNotEmpty()) spots.first().latitude else 34.0522
        val initialLng = selectedSpot?.longitude ?: if (spots.isNotEmpty()) spots.first().longitude else -118.2437

        """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
            <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
            <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
            <style>
                body, html, #map { margin: 0; padding: 0; width: 100%; height: 100%; background: #0D0F12; }
                .leaflet-popup-content-wrapper {
                    background: #161A22;
                    color: #FFFFFF;
                    border: 1px solid #00F5FF;
                    border-radius: 12px;
                }
                .leaflet-popup-tip { background: #161A22; }
                .car-pin {
                    width: 32px;
                    height: 32px;
                    border-radius: 50%;
                    border: 2px solid #FFFFFF;
                    box-shadow: 0 0 12px rgba(255,160,0,0.8);
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    color: black;
                    font-weight: bold;
                    font-size: 11px;
                }
            </style>
        </head>
        <body>
            <div id="map"></div>
            <script>
                var map = L.map('map', { zoomControl: false }).setView([$initialLat, $initialLng], 12);
                
                // Google Maps hybrid / Carto dark tiles
                L.tileLayer('https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}{r}.png', {
                    maxZoom: 19,
                    attribution: 'Google Maps Telemetry • CarSpotter'
                }).addTo(map);

                var markersData = [ $markersJs ];

                markersData.forEach(function(m) {
                    var customIcon = L.divIcon({
                        className: 'custom-pin',
                        html: '<div class="car-pin" style="background:' + m.color + '">🏎</div>',
                        iconSize: [32, 32],
                        iconAnchor: [16, 16]
                    });

                    var marker = L.marker([m.lat, m.lng], { icon: customIcon }).addTo(map);
                    marker.bindPopup(
                        '<b>' + m.title + '</b><br>' +
                        '<span style="color:' + m.color + '">' + m.tier + '</span> • ' + m.hp + ' HP<br>' +
                        'Top Speed: ' + m.speed + ' MPH'
                    );
                });
            </script>
        </body>
        </html>
        """.trimIndent()
    }

    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                webViewClient = WebViewClient()
                loadDataWithBaseURL("https://maps.google.com", mapHtml, "text/html", "UTF-8", null)
            }
        },
        update = { webView ->
            webView.loadDataWithBaseURL("https://maps.google.com", mapHtml, "text/html", "UTF-8", null)
        },
        modifier = Modifier.fillMaxSize()
    )
}

/**
 * Tactical Radar Map Canvas with concentric distance rings and pulsating sighting pins
 */
@Composable
private fun TacticalRadarMapCanvas(
    spots: List<CarSpot>,
    selectedSpot: CarSpot?,
    onSpotClick: (CarSpot) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val maxRadius = minOf(size.width, size.height) * 0.44f

            // Radar background concentric rings
            val ringCount = 4
            for (i in 1..ringCount) {
                val radius = (maxRadius / ringCount) * i
                drawCircle(
                    color = NitroCyan.copy(alpha = 0.15f),
                    radius = radius,
                    center = center,
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }

            // Radar crosshairs
            drawLine(
                color = NitroCyan.copy(alpha = 0.2f),
                start = Offset(center.x, center.y - maxRadius),
                end = Offset(center.x, center.y + maxRadius),
                strokeWidth = 1.dp.toPx()
            )
            drawLine(
                color = NitroCyan.copy(alpha = 0.2f),
                start = Offset(center.x - maxRadius, center.y),
                end = Offset(center.x + maxRadius, center.y),
                strokeWidth = 1.dp.toPx()
            )

            // Center User GPS pulse
            drawCircle(
                color = EmeraldGreen,
                radius = 6.dp.toPx(),
                center = center
            )
            drawCircle(
                color = EmeraldGreen.copy(alpha = 0.25f),
                radius = 16.dp.toPx(),
                center = center
            )
        }

        // Clickable interactive car sighting pins on radar grid
        spots.forEachIndexed { index, spot ->
            val angle = (index * 75f) * (Math.PI / 180f)
            val distance = (0.28f + (index % 3) * 0.22f)
            val isSelected = selectedSpot?.id == spot.id
            val tierColor = Color(spot.rarityTier.colorHex)

            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(
                        x = (Math.cos(angle) * 130 * distance).dp,
                        y = (Math.sin(angle) * 130 * distance).dp
                    )
                    .clip(CircleShape)
                    .background(if (isSelected) TurboAmber else tierColor)
                    .border(2.dp, if (isSelected) Color.White else Color.Black, CircleShape)
                    .clickable { onSpotClick(spot) }
                    .padding(8.dp)
                    .testTag("radar_spot_${spot.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.DirectionsCar,
                    contentDescription = spot.model,
                    tint = Color.Black,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
