package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
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
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.NitroCyan
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TurboAmber
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

private const val TAG = "CameraCaptureScreen"

@Composable
fun CameraCaptureScreen(
    viewModel: CarSpotterViewModel
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val isAnalyzing by viewModel.isAnalyzing.collectAsState()
    val scanningText by viewModel.scanningStepText.collectAsState()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    var cameraSelector by remember { mutableStateOf(CameraSelector.DEFAULT_BACK_CAMERA) }
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    var activeCamera: Camera? by remember { mutableStateOf(null) }
    var isTorchEnabled by remember { mutableStateOf(false) }

    val imageProcessingExecutor = remember { Executors.newSingleThreadExecutor() }
    DisposableEffect(Unit) {
        onDispose {
            imageProcessingExecutor.shutdown()
        }
    }

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

    // Stable PreviewView instance
    val previewView = remember {
        PreviewView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            // Use COMPATIBLE (TextureView) for reliable Compose rendering and compositing
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    // BIND CAMERAX ONLY ONCE or when selector/permission changes.
    // NEVER unbind inside AndroidView.update, which would kill the live camera feed on recomposition!
    LaunchedEffect(hasCameraPermission, cameraSelector) {
        if (!hasCameraPermission) return@LaunchedEffect

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
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

                // Resolve available camera lens
                val targetSelector = when {
                    cameraProvider.hasCamera(cameraSelector) -> cameraSelector
                    cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) -> CameraSelector.DEFAULT_BACK_CAMERA
                    cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) -> CameraSelector.DEFAULT_FRONT_CAMERA
                    else -> cameraSelector
                }

                cameraProvider.unbindAll()
                activeCamera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    targetSelector,
                    preview,
                    capture
                )
                Log.d(TAG, "CameraX successfully bound to PreviewView with lens: $targetSelector")
            } catch (exc: Exception) {
                Log.e(TAG, "Camera use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("camera_capture_screen")
    ) {
        if (hasCameraPermission) {
            // Live CameraX Viewfinder stream
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { previewView }
            )
        } else {
            // Permission request banner
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
                        text = "Enable camera access to scan cars in real-time.",
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
                // Autonomous AI Anti-Cheat Status Badge (Embedded directly into AI model)
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(CarbonCard.copy(alpha = 0.85f))
                        .border(
                            1.dp,
                            EmeraldGreen.copy(alpha = 0.6f),
                            RoundedCornerShape(20.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .testTag("anti_cheat_badge"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = EmeraldGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "AI ANTI-CHEAT ACTIVE",
                        color = EmeraldGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
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

                // Primary Optical Shutter Button with full in-memory Bitmap pipeline
                ShutterButton(
                    onClick = {
                        captureRealTimeImage(
                            context = context,
                            imageCapture = imageCapture,
                            processingExecutor = imageProcessingExecutor,
                            onCaptured = { filePath, bitmap ->
                                viewModel.snapCar(photoUri = filePath, bitmap = bitmap)
                            },
                            onError = { errMsg ->
                                viewModel.snapCar(photoUri = null, bitmap = null, captureError = errMsg)
                            }
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

        // Viewfinder Framing Brackets
        val boxWidth = width * 0.85f
        val boxHeight = height * 0.45f
        val left = (width - boxWidth) / 2f
        val top = (height - boxHeight) / 2f - 30f
        val right = left + boxWidth
        val bottom = top + boxHeight
        val cornerLen = 36f
        val bracketColor = if (isAnalyzing) TurboAmber else NitroCyan.copy(alpha = 0.85f)

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

/**
 * Triggers CameraX image capture with the refactored Bitmap processing pipeline:
 * 1. Takes image directly to ImageProxy in memory
 * 2. Extracts rotation degrees and creates an accurately rotated Bitmap
 * 3. Scales the Bitmap down to standard 1920px max dimension with high quality filtering
 * 4. Writes the processed Bitmap to a pristine JPEG on device storage
 * 5. Returns both the file path and the Bitmap instance so UI displays the real photo immediately
 */
private fun captureRealTimeImage(
    context: Context,
    imageCapture: ImageCapture?,
    processingExecutor: ExecutorService,
    onCaptured: (filePath: String, bitmap: Bitmap) -> Unit,
    onError: (String) -> Unit
) {
    if (imageCapture == null) {
        Log.e(TAG, "ImageCapture is not initialized yet")
        onError("Camera is still warming up. Please tap again in a moment.")
        return
    }

    val mainExecutor = ContextCompat.getMainExecutor(context)

    imageCapture.takePicture(
        processingExecutor,
        object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(imageProxy: ImageProxy) {
                try {
                    val rotationDegrees = imageProxy.imageInfo.rotationDegrees
                    val rawBitmap = imageProxy.toBitmap()
                    imageProxy.close()

                    // Rotate according to hardware sensor metadata
                    val rotatedBitmap = if (rotationDegrees != 0) {
                        val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
                        Bitmap.createBitmap(rawBitmap, 0, 0, rawBitmap.width, rawBitmap.height, matrix, true)
                    } else {
                        rawBitmap
                    }

                    // Scale down to max 1920px maintaining aspect ratio
                    val maxDim = 1920
                    val scale = minOf(maxDim.toFloat() / rotatedBitmap.width, maxDim.toFloat() / rotatedBitmap.height, 1.0f)
                    val finalBitmap = if (scale < 1.0f) {
                        Bitmap.createScaledBitmap(
                            rotatedBitmap,
                            (rotatedBitmap.width * scale).toInt().coerceAtLeast(1),
                            (rotatedBitmap.height * scale).toInt().coerceAtLeast(1),
                            true
                        )
                    } else {
                        rotatedBitmap
                    }

                    // Save the pristine, processed JPEG to app storage
                    val photoFile = File(context.filesDir, "carspot_${System.currentTimeMillis()}.jpg")
                    FileOutputStream(photoFile).use { out ->
                        finalBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                    }

                    Log.d(TAG, "Processed image saved: ${photoFile.absolutePath}, ${finalBitmap.width}x${finalBitmap.height}")
                    mainExecutor.execute {
                        onCaptured(photoFile.absolutePath, finalBitmap)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing captured image: ${e.message}", e)
                    mainExecutor.execute {
                        onError("Failed to process captured image: ${e.message}")
                    }
                }
            }

            override fun onError(exception: ImageCaptureException) {
                Log.e(TAG, "Camera capture failed: ${exception.message}", exception)
                mainExecutor.execute {
                    onError("Camera capture failed: ${exception.message}")
                }
            }
        }
    )
}
