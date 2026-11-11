package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.LeaderboardEntry
import com.example.model.PlayerSearchResult
import com.example.model.UserProfile
import com.example.ui.CarSpotterViewModel
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
            .background(CarbonDark)
            .testTag("community_screen"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header with Title and Search Toggle Button
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "COMMUNITY & RANKS",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Global leaderboards, spotter XP and player discovery",
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                }

                // Search Players Action Button
                IconButton(
                    onClick = { isSearchExpanded = !isSearchExpanded },
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(if (isSearchExpanded) TurboAmber else CarbonCardElevated)
                        .border(1.dp, if (isSearchExpanded) TurboAmber else CarbonBorder, CircleShape)
                        .testTag("search_players_toggle_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search Players",
                        tint = if (isSearchExpanded) Color.Black else NitroCyan,
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
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = CarbonCard),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CarbonBorder)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = TurboAmber,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "FIND SPOTTERS & FRIENDS",
                                color = TurboAmber,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.setPlayerSearchQuery(it) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("player_search_input"),
                            placeholder = { Text("Search by name, tag, or car...", color = TextMuted, fontSize = 13.sp) },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            trailingIcon = {
                                if (searchQuery.isNotBlank()) {
                                    IconButton(onClick = { viewModel.setPlayerSearchQuery("") }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextMuted)
                                    }
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = CarbonCardElevated,
                                unfocusedContainerColor = CarbonCardElevated,
                                focusedBorderColor = TurboAmber,
                                unfocusedBorderColor = CarbonBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )
                    }
                }
            }
        }

        // Display Search Results if user is actively searching
        if (searchQuery.isNotBlank()) {
            item {
                Text(
                    text = "SEARCH RESULTS (${searchResults.size})",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    letterSpacing = 1.sp
                )
            }

            if (searchResults.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No players found matching \"$searchQuery\"",
                            color = TextMuted,
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

        // Profile & XP Rank Banner (shown when not actively searching)
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

            // Top 3 Podium
            item {
                Text(
                    text = "GLOBAL PODIUM",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    letterSpacing = 1.5.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            item {
                PodiumSection(leaderboard = leaderboard)
            }

            // Full Leaderboard Header
            item {
                Text(
                    text = "ALL RANKED SPOTTERS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    letterSpacing = 1.5.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // Leaderboard Items
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
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CarbonCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, CarbonBorder)
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
                        .background(
                            Brush.radialGradient(
                                listOf(NitroCyan.copy(alpha = 0.35f), CarbonCardElevated)
                            )
                        )
                        .border(1.dp, NitroCyan.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = player.username.take(1).uppercase(),
                        color = NitroCyan,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = player.username,
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${player.handle} • ${player.rankTitle}",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Top: ${player.topCar} • ${player.totalScore} pts",
                        color = TurboAmber,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = onToggleFriend,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (player.isFriend) EmeraldGreen.copy(alpha = 0.2f) else TurboAmber,
                    contentColor = if (player.isFriend) EmeraldGreen else Color.Black
                ),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                border = if (player.isFriend) androidx.compose.foundation.BorderStroke(1.dp, EmeraldGreen) else null,
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
    authUser: com.google.firebase.auth.FirebaseUser?,
    isLoading: Boolean,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CarbonCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, CarbonBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
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
                                .size(44.dp)
                                .clip(CircleShape)
                                .border(1.5.dp, TurboAmber, CircleShape)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(CarbonCardElevated)
                                .border(1.5.dp, TurboAmber, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = TurboAmber,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = authUser?.displayName ?: userProfile.username,
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = userProfile.rankTitle,
                            color = TurboAmber,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                if (authUser == null) {
                    Button(
                        onClick = onSignIn,
                        colors = ButtonDefaults.buttonColors(containerColor = TurboAmber, contentColor = Color.Black),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        enabled = !isLoading,
                        modifier = Modifier.testTag("google_signin_button")
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.AutoMirrored.Filled.Login, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Sign In", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    IconButton(onClick = onSignOut, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Sign Out", tint = TextMuted, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // XP Progression Bar
            val xpProgress = (userProfile.totalScore % 1000) / 1000f
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "LEVEL PROGRESS",
                    color = TextMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "${userProfile.totalScore} TOTAL XP",
                    color = NitroCyan,
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
                color = TurboAmber,
                trackColor = CarbonCardElevated
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
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        // 2nd Place (Silver)
        PodiumPillar(entry = second, rank = 2, color = Color(0xFFC0C0C0), height = 110.dp)

        // 1st Place (Gold)
        PodiumPillar(entry = first, rank = 1, color = GoldenTrophy, height = 135.dp)

        // 3rd Place (Bronze)
        PodiumPillar(entry = third, rank = 3, color = Color(0xFFCD7F32), height = 95.dp)
    }
}

@Composable
private fun PodiumPillar(
    entry: LeaderboardEntry,
    rank: Int,
    color: Color,
    height: androidx.compose.ui.unit.Dp
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(96.dp)
    ) {
        // Top Icon or Initials
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.2f))
                .border(2.dp, color, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$rank",
                color = color,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = entry.username,
            color = TextPrimary,
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

        Spacer(modifier = Modifier.height(6.dp))

        // Pedestal
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(color.copy(alpha = 0.35f), CarbonCard)
                    )
                )
                .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.EmojiEvents,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
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
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (entry.isCurrentUser) TurboAmber.copy(alpha = 0.1f) else CarbonCard
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (entry.isCurrentUser) TurboAmber else CarbonBorder
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "#${entry.rank}",
                    color = if (entry.rank <= 3) GoldenTrophy else TextMuted,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.width(32.dp)
                )

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = entry.username,
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (entry.isCurrentUser) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(TurboAmber)
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text("YOU", color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                    Text(
                        text = "Top: ${entry.topCar} • ${entry.uniqueSpots} spots",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
            }

            Text(
                text = "${entry.totalScore} pts",
                color = TurboAmber,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
