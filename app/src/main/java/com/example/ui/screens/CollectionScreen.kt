package com.example.ui.screens

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CarSpot
import com.example.model.RarityTier
import com.example.ui.CarSpotterViewModel
import com.example.ui.SortOrder
import com.example.ui.components.CarSpotImage
import com.example.ui.theme.CobaltAccent
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.InkMuted
import com.example.ui.theme.InkPrimary
import com.example.ui.theme.InkSecondary
import com.example.ui.theme.MistSurface
import com.example.ui.theme.OutfitDisplayFont
import com.example.ui.theme.PorcelainSurface
import com.example.ui.theme.PureWhite
import com.example.ui.theme.RacingRed
import com.example.ui.theme.WarmGold
import com.example.ui.theme.WarmGoldSoft
import com.example.util.SocialShareUtil
import java.text.NumberFormat
import java.util.Locale

@Composable
fun CollectionScreen(
    viewModel: CarSpotterViewModel
) {
    val context = LocalContext.current
    val allSpots by viewModel.allSpots.collectAsState()
    val filteredSpots by viewModel.filteredSpots.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedBrand by viewModel.selectedBrandFilter.collectAsState()
    val selectedTier by viewModel.selectedTierFilter.collectAsState()
    val selectedSort by viewModel.selectedSortOrder.collectAsState()

    var showSortMenu by remember { mutableStateOf(false) }

    val brands = listOf("All", "Porsche", "Ferrari", "McLaren", "Lamborghini", "BMW", "Chevrolet", "Nissan", "Ford", "Bugatti")

    val totalSpots = allSpots.size
    val totalPoints = allSpots.sumOf { it.pointsAwarded }
    val totalValue = allSpots.sumOf { it.estimatedPriceUsd }
    val formattedTotalValue = NumberFormat.getCurrencyInstance(Locale.US).apply {
        maximumFractionDigits = 0
    }.format(totalValue)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(PureWhite)
            .testTag("collection_screen"),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Website-Style Graphic Editorial Hero Section
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(PorcelainSurface)
                    .border(1.dp, HairlineBorder, RoundedCornerShape(22.dp))
            ) {
                // Subtle graphic architectural grid lines
                Canvas(modifier = Modifier.matchParentSize()) {
                    val step = 36.dp.toPx()
                    var x = step
                    while (x < size.width) {
                        drawLine(
                            color = HairlineBorder.copy(alpha = 0.55f),
                            start = Offset(x, 0f),
                            end = Offset(x, size.height),
                            strokeWidth = 1f
                        )
                        x += step
                    }
                    drawCircle(
                        color = CobaltAccent.copy(alpha = 0.06f),
                        radius = size.width * 0.34f,
                        center = Offset(size.width * 0.92f, size.height * 0.12f)
                    )
                }

                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "CURATED GARAGE",
                                color = CobaltAccent,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.4.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Your Collection.",
                                fontFamily = OutfitDisplayFont,
                                color = InkPrimary,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.5).sp
                            )
                            Text(
                                text = "$totalSpots authenticated real-world sightings",
                                color = InkSecondary,
                                fontSize = 12.sp
                            )
                        }

                        // Clean Portfolio Value Pill
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(PureWhite)
                                .border(1.dp, HairlineBorder, RoundedCornerShape(14.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "EST. VALUE",
                                    color = InkMuted,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp
                                )
                                Text(
                                    text = formattedTotalValue,
                                    fontFamily = OutfitDisplayFont,
                                    color = InkPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(thickness = 1.dp, color = HairlineBorder)
                    Spacer(modifier = Modifier.height(14.dp))

                    // Editorial 3-column metrics strip
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        GarageStatItem(
                            title = "TOTAL POINTS",
                            value = "$totalPoints PTS",
                            accentColor = WarmGold,
                            modifier = Modifier.weight(1f)
                        )
                        GarageStatItem(
                            title = "RAREST MODEL",
                            value = allSpots.maxByOrNull { it.rarityScore }?.model ?: "None",
                            accentColor = CobaltAccent,
                            modifier = Modifier.weight(1f)
                        )
                        GarageStatItem(
                            title = "PEAK SPEED",
                            value = "${allSpots.maxOfOrNull { it.topSpeedMph } ?: 0} MPH",
                            accentColor = RacingRed,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Clean Website Search Bar & Sort Button
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("collection_search_bar"),
                    placeholder = { Text("Search make, model, city...", color = InkMuted, fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = InkMuted)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear", tint = InkMuted)
                            }
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = InkPrimary,
                        unfocusedBorderColor = HairlineBorder,
                        focusedContainerColor = PorcelainSurface,
                        unfocusedContainerColor = PorcelainSurface,
                        focusedTextColor = InkPrimary,
                        unfocusedTextColor = InkPrimary
                    ),
                    shape = RoundedCornerShape(14.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Box {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(PureWhite)
                            .border(1.dp, HairlineBorder, RoundedCornerShape(14.dp))
                            .clickable { showSortMenu = true }
                            .testTag("sort_filter_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "Sort",
                            tint = InkPrimary
                        )
                    }

                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false },
                        modifier = Modifier.background(PureWhite)
                    ) {
                        SortOrder.values().forEach { order ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = order.label,
                                        color = if (selectedSort == order) CobaltAccent else InkPrimary,
                                        fontWeight = if (selectedSort == order) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 13.sp
                                    )
                                },
                                onClick = {
                                    viewModel.setSortOrder(order)
                                    showSortMenu = false
                                }
                            )
                        }
                    }
                }
            }
        }

        // Brand Filter Pills
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    brands.forEach { brand ->
                        val isSelected = selectedBrand == brand
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(if (isSelected) InkPrimary else PureWhite)
                                .border(
                                    1.dp,
                                    if (isSelected) InkPrimary else HairlineBorder,
                                    RoundedCornerShape(999.dp)
                                )
                                .clickable { viewModel.setBrandFilter(brand) }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .testTag("filter_brand_$brand")
                        ) {
                            Text(
                                text = brand,
                                color = if (isSelected) PureWhite else InkSecondary,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }

                // Rarity Tier Filter Pills
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val isAll = selectedTier == null
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(if (isAll) CobaltAccent else PorcelainSurface)
                            .border(1.dp, if (isAll) CobaltAccent else HairlineBorder, RoundedCornerShape(999.dp))
                            .clickable { viewModel.setTierFilter(null) }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                            .testTag("filter_tier_all")
                    ) {
                        Text(
                            text = "All Ranks",
                            color = if (isAll) PureWhite else InkSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    RarityTier.values().forEach { tier ->
                        val isSelected = selectedTier == tier
                        val tierColor = Color(tier.colorHex)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(if (isSelected) tierColor else PureWhite)
                                .border(1.dp, if (isSelected) tierColor else HairlineBorder, RoundedCornerShape(999.dp))
                                .clickable { viewModel.setTierFilter(tier) }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                                .testTag("filter_tier_${tier.name}")
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
        }

        // Vehicles List or Editorial Empty State
        if (filteredSpots.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 48.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(PorcelainSurface)
                        .border(1.dp, HairlineBorder, RoundedCornerShape(20.dp))
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(PureWhite)
                                .border(1.dp, HairlineBorder, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DirectionsCar,
                                contentDescription = null,
                                tint = InkMuted,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "No vehicles found",
                            fontFamily = OutfitDisplayFont,
                            color = InkPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Open the Spot camera tab to photograph a real car and add it to your white gallery.",
                            color = InkSecondary,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(filteredSpots, key = { it.id }) { car ->
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    CarCollectionCard(
                        car = car,
                        onClick = { viewModel.inspectCar(car) },
                        onShare = { SocialShareUtil.shareGeneric(context, car) }
                    )
                }
            }
        }
    }
}

@Composable
fun CarCollectionCard(
    car: CarSpot,
    onClick: () -> Unit,
    onShare: () -> Unit
) {
    val tierColor = Color(car.rarityTier.colorHex)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("car_card_${car.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        border = BorderStroke(1.dp, HairlineBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // Crisp Full-Bleed Vehicle Photo Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(185.dp)
                    .background(PorcelainSurface)
            ) {
                CarSpotImage(
                    car = car,
                    modifier = Modifier.fillMaxSize()
                )

                // Subtle bottom readability fade only
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.25f)
                                )
                            )
                        )
                )

                // Crisp White Rarity Tag Top Left
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(PureWhite.copy(alpha = 0.94f))
                        .border(1.dp, HairlineBorder, RoundedCornerShape(999.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = car.rarityTier.label.uppercase(),
                        color = tierColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        letterSpacing = 0.5.sp
                    )
                }

                // Points Pill Top Right
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(InkPrimary.copy(alpha = 0.9f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "+${car.pointsAwarded} PTS",
                        color = PureWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }

                // Clean White Floating Share Button
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp)
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(PureWhite.copy(alpha = 0.95f))
                        .border(1.dp, HairlineBorder, CircleShape)
                        .clickable(onClick = onShare),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = InkPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Editorial White Details Block
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PureWhite)
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${car.year} ${car.make} ${car.model}",
                            fontFamily = OutfitDisplayFont,
                            color = InkPrimary,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.3).sp
                        )
                        Text(
                            text = car.trim,
                            color = InkSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(WarmGoldSoft)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${car.rarityScore}/100",
                            color = WarmGold,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Clean Specs Pills
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MiniSpecPill(
                        icon = Icons.Default.ElectricBolt,
                        text = "${car.horsepower} HP",
                        iconColor = WarmGold
                    )
                    MiniSpecPill(
                        icon = Icons.Default.Speed,
                        text = "${car.zeroToSixtyMph}s 0-60",
                        iconColor = CobaltAccent
                    )
                    MiniSpecPill(
                        icon = Icons.Default.Speed,
                        text = "${car.topSpeedMph} MPH",
                        iconColor = RacingRed
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Location Stamp
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = InkMuted,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = car.locationName,
                        color = InkMuted,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
fun MiniSpecPill(
    icon: ImageVector,
    text: String,
    iconColor: Color
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(PorcelainSurface)
            .border(1.dp, HairlineBorder, RoundedCornerShape(8.dp))
            .padding(horizontal = 9.dp, vertical = 5.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = text,
                color = InkPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun GarageStatItem(
    title: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = title,
            color = InkMuted,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = value,
            fontFamily = OutfitDisplayFont,
            color = accentColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
