package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.CarSpotterViewModel
import com.example.ui.theme.ApexCrimson
import com.example.ui.theme.CarbonBorder
import com.example.ui.theme.CarbonCard
import com.example.ui.theme.CarbonDark
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.GoldenTrophy
import com.example.ui.theme.NitroCyan
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TurboAmber

@Composable
fun CameraSpotScreen(
    viewModel: CarSpotterViewModel
) {
    val isAnalyzing by viewModel.isAnalyzing.collectAsState()
    val scanningText by viewModel.scanningStepText.collectAsState()
    val antiCheatMode by viewModel.antiCheatMode.collectAsState()

    var showAntiCheatMenu by remember { mutableStateOf(false) }

    // Photo picker launcher (for custom camera roll / car pictures)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.snapCar(photoUri = uri.toString())
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CarbonDark)
            .testTag("camera_spot_screen")
    ) {
        // Camera Viewfinder Canvas with automotive HUD
        CameraViewfinderHUD(isAnalyzing = isAnalyzing)

        // Top Status Bar: Anti-Cheat Shield & Flash
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Anti-Cheat status badge with test switcher
                Box {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (antiCheatMode == "AUTHENTIC") CarbonCard else ApexCrimson.copy(alpha = 0.2f)
                            )
                            .border(
                                1.dp,
                                if (antiCheatMode == "AUTHENTIC") EmeraldGreen else ApexCrimson,
                                RoundedCornerShape(20.dp)
                            )
                            .clickable { showAntiCheatMenu = true }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .testTag("anti_cheat_mode_badge"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = if (antiCheatMode == "AUTHENTIC") EmeraldGreen else ApexCrimson,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = when (antiCheatMode) {
                                "SCREEN_CHEAT" -> "ANTI-CHEAT: Screen Test"
                                "TOY_CHEAT" -> "ANTI-CHEAT: Toy Test"
                                else -> "ANTI-CHEAT: ARMED"
                            },
                            color = if (antiCheatMode == "AUTHENTIC") EmeraldGreen else ApexCrimson,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    DropdownMenu(
                        expanded = showAntiCheatMenu,
                        onDismissRequest = { showAntiCheatMenu = false },
                        modifier = Modifier.background(CarbonCard)
                    ) {
                        DropdownMenuItem(
                            text = { Text("✓ Authentic Mode (Pass)", color = EmeraldGreen, fontSize = 12.sp) },
                            onClick = {
                                viewModel.setAntiCheatMode("AUTHENTIC")
                                showAntiCheatMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("✕ Test Screen Cheat (Fail)", color = ApexCrimson, fontSize = 12.sp) },
                            onClick = {
                                viewModel.setAntiCheatMode("SCREEN_CHEAT")
                                showAntiCheatMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("✕ Test Toy/Model Cheat (Fail)", color = ApexCrimson, fontSize = 12.sp) },
                            onClick = {
                                viewModel.setAntiCheatMode("TOY_CHEAT")
                                showAntiCheatMenu = false
                            }
                        )
                    }
                }

                // AI Engine Notice Tag
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(CarbonCard)
                        .border(1.dp, CarbonBorder, RoundedCornerShape(16.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.FlashOn,
                        contentDescription = null,
                        tint = TurboAmber,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "AI VISION 3.8",
                        color = TurboAmber,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Horizon alignment reminder
            Text(
                text = "FRAME ENTIRE VEHICLE • AUTO-TELEMETRY LOCK",
                color = TextMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Scanning Animation HUD Overlay
        if (isAnalyzing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    CircularProgressIndicator(
                        color = TurboAmber,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "AI VEHICLE IDENTIFICATION",
                        color = NitroCyan,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = scanningText,
                        color = TextPrimary,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Auditing reflections, depth map & anti-cheat authenticity",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
            }
        }

        // Bottom Controls HUD (Shutter, Quick Test Presets, and Gallery)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Quick preset pills for quick simulator testing or live camera roll
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                QuickPresetButton(
                    label = "Porsche GT3 RS",
                    onClick = { viewModel.snapCar("res:car_porsche") }
                )
                Spacer(modifier = Modifier.width(8.dp))
                QuickPresetButton(
                    label = "Ferrari 296 GTB",
                    onClick = { viewModel.snapCar("res:car_ferrari") }
                )
                Spacer(modifier = Modifier.width(8.dp))
                QuickPresetButton(
                    label = "New Spot",
                    onClick = { viewModel.snapCar(null) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Shutter Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Gallery photo picker button
                IconButton(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(CarbonCard)
                        .border(1.dp, CarbonBorder, CircleShape)
                        .testTag("gallery_picker_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = "Pick Car Photo",
                        tint = NitroCyan
                    )
                }

                // Shutter Button with animated neon pulse
                ShutterButton(
                    onClick = { viewModel.snapCar(null) },
                    isAnalyzing = isAnalyzing
                )

                // Flash / Sensor indicator
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(CarbonCard)
                        .border(1.dp, CarbonBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Camera Ready",
                        tint = TurboAmber,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "TAP SHUTTER TO CAPTURE & IDENTIFY",
                color = TextMuted,
                fontSize = 10.sp,
                letterSpacing = 1.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun QuickPresetButton(
    label: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(CarbonCard.copy(alpha = 0.85f))
            .border(1.dp, CarbonBorder, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag("preset_$label")
    ) {
        Text(
            text = label,
            color = TextPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun ShutterButton(
    onClick: () -> Unit,
    isAnalyzing: Boolean
) {
    val infiniteTransition = rememberInfiniteTransition(label = "shutter_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Box(
        modifier = Modifier
            .size(80.dp)
            .clickable(enabled = !isAnalyzing, onClick = onClick)
            .testTag("shutter_button"),
        contentAlignment = Alignment.Center
    ) {
        // Outer glowing pulse ring
        Box(
            modifier = Modifier
                .size((76 * pulseScale).dp)
                .clip(CircleShape)
                .border(2.dp, TurboAmber.copy(alpha = 0.5f), CircleShape)
        )

        // Middle solid ring
        Box(
            modifier = Modifier
                .size(68.dp)
                .clip(CircleShape)
                .border(3.dp, TurboAmber, CircleShape)
                .background(Color.Transparent)
        )

        // Inner core button
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(TurboAmber, Color(0xFFCC5500))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = "Snap Car",
                tint = Color.Black,
                modifier = Modifier.size(26.dp)
            )
        }
    }
}

@Composable
fun CameraViewfinderHUD(isAnalyzing: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "scan_line")
    val scanY by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scan_y"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        // Dark gradient vignette
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.Black.copy(alpha = 0.6f),
                    Color.Transparent,
                    Color.Transparent,
                    Color.Black.copy(alpha = 0.7f)
                )
            )
        )

        // Viewfinder Framing Brackets
        val boxWidth = width * 0.85f
        val boxHeight = height * 0.45f
        val left = (width - boxWidth) / 2f
        val top = (height - boxHeight) / 2f - 30f
        val right = left + boxWidth
        val bottom = top + boxHeight
        val cornerLen = 36f
        val bracketColor = if (isAnalyzing) TurboAmber else NitroCyan.copy(alpha = 0.8f)

        // Top-left
        drawLine(bracketColor, Offset(left, top), Offset(left + cornerLen, top), strokeWidth = 5f)
        drawLine(bracketColor, Offset(left, top), Offset(left, top + cornerLen), strokeWidth = 5f)

        // Top-right
        drawLine(bracketColor, Offset(right, top), Offset(right - cornerLen, top), strokeWidth = 5f)
        drawLine(bracketColor, Offset(right, top), Offset(right, top + cornerLen), strokeWidth = 5f)

        // Bottom-left
        drawLine(bracketColor, Offset(left, bottom), Offset(left + cornerLen, bottom), strokeWidth = 5f)
        drawLine(bracketColor, Offset(left, bottom), Offset(left, bottom - cornerLen), strokeWidth = 5f)

        // Bottom-right
        drawLine(bracketColor, Offset(right, bottom), Offset(right - cornerLen, bottom), strokeWidth = 5f)
        drawLine(bracketColor, Offset(right, bottom), Offset(right, bottom - cornerLen), strokeWidth = 5f)

        // Center reticle crosshair
        val cx = width / 2f
        val cy = top + boxHeight / 2f
        val reticleLen = 16f
        drawCircle(
            color = bracketColor.copy(alpha = 0.4f),
            radius = 32f,
            center = Offset(cx, cy),
            style = Stroke(width = 2f)
        )
        drawLine(bracketColor, Offset(cx - reticleLen, cy), Offset(cx + reticleLen, cy), strokeWidth = 2f)
        drawLine(bracketColor, Offset(cx, cy - reticleLen), Offset(cx, cy + reticleLen), strokeWidth = 2f)

        // Horizon pitch balance ticks
        drawLine(
            bracketColor.copy(alpha = 0.35f),
            Offset(left + 20f, cy),
            Offset(left + 60f, cy),
            strokeWidth = 2f
        )
        drawLine(
            bracketColor.copy(alpha = 0.35f),
            Offset(right - 60f, cy),
            Offset(right - 20f, cy),
            strokeWidth = 2f
        )

        // Laser scan line when analyzing
        if (isAnalyzing) {
            val laserY = top + (boxHeight * scanY)
            drawLine(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        TurboAmber,
                        Color.White,
                        TurboAmber,
                        Color.Transparent
                    )
                ),
                start = Offset(left, laserY),
                end = Offset(right, laserY),
                strokeWidth = 4f
            )
        }
    }
}
