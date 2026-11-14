package com.example.ui.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CarSpot
import com.example.ui.theme.CobaltAccent
import com.example.ui.theme.CobaltSoft
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.InkMuted
import com.example.ui.theme.InkPrimary
import com.example.ui.theme.InkSecondary
import com.example.ui.theme.OutfitDisplayFont
import com.example.ui.theme.PorcelainSurface
import com.example.ui.theme.PureWhite
import com.example.ui.theme.RacingRed
import com.example.ui.theme.WarmGold
import com.example.util.SocialShareUtil
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CarDetailSheet(
    car: CarSpot,
    onDismiss: () -> Unit,
    onDelete: (CarSpot) -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val tierColor = Color(car.rarityTier.colorHex)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = PureWhite,
        tonalElevation = 0.dp,
        modifier = Modifier.testTag("car_detail_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(tierColor.copy(alpha = 0.12f))
                        .border(1.dp, tierColor.copy(alpha = 0.35f), RoundedCornerShape(999.dp))
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "${car.rarityTier.label.uppercase()} • ${car.rarityScore}/100",
                        color = tierColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("close_detail_sheet")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = InkSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Car Image
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(PorcelainSurface)
                    .border(1.dp, HairlineBorder, RoundedCornerShape(18.dp))
            ) {
                CarSpotImage(
                    car = car,
                    modifier = Modifier.matchParentSize()
                )

                // Points Pill
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(InkPrimary.copy(alpha = 0.92f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "+${car.pointsAwarded} PTS",
                        color = PureWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Title & Trim
            Text(
                text = "${car.year} ${car.make} ${car.model}",
                fontFamily = OutfitDisplayFont,
                color = InkPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.3).sp
            )
            Text(
                text = car.trim,
                color = CobaltAccent,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Date & Location
            val dateFormatted = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault())
                .format(Date(car.timestamp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = CobaltAccent,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${car.locationName} • $dateFormatted",
                    color = InkSecondary,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Anti-cheat verification (Cobalt editorial instead of neon green)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CobaltSoft)
                    .border(1.dp, CobaltAccent.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.VerifiedUser,
                    contentDescription = null,
                    tint = CobaltAccent,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Authenticity Certified (Confidence: ${(car.antiCheatConfidence * 100).toInt()}%)",
                    color = CobaltAccent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Specs Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SpecBadge(
                    icon = Icons.Default.ElectricBolt,
                    title = "HORSEPOWER",
                    value = "${car.horsepower} HP",
                    accentColor = WarmGold,
                    modifier = Modifier.weight(1f)
                )
                SpecBadge(
                    icon = Icons.Default.Speed,
                    title = "0-60 MPH",
                    value = "${car.zeroToSixtyMph}s",
                    accentColor = CobaltAccent,
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
                    accentColor = RacingRed,
                    modifier = Modifier.weight(1f)
                )
                val formattedPrice = NumberFormat.getCurrencyInstance(Locale.US).apply {
                    maximumFractionDigits = 0
                }.format(car.estimatedPriceUsd)
                SpecBadge(
                    icon = Icons.Default.AttachMoney,
                    title = "EST. MSRP",
                    value = formattedPrice,
                    accentColor = InkPrimary,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Engine & Trivia
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(PorcelainSurface)
                    .border(1.dp, HairlineBorder, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Text(
                    text = "ENGINE SPECIFICATION",
                    color = InkMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = car.engineType,
                    color = InkPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = car.drivetrain,
                    color = InkSecondary,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = car.description,
                    color = InkSecondary,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Trivia: ${car.trivia}",
                    color = CobaltAccent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Social Sharing Buttons
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(PorcelainSurface)
                    .border(1.dp, HairlineBorder, RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Text(
                    text = "SHARE TROPHY",
                    color = InkMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SocialPlatformButton(
                        platformName = "X / Twitter",
                        containerColor = Color(0xFF0F172A),
                        contentColor = Color.White,
                        onClick = { SocialShareUtil.shareToTwitter(context, car) },
                        modifier = Modifier.weight(1f),
                        testTag = "detail_share_twitter"
                    )
                    SocialPlatformButton(
                        platformName = "Instagram",
                        containerColor = Color(0xFFE1306C),
                        contentColor = Color.White,
                        onClick = { SocialShareUtil.shareToInstagram(context, car) },
                        modifier = Modifier.weight(1f),
                        testTag = "detail_share_instagram"
                    )
                    SocialPlatformButton(
                        platformName = "Facebook",
                        containerColor = Color(0xFF1877F2),
                        contentColor = Color.White,
                        onClick = { SocialShareUtil.shareToFacebook(context, car) },
                        modifier = Modifier.weight(1f),
                        testTag = "detail_share_facebook"
                    )
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(PureWhite)
                            .border(1.dp, HairlineBorder, RoundedCornerShape(8.dp))
                            .clickable { SocialShareUtil.shareGeneric(context, car) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Public,
                            contentDescription = "Share",
                            tint = InkPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Delete button
            OutlinedButton(
                onClick = { onDelete(car) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("detail_delete_button"),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = RacingRed),
                border = BorderStroke(1.dp, RacingRed.copy(alpha = 0.45f)),
                shape = RoundedCornerShape(999.dp)
            ) {
                Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Remove from Garage", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
