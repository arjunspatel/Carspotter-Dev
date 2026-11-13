package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import com.example.ai.CarRecognitionEngine
import com.example.ai.RecognitionResult
import com.example.ai.RecognitionStatus
import com.example.model.AntiCheatResult
import com.example.ui.components.TrophyRevealDialog
import com.example.ui.theme.CarSpotterTheme
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CarRecognitionEngineTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `missing photo returns NO_VEHICLE status instead of cheat rejection`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val engine = CarRecognitionEngine(context)

        val result = engine.analyzeVehicleImage(photoUri = null, capturedBitmap = null)

        assertFalse(result.isSuccess)
        assertEquals(RecognitionStatus.NO_VEHICLE, result.status)
    }

    @Test
    fun `uniform black frame returns NO_VEHICLE locally`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val engine = CarRecognitionEngine(context)
        val blankBitmap = Bitmap.createBitmap(128, 128, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.BLACK)
        }

        val result = engine.analyzeVehicleImage(photoUri = "mem://test", capturedBitmap = blankBitmap)

        assertFalse(result.isSuccess)
        assertEquals(RecognitionStatus.NO_VEHICLE, result.status)
    }

    @Test
    fun `textured frame without network returns NETWORK_OR_TIMEOUT cleanly`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        @Suppress("DEPRECATION")
        org.robolectric.Shadows.shadowOf(cm).setActiveNetworkInfo(null)
        val engine = CarRecognitionEngine(context)
        val texturedBitmap = Bitmap.createBitmap(128, 128, Bitmap.Config.ARGB_8888).apply {
            for (y in 0 until 128) {
                for (x in 0 until 128) {
                    setPixel(x, y, if (((x / 32) + (y / 32)) % 2 == 0) Color.WHITE else Color.BLACK)
                }
            }
        }

        val result = engine.analyzeVehicleImage(photoUri = "mem://test", capturedBitmap = texturedBitmap)

        assertFalse(result.isSuccess)
        assertEquals(RecognitionStatus.NETWORK_OR_TIMEOUT, result.status)
        assertEquals("NO INTERNET CONNECTION", result.antiCheatResult.verdict)
    }

    @Test
    fun `timeout dialog displays connection header and retry action instead of anti-cheat rejection`() {
        val timeoutResult = RecognitionResult(
            carSpot = null,
            antiCheatResult = AntiCheatResult(
                isAuthentic = false,
                confidence = 0f,
                verdict = "AI REQUEST TIMED OUT",
                details = "The AI service took over 60 seconds to respond.",
                detectionFlags = listOf("60s Cloud Timeout")
            ),
            isSuccess = false,
            status = RecognitionStatus.NETWORK_OR_TIMEOUT
        )

        composeTestRule.setContent {
            CarSpotterTheme {
                TrophyRevealDialog(
                    result = timeoutResult,
                    capturedBitmap = null,
                    onRetry = {},
                    onDismiss = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("CONNECTION / TIMEOUT").assertIsDisplayed()
        composeTestRule.onNodeWithText("ANTI-CHEAT REJECTION").assertDoesNotExist()
        composeTestRule.onNodeWithTag("retry_analysis_button").assertIsDisplayed()
    }

    @Test
    fun `cheat rejection dialog displays anti-cheat header and hides retry button`() {
        val cheatResult = RecognitionResult(
            carSpot = null,
            antiCheatResult = AntiCheatResult(
                isAuthentic = false,
                confidence = 0.98f,
                verdict = "REJECTED: Display Screen Detected",
                details = "Detected display screen patterns.",
                detectionFlags = listOf("AI Screen Detection")
            ),
            isSuccess = false,
            status = RecognitionStatus.REJECTED_CHEAT
        )

        composeTestRule.setContent {
            CarSpotterTheme {
                TrophyRevealDialog(
                    result = cheatResult,
                    capturedBitmap = null,
                    onRetry = {},
                    onDismiss = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("ANTI-CHEAT REJECTION").assertIsDisplayed()
        composeTestRule.onNodeWithTag("retry_analysis_button").assertDoesNotExist()
    }
}
