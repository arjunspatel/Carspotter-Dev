package com.example.ui.components

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.R
import com.example.ai.RecognitionResult
import com.example.ai.RecognitionStatus
import com.example.model.CarSpot
import com.example.ui.theme.ApexCrimson
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
import com.example.util.SocialShareUtil
import java.text.NumberFormat
import android.graphics.Bitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.util.Locale

@Composable
fun TrophyRevealDialog(
    result: RecognitionResult,
    capturedBitmap: Bitmap? = null,
    onRetry: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val car = result.carSpot
    val accentBorderColor = when {
        result.isSuccess && car != null -> Color(car.rarityTier.colorHex)
        result.status == RecognitionStatus.NETWORK_OR_TIMEOUT ||
            result.status == RecognitionStatus.AI_SERVICE_ERROR -> TurboAmber
        else -> ApexCrimson
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 24.dp)
                .clip(RoundedCornerShape(24.dp))
                .border(
                    width = 1.5.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(accentBorderColor, CarbonBorder)
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
                .testTag("trophy_reveal_dialog"),
            color = CarbonDark,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (!result.isSuccess || car == null) {
                    DiagnosticOrRejectionCard(
                        status = result.status,
                        verdict = result.antiCheatResult.verdict,
                        details = result.antiCheatResult.details,
                        flags = result.antiCheatResult.detectionFlags,
                        capturedBitmap = capturedBitmap,
                        onRetry = onRetry,
                        onDismiss = onDismiss
                    )
                } else {
                    TrophySuccessContent(
                        car = car,
                        capturedBitmap = capturedBitmap,
                        context = context,
                        onDismiss = onDismiss
                    )
                }
            }
        }
    }
}

@Composable
private fun TrophySuccessContent(
    car: CarSpot,
    capturedBitmap: Bitmap? = null,
    context: Context,
    onDismiss: () -> Unit
) {
    val tierColor = Color(car.rarityTier.colorHex)

    // Top Header: Trophy & Bonus
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = GoldenTrophy,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "TROPHY UNLOCKED",
                color = GoldenTrophy,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                letterSpacing = 1.sp
            )
        }

        // Points badge
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(tierColor.copy(alpha = 0.2f))
                .border(1.dp, tierColor, RoundedCornerShape(12.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(
                text = "+${car.pointsAwarded} PTS",
                color = tierColor,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 14.sp
            )
        }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // Car Photo with Rarity Ribbon: Displays the processed, correctly-scaled Bitmap immediately!
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, CarbonBorder, RoundedCornerShape(16.dp))
    ) {
        if (capturedBitmap != null) {
            Image(
                bitmap = capturedBitmap.asImageBitmap(),
                contentDescription = "${car.make} ${car.model}",
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            CarSpotImage(
                car = car,
                modifier = Modifier.matchParentSize()
            )
        }

        // Rarity Tag Floating Pill
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(10.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Black.copy(alpha = 0.75f))
                .border(1.dp, tierColor, RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = car.rarityTier.label.uppercase(),
                color = tierColor,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
        }

        // Rarity Score Circle
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(10.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.85f))
                .border(1.dp, tierColor, CircleShape)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = "${car.rarityScore}/100",
                color = tierColor,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 11.sp
            )
        }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // Car Title & Trim
    Text(
        text = "${car.year} ${car.make} ${car.model}",
        color = TextPrimary,
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center
    )
    Text(
        text = car.trim,
        color = NitroCyan,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(10.dp))

    // Anti-Cheat Verification Badge
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(EmeraldGreen.copy(alpha = 0.12f))
            .border(1.dp, EmeraldGreen.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.VerifiedUser,
            contentDescription = null,
            tint = EmeraldGreen,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = "ANTI-CHEAT VERIFIED SIGHTING",
                color = EmeraldGreen,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
            Text(
                text = car.antiCheatDetails,
                color = TextSecondary,
                fontSize = 10.sp,
                lineHeight = 13.sp
            )
        }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // Performance Specs Grid (4 key metrics)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SpecBadge(
            icon = Icons.Default.ElectricBolt,
            title = "HORSEPOWER",
            value = "${car.horsepower} HP",
            accentColor = TurboAmber,
            modifier = Modifier.weight(1f)
        )
        SpecBadge(
            icon = Icons.Default.Speed,
            title = "0-60 MPH",
            value = "${car.zeroToSixtyMph}s",
            accentColor = NitroCyan,
            modifier = Modifier.weight(1f)
        )
    }

    Spacer(modifier = Modifier.height(8.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SpecBadge(
            icon = Icons.Default.Speed,
            title = "TOP SPEED",
            value = "${car.topSpeedMph} MPH",
            accentColor = ApexCrimson,
            modifier = Modifier.weight(1f)
        )
        val formattedPrice = NumberFormat.getCurrencyInstance(Locale.US).apply {
            maximumFractionDigits = 0
        }.format(car.estimatedPriceUsd)
        SpecBadge(
            icon = Icons.Default.AttachMoney,
            title = "EST. PRICE",
            value = formattedPrice,
            accentColor = GoldenTrophy,
            modifier = Modifier.weight(1f)
        )
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Powertrain & Description
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CarbonCard)
            .border(1.dp, CarbonBorder, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Text(
            text = "POWERTRAIN & AERO",
            color = TextMuted,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = car.engineType,
            color = TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = car.drivetrain,
            color = TextSecondary,
            fontSize = 11.sp
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = car.description,
            color = TextSecondary,
            fontSize = 11.sp,
            lineHeight = 15.sp
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "💡 ${car.trivia}",
            color = TurboAmber,
            fontSize = 10.sp,
            lineHeight = 14.sp
        )
    }

    Spacer(modifier = Modifier.height(10.dp))

    // Location stamp
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.LocationOn,
            contentDescription = null,
            tint = NitroCyan,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = car.locationName,
            color = TextSecondary,
            fontSize = 11.sp
        )
    }

    Spacer(modifier = Modifier.height(16.dp))

    // SOCIAL MEDIA SHARING SECTION
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CarbonCardElevated)
            .border(1.dp, CarbonBorder, RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "SHARE VEHICLE FINDING",
                color = NitroCyan,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Icon(
                imageVector = Icons.Default.Share,
                contentDescription = null,
                tint = NitroCyan,
                modifier = Modifier.size(14.dp)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Pre-populated share preview text
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Black.copy(alpha = 0.5f))
                .padding(8.dp)
        ) {
            Text(
                text = "\"I just spotted a ${car.make} ${car.model} with CarSpotter! #CarSpotter #CarSpotting\"",
                color = TextSecondary,
                fontSize = 11.sp,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Social Platform Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Twitter / X
            SocialPlatformButton(
                platformName = "X / Twitter",
                containerColor = Color(0xFF1D9BF0),
                contentColor = Color.White,
                onClick = { SocialShareUtil.shareToTwitter(context, car) },
                modifier = Modifier.weight(1f),
                testTag = "share_twitter_button"
            )

            // Instagram
            SocialPlatformButton(
                platformName = "Instagram",
                containerColor = Color(0xFFE1306C),
                contentColor = Color.White,
                onClick = { SocialShareUtil.shareToInstagram(context, car) },
                modifier = Modifier.weight(1f),
                testTag = "share_instagram_button"
            )

            // Facebook
            SocialPlatformButton(
                platformName = "Facebook",
                containerColor = Color(0xFF1877F2),
                contentColor = Color.White,
                onClick = { SocialShareUtil.shareToFacebook(context, car) },
                modifier = Modifier.weight(1f),
                testTag = "share_facebook_button"
            )

            // System All
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(CarbonCard)
                    .border(1.dp, CarbonBorder, RoundedCornerShape(8.dp))
                    .clickable { SocialShareUtil.shareGeneric(context, car) }
                    .testTag("share_generic_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Public,
                    contentDescription = "Share to other apps",
                    tint = TextPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Bottom Action: Add to Garage
    Button(
        onClick = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag("collect_trophy_button"),
        colors = ButtonDefaults.buttonColors(
            containerColor = TurboAmber,
            contentColor = Color.Black
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("Add to Trophy Garage", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
    }
}

@Composable
fun SocialPlatformButton(
    platformName: String,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String
) {
    Box(
        modifier = modifier
            .height(38.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(containerColor)
            .clickable(onClick = onClick)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = platformName,
            color = contentColor,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun DiagnosticOrRejectionCard(
    status: RecognitionStatus,
    verdict: String,
    details: String,
    flags: List<String>,
    capturedBitmap: Bitmap? = null,
    onRetry: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val isConnectivityOrServiceIssue =
        status == RecognitionStatus.NETWORK_OR_TIMEOUT || status == RecognitionStatus.AI_SERVICE_ERROR
    val accentColor = if (isConnectivityOrServiceIssue) TurboAmber else ApexCrimson

    val headerLabel = when (status) {
        RecognitionStatus.REJECTED_CHEAT -> "ANTI-CHEAT REJECTION"
        RecognitionStatus.NO_VEHICLE -> "NO VEHICLE DETECTED"
        RecognitionStatus.NETWORK_OR_TIMEOUT -> "CONNECTION / TIMEOUT"
        RecognitionStatus.AI_SERVICE_ERROR -> "AI SERVICE DIAGNOSTIC"
        RecognitionStatus.SUCCESS -> ""
    }

    val headerIcon = when (status) {
        RecognitionStatus.NETWORK_OR_TIMEOUT -> Icons.Default.WifiOff
        RecognitionStatus.NO_VEHICLE -> Icons.Default.DirectionsCar
        else -> Icons.Default.Error
    }

    Icon(
        imageVector = headerIcon,
        contentDescription = null,
        tint = accentColor,
        modifier = Modifier.size(54.dp)
    )
    Spacer(modifier = Modifier.height(10.dp))
    Text(
        text = headerLabel,
        color = accentColor,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 14.sp,
        letterSpacing = 1.sp
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text = verdict,
        color = TextPrimary,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(10.dp))
    Text(
        text = details,
        color = TextSecondary,
        fontSize = 12.sp,
        textAlign = TextAlign.Center,
        lineHeight = 17.sp
    )

    if (capturedBitmap != null) {
        Spacer(modifier = Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .size(130.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(1.5.dp, accentColor.copy(alpha = 0.8f), RoundedCornerShape(12.dp))
        ) {
            Image(
                bitmap = capturedBitmap.asImageBitmap(),
                contentDescription = "Captured Photo",
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop
            )
        }
    }

    Spacer(modifier = Modifier.height(14.dp))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CarbonCard)
            .border(1.dp, CarbonBorder, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Text(
            text = if (isConnectivityOrServiceIssue) "DIAGNOSTIC DETAILS:" else "DETECTED SIGNALS:",
            color = TextMuted,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
        flags.forEach { flag ->
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "• $flag",
                color = accentColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }

    Spacer(modifier = Modifier.height(20.dp))

    if (isConnectivityOrServiceIssue && onRetry != null) {
        Button(
            onClick = onRetry,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("retry_analysis_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = TurboAmber,
                contentColor = Color.Black
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Retry Identification", fontWeight = FontWeight.ExtraBold)
        }
        Spacer(modifier = Modifier.height(10.dp))
    }

    OutlinedButton(
        onClick = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag("dismiss_cheat_button"),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = when (status) {
                RecognitionStatus.REJECTED_CHEAT -> "Try Authentic Sighting"
                RecognitionStatus.NO_VEHICLE -> "Point Camera at a Car"
                else -> "Retake Photo"
            },
            color = TextPrimary
        )
    }
}

@Composable
fun SpecBadge(
    icon: ImageVector,
    title: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(CarbonCard)
            .border(1.dp, CarbonBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = title,
                    color = TextMuted,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

