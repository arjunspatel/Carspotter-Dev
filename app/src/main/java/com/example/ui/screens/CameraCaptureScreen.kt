package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
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
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.ui.CarSpotterViewModel
import com.example.ui.theme.ApexCrimson
import com.example.ui.theme.CarbonBorder
import com.example.ui.theme.CarbonCard
import com.example.ui.theme.CarbonCardElevated
import com.example.ui.theme.CarbonDark
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.NitroCyan
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TurboAmber
import java.io.File

private const val TAG = "CameraCaptureScreen"

@Composable
fun CameraCaptureScreen(
    viewModel: CarSpotterViewModel
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val isAnalyzing by viewModel.isAnalyzing.collectAsState()
    val scanningText by viewModel.scanningStepText.collectAsState()
    val antiCheatMode by viewModel.antiCheatMode.collectAsState()

    var showAntiCheatMenu by remember { mutableStateOf(false) }
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    var cameraSelector by remember { mutableStateOf(CameraSelector.DEFAULT_BACK_CAMERA) }
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    var activeCamera: Camera? by remember { mutableStateOf(null) }
    var isTorchEnabled by remember { mutableStateOf(false) }

    // Camera permission request launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (!granted) {
            Toast.makeText(context, "Camera permission needed for live vehicle capture", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CarbonDark)
            .testTag("camera_capture_screen")
    ) {
        if (hasCameraPermission) {
            // Live CameraX Preview View
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    val previewView = PreviewView(ctx).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                    }

                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        try {
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }

                            val capture = ImageCapture.Builder()
                                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                                .build()

                            imageCapture = capture

                            cameraProvider.unbindAll()
                            activeCamera = cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                capture
                            )
                        } catch (exc: Exception) {
                            Log.e(TAG, "Use case binding failed", exc)
                        }
                    }, ContextCompat.getMainExecutor(ctx))

                    previewView
                },
                update = {
                    // Update when cameraSelector changes
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                    cameraProviderFuture.addListener({
                        try {
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.setSurfaceProvider(it.surfaceProvider)
                            }
                            val capture = imageCapture ?: ImageCapture.Builder().build()
                            cameraProvider.unbindAll()
                            activeCamera = cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                capture
                            )
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed updating camera selector", e)
                        }
                    }, ContextCompat.getMainExecutor(context))
                }
            )
        } else {
            // Fallback placeholder when camera permission is pending or denied
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(CarbonCardElevated),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = TurboAmber,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Camera Access Required",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Enable camera access to scan cars in real-time or pick photos from your library.",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                        colors = ButtonDefaults.buttonColors(containerColor = TurboAmber, contentColor = Color.Black),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Grant Camera Permission", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Tactical Viewfinder HUD Overlay
        CameraViewfinderHUD(isAnalyzing = isAnalyzing)

        // Top Status Bar: Anti-Cheat Shield, Torch & Lens Switcher
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
                                if (antiCheatMode == "AUTHENTIC") CarbonCard.copy(alpha = 0.85f) else ApexCrimson.copy(alpha = 0.25f)
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

                // Controls: Torch & Camera Switch
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Torch Toggle
                    IconButton(
                        onClick = {
                            val next = !isTorchEnabled
                            isTorchEnabled = next
                            activeCamera?.cameraControl?.enableTorch(next)
                        },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(CarbonCard.copy(alpha = 0.8f))
                            .border(1.dp, CarbonBorder, CircleShape)
                            .testTag("torch_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (isTorchEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                            contentDescription = "Torch",
                            tint = if (isTorchEnabled) TurboAmber else TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Lens Switcher (Back/Front)
                    IconButton(
                        onClick = {
                            cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
                                CameraSelector.DEFAULT_FRONT_CAMERA
                            } else {
                                CameraSelector.DEFAULT_BACK_CAMERA
                            }
                        },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(CarbonCard.copy(alpha = 0.8f))
                            .border(1.dp, CarbonBorder, CircleShape)
                            .testTag("camera_switch_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cameraswitch,
                            contentDescription = "Switch Camera",
                            tint = NitroCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Horizon alignment reminder
            Text(
                text = "REAL-TIME CAMERAX • CENTER VEHICLE FOR AI SCAN",
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
                    .background(Color.Black.copy(alpha = 0.75f)),
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

        // Bottom Controls HUD - Sleek, Uncluttered Modern Graphic Layout
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "CENTER VEHICLE IN RETICLE",
                color = NitroCyan.copy(alpha = 0.8f),
                fontSize = 11.sp,
                letterSpacing = 2.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Shutter Row with minimalist side actions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Torch Flashlight Toggle
                IconButton(
                    onClick = {
                        val next = !isTorchEnabled
                        isTorchEnabled = next
                        activeCamera?.cameraControl?.enableTorch(next)
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(CarbonCard.copy(alpha = 0.85f))
                        .border(1.dp, CarbonBorder, CircleShape)
                        .testTag("torch_toggle_button")
                ) {
                    Icon(
                        imageVector = if (isTorchEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        contentDescription = "Torch",
                        tint = if (isTorchEnabled) TurboAmber else TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Primary Optical Shutter Button
                ShutterButton(
                    onClick = {
                        captureRealTimeImage(
                            context = context,
                            imageCapture = imageCapture,
                            onCaptured = { uri -> viewModel.snapCar(uri?.toString()) },
                            onError = { viewModel.snapCar(null) }
                        )
                    },
                    isAnalyzing = isAnalyzing
                )

                // Lens Switcher (Back / Front)
                IconButton(
                    onClick = {
                        cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
                            CameraSelector.DEFAULT_FRONT_CAMERA
                        } else {
                            CameraSelector.DEFAULT_BACK_CAMERA
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(CarbonCard.copy(alpha = 0.85f))
                        .border(1.dp, CarbonBorder, CircleShape)
                        .testTag("camera_switch_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Cameraswitch,
                        contentDescription = "Switch Camera",
                        tint = NitroCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "TAP SHUTTER TO CAPTURE REAL-TIME SIGHTING",
                color = TextMuted,
                fontSize = 10.sp,
                letterSpacing = 1.2.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * Triggers CameraX image capture and saves the output to a temporary file
 */
private fun captureRealTimeImage(
    context: Context,
    imageCapture: ImageCapture?,
    onCaptured: (Uri?) -> Unit,
    onError: () -> Unit
) {
    if (imageCapture == null) {
        // Fallback for emulator without hardware camera
        onCaptured(null)
        return
    }

    val photoFile = File(
        context.cacheDir,
        "carspot_${System.currentTimeMillis()}.jpg"
    )

    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

    imageCapture.takePicture(
        outputOptions,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                val savedUri = Uri.fromFile(photoFile)
                onCaptured(savedUri)
            }

            override fun onError(exception: ImageCaptureException) {
                Log.e(TAG, "Camera capture failed: ${exception.message}", exception)
                onError()
            }
        }
    )
}
