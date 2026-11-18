package com.example.ui

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.CarRecognitionEngine
import com.example.ai.RecognitionResult
import com.example.ai.RecognitionStatus
import com.example.data.FirestoreCollectionService
import com.example.model.AntiCheatResult
import com.example.model.CarSpot
import com.example.model.FriendActivity
import com.example.model.LeaderboardEntry
import com.example.model.PlayerSearchResult
import com.example.model.RadarSighting
import com.example.model.RarityTier
import com.example.model.SpotterProfile
import com.example.model.UserProfile
import com.example.util.LocationHelper
import com.example.util.SpotCoordinates
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

enum class TabDestination(val label: String) {
    COLLECTION("Garage"),
    MAP("Radar"),
    SPOT("Spot"),
    FRIENDS("Friends"),
    COMMUNITY("Community")
}

enum class SortOrder(val label: String) {
    POINTS_DESC("Highest Points"),
    DATE_DESC("Newest Spots"),
    HORSEPOWER_DESC("Max Horsepower"),
    SPEED_DESC("Top Speed")
}

private const val TAG = "CarSpotterViewModel"

class CarSpotterViewModel(
    private val appContext: Context,
    private val recognitionEngine: CarRecognitionEngine,
    private val firestoreService: FirestoreCollectionService,
    val currentUserId: String,
    val userDisplayName: String?,
    val userAvatarUrl: String?
) : ViewModel() {

    private val _syncError = MutableStateFlow<String?>(null)
    val syncError: StateFlow<String?> = _syncError.asStateFlow()

    // Live GPS coordinates for current user's Radar position and geotagging new spots
    private val _currentUserLocation = MutableStateFlow<SpotCoordinates?>(null)
    val currentUserLocation: StateFlow<SpotCoordinates?> = _currentUserLocation.asStateFlow()

    // Current navigation tab
    private val _currentTab = MutableStateFlow(TabDestination.SPOT)
    val currentTab: StateFlow<TabDestination> = _currentTab.asStateFlow()

    // Player Search State
    private val _playerSearchQuery = MutableStateFlow("")
    val playerSearchQuery: StateFlow<String> = _playerSearchQuery.asStateFlow()

    // Live Cloud Garage scoped strictly to currentUserId
    val allSpots: StateFlow<List<CarSpot>> = firestoreService.observeUserGarage(currentUserId)
        .catch { e ->
            Log.e(TAG, "Garage listener error: ${e.message}", e)
            _syncError.value = e.localizedMessage ?: "Failed syncing garage"
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Live Public Sightings across ALL accounts for the Google Maps Radar
    private val publicSightings: StateFlow<List<RadarSighting>> = firestoreService.observePublicSightings(currentUserId)
        .catch { e ->
            Log.e(TAG, "Public sightings listener error: ${e.message}", e)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Merged Radar Sightings (yours with local photo support + other accounts' public sightings)
    val radarSightings: StateFlow<List<RadarSighting>> = combine(
        allSpots,
        publicSightings
    ) { mySpots, pubSightings ->
        val myName = FirestoreCollectionService.sanitizeDisplayName(userDisplayName)
        val myHandle = FirestoreCollectionService.buildPublicHandle(myName, currentUserId)
        val merged = LinkedHashMap<String, RadarSighting>()
        pubSightings.forEach { sighting ->
            merged[sighting.carSpot.id] = sighting
        }
        mySpots.forEach { spot ->
            merged[spot.id] = RadarSighting(
                carSpot = spot,
                spotterUserId = currentUserId,
                spotterName = myName,
                spotterHandle = myHandle,
                spotterAvatarUrl = userAvatarUrl,
                isOwnSpot = true
            )
        }
        merged.values.sortedByDescending { it.carSpot.timestamp }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Live Public Spotters Directory
    private val publicSpotters: StateFlow<List<SpotterProfile>> = firestoreService.observePublicSpotters()
        .catch { e ->
            Log.e(TAG, "Public spotters listener error: ${e.message}", e)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Live Added Friends UIDs scoped to currentUserId
    private val friendIds: StateFlow<Set<String>> = firestoreService.observeFriendIds(currentUserId)
        .catch { e ->
            Log.e(TAG, "Friends listener error: ${e.message}", e)
            emit(emptySet())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    // Filtering & Sorting state for Garage / Collection
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedBrandFilter = MutableStateFlow("All")
    val selectedBrandFilter = _selectedBrandFilter.asStateFlow()

    private val _selectedTierFilter = MutableStateFlow<RarityTier?>(null)
    val selectedTierFilter = _selectedTierFilter.asStateFlow()

    private val _selectedSortOrder = MutableStateFlow(SortOrder.POINTS_DESC)
    val selectedSortOrder = _selectedSortOrder.asStateFlow()

    val filteredSpots: StateFlow<List<CarSpot>> = combine(
        allSpots,
        _searchQuery,
        _selectedBrandFilter,
        _selectedTierFilter,
        _selectedSortOrder
    ) { spots, query, brand, tier, sort ->
        spots.filter { spot ->
            val matchesQuery = query.isBlank() ||
                spot.make.contains(query, ignoreCase = true) ||
                spot.model.contains(query, ignoreCase = true) ||
                spot.locationName.contains(query, ignoreCase = true)
            val matchesBrand = brand == "All" || spot.make.equals(brand, ignoreCase = true)
            val matchesTier = tier == null || spot.rarityTier == tier
            matchesQuery && matchesBrand && matchesTier
        }.sortedWith { a, b ->
            when (sort) {
                SortOrder.POINTS_DESC -> b.pointsAwarded.compareTo(a.pointsAwarded)
                SortOrder.DATE_DESC -> b.timestamp.compareTo(a.timestamp)
                SortOrder.HORSEPOWER_DESC -> b.horsepower.compareTo(a.horsepower)
                SortOrder.SPEED_DESC -> b.topSpeedMph.compareTo(a.topSpeedMph)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Camera Scanning & AI State
    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    private val _scanningStepText = MutableStateFlow("Aligning optical targeting...")
    val scanningStepText: StateFlow<String> = _scanningStepText.asStateFlow()

    private val _latestRecognitionResult = MutableStateFlow<RecognitionResult?>(null)
    val latestRecognitionResult: StateFlow<RecognitionResult?> = _latestRecognitionResult.asStateFlow()

    private val _latestCapturedBitmap = MutableStateFlow<Bitmap?>(null)
    val latestCapturedBitmap: StateFlow<Bitmap?> = _latestCapturedBitmap.asStateFlow()

    private var latestCapturedPhotoUri: String? = null

    private val _showTrophyRevealDialog = MutableStateFlow(false)
    val showTrophyRevealDialog: StateFlow<Boolean> = _showTrophyRevealDialog.asStateFlow()

    private val _inspectedSighting = MutableStateFlow<RadarSighting?>(null)
    val inspectedSighting: StateFlow<RadarSighting?> = _inspectedSighting.asStateFlow()

    val totalScore: StateFlow<Int> = allSpots
        .map { spots -> spots.sumOf { it.pointsAwarded } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val leaderboard: StateFlow<List<LeaderboardEntry>> = combine(
        publicSpotters,
        allSpots,
        totalScore
    ) { spotters, mySpots, myScore ->
        val myName = FirestoreCollectionService.sanitizeDisplayName(userDisplayName)
        val myHandle = FirestoreCollectionService.buildPublicHandle(myName, currentUserId)
        val myUniqueModels = mySpots.map { "${it.make}_${it.model}" }.distinct().size
        val myTopCar = (mySpots.maxByOrNull { it.horsepower } ?: mySpots.maxByOrNull { it.pointsAwarded })
            ?.let { "${it.year} ${it.make} ${it.model}".trim() } ?: "No Spots Yet"

        val entriesMap = LinkedHashMap<String, LeaderboardEntry>()
        spotters.forEach { sp ->
            val isMe = sp.userId == currentUserId
            entriesMap[sp.userId] = LeaderboardEntry(
                userId = sp.userId,
                rank = 0,
                username = if (isMe) myName else sp.username,
                handle = if (isMe) myHandle else sp.handle,
                totalScore = if (isMe) myScore else sp.totalScore,
                uniqueSpots = if (isMe) myUniqueModels else sp.uniqueSpotsCount,
                topCar = if (isMe) myTopCar else sp.topCar.ifBlank { "No Spots Yet" },
                isCurrentUser = isMe,
                badge = FirestoreCollectionService.computeRankTitle(if (isMe) myScore else sp.totalScore)
            )
        }

        if (!entriesMap.containsKey(currentUserId)) {
            entriesMap[currentUserId] = LeaderboardEntry(
                userId = currentUserId,
                rank = 0,
                username = myName,
                handle = myHandle,
                totalScore = myScore,
                uniqueSpots = myUniqueModels,
                topCar = myTopCar,
                isCurrentUser = true,
                badge = FirestoreCollectionService.computeRankTitle(myScore)
            )
        }

        entriesMap.values
            .sortedWith(compareByDescending<LeaderboardEntry> { it.totalScore }.thenByDescending { it.uniqueSpots })
            .mapIndexed { index, entry -> entry.copy(rank = index + 1) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userProfile: StateFlow<UserProfile> = combine(
        allSpots,
        totalScore,
        leaderboard
    ) { spots, score, board ->
        val name = FirestoreCollectionService.sanitizeDisplayName(userDisplayName)
        val handle = FirestoreCollectionService.buildPublicHandle(name, currentUserId)
        val uniqueModels = spots.map { "${it.make}_${it.model}" }.distinct().size
        val rankPosition = board.firstOrNull { it.isCurrentUser }?.rank ?: 1
        UserProfile(
            id = currentUserId,
            username = name,
            handle = handle,
            avatarUrl = userAvatarUrl,
            totalScore = score,
            rankTitle = FirestoreCollectionService.computeRankTitle(score),
            uniqueSpotsCount = uniqueModels,
            totalSpotsCount = spots.size,
            globalRank = rankPosition
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        UserProfile(
            id = currentUserId,
            username = FirestoreCollectionService.sanitizeDisplayName(userDisplayName),
            handle = FirestoreCollectionService.buildPublicHandle(
                FirestoreCollectionService.sanitizeDisplayName(userDisplayName),
                currentUserId
            ),
            avatarUrl = userAvatarUrl
        )
    )

    val searchResults: StateFlow<List<PlayerSearchResult>> = combine(
        publicSpotters,
        friendIds,
        _playerSearchQuery
    ) { spotters, myFriends, query ->
        spotters
            .filter { it.userId != currentUserId }
            .map { sp ->
                PlayerSearchResult(
                    id = sp.userId,
                    username = sp.username,
                    handle = sp.handle,
                    avatarUrl = sp.avatarUrl,
                    rankTitle = sp.rankTitle,
                    totalScore = sp.totalScore,
                    spotsCount = sp.totalSpotsCount,
                    topCar = sp.topCar.ifBlank { "No Spots Yet" },
                    topHorsepower = sp.topHorsepower,
                    topSpeedMph = sp.topSpeedMph,
                    isFriend = myFriends.contains(sp.userId)
                )
            }
            .filter { player ->
                query.isBlank() ||
                    player.username.contains(query, ignoreCase = true) ||
                    player.handle.contains(query, ignoreCase = true) ||
                    player.topCar.contains(query, ignoreCase = true)
            }
            .sortedByDescending { it.totalScore }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val addedFriends: StateFlow<List<SpotterProfile>> = combine(
        publicSpotters,
        friendIds
    ) { spotters, myFriends ->
        spotters.filter { myFriends.contains(it.userId) }
            .sortedByDescending { it.totalScore }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val friendsFeed: StateFlow<List<FriendActivity>> = combine(
        publicSpotters,
        friendIds
    ) { spotters, myFriends ->
        spotters
            .filter { myFriends.contains(it.userId) }
            .flatMap { friend ->
                friend.recentSpots.map { spot ->
                    FriendActivity(
                        id = "${friend.userId}_${spot.id}",
                        friendId = friend.userId,
                        username = friend.username,
                        handle = friend.handle,
                        avatarUrl = friend.avatarUrl,
                        carName = spot.carName,
                        rarityTier = spot.rarityTier,
                        pointsEarned = spot.pointsEarned,
                        location = spot.locationName.ifBlank { "Location Private" },
                        timeAgo = formatRelativeTime(spot.timestamp),
                        friendTopCar = friend.topCar.ifBlank { spot.carName },
                        friendTopHorsepower = if (friend.topHorsepower > 0) friend.topHorsepower else spot.horsepower,
                        friendTopSpeedMph = if (friend.topSpeedMph > 0) friend.topSpeedMph else spot.topSpeedMph,
                        isVerified = spot.isVerified
                    ) to spot.timestamp
                }
            }
            .sortedByDescending { it.second }
            .map { it.first }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        warmUpAi()
        refreshUserLocation()
        viewModelScope.launch {
            runCatching {
                firestoreService.ensureSpotterProfile(
                    userId = currentUserId,
                    displayName = userDisplayName,
                    avatarUrl = userAvatarUrl
                )
            }.onFailure { e ->
                Log.e(TAG, "Failed ensuring initial spotter profile: ${e.message}", e)
                _syncError.value = e.localizedMessage
            }
        }
    }

    fun refreshUserLocation() {
        viewModelScope.launch {
            val coords = LocationHelper.getCurrentSpotLocation(appContext)
            if (coords != null) {
                _currentUserLocation.value = coords
            }
        }
    }

    fun clearSyncError() {
        _syncError.value = null
    }

    fun setTab(tab: TabDestination) {
        _currentTab.value = tab
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setBrandFilter(brand: String) {
        _selectedBrandFilter.value = brand
    }

    fun setTierFilter(tier: RarityTier?) {
        _selectedTierFilter.value = tier
    }

    fun setSortOrder(order: SortOrder) {
        _selectedSortOrder.value = order
    }

    fun setPlayerSearchQuery(query: String) {
        _playerSearchQuery.value = query
    }

    fun toggleFriend(playerId: String) {
        if (playerId == currentUserId) return
        val currentlyFriend = friendIds.value.contains(playerId)
        viewModelScope.launch {
            runCatching {
                if (currentlyFriend) {
                    firestoreService.removeFriend(currentUserId, playerId)
                } else {
                    firestoreService.addFriend(currentUserId, playerId)
                }
            }.onFailure { e ->
                Log.e(TAG, "Failed updating friend $playerId: ${e.message}", e)
                _syncError.value = e.localizedMessage ?: "Failed updating friend"
            }
        }
    }

    fun inspectCar(car: CarSpot?) {
        if (car == null) {
            _inspectedSighting.value = null
        } else {
            val myName = FirestoreCollectionService.sanitizeDisplayName(userDisplayName)
            val myHandle = FirestoreCollectionService.buildPublicHandle(myName, currentUserId)
            _inspectedSighting.value = RadarSighting(
                carSpot = car,
                spotterUserId = currentUserId,
                spotterName = myName,
                spotterHandle = myHandle,
                spotterAvatarUrl = userAvatarUrl,
                isOwnSpot = true
            )
        }
    }

    fun inspectSighting(sighting: RadarSighting?) {
        _inspectedSighting.value = sighting
    }

    fun dismissTrophyDialog() {
        _showTrophyRevealDialog.value = false
        _latestRecognitionResult.value = null
        _latestCapturedBitmap.value = null
        latestCapturedPhotoUri = null
    }

    fun deleteCar(car: CarSpot) {
        viewModelScope.launch {
            runCatching {
                firestoreService.deleteCarSpotFromFirestore(
                    userId = currentUserId,
                    spotId = car.id,
                    displayName = userDisplayName,
                    avatarUrl = userAvatarUrl
                )
            }.onFailure { e ->
                Log.e(TAG, "Failed deleting car ${car.id}: ${e.message}", e)
                _syncError.value = e.localizedMessage ?: "Failed removing vehicle"
            }
            if (_inspectedSighting.value?.carSpot?.id == car.id) {
                _inspectedSighting.value = null
            }
        }
    }

    private var isCaptureInProgress = false

    fun warmUpAi() {
        viewModelScope.launch {
            recognitionEngine.warmUp()
        }
    }

    fun beginCapture() {
        if (isCaptureInProgress || _isAnalyzing.value) return
        isCaptureInProgress = true
        _isAnalyzing.value = true
        _scanningStepText.value = "Capturing optical frame..."
    }

    fun snapCar(photoUri: String? = null, bitmap: Bitmap? = null, captureError: String? = null) {
        if (_isAnalyzing.value && !isCaptureInProgress) return
        isCaptureInProgress = false
        _latestCapturedBitmap.value = bitmap
        latestCapturedPhotoUri = photoUri
        viewModelScope.launch {
            if (captureError != null && photoUri == null && bitmap == null) {
                _isAnalyzing.value = false
                _latestRecognitionResult.value = RecognitionResult(
                    carSpot = null,
                    antiCheatResult = AntiCheatResult(
                        isAuthentic = false,
                        confidence = 1.0f,
                        verdict = "CAMERA CAPTURE ERROR",
                        details = captureError,
                        detectionFlags = listOf("Hardware Shutter Glitch")
                    ),
                    isSuccess = false,
                    status = RecognitionStatus.AI_SERVICE_ERROR,
                    errorMessage = captureError
                )
                _showTrophyRevealDialog.value = true
                return@launch
            }

            runVehicleAnalysis(photoUri = photoUri, bitmap = bitmap)
        }
    }

    fun retryLastAnalysis() {
        if (_isAnalyzing.value) return
        val uri = latestCapturedPhotoUri
        val bmp = _latestCapturedBitmap.value
        if (uri == null && bmp == null) {
            dismissTrophyDialog()
            return
        }
        _showTrophyRevealDialog.value = false
        viewModelScope.launch {
            runVehicleAnalysis(photoUri = uri, bitmap = bmp)
        }
    }

    private suspend fun runVehicleAnalysis(photoUri: String?, bitmap: Bitmap?) {
        _isAnalyzing.value = true
        _scanningStepText.value = "Identifying vehicle with Gemini AI..."

        val locationDeferred = viewModelScope.async {
            LocationHelper.getCurrentSpotLocation(appContext) ?: _currentUserLocation.value
        }

        val statusUpdater = viewModelScope.launch {
            delay(4_500L)
            _scanningStepText.value = "Verifying authenticity & trim specs..."
            delay(10_000L)
            _scanningStepText.value = "Still working — waiting on network response..."
        }

        val rawResult = try {
            recognitionEngine.analyzeVehicleImage(
                photoUri = photoUri,
                capturedBitmap = bitmap
            )
        } finally {
            statusUpdater.cancel()
        }

        val coords = locationDeferred.await()
        if (coords != null) {
            _currentUserLocation.value = coords
        }

        val finalResult = if (rawResult.isSuccess && rawResult.carSpot != null) {
            val enrichedSpot = if (coords != null) {
                rawResult.carSpot.copy(
                    latitude = coords.latitude,
                    longitude = coords.longitude,
                    locationName = coords.locationName
                )
            } else {
                rawResult.carSpot.copy(
                    latitude = 0.0,
                    longitude = 0.0,
                    locationName = if (LocationHelper.hasLocationPermission(appContext)) "GPS Unavailable" else "Location Off"
                )
            }
            rawResult.copy(carSpot = enrichedSpot)
        } else {
            rawResult
        }

        _latestRecognitionResult.value = finalResult
        _isAnalyzing.value = false
        _showTrophyRevealDialog.value = true

        val spotToSave = finalResult.carSpot
        if (finalResult.isSuccess && spotToSave != null) {
            runCatching {
                firestoreService.saveCarSpotToFirestore(
                    userId = currentUserId,
                    car = spotToSave,
                    displayName = userDisplayName,
                    avatarUrl = userAvatarUrl
                )
            }.onFailure { e ->
                Log.e(TAG, "Failed saving spotted car to user garage: ${e.message}", e)
                _syncError.value = e.localizedMessage ?: "Failed syncing vehicle to cloud garage"
            }
        }
    }

    private fun formatRelativeTime(timestampMs: Long): String {
        if (timestampMs <= 0L) return "Recently"
        val diff = (System.currentTimeMillis() - timestampMs).coerceAtLeast(0L)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
        val hours = TimeUnit.MILLISECONDS.toHours(diff)
        val days = TimeUnit.MILLISECONDS.toDays(diff)
        return when {
            minutes < 1 -> "Just now"
            minutes < 60 -> "${minutes}m ago"
            hours < 24 -> "${hours}h ago"
            days < 7 -> "${days}d ago"
            else -> "${days / 7}w ago"
        }
    }
}
