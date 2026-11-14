package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.LeaderboardEntry
import com.example.model.PlayerSearchResult
import com.example.model.UserProfile
import com.example.ui.CarSpotterViewModel
import com.example.ui.theme.CobaltAccent
import com.example.ui.theme.CobaltSoft
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.InkMuted
import com.example.ui.theme.InkPrimary
import com.example.ui.theme.InkSecondary
import com.example.ui.theme.MistSurface
import com.example.ui.theme.OutfitDisplayFont
import com.example.ui.theme.PorcelainSurface
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SlateAccent
import com.example.ui.theme.WarmGold
import com.example.ui.theme.WarmGoldSoft
import com.google.firebase.auth.FirebaseUser

@Composable
fun CommunityScreen(
    viewModel: CarSpotterViewModel
) {
    val leaderboard by viewModel.leaderboard.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val searchQuery by viewModel.playerSearchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val authUser by viewModel.authUser.collectAsState()
    val isAuthLoading by viewModel.isAuthLoading.collectAsState()

    var isSearchExpanded by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(PureWhite)
            .testTag("community_screen"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Editorial Screen Header with Search Toggle Button
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "GLOBAL STANDINGS",
                        color = CobaltAccent,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.4.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Community.",
                        fontFamily = OutfitDisplayFont,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = InkPrimary,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = "Leaderboards, spotter XP and player discovery",
                        fontSize = 12.sp,
                        color = InkSecondary
                    )
                }

                IconButton(
                    onClick = { isSearchExpanded = !isSearchExpanded },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (isSearchExpanded) InkPrimary else PorcelainSurface)
                        .border(1.dp, if (isSearchExpanded) InkPrimary else HairlineBorder, CircleShape)
                        .testTag("search_players_toggle_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search Players",
                        tint = if (isSearchExpanded) PureWhite else InkPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Search Players Input Bar
        item {
            AnimatedVisibility(visible = isSearchExpanded || searchQuery.isNotBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = PorcelainSurface),
                    border = BorderStroke(1.dp, HairlineBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = CobaltAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "FIND SPOTTERS & FRIENDS",
                                color = CobaltAccent,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.setPlayerSearchQuery(it) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("player_search_input"),
                            placeholder = { Text("Search by name, tag, or car...", color = InkMuted, fontSize = 13.sp) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            trailingIcon = {
                                if (searchQuery.isNotBlank()) {
                                    IconButton(onClick = { viewModel.setPlayerSearchQuery("") }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = InkMuted)
                                    }
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = PureWhite,
                                unfocusedContainerColor = PureWhite,
                                focusedBorderColor = InkPrimary,
                                unfocusedBorderColor = HairlineBorder,
                                focusedTextColor = InkPrimary,
                                unfocusedTextColor = InkPrimary
                            )
                        )
                    }
                }
            }
        }

        // Search Results
        if (searchQuery.isNotBlank()) {
            item {
                Text(
                    text = "SEARCH RESULTS (${searchResults.size})",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = InkMuted,
                    letterSpacing = 1.2.sp
                )
            }

            if (searchResults.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 28.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No players found matching \"$searchQuery\"",
                            color = InkMuted,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                items(searchResults) { player ->
                    PlayerResultCard(
                        player = player,
                        onToggleFriend = { viewModel.toggleFriend(player.id) }
                    )
                }
            }
        }

        // Profile & XP Rank Banner + Leaderboard (when not searching)
        if (searchQuery.isBlank()) {
            item {
                ProfileStatusCard(
                    userProfile = userProfile,
                    authUser = authUser,
                    isLoading = isAuthLoading,
                    onSignIn = { viewModel.signInWithGoogle() },
                    onSignOut = { viewModel.signOut() }
                )
            }

            item {
                Text(
                    text = "GLOBAL PODIUM",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = InkMuted,
                    letterSpacing = 1.4.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            item {
                PodiumSection(leaderboard = leaderboard)
            }

            item {
                Text(
                    text = "ALL RANKED SPOTTERS",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = InkMuted,
                    letterSpacing = 1.4.sp,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            items(leaderboard) { entry ->
                LeaderboardRow(entry = entry)
            }
        }

        item {
            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
private fun PlayerResultCard(
    player: PlayerSearchResult,
    onToggleFriend: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("player_card_${player.id}"),
        shape = RoundedCornerShape(16.dp),
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(CobaltSoft)
                        .border(1.dp, CobaltAccent.copy(alpha = 0.25f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = player.username.take(1).uppercase(),
                        fontFamily = OutfitDisplayFont,
                        color = CobaltAccent,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = player.username,
                        color = InkPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${player.handle} • ${player.rankTitle}",
                        color = InkMuted,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Top: ${player.topCar} • ${player.totalScore} pts",
                        color = CobaltAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = onToggleFriend,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (player.isFriend) CobaltSoft else InkPrimary,
                    contentColor = if (player.isFriend) CobaltAccent else PureWhite
                ),
                shape = RoundedCornerShape(999.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                border = if (player.isFriend) BorderStroke(1.dp, CobaltAccent.copy(alpha = 0.35f)) else null,
                modifier = Modifier.testTag("toggle_friend_${player.id}")
            ) {
                Icon(
                    imageVector = if (player.isFriend) Icons.Default.Check else Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (player.isFriend) "Friend" else "Add",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ProfileStatusCard(
    userProfile: UserProfile,
    authUser: FirebaseUser?,
    isLoading: Boolean,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = PorcelainSurface),
        border = BorderStroke(1.dp, HairlineBorder)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (authUser?.photoUrl != null) {
                        AsyncImage(
                            model = authUser.photoUrl,
                            contentDescription = "Avatar",
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .border(1.5.dp, CobaltAccent, CircleShape)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(PureWhite)
                                .border(1.dp, HairlineBorder, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = InkPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = authUser?.displayName ?: userProfile.username,
                            fontFamily = OutfitDisplayFont,
                            color = InkPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = userProfile.rankTitle,
                            color = CobaltAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                if (authUser == null) {
                    Button(
                        onClick = onSignIn,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = InkPrimary,
                            contentColor = PureWhite
                        ),
                        shape = RoundedCornerShape(999.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 7.dp),
                        enabled = !isLoading,
                        modifier = Modifier.testTag("google_signin_button")
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = PureWhite,
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                Icons.AutoMirrored.Filled.Login,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text("Sign In", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    IconButton(onClick = onSignOut, modifier = Modifier.size(36.dp)) {
                        Icon(
                            Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Sign Out",
                            tint = InkMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val xpProgress = (userProfile.totalScore % 1000) / 1000f
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "LEVEL PROGRESS",
                    color = InkMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "${userProfile.totalScore} TOTAL XP",
                    color = InkPrimary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { xpProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = CobaltAccent,
                trackColor = MistSurface
            )
        }
    }
}

@Composable
private fun PodiumSection(leaderboard: List<LeaderboardEntry>) {
    if (leaderboard.size < 3) return

    val first = leaderboard.getOrNull(0) ?: return
    val second = leaderboard.getOrNull(1) ?: return
    val third = leaderboard.getOrNull(2) ?: return

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(PorcelainSurface)
            .border(1.dp, HairlineBorder, RoundedCornerShape(20.dp))
            .padding(top = 16.dp, start = 12.dp, end = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        PodiumPillar(entry = second, rank = 2, color = SlateAccent, height = 96.dp)
        PodiumPillar(entry = first, rank = 1, color = WarmGold, height = 122.dp)
        PodiumPillar(entry = third, rank = 3, color = Color(0xFFB45309), height = 82.dp)
    }
}

@Composable
private fun PodiumPillar(
    entry: LeaderboardEntry,
    rank: Int,
    color: Color,
    height: Dp
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(96.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(PureWhite)
                .border(1.5.dp, color, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$rank",
                fontFamily = OutfitDisplayFont,
                color = color,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = entry.username,
            color = InkPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            textAlign = TextAlign.Center
        )
        Text(
            text = "${entry.totalScore} pts",
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(color.copy(alpha = 0.14f), PureWhite)
                    )
                )
                .border(1.dp, HairlineBorder, RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.EmojiEvents,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun LeaderboardRow(entry: LeaderboardEntry) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("leaderboard_row_${entry.rank}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (entry.isCurrentUser) CobaltSoft else PureWhite
        ),
        border = BorderStroke(
            1.dp,
            if (entry.isCurrentUser) CobaltAccent.copy(alpha = 0.45f) else HairlineBorder
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "#${entry.rank}",
                    fontFamily = OutfitDisplayFont,
                    color = if (entry.rank <= 3) WarmGold else InkMuted,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(34.dp)
                )

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = entry.username,
                            color = InkPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (entry.isCurrentUser) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(CobaltAccent)
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            ) {
                                Text("YOU", color = PureWhite, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Text(
                        text = "Top: ${entry.topCar} • ${entry.uniqueSpots} spots",
                        color = InkMuted,
                        fontSize = 11.sp
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (entry.isCurrentUser) PureWhite else WarmGoldSoft)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "${entry.totalScore} pts",
                    color = if (entry.isCurrentUser) CobaltAccent else WarmGold,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
