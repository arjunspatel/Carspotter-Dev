package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.model.CarSpot
import com.example.model.FriendActivity
import com.example.model.RarityTier
import com.example.model.SpotterProfile
import com.example.ui.CarSpotterViewModel
import com.example.ui.TabDestination
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
import com.example.ui.theme.WarmGoldSoft

private data class BattleOpponent(
    val username: String,
    val topCar: String,
    val horsepower: Int,
    val topSpeedMph: Int
)

@Composable
fun FriendsScreen(
    viewModel: CarSpotterViewModel
) {
    val addedFriends by viewModel.addedFriends.collectAsState()
    val friendsFeed by viewModel.friendsFeed.collectAsState()
    val allSpots by viewModel.allSpots.collectAsState()
    var battleOpponent by remember { mutableStateOf<BattleOpponent?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(PureWhite)
            .testTag("friends_screen"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "SOCIAL FEED",
                        color = CobaltAccent,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.4.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Friends Garage.",
                        fontFamily = OutfitDisplayFont,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = InkPrimary,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = "Real friend sightings and garage head-to-head battles",
                        fontSize = 12.sp,
                        color = InkSecondary
                    )
                }
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(PorcelainSurface)
                        .border(1.dp, HairlineBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.People,
                        contentDescription = "Friends",
                        tint = InkPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Quick Battle Editorial Banner
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("garage_battle_banner"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = PorcelainSurface),
                border = BorderStroke(1.dp, HairlineBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.ElectricBolt,
                                contentDescription = null,
                                tint = WarmGold,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "GARAGE BATTLES",
                                color = WarmGold,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (addedFriends.isNotEmpty()) {
                                "Challenge a friend to compare your real top horsepower and speed."
                            } else {
                                "Add fellow spotters in Community to compare garages head-to-head."
                            },
                            color = InkSecondary,
                            fontSize = 12.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    if (addedFriends.isNotEmpty()) {
                        val topFriend = addedFriends.first()
                        Button(
                            onClick = {
                                battleOpponent = BattleOpponent(
                                    username = topFriend.username,
                                    topCar = topFriend.topCar.ifBlank { "No Spots Yet" },
                                    horsepower = topFriend.topHorsepower,
                                    topSpeedMph = topFriend.topSpeedMph
                                )
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = InkPrimary,
                                contentColor = PureWhite
                            ),
                            shape = RoundedCornerShape(999.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 9.dp)
                        ) {
                            Text("Quick Battle", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Button(
                            onClick = { viewModel.setTab(TabDestination.COMMUNITY) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = InkPrimary,
                                contentColor = PureWhite
                            ),
                            shape = RoundedCornerShape(999.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 9.dp),
                            modifier = Modifier.testTag("find_friends_button")
                        ) {
                            Text("Find Friends", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        if (addedFriends.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                        .testTag("friends_empty_state"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = PorcelainSurface),
                    border = BorderStroke(1.dp, HairlineBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(PureWhite)
                                .border(1.dp, HairlineBorder, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PersonSearch,
                                contentDescription = null,
                                tint = CobaltAccent,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "No friends connected yet",
                            fontFamily = OutfitDisplayFont,
                            color = InkPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Search real CarSpotter accounts in the Community tab to follow their spotted cars and battle garages.",
                            color = InkSecondary,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedButton(
                            onClick = { viewModel.setTab(TabDestination.COMMUNITY) },
                            shape = RoundedCornerShape(999.dp),
                            border = BorderStroke(1.dp, HairlineBorder)
                        ) {
                            Text("Browse Community", color = InkPrimary, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        } else {
            item {
                Text(
                    text = "YOUR FRIENDS (${addedFriends.size})",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = InkMuted,
                    letterSpacing = 1.3.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            items(addedFriends, key = { it.userId }) { friend ->
                FriendRosterCard(
                    friend = friend,
                    onBattleClick = {
                        battleOpponent = BattleOpponent(
                            username = friend.username,
                            topCar = friend.topCar.ifBlank { "No Spots Yet" },
                            horsepower = friend.topHorsepower,
                            topSpeedMph = friend.topSpeedMph
                        )
                    },
                    onRemoveClick = { viewModel.toggleFriend(friend.userId) }
                )
            }

            item {
                Text(
                    text = "RECENT FRIEND SIGHTINGS (${friendsFeed.size})",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = InkMuted,
                    letterSpacing = 1.3.sp,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            if (friendsFeed.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(PorcelainSurface)
                            .border(1.dp, HairlineBorder, RoundedCornerShape(16.dp))
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Your friends haven't snapped a car yet. Check back soon!",
                            color = InkSecondary,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                items(friendsFeed, key = { it.id }) { activity ->
                    FriendSpotCard(
                        activity = activity,
                        onBattleClick = {
                            battleOpponent = BattleOpponent(
                                username = activity.username,
                                topCar = activity.friendTopCar.ifBlank { activity.carName },
                                horsepower = activity.friendTopHorsepower,
                                topSpeedMph = activity.friendTopSpeedMph
                            )
                        }
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(30.dp))
        }
    }

    if (battleOpponent != null) {
        val userBestCar = allSpots.maxByOrNull { it.horsepower }
        GarageBattleDialog(
            opponent = battleOpponent!!,
            userBestCar = userBestCar,
            onDismiss = { battleOpponent = null }
        )
    }
}

@Composable
private fun FriendRosterCard(
    friend: SpotterProfile,
    onBattleClick: () -> Unit,
    onRemoveClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("friend_roster_${friend.userId}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        border = BorderStroke(1.dp, HairlineBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                if (!friend.avatarUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = friend.avatarUrl,
                        contentDescription = friend.username,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(CobaltSoft)
                            .border(1.dp, CobaltAccent.copy(alpha = 0.25f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = friend.username.take(1).uppercase(),
                            fontFamily = OutfitDisplayFont,
                            color = CobaltAccent,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = friend.username,
                        color = InkPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${friend.totalScore} pts • ${friend.totalSpotsCount} cars",
                        color = InkMuted,
                        fontSize = 11.sp
                    )
                    if (friend.topCar.isNotBlank()) {
                        Text(
                            text = "Best: ${friend.topCar}",
                            color = CobaltAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(
                    onClick = onBattleClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = InkPrimary,
                        contentColor = PureWhite
                    ),
                    shape = RoundedCornerShape(999.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("Battle", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = onRemoveClick,
                    shape = RoundedCornerShape(999.dp),
                    border = BorderStroke(1.dp, HairlineBorder),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text("Remove", color = InkMuted, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun FriendSpotCard(
    activity: FriendActivity,
    onBattleClick: () -> Unit
) {
    val tierColor = Color(activity.rarityTier.colorHex)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("friend_spot_${activity.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        border = BorderStroke(1.dp, HairlineBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!activity.avatarUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = activity.avatarUrl,
                            contentDescription = activity.username,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(CobaltSoft)
                                .border(1.dp, CobaltAccent.copy(alpha = 0.25f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = activity.username.take(1).uppercase(),
                                fontFamily = OutfitDisplayFont,
                                color = CobaltAccent,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = activity.username,
                                color = InkPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (activity.isVerified) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Verified Sighting",
                                    tint = CobaltAccent,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                        Text(
                            text = "${activity.handle} • ${activity.timeAgo}",
                            color = InkMuted,
                            fontSize = 11.sp
                        )
                    }
                }

                Button(
                    onClick = onBattleClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PorcelainSurface,
                        contentColor = InkPrimary
                    ),
                    shape = RoundedCornerShape(999.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    border = BorderStroke(1.dp, HairlineBorder)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.CompareArrows,
                        contentDescription = null,
                        tint = CobaltAccent,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Battle", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(PorcelainSurface)
                    .border(1.dp, HairlineBorder, RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = activity.carName,
                        fontFamily = OutfitDisplayFont,
                        color = InkPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = activity.location,
                        color = InkMuted,
                        fontSize = 11.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(tierColor.copy(alpha = 0.12f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = activity.rarityTier.label.uppercase(),
                            color = tierColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(WarmGoldSoft)
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "+${activity.pointsEarned} pts",
                            color = WarmGold,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GarageBattleDialog(
    opponent: BattleOpponent,
    userBestCar: CarSpot?,
    onDismiss: () -> Unit
) {
    val myHp = userBestCar?.horsepower ?: 0
    val friendHp = opponent.horsepower
    val winnerLabel = when {
        myHp > friendHp -> "Your Garage Takes the Lead"
        friendHp > myHp -> "${opponent.username} Leads on Horsepower"
        myHp == 0 && friendHp == 0 -> "Spot a car to set your benchmark"
        else -> "Dead-Even Horsepower Tie"
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("garage_battle_modal"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = PureWhite),
            border = BorderStroke(1.dp, HairlineBorder),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(CobaltSoft),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.CompareArrows,
                        contentDescription = null,
                        tint = CobaltAccent,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Garage Battle Showdown",
                    fontFamily = OutfitDisplayFont,
                    color = InkPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = winnerLabel,
                    color = CobaltAccent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(thickness = 1.dp, color = HairlineBorder)
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "YOUR GARAGE",
                            color = CobaltAccent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = userBestCar?.let { "${it.make} ${it.model}" } ?: "No Spot Yet",
                            color = InkPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "$myHp HP",
                            fontFamily = OutfitDisplayFont,
                            color = InkPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${userBestCar?.topSpeedMph ?: 0} MPH",
                            color = InkSecondary,
                            fontSize = 11.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(PorcelainSurface)
                            .border(1.dp, HairlineBorder, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "VS",
                            color = RacingRed,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = opponent.username.uppercase(),
                            color = WarmGold,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = opponent.topCar,
                            color = InkPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "$friendHp HP",
                            fontFamily = OutfitDisplayFont,
                            color = WarmGold,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${opponent.topSpeedMph} MPH",
                            color = InkSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = InkPrimary,
                        contentColor = PureWhite
                    ),
                    shape = RoundedCornerShape(999.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                ) {
                    Text("Close Battle Result", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
