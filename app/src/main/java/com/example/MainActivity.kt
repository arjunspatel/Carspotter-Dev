package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.People
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.CarSpotterViewModel
import com.example.ui.TabDestination
import com.example.ui.components.CarDetailSheet
import com.example.ui.components.TrophyRevealDialog
import com.example.ui.screens.CameraCaptureScreen
import com.example.ui.screens.CollectionScreen
import com.example.ui.screens.CommunityScreen
import com.example.ui.screens.FriendsScreen
import com.example.ui.screens.MapSpotScreen
import com.example.ui.theme.CarSpotterTheme
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

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                android.graphics.Color.WHITE,
                android.graphics.Color.WHITE
            ),
            navigationBarStyle = SystemBarStyle.light(
                android.graphics.Color.WHITE,
                android.graphics.Color.WHITE
            )
        )

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
    val latestCapturedBitmap by viewModel.latestCapturedBitmap.collectAsState()

    // Handle back button: return to SPOT tab if on another screen
    BackHandler(enabled = currentTab != TabDestination.SPOT) {
        viewModel.setTab(TabDestination.SPOT)
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(PureWhite)
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
        containerColor = PureWhite
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(PureWhite)
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

    // Trophy / Diagnostic Dialog
    if (showTrophyDialog && latestResult != null) {
        TrophyRevealDialog(
            result = latestResult!!,
            capturedBitmap = latestCapturedBitmap,
            onRetry = { viewModel.retryLastAnalysis() },
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
    Column(modifier = Modifier.background(PureWhite)) {
        HorizontalDivider(thickness = 1.dp, color = HairlineBorder)
        NavigationBar(
            modifier = Modifier
                .navigationBarsPadding()
                .testTag("carspotter_bottom_nav"),
            containerColor = PureWhite,
            tonalElevation = 0.dp
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
                        fontWeight = if (currentTab == TabDestination.COLLECTION) FontWeight.Bold else FontWeight.Medium
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = InkPrimary,
                    selectedTextColor = InkPrimary,
                    indicatorColor = MistSurface,
                    unselectedIconColor = InkMuted,
                    unselectedTextColor = InkMuted
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
                        fontWeight = if (currentTab == TabDestination.MAP) FontWeight.Bold else FontWeight.Medium
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = InkPrimary,
                    selectedTextColor = InkPrimary,
                    indicatorColor = MistSurface,
                    unselectedIconColor = InkMuted,
                    unselectedTextColor = InkMuted
                ),
                modifier = Modifier.testTag("nav_item_map")
            )

            // Tab 3: Spot / Camera - Clean Editorial Center Action
            NavigationBarItem(
                selected = currentTab == TabDestination.SPOT,
                onClick = { onTabSelected(TabDestination.SPOT) },
                icon = {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (currentTab == TabDestination.SPOT) InkPrimary else PorcelainSurface)
                            .border(
                                1.5.dp,
                                if (currentTab == TabDestination.SPOT) InkPrimary else HairlineBorder,
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CameraAlt,
                            contentDescription = "Spot Car",
                            tint = if (currentTab == TabDestination.SPOT) PureWhite else InkPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                },
                label = {
                    Text(
                        text = "Spot",
                        fontSize = 10.sp,
                        color = if (currentTab == TabDestination.SPOT) InkPrimary else InkMuted,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = PureWhite,
                    selectedTextColor = InkPrimary,
                    indicatorColor = Color.Transparent,
                    unselectedIconColor = InkPrimary,
                    unselectedTextColor = InkMuted
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
                        fontWeight = if (currentTab == TabDestination.FRIENDS) FontWeight.Bold else FontWeight.Medium
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = InkPrimary,
                    selectedTextColor = InkPrimary,
                    indicatorColor = MistSurface,
                    unselectedIconColor = InkMuted,
                    unselectedTextColor = InkMuted
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
                        fontWeight = if (currentTab == TabDestination.COMMUNITY) FontWeight.Bold else FontWeight.Medium
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = InkPrimary,
                    selectedTextColor = InkPrimary,
                    indicatorColor = MistSurface,
                    unselectedIconColor = InkMuted,
                    unselectedTextColor = InkMuted
                ),
                modifier = Modifier.testTag("nav_item_community")
            )
        }
    }
}

@Composable
fun CarSpotterTopBar(
    authUser: com.google.firebase.auth.FirebaseUser?,
    onSignInClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    Column(modifier = Modifier.background(PureWhite)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(PureWhite)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Editorial Website Brand Header with Cartoony Classic Car Logo
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable(onClick = onProfileClick)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(PureWhite)
                        .border(1.dp, HairlineBorder, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.carspotter_cartoon_car_logo_1794609260359),
                        contentDescription = "CarSpotter Logo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "CarSpotter",
                        fontFamily = OutfitDisplayFont,
                        color = InkPrimary,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.4).sp
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(RacingRed)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "GARAGE EDITION",
                            color = InkMuted,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        )
                    }
                }
            }

            // Auth Identity Pill
            if (authUser == null) {
                Button(
                    onClick = onSignInClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = InkPrimary,
                        contentColor = PureWhite
                    ),
                    shape = RoundedCornerShape(999.dp),
                    modifier = Modifier
                        .height(36.dp)
                        .testTag("topbar_google_signin_button"),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Login,
                        contentDescription = null,
                        tint = PureWhite,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Sign In", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = PureWhite)
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(PorcelainSurface)
                        .border(1.dp, HairlineBorder, RoundedCornerShape(999.dp))
                        .clickable(onClick = onProfileClick)
                        .padding(start = 5.dp, end = 12.dp, top = 5.dp, bottom = 5.dp)
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
                                .background(CobaltAccent),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = authUser.displayName?.take(1) ?: "U",
                                color = PureWhite,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = authUser.displayName?.split(" ")?.first() ?: "Spotter",
                        color = InkPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
        HorizontalDivider(thickness = 1.dp, color = HairlineBorder)
    }
}
