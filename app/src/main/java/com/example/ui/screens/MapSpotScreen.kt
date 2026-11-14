package com.example.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.model.CarSpot
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
import com.example.ui.theme.WarmGold

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
            .background(PureWhite)
            .testTag("map_spot_screen")
    ) {
        if (isGoogleMapsMode) {
            GoogleMapsEmbedView(
                spots = filteredSpots,
                selectedSpot = selectedSpotOnMap,
                onSpotSelected = { selectedSpotOnMap = it }
            )
        } else {
            TacticalRadarMapCanvas(
                spots = filteredSpots,
                selectedSpot = selectedSpotOnMap,
                onSpotClick = { selectedSpotOnMap = it }
            )
        }

        // Top Floating Website Header, Mode Switcher & Filter Bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(16.dp)
        ) {
            // Crisp Pure-White Floating Header Card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(PureWhite.copy(alpha = 0.96f))
                    .border(1.dp, HairlineBorder, RoundedCornerShape(18.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(CobaltSoft),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isGoogleMapsMode) Icons.Default.Map else Icons.Default.Radar,
                            contentDescription = null,
                            tint = CobaltAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (isGoogleMapsMode) "SPOTTER MAP" else "RADAR GRID",
                            color = CobaltAccent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        )
                        Text(
                            text = "${filteredSpots.size} Sighting Pins Active",
                            fontFamily = OutfitDisplayFont,
                            color = InkPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Mode Switcher Pill
                Button(
                    onClick = { isGoogleMapsMode = !isGoogleMapsMode },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = InkPrimary,
                        contentColor = PureWhite
                    ),
                    shape = RoundedCornerShape(999.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("toggle_map_view_mode")
                ) {
                    Icon(
                        imageVector = if (isGoogleMapsMode) Icons.Default.Radar else Icons.Default.Map,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = if (isGoogleMapsMode) "Radar View" else "Map View",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Notification & Location Permission Request Banner
            if (showPermissionBanner) {
                Spacer(modifier = Modifier.height(10.dp))
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
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(CobaltSoft),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = null,
                                    tint = CobaltAccent,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Enable Spotter Radar & Alerts",
                                    fontFamily = OutfitDisplayFont,
                                    color = InkPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Get notified when rare cars are spotted near you",
                                    color = InkMuted,
                                    fontSize = 11.sp
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
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                    perms.add(Manifest.permission.POST_NOTIFICATIONS)
                                }
                                permissionLauncher.launch(perms.toTypedArray())
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CobaltAccent,
                                contentColor = PureWhite
                            ),
                            shape = RoundedCornerShape(999.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text("Enable", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Rarity filter pills over map
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val isAll = selectedTierFilter == null
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (isAll) InkPrimary else PureWhite.copy(alpha = 0.95f))
                        .border(1.dp, if (isAll) InkPrimary else HairlineBorder, RoundedCornerShape(999.dp))
                        .clickable { selectedTierFilter = null }
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                        .testTag("map_filter_all")
                ) {
                    Text(
                        text = "All Pins",
                        color = if (isAll) PureWhite else InkSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                RarityTier.values().forEach { tier ->
                    val isSelected = selectedTierFilter == tier
                    val tierColor = Color(tier.colorHex)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(if (isSelected) tierColor else PureWhite.copy(alpha = 0.95f))
                            .border(1.dp, if (isSelected) tierColor else HairlineBorder, RoundedCornerShape(999.dp))
                            .clickable { selectedTierFilter = tier }
                            .padding(horizontal = 14.dp, vertical = 7.dp)
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
                        .testTag("map_pin_preview_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = PureWhite),
                    border = BorderStroke(1.dp, HairlineBorder),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(76.dp)
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
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
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "+${spot.pointsAwarded} pts",
                                    color = WarmGold,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "${spot.make} ${spot.model}",
                                fontFamily = OutfitDisplayFont,
                                color = InkPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                text = "${spot.horsepower} HP • ${spot.topSpeedMph} MPH",
                                color = InkSecondary,
                                fontSize = 11.sp
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
                                    text = spot.locationName,
                                    color = CobaltAccent,
                                    fontSize = 10.sp,
                                    maxLines = 1
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Button(
                                onClick = { viewModel.inspectCar(spot) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = InkPrimary,
                                    contentColor = PureWhite
                                ),
                                shape = RoundedCornerShape(999.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text("Inspect", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Close",
                                color = InkMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
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
 * Clean minimal light-mode Leaflet map view styled to harmonize with the pure-white editorial UI,
 * hardened against Android WebView renderer crashes with automatic fallback to a native vector map.
 */
@SuppressLint("SetJavaScriptEnabled", "JavascriptInterface")
@Composable
private fun GoogleMapsEmbedView(
    spots: List<CarSpot>,
    selectedSpot: CarSpot?,
    onSpotSelected: (CarSpot) -> Unit
) {
    val context = LocalContext.current
    var useNativeFallback by remember { mutableStateOf(false) }

    val mapHtml = remember(spots) {
        val markersJs = spots.joinToString(",\n") { spot ->
            val color = when (spot.rarityTier) {
                RarityTier.LEGENDARY -> "#DC2626"
                RarityTier.EXOTIC -> "#D97706"
                RarityTier.RARE -> "#4F46E5"
                RarityTier.UNCOMMON -> "#0284C7"
                RarityTier.COMMON -> "#64748B"
            }
            val safeId = spot.id.replace("'", "\\'")
            val safeTitle = "${spot.make} ${spot.model}".replace("'", "\\'")
            val safeTier = spot.rarityTier.label.replace("'", "\\'")
            """{
                id: '$safeId',
                title: '$safeTitle',
                lat: ${spot.latitude},
                lng: ${spot.longitude},
                hp: ${spot.horsepower},
                speed: ${spot.topSpeedMph},
                tier: '$safeTier',
                color: '$color'
            }"""
        }

        val initialLat = if (spots.isNotEmpty()) spots.first().latitude else 34.0522
        val initialLng = if (spots.isNotEmpty()) spots.first().longitude else -118.2437

        """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
            <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
            <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
            <style>
                body, html, #map { margin: 0; padding: 0; width: 100%; height: 100%; background: #FFFFFF; font-family: sans-serif; }
                .leaflet-popup-content-wrapper {
                    background: #FFFFFF;
                    color: #0F172A;
                    border: 1px solid #E2E8F0;
                    border-radius: 12px;
                }
                .leaflet-popup-tip { background: #FFFFFF; }
            </style>
        </head>
        <body>
            <div id="map"></div>
            <script>
                (function() {
                    if (typeof L === 'undefined') { return; }
                    var map = L.map('map', { zoomControl: false, preferCanvas: true }).setView([$initialLat, $initialLng], 12);

                    L.tileLayer('https://{s}.basemaps.cartocdn.com/light_all/{z}/{x}/{y}{r}.png', {
                        maxZoom: 19,
                        attribution: 'CarSpotter Atlas'
                    }).addTo(map);

                    var markersData = [ $markersJs ];
                    var markersById = {};

                    markersData.forEach(function(m) {
                        var marker = L.circleMarker([m.lat, m.lng], {
                            radius: 10,
                            fillColor: m.color,
                            color: '#FFFFFF',
                            weight: 2.5,
                            opacity: 1,
                            fillOpacity: 0.95
                        }).addTo(map);

                        marker.bindPopup(
                            '<b style="color:#0F172A">' + m.title + '</b><br>' +
                            '<span style="color:' + m.color + ';font-weight:bold">' + m.tier + '</span> • ' + m.hp + ' HP<br>' +
                            '<span style="color:#475569">Top Speed: ' + m.speed + ' MPH</span>'
                        );

                        marker.on('click', function() {
                            if (window.CarSpotBridge && window.CarSpotBridge.selectSpot) {
                                window.CarSpotBridge.selectSpot(m.id);
                            }
                        });

                        markersById[m.id] = marker;
                    });

                    window.focusSpot = function(spotId) {
                        var target = markersById[spotId];
                        if (target) {
                            map.panTo(target.getLatLng());
                            target.openPopup();
                        }
                    };
                })();
            </script>
        </body>
        </html>
        """.trimIndent()
    }

    if (useNativeFallback) {
        NativeVectorMapFallback(
            spots = spots,
            selectedSpot = selectedSpot,
            onSpotSelected = onSpotSelected
        )
        return
    }

    val currentSpots by androidx.compose.runtime.rememberUpdatedState(spots)
    val currentOnSpotSelected by androidx.compose.runtime.rememberUpdatedState(onSpotSelected)

    val bridge = remember(context) {
        object {
            @JavascriptInterface
            fun selectSpot(spotId: String) {
                ContextCompat.getMainExecutor(context).execute {
                    currentSpots.firstOrNull { it.id == spotId }?.let { currentOnSpotSelected(it) }
                }
            }
        }
    }

    val webViewCreationFailed = remember { mutableStateOf(false) }
    if (webViewCreationFailed.value) {
        NativeVectorMapFallback(
            spots = spots,
            selectedSpot = selectedSpot,
            onSpotSelected = onSpotSelected
        )
        return
    }

    AndroidView(
        factory = { ctx ->
            try {
                WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    setBackgroundColor(android.graphics.Color.WHITE)
                    // Software layer prevents Chromium GPU/SwiftShader renderer crashes in emulators
                    setLayerType(View.LAYER_TYPE_SOFTWARE, null)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        setRendererPriorityPolicy(WebView.RENDERER_PRIORITY_IMPORTANT, false)
                    }
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.cacheMode = WebSettings.LOAD_DEFAULT
                    settings.useWideViewPort = true
                    settings.loadWithOverviewMode = true
                    addJavascriptInterface(bridge, "CarSpotBridge")

                    webViewClient = object : WebViewClient() {
                        override fun onRenderProcessGone(
                            view: WebView?,
                            detail: RenderProcessGoneDetail?
                        ): Boolean {
                            Log.w(
                                "MapSpotScreen",
                                "WebView renderer gone (didCrash=${detail?.didCrash()}); switching to native map fallback"
                            )
                            (view?.parent as? ViewGroup)?.removeView(view)
                            try {
                                view?.destroy()
                            } catch (_: Exception) {
                            }
                            useNativeFallback = true
                            return true
                        }

                        override fun onReceivedError(
                            view: WebView?,
                            request: WebResourceRequest?,
                            error: WebResourceError?
                        ) {
                            if (request?.isForMainFrame == true) {
                                Log.w("MapSpotScreen", "Map main-frame error: ${error?.description}")
                            }
                        }
                    }

                    tag = mapHtml
                    loadDataWithBaseURL("https://carspotter.local/", mapHtml, "text/html", "UTF-8", null)
                }
            } catch (t: Throwable) {
                Log.e("MapSpotScreen", "Failed to create WebView, using native vector map fallback", t)
                webViewCreationFailed.value = true
                View(ctx).apply { setBackgroundColor(android.graphics.Color.WHITE) }
            }
        },
        update = { view ->
            val webView = view as? WebView ?: return@AndroidView
            if (webView.tag != mapHtml) {
                webView.tag = mapHtml
                webView.loadDataWithBaseURL("https://carspotter.local/", mapHtml, "text/html", "UTF-8", null)
            } else if (selectedSpot != null) {
                val safeId = selectedSpot.id.replace("'", "")
                webView.evaluateJavascript("if (window.focusSpot) { window.focusSpot('$safeId'); }", null)
            }
        },
        onRelease = { view ->
            (view.parent as? ViewGroup)?.removeView(view)
            val webView = view as? WebView ?: return@AndroidView
            try {
                webView.stopLoading()
                webView.clearHistory()
                webView.removeJavascriptInterface("CarSpotBridge")
                webView.destroy()
            } catch (_: Exception) {
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

/**
 * Pure-white editorial native Compose vector street map fallback (used seamlessly if WebView renderer is unavailable)
 */
@Composable
private fun NativeVectorMapFallback(
    spots: List<CarSpot>,
    selectedSpot: CarSpot?,
    onSpotSelected: (CarSpot) -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(PureWhite)
    ) {
        val widthDp = maxWidth
        val heightDp = maxHeight

        Canvas(modifier = Modifier.fillMaxSize()) {
            // Subtle architectural city grid
            val step = 44.dp.toPx()
            var x = step
            while (x < size.width) {
                drawLine(
                    color = HairlineBorder.copy(alpha = 0.7f),
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = 1.dp.toPx()
                )
                x += step
            }
            var y = step
            while (y < size.height) {
                drawLine(
                    color = HairlineBorder.copy(alpha = 0.7f),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1.dp.toPx()
                )
                y += step
            }

            // Highlighted arterial boulevard lines
            drawLine(
                color = HairlineStrong,
                start = Offset(0f, size.height * 0.42f),
                end = Offset(size.width, size.height * 0.56f),
                strokeWidth = 3.dp.toPx()
            )
            drawLine(
                color = HairlineStrong,
                start = Offset(size.width * 0.38f, 0f),
                end = Offset(size.width * 0.58f, size.height),
                strokeWidth = 3.dp.toPx()
            )
        }

        val minLat = spots.minOfOrNull { it.latitude } ?: 34.0
        val maxLat = spots.maxOfOrNull { it.latitude } ?: 34.1
        val minLng = spots.minOfOrNull { it.longitude } ?: -118.3
        val maxLng = spots.maxOfOrNull { it.longitude } ?: -118.2
        val latSpan = (maxLat - minLat).coerceAtLeast(0.02)
        val lngSpan = (maxLng - minLng).coerceAtLeast(0.02)

        spots.forEachIndexed { index, spot ->
            val normX = if (spots.size <= 1) 0.5f else (((spot.longitude - minLng) / lngSpan).toFloat()).coerceIn(0.12f, 0.88f)
            val normY = if (spots.size <= 1) 0.52f else (1f - ((spot.latitude - minLat) / latSpan).toFloat()).coerceIn(0.28f, 0.78f)
            val isSelected = selectedSpot?.id == spot.id
            val tierColor = Color(spot.rarityTier.colorHex)

            Box(
                modifier = Modifier
                    .offset(
                        x = widthDp * normX - 18.dp,
                        y = heightDp * normY - 18.dp
                    )
                    .size(if (isSelected) 40.dp else 34.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) InkPrimary else tierColor)
                    .border(2.5.dp, PureWhite, CircleShape)
                    .clickable { onSpotSelected(spot) }
                    .testTag("vector_map_pin_${spot.id}_$index"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.DirectionsCar,
                    contentDescription = "${spot.make} ${spot.model}",
                    tint = PureWhite,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/**
 * Clean Pure-White Radar Map Canvas with hairline distance rings and Cobalt GPS marker
 */
@Composable
private fun TacticalRadarMapCanvas(
    spots: List<CarSpot>,
    selectedSpot: CarSpot?,
    onSpotClick: (CarSpot) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PureWhite)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val maxRadius = minOf(size.width, size.height) * 0.44f

            // Subtle architectural concentric distance rings
            val ringCount = 4
            for (i in 1..ringCount) {
                val radius = (maxRadius / ringCount) * i
                drawCircle(
                    color = if (i % 2 == 0) HairlineStrong else HairlineBorder,
                    radius = radius,
                    center = center,
                    style = Stroke(width = 1.2.dp.toPx())
                )
            }

            // Hairline radar crosshairs
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

            // Center User GPS pulse (Cobalt Blue, non-neon)
            drawCircle(
                color = CobaltAccent.copy(alpha = 0.14f),
                radius = 18.dp.toPx(),
                center = center
            )
            drawCircle(
                color = CobaltAccent,
                radius = 6.dp.toPx(),
                center = center
            )
        }

        // Clickable car sighting pins on radar grid
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
                    .background(if (isSelected) InkPrimary else tierColor)
                    .border(2.dp, PureWhite, CircleShape)
                    .clickable { onSpotClick(spot) }
                    .padding(8.dp)
                    .testTag("radar_spot_${spot.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.DirectionsCar,
                    contentDescription = spot.model,
                    tint = PureWhite,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
