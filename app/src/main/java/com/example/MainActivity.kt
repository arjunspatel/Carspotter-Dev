package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Login
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import coil.compose.AsyncImage
import com.example.ui.theme.EmeraldGreen
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.outlined.People
import com.example.ui.screens.FriendsScreen
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.CarSpotterViewModel
import com.example.ui.TabDestination
import com.example.ui.components.CarDetailSheet
import com.example.ui.components.TrophyRevealDialog
import com.example.ui.screens.CameraCaptureScreen
import com.example.ui.screens.CameraSpotScreen
import com.example.ui.screens.CollectionScreen
import com.example.ui.screens.CommunityScreen
import com.example.ui.screens.MapSpotScreen
import com.example.ui.theme.CarbonBorder
import com.example.ui.theme.CarbonCard
import com.example.ui.theme.CarbonDark
import com.example.ui.theme.CarSpotterTheme
import com.example.ui.theme.NitroCyan
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TurboAmber
import com.example.util.configureAppCheck

class MainActivity : ComponentActivity() {

    private val viewModel: CarSpotterViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Mandatory App Check configuration with launch intent before UI/Firebase
        configureAppCheck(this, intent)

        // Initialize Firebase Firestore with provisioned database ID
        try {
            val dbId = getString(R.string.firestore_database_id)
            com.google.firebase.firestore.FirebaseFirestore.getInstance(dbId)
            android.util.Log.d("MainActivity", "Firebase Firestore initialized with database: $dbId")
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Firestore initialization: ${e.message}")
        }

        enableEdgeToEdge()

        setContent {
            CarSpotterTheme {
                CarSpotterApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun CarSpotterApp(viewModel: CarSpotterViewModel) {
    val currentTab by viewModel.currentTab.collectAsState()
    val showTrophyDialog by viewModel.showTrophyRevealDialog.collectAsState()
    val latestResult by viewModel.latestRecognitionResult.collectAsState()
    val inspectedCar by viewModel.inspectedCar.collectAsState()

    val authUser by viewModel.authUser.collectAsState()

    // Handle back button: return to SPOT tab if on another screen
    BackHandler(enabled = currentTab != TabDestination.SPOT) {
        viewModel.setTab(TabDestination.SPOT)
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars),
        topBar = {
            if (currentTab != TabDestination.SPOT) {
                CarSpotterTopBar(
                    authUser = authUser,
                    onSignInClick = { viewModel.signInWithGoogle() },
                    onProfileClick = { viewModel.setTab(TabDestination.COMMUNITY) }
                )
            }
        },
        bottomBar = {
            CarSpotterBottomBar(
                currentTab = currentTab,
                onTabSelected = { viewModel.setTab(it) }
            )
        },
        containerColor = CarbonDark
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                TabDestination.COLLECTION -> CollectionScreen(viewModel = viewModel)
                TabDestination.MAP -> MapSpotScreen(viewModel = viewModel)
                TabDestination.SPOT -> CameraCaptureScreen(viewModel = viewModel)
                TabDestination.FRIENDS -> FriendsScreen(viewModel = viewModel)
                TabDestination.COMMUNITY -> CommunityScreen(viewModel = viewModel)
            }
        }
    }

    // Celebratory Trophy Reveal Popup after snapping a car
    if (showTrophyDialog && latestResult != null) {
        TrophyRevealDialog(
            result = latestResult!!,
            onDismiss = { viewModel.dismissTrophyDialog() }
        )
    }

    // Car Full Specs Inspection Sheet
    if (inspectedCar != null) {
        CarDetailSheet(
            car = inspectedCar!!,
            onDismiss = { viewModel.inspectCar(null) },
            onDelete = { car -> viewModel.deleteCar(car) }
        )
    }
}

@Composable
fun CarSpotterBottomBar(
    currentTab: TabDestination,
    onTabSelected: (TabDestination) -> Unit
) {
    NavigationBar(
        modifier = Modifier
            .navigationBarsPadding()
            .testTag("carspotter_bottom_nav"),
        containerColor = CarbonCard,
        tonalElevation = 8.dp
    ) {
        // Tab 1: Collection / Garage
        NavigationBarItem(
            selected = currentTab == TabDestination.COLLECTION,
            onClick = { onTabSelected(TabDestination.COLLECTION) },
            icon = {
                Icon(
                    imageVector = if (currentTab == TabDestination.COLLECTION) Icons.Filled.DirectionsCar else Icons.Outlined.DirectionsCar,
                    contentDescription = "Garage Collection",
                    modifier = Modifier.size(22.dp)
                )
            },
            label = {
                Text(
                    text = "Garage",
                    fontSize = 10.sp,
                    fontWeight = if (currentTab == TabDestination.COLLECTION) FontWeight.Bold else FontWeight.Normal
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.Black,
                selectedTextColor = TurboAmber,
                indicatorColor = TurboAmber,
                unselectedIconColor = TextMuted,
                unselectedTextColor = TextMuted
            ),
            modifier = Modifier.testTag("nav_item_garage")
        )

        // Tab 2: Spot Map / Radar
        NavigationBarItem(
            selected = currentTab == TabDestination.MAP,
            onClick = { onTabSelected(TabDestination.MAP) },
            icon = {
                Icon(
                    imageVector = if (currentTab == TabDestination.MAP) Icons.Filled.LocationOn else Icons.Outlined.LocationOn,
                    contentDescription = "Radar Map",
                    modifier = Modifier.size(22.dp)
                )
            },
            label = {
                Text(
                    text = "Radar",
                    fontSize = 10.sp,
                    fontWeight = if (currentTab == TabDestination.MAP) FontWeight.Bold else FontWeight.Normal
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.Black,
                selectedTextColor = NitroCyan,
                indicatorColor = NitroCyan,
                unselectedIconColor = TextMuted,
                unselectedTextColor = TextMuted
            ),
            modifier = Modifier.testTag("nav_item_map")
        )

        // Tab 3: Spot / Camera - PROMINENT CENTER ACTION
        NavigationBarItem(
            selected = currentTab == TabDestination.SPOT,
            onClick = { onTabSelected(TabDestination.SPOT) },
            icon = {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(
                            if (currentTab == TabDestination.SPOT)
                                Brush.radialGradient(listOf(TurboAmber, Color(0xFFCC5500)))
                            else
                                Brush.radialGradient(listOf(TurboAmber.copy(alpha = 0.25f), CarbonCard))
                        )
                        .border(
                            1.5.dp,
                            if (currentTab == TabDestination.SPOT) TurboAmber else TurboAmber.copy(alpha = 0.6f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.CameraAlt,
                        contentDescription = "Spot Car",
                        tint = if (currentTab == TabDestination.SPOT) Color.Black else TurboAmber,
                        modifier = Modifier.size(18.dp)
                    )
                }
            },
            label = {
                Text(
                    text = "Spot",
                    fontSize = 10.sp,
                    color = TurboAmber,
                    fontWeight = FontWeight.Bold
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.Black,
                selectedTextColor = TurboAmber,
                indicatorColor = Color.Transparent,
                unselectedIconColor = TurboAmber,
                unselectedTextColor = TurboAmber
            ),
            modifier = Modifier.testTag("nav_item_spot")
        )

        // Tab 4: Friends Feed & Battles
        NavigationBarItem(
            selected = currentTab == TabDestination.FRIENDS,
            onClick = { onTabSelected(TabDestination.FRIENDS) },
            icon = {
                Icon(
                    imageVector = if (currentTab == TabDestination.FRIENDS) Icons.Filled.People else Icons.Outlined.People,
                    contentDescription = "Friends",
                    modifier = Modifier.size(22.dp)
                )
            },
            label = {
                Text(
                    text = "Friends",
                    fontSize = 10.sp,
                    fontWeight = if (currentTab == TabDestination.FRIENDS) FontWeight.Bold else FontWeight.Normal
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.Black,
                selectedTextColor = NitroCyan,
                indicatorColor = NitroCyan,
                unselectedIconColor = TextMuted,
                unselectedTextColor = TextMuted
            ),
            modifier = Modifier.testTag("nav_item_friends")
        )

        // Tab 5: Community & Global Leaderboards
        NavigationBarItem(
            selected = currentTab == TabDestination.COMMUNITY,
            onClick = { onTabSelected(TabDestination.COMMUNITY) },
            icon = {
                Icon(
                    imageVector = if (currentTab == TabDestination.COMMUNITY) Icons.Filled.EmojiEvents else Icons.Outlined.EmojiEvents,
                    contentDescription = "Community",
                    modifier = Modifier.size(22.dp)
                )
            },
            label = {
                Text(
                    text = "Community",
                    fontSize = 10.sp,
                    fontWeight = if (currentTab == TabDestination.COMMUNITY) FontWeight.Bold else FontWeight.Normal
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.Black,
                selectedTextColor = TurboAmber,
                indicatorColor = TurboAmber,
                unselectedIconColor = TextMuted,
                unselectedTextColor = TextMuted
            ),
            modifier = Modifier.testTag("nav_item_community")
        )
    }
}

@Composable
fun CarSpotterTopBar(
    authUser: com.google.firebase.auth.FirebaseUser?,
    onSignInClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CarbonCard)
            .border(1.dp, CarbonBorder)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // App Branding
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable(onClick = onProfileClick)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(TurboAmber.copy(alpha = 0.2f))
                    .border(1.dp, TurboAmber, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.DirectionsCar,
                    contentDescription = null,
                    tint = TurboAmber,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "CarSpotter",
                    color = TextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "AI TROPHY RADAR",
                    color = NitroCyan,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }

        // Auth Identity Button
        if (authUser == null) {
            Button(
                onClick = onSignInClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .height(34.dp)
                    .testTag("topbar_google_signin_button"),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Login,
                    contentDescription = null,
                    tint = Color(0xFF4285F4),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Sign In", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            }
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(CarbonDark)
                    .border(1.dp, EmeraldGreen.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                    .clickable(onClick = onProfileClick)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .testTag("topbar_user_profile")
            ) {
                if (authUser.photoUrl != null) {
                    AsyncImage(
                        model = authUser.photoUrl,
                        contentDescription = "Avatar",
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(EmeraldGreen),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = authUser.displayName?.take(1) ?: "U",
                            color = Color.Black,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = authUser.displayName?.split(" ")?.first() ?: "Spotter",
                    color = TextPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

