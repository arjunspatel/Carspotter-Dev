package com.example

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CloudSync
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.ai.CarRecognitionEngine
import com.example.auth.AuthService
import com.example.data.CarSpotterDatabase
import com.example.data.FirestoreCollectionService
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
import com.example.ui.theme.CobaltSoft
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.InkMuted
import com.example.ui.theme.InkPrimary
import com.example.ui.theme.InkSecondary
import com.example.ui.theme.MistSurface
import com.example.ui.theme.OutfitDisplayFont
import com.example.ui.theme.PorcelainSurface
import com.example.ui.theme.PureWhite
import com.example.ui.theme.RacingRed
import com.example.ui.theme.RacingRedSoft
import com.example.util.configureAppCheck
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Mandatory App Check configuration with launch intent before UI/Firebase
        configureAppCheck(this, intent)

        // Purge any legacy un-scoped Room database so garages stay strictly per Google account
        CarSpotterDatabase.clearLegacyLocalGarage(this)

        // Initialize Firebase Firestore with provisioned database ID
        try {
            val dbId = getString(R.string.firestore_database_id)
            FirebaseFirestore.getInstance(dbId)
            Log.d("MainActivity", "Firebase Firestore initialized with database: $dbId")
        } catch (e: Exception) {
            Log.e("MainActivity", "Firestore initialization: ${e.message}")
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
                CarSpotterRoot()
            }
        }
    }
}

@Composable
fun CarSpotterRoot() {
    val context = LocalContext.current
    val activityScope = rememberCoroutineScope()
    val authService = remember(context) { AuthService(context) }

    val authUser by authService.authStateFlow.collectAsStateWithLifecycle(initialValue = authService.currentUser)
    var isCheckingSession by remember { mutableStateOf(authService.currentUser == null) }
    var isSigningIn by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        if (authService.currentUser == null) {
            authService.attemptAutoSignIn()
        }
        isCheckingSession = false
    }

    val currentUser = authUser
    if (currentUser == null) {
        AuthGateScreen(
            isCheckingSession = isCheckingSession,
            isSigningIn = isSigningIn,
            errorMessage = authError,
            onSignInClick = {
                activityScope.launch {
                    isSigningIn = true
                    authError = null
                    val result = authService.signInWithGoogle()
                    isSigningIn = false
                    if (result.isFailure) {
                        val ex = result.exceptionOrNull()
                        if (ex !is GetCredentialCancellationException) {
                            authError = ex?.localizedMessage ?: "Google Sign-In failed"
                        }
                    }
                }
            }
        )
    } else {
        key(currentUser.uid) {
            val appContext = context.applicationContext
            val viewModel: CarSpotterViewModel = viewModel(
                key = "car_spotter_vm_${currentUser.uid}",
                factory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return CarSpotterViewModel(
                            appContext = appContext,
                            recognitionEngine = CarRecognitionEngine(appContext),
                            firestoreService = FirestoreCollectionService(appContext),
                            currentUserId = currentUser.uid,
                            userDisplayName = currentUser.displayName,
                            userAvatarUrl = currentUser.photoUrl?.toString()
                        ) as T
                    }
                }
            )

            CarSpotterApp(
                viewModel = viewModel,
                authUser = currentUser,
                onSignOut = {
                    activityScope.launch {
                        authService.signOut()
                    }
                }
            )
        }
    }
}

@Composable
fun AuthGateScreen(
    isCheckingSession: Boolean,
    isSigningIn: Boolean,
    errorMessage: String?,
    onSignInClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PureWhite)
            .windowInsetsPadding(WindowInsets.statusBars)
            .navigationBarsPadding()
            .padding(24.dp)
            .testTag("auth_gate_screen"),
        contentAlignment = Alignment.Center
    ) {
        // Subtle editorial geometric backdrop
        Canvas(modifier = Modifier.fillMaxSize()) {
            val step = 48.dp.toPx()
            var x = step
            while (x < size.width) {
                drawLine(
                    color = HairlineBorder.copy(alpha = 0.45f),
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = 1f
                )
                x += step
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 420.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(PorcelainSurface)
                    .border(1.dp, HairlineBorder, RoundedCornerShape(26.dp))
                    .padding(10.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.carspotter_cartoon_car_logo_1794609260359),
                    contentDescription = "CarSpotter Logo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(18.dp))
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(CobaltSoft)
                    .border(1.dp, CobaltAccent.copy(alpha = 0.2f), RoundedCornerShape(999.dp))
                    .padding(horizontal = 12.dp, vertical = 5.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CloudSync,
                    contentDescription = null,
                    tint = CobaltAccent,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "ACCOUNT-SYNCED GARAGE",
                    color = CobaltAccent,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "CarSpotter.",
                fontFamily = OutfitDisplayFont,
                color = InkPrimary,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-1.0).sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Sign in with your Google account to save spotted cars to your personal cloud garage, add real friends, and climb the live leaderboard.",
                color = InkSecondary,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            if (!errorMessage.isNullOrBlank()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = RacingRedSoft),
                    border = BorderStroke(1.dp, RacingRed.copy(alpha = 0.25f))
                ) {
                    Text(
                        text = errorMessage,
                        color = RacingRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(14.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Button(
                onClick = onSignInClick,
                enabled = !isCheckingSession && !isSigningIn,
                colors = ButtonDefaults.buttonColors(
                    containerColor = InkPrimary,
                    contentColor = PureWhite
                ),
                shape = RoundedCornerShape(999.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("google_signin_button")
            ) {
                if (isCheckingSession || isSigningIn) {
                    CircularProgressIndicator(
                        color = PureWhite,
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (isCheckingSession) "Checking Session..." else "Signing In...",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Login,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Continue with Google",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}

@Composable
fun CarSpotterApp(
    viewModel: CarSpotterViewModel,
    authUser: FirebaseUser,
    onSignOut: () -> Unit
) {
    val currentTab by viewModel.currentTab.collectAsState()
    val showTrophyDialog by viewModel.showTrophyRevealDialog.collectAsState()
    val latestResult by viewModel.latestRecognitionResult.collectAsState()
    val inspectedSighting by viewModel.inspectedSighting.collectAsState()
    val latestCapturedBitmap by viewModel.latestCapturedBitmap.collectAsState()
    val syncError by viewModel.syncError.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(syncError) {
        val msg = syncError
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSyncError()
        }
    }

    // Handle back button: return to SPOT tab if on another screen
    BackHandler(enabled = currentTab != TabDestination.SPOT) {
        viewModel.setTab(TabDestination.SPOT)
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(PureWhite)
            .windowInsetsPadding(WindowInsets.statusBars),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            if (currentTab != TabDestination.SPOT) {
                CarSpotterTopBar(
                    authUser = authUser,
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
                TabDestination.COMMUNITY -> CommunityScreen(
                    viewModel = viewModel,
                    authUser = authUser,
                    onSignOut = onSignOut
                )
            }
        }
    }

    if (showTrophyDialog && latestResult != null) {
        TrophyRevealDialog(
            result = latestResult!!,
            capturedBitmap = latestCapturedBitmap,
            onRetry = { viewModel.retryLastAnalysis() },
            onDismiss = { viewModel.dismissTrophyDialog() }
        )
    }

    val activeSighting = inspectedSighting
    if (activeSighting != null) {
        CarDetailSheet(
            car = activeSighting.carSpot,
            onDismiss = { viewModel.inspectSighting(null) },
            onDelete = { car -> viewModel.deleteCar(car) },
            canDelete = activeSighting.isOwnSpot,
            spottedByLabel = if (activeSighting.isOwnSpot) {
                null
            } else {
                "Spotted by ${activeSighting.spotterName} (${activeSighting.spotterHandle})"
            }
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
    authUser: FirebaseUser,
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
                            text = authUser.displayName?.take(1)?.uppercase() ?: "S",
                            color = PureWhite,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = authUser.displayName?.split(" ")?.firstOrNull()?.takeIf { it.isNotBlank() } ?: "Spotter",
                    color = InkPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        HorizontalDivider(thickness = 1.dp, color = HairlineBorder)
    }
}
