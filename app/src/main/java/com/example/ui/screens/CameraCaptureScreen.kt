package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import android.util.Size
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
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
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
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ui.CarSpotterViewModel
import com.example.ui.theme.CobaltAccent
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.InkMuted
import com.example.ui.theme.InkPrimary
import com.example.ui.theme.InkSecondary
import com.example.ui.theme.OutfitDisplayFont
import com.example.ui.theme.PorcelainSurface
import com.example.ui.theme.PureWhite
import com.example.ui.theme.RacingRed
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

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (!granted) {
            Toast.makeText(context, "Camera permission needed for live vehicle capture", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.warmUpAi()
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val previewView = remember {
        PreviewView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    LaunchedEffect(hasCameraPermission, cameraSelector) {
        if (!hasCameraPermission) return@LaunchedEffect

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val resolutionSelector = ResolutionSelector.Builder()
                    .setResolutionStrategy(
                        ResolutionStrategy(
                            Size(1280, 960),
                            ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                        )
                    )
                    .build()

                val capture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .setResolutionSelector(resolutionSelector)
                    .build()

                imageCapture = capture

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
            .background(PureWhite)
            .testTag("camera_capture_screen")
    ) {
        if (hasCameraPermission) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { previewView }
            )
        } else {
            // Pure White Editorial Camera Permission Card
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(PureWhite),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .padding(24.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(PorcelainSurface)
                        .border(1.dp, HairlineBorder, RoundedCornerShape(22.dp))
                        .padding(28.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(58.dp)
                            .clip(CircleShape)
                            .background(PureWhite)
                            .border(1.dp, HairlineBorder, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = InkPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Camera Access Required",
                        fontFamily = OutfitDisplayFont,
                        color = InkPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Enable camera access to identify real cars in your surroundings.",
                        color = InkSecondary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    Button(
                        onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                        colors = ButtonDefaults.buttonColors(containerColor = InkPrimary, contentColor = PureWhite),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Grant Camera Permission", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Clean White Reticle Viewfinder Overlay
        if (hasCameraPermission) {
            CameraViewfinderHUD(isAnalyzing = isAnalyzing)
        }

        // Top Clean White Frosted Status Bar (No Neon)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(PureWhite.copy(alpha = 0.94f))
                    .border(1.dp, HairlineBorder, RoundedCornerShape(999.dp))
                    .padding(horizontal = 12.dp, vertical = 7.dp)
                    .testTag("anti_cheat_badge"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.VerifiedUser,
                    contentDescription = null,
                    tint = CobaltAccent,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "AI ANTI-CHEAT ACTIVE",
                    color = InkPrimary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.6.sp
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = {
                        val next = !isTorchEnabled
                        isTorchEnabled = next
                        activeCamera?.cameraControl?.enableTorch(next)
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(PureWhite.copy(alpha = 0.94f))
                        .border(1.dp, HairlineBorder, CircleShape)
                        .testTag("torch_toggle_button")
                ) {
                    Icon(
                        imageVector = if (isTorchEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        contentDescription = "Torch",
                        tint = if (isTorchEnabled) RacingRed else InkPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
                            CameraSelector.DEFAULT_FRONT_CAMERA
                        } else {
                            CameraSelector.DEFAULT_BACK_CAMERA
                        }
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(PureWhite.copy(alpha = 0.94f))
                        .border(1.dp, HairlineBorder, CircleShape)
                        .testTag("camera_switch_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Cameraswitch,
                        contentDescription = "Switch Camera",
                        tint = InkPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Pure White Editorial Scanning Progress Card
        if (isAnalyzing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(PureWhite.copy(alpha = 0.72f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.84f)
                        .padding(16.dp),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = PureWhite),
                    border = androidx.compose.foundation.BorderStroke(1.dp, HairlineBorder),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        CircularProgressIndicator(
                            color = CobaltAccent,
                            trackColor = PorcelainSurface,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        Text(
                            text = "Identifying Vehicle",
                            fontFamily = OutfitDisplayFont,
                            color = InkPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = scanningText,
                            color = InkSecondary,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // Bottom Clean White Capture Dock
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(PureWhite.copy(alpha = 0.92f))
                    .border(1.dp, HairlineBorder, RoundedCornerShape(999.dp))
                    .padding(horizontal = 14.dp, vertical = 5.dp)
            ) {
                Text(
                    text = "CENTER CAR IN FRAME",
                    color = InkPrimary,
                    fontSize = 10.sp,
                    letterSpacing = 1.2.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            ShutterButton(
                onClick = {
                    viewModel.beginCapture()
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
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
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
        Box(
            modifier = Modifier
                .size((76 * pulseScale).dp)
                .clip(CircleShape)
                .background(PureWhite.copy(alpha = 0.92f))
                .border(2.dp, HairlineBorder, CircleShape)
        )

        Box(
            modifier = Modifier
                .size(62.dp)
                .clip(CircleShape)
                .background(InkPrimary),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = "Snap Car",
                tint = PureWhite,
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
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scan_y"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        val boxWidth = width * 0.84f
        val boxHeight = height * 0.44f
        val left = (width - boxWidth) / 2f
        val top = (height - boxHeight) / 2f - 24f
        val right = left + boxWidth
        val bottom = top + boxHeight
        val cornerLen = 36f
        val bracketColor = if (isAnalyzing) CobaltAccent else PureWhite.copy(alpha = 0.92f)

        drawLine(bracketColor, Offset(left, top), Offset(left + cornerLen, top), strokeWidth = 5f)
        drawLine(bracketColor, Offset(left, top), Offset(left, top + cornerLen), strokeWidth = 5f)

        drawLine(bracketColor, Offset(right, top), Offset(right - cornerLen, top), strokeWidth = 5f)
        drawLine(bracketColor, Offset(right, top), Offset(right, top + cornerLen), strokeWidth = 5f)

        drawLine(bracketColor, Offset(left, bottom), Offset(left + cornerLen, bottom), strokeWidth = 5f)
        drawLine(bracketColor, Offset(left, bottom), Offset(left, bottom - cornerLen), strokeWidth = 5f)

        drawLine(bracketColor, Offset(right, bottom), Offset(right - cornerLen, bottom), strokeWidth = 5f)
        drawLine(bracketColor, Offset(right, bottom), Offset(right, bottom - cornerLen), strokeWidth = 5f)

        val cx = width / 2f
        val cy = top + boxHeight / 2f
        val reticleLen = 14f
        drawCircle(
            color = bracketColor.copy(alpha = 0.5f),
            radius = 28f,
            center = Offset(cx, cy),
            style = Stroke(width = 2f)
        )
        drawLine(bracketColor, Offset(cx - reticleLen, cy), Offset(cx + reticleLen, cy), strokeWidth = 2f)
        drawLine(bracketColor, Offset(cx, cy - reticleLen), Offset(cx, cy + reticleLen), strokeWidth = 2f)

        if (isAnalyzing) {
            val laserY = top + (boxHeight * scanY)
            drawLine(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        CobaltAccent,
                        PureWhite,
                        CobaltAccent,
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

                    val rotatedBitmap = if (rotationDegrees != 0) {
                        val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
                        Bitmap.createBitmap(rawBitmap, 0, 0, rawBitmap.width, rawBitmap.height, matrix, true)
                    } else {
                        rawBitmap
                    }

                    val maxDim = 1080
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

                    val photoFile = File(context.filesDir, "carspot_${System.currentTimeMillis()}.jpg")
                    FileOutputStream(photoFile).use { out ->
                        finalBitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
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
