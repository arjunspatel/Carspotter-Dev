package com.example.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.model.CarSpot
import com.example.model.RarityTier
import com.example.ui.CarSpotterViewModel
import com.example.ui.SortOrder
import com.example.ui.theme.CarbonBorder
import com.example.ui.theme.CarbonCard
import com.example.ui.theme.CarbonCardElevated
import com.example.ui.theme.CarbonDark
import com.example.ui.theme.GoldenTrophy
import com.example.ui.theme.NitroCyan
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TurboAmber
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

    // Stats calculations
    val totalSpots = allSpots.size
    val totalPoints = allSpots.sumOf { it.pointsAwarded }
    val totalValue = allSpots.sumOf { it.estimatedPriceUsd }
    val formattedTotalValue = NumberFormat.getCurrencyInstance(Locale.US).apply {
        maximumFractionDigits = 0
    }.format(totalValue)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CarbonDark)
            .testTag("collection_screen")
    ) {
        // Top Garage Header Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(CarbonCardElevated, CarbonCard)
                    )
                )
                .border(1.dp, CarbonBorder, RoundedCornerShape(20.dp))
                .padding(16.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "TROPHY GARAGE",
                            color = TurboAmber,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "$totalSpots Vehicles Spotted",
                            color = TextPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Garage Value Pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(GoldenTrophy.copy(alpha = 0.15f))
                            .border(1.dp, GoldenTrophy.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "EST. VALUE",
                                color = GoldenTrophy,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = formattedTotalValue,
                                color = GoldenTrophy,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Stats row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    GarageStatItem(
                        title = "TOTAL POINTS",
                        value = "$totalPoints PTS",
                        color = TurboAmber
                    )
                    GarageStatItem(
                        title = "RAREST SPOT",
                        value = allSpots.maxByOrNull { it.rarityScore }?.model ?: "None",
                        color = NitroCyan
                    )
                    GarageStatItem(
                        title = "MAX SPEED",
                        value = "${allSpots.maxOfOrNull { it.topSpeedMph } ?: 0} MPH",
                        color = Color(0xFFFF1744)
                    )
                }
            }
        }

        // Search Bar & Sort Button
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
                placeholder = { Text("Search make, model, city...", color = TextMuted, fontSize = 12.sp) },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = TextMuted)
                },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear", tint = TextMuted)
                        }
                    }
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TurboAmber,
                    unfocusedBorderColor = CarbonBorder,
                    focusedContainerColor = CarbonCard,
                    unfocusedContainerColor = CarbonCard,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                shape = RoundedCornerShape(14.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Sort dropdown trigger
            Box {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(CarbonCard)
                        .border(1.dp, CarbonBorder, RoundedCornerShape(14.dp))
                        .clickable { showSortMenu = true }
                        .testTag("sort_filter_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = "Sort",
                        tint = NitroCyan
                    )
                }

                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { showSortMenu = false },
                    modifier = Modifier.background(CarbonCard)
                ) {
                    SortOrder.values().forEach { order ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = order.label,
                                    color = if (selectedSort == order) TurboAmber else TextPrimary,
                                    fontSize = 12.sp
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

        Spacer(modifier = Modifier.height(10.dp))

        // Horizontal Brand Filter Chips
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
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isSelected) TurboAmber else CarbonCard)
                        .border(
                            1.dp,
                            if (isSelected) TurboAmber else CarbonBorder,
                            RoundedCornerShape(20.dp)
                        )
                        .clickable { viewModel.setBrandFilter(brand) }
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                        .testTag("filter_brand_$brand")
                ) {
                    Text(
                        text = brand,
                        color = if (isSelected) Color.Black else TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Horizontal Rarity Tier Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // All Tiers chip
            val isAll = selectedTier == null
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isAll) NitroCyan else CarbonCard)
                    .border(1.dp, if (isAll) NitroCyan else CarbonBorder, RoundedCornerShape(16.dp))
                    .clickable { viewModel.setTierFilter(null) }
                    .padding(horizontal = 12.dp, vertical = 5.dp)
                    .testTag("filter_tier_all")
            ) {
                Text(
                    text = "All Ranks",
                    color = if (isAll) Color.Black else TextSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            RarityTier.values().forEach { tier ->
                val isSelected = selectedTier == tier
                val tierColor = Color(tier.colorHex)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSelected) tierColor else CarbonCard)
                        .border(1.dp, if (isSelected) tierColor else CarbonBorder, RoundedCornerShape(16.dp))
                        .clickable { viewModel.setTierFilter(tier) }
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                        .testTag("filter_tier_${tier.name}")
                ) {
                    Text(
                        text = tier.label,
                        color = if (isSelected) Color.Black else tierColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Cars List or Empty State
        if (filteredSpots.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.DirectionsCar,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No vehicles found in garage",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Snap new cars with the camera tab to build your trophy collection!",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredSpots, key = { it.id }) { car ->
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
            .clip(RoundedCornerShape(18.dp))
            .border(1.dp, CarbonBorder, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .testTag("car_card_${car.id}"),
        colors = CardDefaults.cardColors(containerColor = CarbonCard)
    ) {
        Column {
            // Photo Banner with Floating Badges
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            ) {
                when {
                    car.photoUri == "res:car_porsche" -> {
                        Image(
                            painter = painterResource(id = R.drawable.car_porsche),
                            contentDescription = car.model,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    car.photoUri == "res:car_ferrari" -> {
                        Image(
                            painter = painterResource(id = R.drawable.car_ferrari),
                            contentDescription = car.model,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    car.photoUri != null -> {
                        AsyncImage(
                            model = car.photoUri,
                            contentDescription = car.model,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    else -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(CarbonCardElevated),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DirectionsCar,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }
                }

                // Gradient Vignette
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.5f),
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.8f)
                                )
                            )
                        )
                )

                // Rarity Tag Top Left
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.75f))
                        .border(1.dp, tierColor, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = car.rarityTier.label.uppercase(),
                        color = tierColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                }

                // Points Badge Top Right
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(tierColor.copy(alpha = 0.25f))
                        .border(1.dp, tierColor, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "+${car.pointsAwarded} PTS",
                        color = tierColor,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.sp
                    )
                }

                // Quick Share Icon Button Floating Bottom Right
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(10.dp)
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.7f))
                        .border(1.dp, NitroCyan, CircleShape)
                        .clickable(onClick = onShare),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = NitroCyan,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Info Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "${car.year} ${car.make} ${car.model}",
                            color = TextPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = car.trim,
                            color = NitroCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Text(
                        text = "${car.rarityScore}/100",
                        color = tierColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Specs Pill Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MiniSpecPill(
                        icon = Icons.Default.ElectricBolt,
                        text = "${car.horsepower} HP",
                        color = TurboAmber
                    )
                    MiniSpecPill(
                        icon = Icons.Default.Speed,
                        text = "${car.zeroToSixtyMph}s 0-60",
                        color = NitroCyan
                    )
                    MiniSpecPill(
                        icon = Icons.Default.Speed,
                        text = "${car.topSpeedMph} MPH",
                        color = Color(0xFFFF1744)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Location stamp
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = car.locationName,
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
fun MiniSpecPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    color: Color
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(CarbonCardElevated)
            .border(1.dp, CarbonBorder, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = text,
                color = TextPrimary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun GarageStatItem(
    title: String,
    value: String,
    color: Color
) {
    Column {
        Text(
            text = title,
            color = TextMuted,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            color = color,
            fontSize = 13.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}
