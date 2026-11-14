package com.example.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.util.Log
import com.example.model.AntiCheatResult
import com.example.model.CarSpot
import com.example.model.RarityTier
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.ThinkingLevel
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import com.google.firebase.ai.type.thinkingConfig
import com.google.firebase.appcheck.FirebaseAppCheck
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.UUID

private const val TAG = "CarRecognitionEngine"
private const val AI_TIMEOUT_MS = 45_000L

enum class RecognitionStatus {
    SUCCESS,
    REJECTED_CHEAT,
    NO_VEHICLE,
    NETWORK_OR_TIMEOUT,
    AI_SERVICE_ERROR
}

data class RecognitionResult(
    val carSpot: CarSpot?,
    val antiCheatResult: AntiCheatResult,
    val isSuccess: Boolean,
    val status: RecognitionStatus = if (isSuccess) RecognitionStatus.SUCCESS else RecognitionStatus.REJECTED_CHEAT,
    val errorMessage: String? = null
)

class CarRecognitionEngine(private val context: Context) {

    // Streamlined schema embedding autonomous anti-cheat detection + specs into a single Gemini pass.
    private val carSchema = Schema.obj(
        properties = mapOf(
            "isRealCar" to Schema.boolean(
                description = "TRUE only if a real physical motor vehicle is visible in the real world. FALSE if non-car (wall, floor, foot, person), screen, or toy."
            ),
            "isScreenOrDisplay" to Schema.boolean(
                description = "TRUE if photographing a monitor, phone, tablet, TV screen, or printed photo of a car."
            ),
            "isToyOrModel" to Schema.boolean(
                description = "TRUE if this is a toy car, Hot Wheels, RC car, or die-cast scale model."
            ),
            "detectedSubject" to Schema.string(
                description = "Brief description of the subject seen (e.g. 'Porsche 911 GT3 RS', 'Sidewalk and shoe', 'Monitor displaying a Ferrari')."
            ),
            "antiCheatVerdict" to Schema.string(
                description = "One of: 'VERIFIED REAL VEHICLE', 'REJECTED: Display Screen Detected', 'REJECTED: Toy/Scale Model Detected', 'REJECTED: Non-Vehicle Detected'."
            ),
            "antiCheatDetails" to Schema.string(
                description = "Brief 1-sentence reason for the authenticity verdict."
            ),
            "make" to Schema.string(description = "Car manufacturer (e.g. Porsche, Ferrari, Ford, Toyota)", nullable = true),
            "model" to Schema.string(description = "Specific car model (e.g. 911 GT3 RS, Mustang GT, Supra)", nullable = true),
            "year" to Schema.integer(description = "Estimated model year", nullable = true),
            "trim" to Schema.string(description = "Trim or package (e.g. Z06, Competition, Base)", nullable = true),
            "rarityTier" to Schema.string(description = "COMMON, UNCOMMON, RARE, EXOTIC, or LEGENDARY", nullable = true),
            "rarityScore" to Schema.integer(description = "Rarity score from 1 to 100", nullable = true),
            "horsepower" to Schema.integer(description = "Estimated horsepower (HP)", nullable = true),
            "zeroToSixtyMph" to Schema.double(description = "Estimated 0-60 mph time in seconds", nullable = true),
            "topSpeedMph" to Schema.integer(description = "Estimated top speed in mph", nullable = true),
            "estimatedPriceUsd" to Schema.integer(description = "Estimated MSRP in USD", nullable = true),
            "engineType" to Schema.string(description = "Engine configuration (e.g. 4.0L Flat-6)", nullable = true),
            "drivetrain" to Schema.string(description = "RWD, AWD, or FWD", nullable = true),
            "trivia" to Schema.string(description = "Short automotive trivia fact", nullable = true)
        ),
        optionalProperties = listOf(
            "make", "model", "year", "trim",
            "rarityTier", "rarityScore", "horsepower",
            "zeroToSixtyMph", "topSpeedMph", "estimatedPriceUsd",
            "engineType", "drivetrain", "trivia"
        )
    )

    private val geminiModel by lazy {
        Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
            modelName = "gemini-3.5-flash-lite",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = carSchema
                temperature = 0.2f
                maxOutputTokens = 512
                thinkingConfig = thinkingConfig {
                    thinkingLevel = ThinkingLevel.MINIMAL
                }
            }
        )
    }

    /**
     * Pre-warms Firebase App Check token and Gemini model instance while the user frames a car,
     * eliminating cold-start attestation & client setup delay at shutter tap.
     */
    suspend fun warmUp() {
        withContext(Dispatchers.IO) {
            try {
                geminiModel
                FirebaseAppCheck.getInstance().getAppCheckToken(false)
            } catch (e: Exception) {
                Log.w(TAG, "Warm-up skipped: ${e.message}")
            }
        }
    }

    /**
     * Fast autonomous vehicle recognition & AI anti-cheat evaluation:
     * 1. Uses already-captured in-memory Bitmap (or decodes from disk off main thread).
     * 2. Local on-device CV filter catches completely blank/obscured frames.
     * 3. Pre-checks active network capability cleanly in <5ms.
     * 4. Compresses frame to compact ~18KB 448px JPEG and invokes Gemini 3.5 Flash-Lite (MINIMAL thinking).
     */
    suspend fun analyzeVehicleImage(
        photoUri: String?,
        capturedBitmap: Bitmap? = null
    ): RecognitionResult {
        if (photoUri.isNullOrBlank() && capturedBitmap == null) {
            return RecognitionResult(
                carSpot = null,
                antiCheatResult = AntiCheatResult(
                    isAuthentic = false,
                    confidence = 1.0f,
                    verdict = "NO PHOTO CAPTURED",
                    details = "Frame a vehicle in the viewfinder and tap the shutter button to capture a photo.",
                    detectionFlags = listOf("Missing Image")
                ),
                isSuccess = false,
                status = RecognitionStatus.NO_VEHICLE,
                errorMessage = "Please frame a vehicle in the camera viewfinder and tap the shutter."
            )
        }

        // Prepare bitmap + compact JPEG bytes off the main thread
        val prepared = withContext(Dispatchers.Default) {
            val bitmap = capturedBitmap ?: decodeBitmapFromUri(photoUri)
            if (bitmap == null) {
                return@withContext PreparedFrame.DecodeFailed
            }

            val (isUniform, surfaceType) = isUniformOrNonVehicleSurface(bitmap)
            if (isUniform) {
                return@withContext PreparedFrame.UniformSurface(surfaceType)
            }

            val maxDim = 448
            val scale = minOf(maxDim.toFloat() / bitmap.width, maxDim.toFloat() / bitmap.height, 1.0f)
            val scaledBitmap = if (scale < 1.0f) {
                Bitmap.createScaledBitmap(
                    bitmap,
                    (bitmap.width * scale).toInt().coerceAtLeast(1),
                    (bitmap.height * scale).toInt().coerceAtLeast(1),
                    true
                )
            } else {
                bitmap
            }

            val baos = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 75, baos)
            val jpegBytes = baos.toByteArray()
            Log.d(TAG, "Prepared JPEG payload: ${scaledBitmap.width}x${scaledBitmap.height}, ${jpegBytes.size} bytes")
            PreparedFrame.Ready(jpegBytes)
        }

        when (prepared) {
            is PreparedFrame.DecodeFailed -> {
                return RecognitionResult(
                    carSpot = null,
                    antiCheatResult = AntiCheatResult(
                        isAuthentic = false,
                        confidence = 1.0f,
                        verdict = "IMAGE LOAD FAILED",
                        details = "Could not decode the captured photo file. Please try snapping again.",
                        detectionFlags = listOf("Decode Failure")
                    ),
                    isSuccess = false,
                    status = RecognitionStatus.AI_SERVICE_ERROR,
                    errorMessage = "Could not decode the captured photo. Please try snapping again."
                )
            }
            is PreparedFrame.UniformSurface -> {
                Log.w(TAG, "Local computer vision intercepted blank/uniform frame: ${prepared.surfaceType}")
                return RecognitionResult(
                    carSpot = null,
                    antiCheatResult = AntiCheatResult(
                        isAuthentic = false,
                        confidence = 0.99f,
                        verdict = "NO VEHICLE DETECTED",
                        details = "Detected ${prepared.surfaceType} with no vehicle contours. Point your camera at an actual car.",
                        detectionFlags = listOf("Uniform / Obscured Frame", "No Vehicle Contours")
                    ),
                    isSuccess = false,
                    status = RecognitionStatus.NO_VEHICLE,
                    errorMessage = "No vehicle detected (${prepared.surfaceType}). Point your camera at an actual car."
                )
            }
            is PreparedFrame.Ready -> Unit
        }

        // Fast-fail if device is genuinely offline before waiting on cloud Gemini Vision
        if (!hasInternetCapability()) {
            Log.w(TAG, "Offline check triggered before Gemini call")
            return RecognitionResult(
                carSpot = null,
                antiCheatResult = AntiCheatResult(
                    isAuthentic = false,
                    confidence = 1.0f,
                    verdict = "NO INTERNET CONNECTION",
                    details = "Your device has no active internet connection. Connect to Wi-Fi or mobile data and tap Retry to identify this photo.",
                    detectionFlags = listOf("Device Offline", "No Internet Capability")
                ),
                isSuccess = false,
                status = RecognitionStatus.NETWORK_OR_TIMEOUT,
                errorMessage = "No internet connection detected. Connect to Wi-Fi or cellular data and retry."
            )
        }

        val jpegBytes = (prepared as PreparedFrame.Ready).jpegBytes

        try {
            val prompt = """
                Analyze this photo as an automotive identifier and anti-cheat judge.
                
                1. ANTI-CHEAT:
                   - Set isScreenOrDisplay = true if photographing a monitor, TV, laptop, phone/tablet screen, or printed photo.
                   - Set isToyOrModel = true if it is a toy car, Hot Wheels, RC car, or die-cast scale model.
                2. VEHICLE VERIFICATION:
                   - Set isRealCar = true ONLY if an actual, full-sized physical motor vehicle (car, truck, SUV, coupe, supercar, van) is visible in the real world (not a screen, not a toy, and not a non-car like a wall, sidewalk, foot, or indoor room).
                3. IDENTIFICATION (if isRealCar = true):
                   - Provide exact make, model, estimated year, trim, horsepower, 0-60 mph, top speed (mph), estimated USD price, engine, drivetrain, rarityTier, rarityScore, and a concise trivia fact.
            """.trimIndent()

            val response = withContext(Dispatchers.IO) {
                withTimeout(AI_TIMEOUT_MS) {
                    geminiModel.generateContent(
                        content {
                            inlineData(jpegBytes, "image/jpeg")
                            text(prompt)
                        }
                    )
                }
            }

            val jsonText = response.text
            if (!jsonText.isNullOrBlank()) {
                val json = JSONObject(jsonText)
                val isRealCar = json.optBoolean("isRealCar", false)
                val isScreen = json.optBoolean("isScreenOrDisplay", false)
                val isToy = json.optBoolean("isToyOrModel", false)
                val subject = json.optCleanString("detectedSubject", "Unknown subject")
                val verdict = json.optCleanString("antiCheatVerdict", "")
                val details = json.optCleanString("antiCheatDetails", "")

                if (isScreen) {
                    return RecognitionResult(
                        carSpot = null,
                        antiCheatResult = AntiCheatResult(
                            isAuthentic = false,
                            confidence = 0.98f,
                            verdict = "REJECTED: Display Screen Detected",
                            details = details.ifBlank {
                                "Detected display screen patterns ($subject). Only physical vehicles spotted in person qualify."
                            },
                            detectionFlags = listOf("AI Screen Detection", "Display Moire / Glare")
                        ),
                        isSuccess = false,
                        status = RecognitionStatus.REJECTED_CHEAT,
                        errorMessage = "Screen cheat detected: photographing screens or photos of cars is not permitted."
                    )
                }

                if (isToy) {
                    return RecognitionResult(
                        carSpot = null,
                        antiCheatResult = AntiCheatResult(
                            isAuthentic = false,
                            confidence = 0.95f,
                            verdict = "REJECTED: Miniature / Toy Model Detected",
                            details = details.ifBlank {
                                "Detected miniature scale model proportions ($subject). Only full-sized vehicles qualify."
                            },
                            detectionFlags = listOf("Scale Disproportion", "Toy / Die-Cast Model")
                        ),
                        isSuccess = false,
                        status = RecognitionStatus.REJECTED_CHEAT,
                        errorMessage = "Die-cast / toy model detected. Only full-sized physical vehicles qualify."
                    )
                }

                if (!isRealCar) {
                    return RecognitionResult(
                        carSpot = null,
                        antiCheatResult = AntiCheatResult(
                            isAuthentic = false,
                            confidence = 0.99f,
                            verdict = "NO VEHICLE DETECTED",
                            details = details.ifBlank {
                                "No motor vehicle detected in photo (Saw: $subject). Frame a real car and try again."
                            },
                            detectionFlags = listOf("Subject: $subject", "No Automotive Target")
                        ),
                        isSuccess = false,
                        status = RecognitionStatus.NO_VEHICLE,
                        errorMessage = "No vehicle detected (Saw: $subject). Point your camera directly at a real car."
                    )
                }

                // Authentic sighting verified by Gemini AI
                val make = json.optCleanString("make", "Identified Car")
                val model = json.optCleanString("model", "Vehicle")
                val year = json.optCleanInt("year", 2024).coerceIn(1920, 2027)
                val trim = json.optCleanString("trim", "Standard")
                val tierStr = json.optCleanString("rarityTier", "RARE").uppercase()
                val tier = try {
                    RarityTier.valueOf(tierStr)
                } catch (_: Exception) {
                    RarityTier.RARE
                }
                val score = json.optCleanInt("rarityScore", 70).coerceIn(10, 100)
                val hp = json.optCleanInt("horsepower", 300).coerceAtLeast(60)
                val zeroToSixty = json.optCleanDouble("zeroToSixtyMph", 5.2).coerceIn(1.5, 18.0)
                val speed = json.optCleanInt("topSpeedMph", 155).coerceAtLeast(75)
                val price = json.optCleanLong("estimatedPriceUsd", 55000L).coerceAtLeast(5000L)
                val engineType = json.optCleanString("engineType", "Performance Powertrain")
                val drivetrain = json.optCleanString("drivetrain", "RWD / AWD")
                val trivia = json.optCleanString("trivia", "Authentic real-world $make $model spotted with CarSpotter.")

                val spot = CarSpot(
                    id = UUID.randomUUID().toString(),
                    make = make,
                    model = model,
                    year = year,
                    trim = trim,
                    rarityTier = tier,
                    rarityScore = score,
                    pointsAwarded = tier.basePoints + (score * 4),
                    horsepower = hp,
                    zeroToSixtyMph = zeroToSixty,
                    topSpeedMph = speed,
                    estimatedPriceUsd = price,
                    engineType = engineType,
                    drivetrain = drivetrain,
                    description = "An authentic spotted $year $make $model ($trim) captured in the wild.",
                    trivia = trivia,
                    photoUri = photoUri,
                    locationName = "Local Sighting",
                    latitude = 34.0522,
                    longitude = -118.2437,
                    timestamp = System.currentTimeMillis(),
                    isAuthentic = true,
                    antiCheatConfidence = 0.99f,
                    antiCheatDetails = details.ifBlank { "Verified authentic physical $make $model sighting by Gemini AI." },
                    isFirstDiscovery = true
                )

                return RecognitionResult(
                    carSpot = spot,
                    antiCheatResult = AntiCheatResult(
                        isAuthentic = true,
                        confidence = 0.99f,
                        verdict = verdict.ifBlank { "VERIFIED SIGHTING: $make $model" },
                        details = "Gemini AI confirmed authentic physical $make $model ($hp HP, $speed MPH).",
                        detectionFlags = listOf("Gemini AI Authenticity Pass", "Physical Reality Match")
                    ),
                    isSuccess = true,
                    status = RecognitionStatus.SUCCESS
                )
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Gemini vision timed out after ${AI_TIMEOUT_MS}ms", e)
            return RecognitionResult(
                carSpot = null,
                antiCheatResult = AntiCheatResult(
                    isAuthentic = false,
                    confidence = 0.0f,
                    verdict = "AI REQUEST TIMED OUT",
                    details = "The AI service took over 45 seconds to respond. Check your Wi-Fi or mobile signal and tap Retry.",
                    detectionFlags = listOf("45s Cloud Timeout", "Slow / Interrupted Connection")
                ),
                isSuccess = false,
                status = RecognitionStatus.NETWORK_OR_TIMEOUT,
                errorMessage = "AI recognition timed out after 45s. Check your connection and tap Retry."
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Gemini vision call failed: ${e::class.simpleName} - ${e.message}", e)
            val isNetworkIssue = isLikelyNetworkException(e)
            val status = if (isNetworkIssue) RecognitionStatus.NETWORK_OR_TIMEOUT else RecognitionStatus.AI_SERVICE_ERROR
            val rawMessage = e.localizedMessage ?: e::class.simpleName ?: "Unknown error"
            return RecognitionResult(
                carSpot = null,
                antiCheatResult = AntiCheatResult(
                    isAuthentic = false,
                    confidence = 0.0f,
                    verdict = if (isNetworkIssue) "NETWORK CONNECTION ISSUE" else "AI SERVICE DIAGNOSTIC",
                    details = if (isNetworkIssue) {
                        "Could not reach Firebase AI ($rawMessage). Verify internet connectivity and tap Retry."
                    } else {
                        "Firebase AI could not complete identification ($rawMessage). Tap Retry or snap another photo."
                    },
                    detectionFlags = listOf(
                        e::class.simpleName ?: "Exception",
                        if (isNetworkIssue) "Network Unreachable" else "Service / Attestation Error"
                    )
                ),
                isSuccess = false,
                status = status,
                errorMessage = rawMessage
            )
        }

        return RecognitionResult(
            carSpot = null,
            antiCheatResult = AntiCheatResult(
                isAuthentic = false,
                confidence = 0.0f,
                verdict = "EMPTY AI RESPONSE",
                details = "Gemini AI returned an empty response. Tap Retry to re-scan this photo.",
                detectionFlags = listOf("Empty Model Output")
            ),
            isSuccess = false,
            status = RecognitionStatus.AI_SERVICE_ERROR,
            errorMessage = "No vehicle data returned. Tap Retry or try snapping another photo."
        )
    }

    private fun hasInternetCapability(): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return true
            val network = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(network) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (e: Exception) {
            Log.w(TAG, "Connectivity check skipped: ${e.message}")
            true
        }
    }

    private fun isLikelyNetworkException(e: Throwable): Boolean {
        var current: Throwable? = e
        while (current != null) {
            if (current is UnknownHostException ||
                current is SocketTimeoutException ||
                current is ConnectException ||
                current is IOException
            ) {
                return true
            }
            val msg = current.message?.lowercase().orEmpty()
            if (msg.contains("unable to resolve host") ||
                msg.contains("network") ||
                msg.contains("timeout") ||
                msg.contains("timed out") ||
                msg.contains("connection reset") ||
                msg.contains("failed to connect")
            ) {
                return true
            }
            current = current.cause
        }
        return false
    }

    /**
     * Local on-device filter for completely black/obscured lenses or flat blank walls.
     */
    private fun isUniformOrNonVehicleSurface(bitmap: Bitmap): Pair<Boolean, String> {
        val sampleW = 64
        val sampleH = 64
        val thumb = Bitmap.createScaledBitmap(bitmap, sampleW, sampleH, true)

        var totalLuminance = 0.0
        val pixels = IntArray(sampleW * sampleH)
        thumb.getPixels(pixels, 0, sampleW, 0, 0, sampleW, sampleH)

        val luminances = DoubleArray(pixels.size)
        for (i in pixels.indices) {
            val color = pixels[i]
            val r = (color shr 16) and 0xFF
            val g = (color shr 8) and 0xFF
            val b = color and 0xFF
            val lum = 0.299 * r + 0.587 * g + 0.114 * b
            luminances[i] = lum
            totalLuminance += lum
        }

        val mean = totalLuminance / pixels.size
        var varianceSum = 0.0
        for (lum in luminances) {
            val diff = lum - mean
            varianceSum += diff * diff
        }
        val stdDev = Math.sqrt(varianceSum / pixels.size)

        var gradientSum = 0.0
        var count = 0
        for (y in 0 until sampleH - 1) {
            for (x in 0 until sampleW - 1) {
                val idx = y * sampleW + x
                val diffX = Math.abs(luminances[idx] - luminances[idx + 1])
                val diffY = Math.abs(luminances[idx] - luminances[idx + sampleW])
                gradientSum += (diffX + diffY)
                count += 2
            }
        }
        val avgGradient = if (count > 0) gradientSum / count else 0.0

        Log.d(TAG, "Computer vision surface check: mean=$mean, stdDev=$stdDev, avgGradient=$avgGradient")

        if (stdDev < 10.0 || avgGradient < 3.5) {
            val surfaceName = when {
                mean < 30.0 -> "Dark/black surface or covered camera lens"
                mean > 220.0 -> "Blank white wall or overexposed frame"
                else -> "Plain flat wall or uniform surface"
            }
            return Pair(true, surfaceName)
        }

        return Pair(false, "")
    }

    private fun decodeBitmapFromUri(photoUri: String?): Bitmap? {
        if (photoUri.isNullOrBlank()) return null
        return try {
            val uri = Uri.parse(photoUri)
            val inputStream = when (uri.scheme) {
                "file" -> File(uri.path ?: "").inputStream()
                "content" -> context.contentResolver.openInputStream(uri)
                else -> File(photoUri).inputStream()
            }

            val originalBitmap = inputStream?.use { BitmapFactory.decodeStream(it) } ?: return null

            val filePath = if (uri.scheme == "file") uri.path else photoUri
            if (!filePath.isNullOrBlank() && File(filePath).exists()) {
                try {
                    val exif = ExifInterface(filePath)
                    val orientation = exif.getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL
                    )
                    val matrix = Matrix()
                    when (orientation) {
                        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                    }
                    if (orientation != ExifInterface.ORIENTATION_NORMAL && orientation != ExifInterface.ORIENTATION_UNDEFINED) {
                        Bitmap.createBitmap(originalBitmap, 0, 0, originalBitmap.width, originalBitmap.height, matrix, true)
                    } else {
                        originalBitmap
                    }
                } catch (_: Exception) {
                    originalBitmap
                }
            } else {
                originalBitmap
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error decoding bitmap from $photoUri", e)
            null
        }
    }

    private sealed interface PreparedFrame {
        data object DecodeFailed : PreparedFrame
        data class UniformSurface(val surfaceType: String) : PreparedFrame
        data class Ready(val jpegBytes: ByteArray) : PreparedFrame
    }
}

private fun JSONObject.optCleanString(key: String, fallback: String): String {
    if (!has(key) || isNull(key)) return fallback
    val value = optString(key, fallback).trim()
    return if (value.isBlank() || value.equals("null", ignoreCase = true)) fallback else value
}

private fun JSONObject.optCleanInt(key: String, fallback: Int): Int {
    if (!has(key) || isNull(key)) return fallback
    return try {
        optInt(key, fallback)
    } catch (_: Exception) {
        fallback
    }
}

private fun JSONObject.optCleanLong(key: String, fallback: Long): Long {
    if (!has(key) || isNull(key)) return fallback
    return try {
        optLong(key, fallback)
    } catch (_: Exception) {
        fallback
    }
}

private fun JSONObject.optCleanDouble(key: String, fallback: Double): Double {
    if (!has(key) || isNull(key)) return fallback
    return try {
        optDouble(key, fallback)
    } catch (_: Exception) {
        fallback
    }
}
