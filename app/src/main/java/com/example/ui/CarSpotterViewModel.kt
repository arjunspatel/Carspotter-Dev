package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.CarRecognitionEngine
import com.example.ai.RecognitionResult
import com.example.data.CarSpotRepository
import com.example.data.CarSpotterDatabase
import com.example.model.CarSpot
import com.example.model.FriendActivity
import com.example.model.LeaderboardEntry
import com.example.model.RarityTier
import com.example.model.UserProfile
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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

class CarSpotterViewModel(application: Application) : AndroidViewModel(application) {

    private val db = CarSpotterDatabase.getInstance(application)
    private val repository = CarSpotRepository(db.carSpotDao())
    private val recognitionEngine = CarRecognitionEngine(application)
    private val firestoreService = com.example.data.FirestoreCollectionService(application)
    private val authService = com.example.auth.AuthService(application)

    // Player Search State
    private val _playerSearchQuery = MutableStateFlow("")
    val playerSearchQuery: StateFlow<String> = _playerSearchQuery.asStateFlow()

    private val _allPlayers = MutableStateFlow(
        listOf(
            com.example.model.PlayerSearchResult(
                id = "p1",
                username = "TrackDayDan",
                handle = "@dan_gt3",
                rankTitle = "Apex Legend (Tier V)",
                totalScore = 4850,
                spotsCount = 28,
                topCar = "Porsche 911 GT3 RS",
                isFriend = true,
                mutualFriends = 4
            ),
            com.example.model.PlayerSearchResult(
                id = "p2",
                username = "DriftQueen",
                handle = "@s15_elena",
                rankTitle = "Supercar Master (Tier IV)",
                totalScore = 3920,
                spotsCount = 22,
                topCar = "Nissan GT-R Nismo",
                isFriend = true,
                mutualFriends = 2
            ),
            com.example.model.PlayerSearchResult(
                id = "p3",
                username = "TurboSam",
                handle = "@sam_boost",
                rankTitle = "Exotic Hunter (Tier III)",
                totalScore = 2950,
                spotsCount = 17,
                topCar = "Ferrari 296 GTB",
                isFriend = false,
                mutualFriends = 3
            ),
            com.example.model.PlayerSearchResult(
                id = "p4",
                username = "StanceKev",
                handle = "@kev_csl",
                rankTitle = "Street Scout (Tier II)",
                totalScore = 1840,
                spotsCount = 11,
                topCar = "BMW M4 CSL",
                isFriend = false,
                mutualFriends = 1
            ),
            com.example.model.PlayerSearchResult(
                id = "p5",
                username = "RedlineRory",
                handle = "@rory_chiron",
                rankTitle = "Apex Legend (Tier V)",
                totalScore = 5200,
                spotsCount = 31,
                topCar = "Bugatti Chiron Super Sport",
                isFriend = false,
                mutualFriends = 5
            ),
            com.example.model.PlayerSearchResult(
                id = "p6",
                username = "V12Victor",
                handle = "@victor_v12",
                rankTitle = "Supercar Master (Tier IV)",
                totalScore = 3450,
                spotsCount = 19,
                topCar = "Lamborghini Revuelto",
                isFriend = false,
                mutualFriends = 0
            ),
            com.example.model.PlayerSearchResult(
                id = "p7",
                username = "CorvetteCole",
                handle = "@cole_z06",
                rankTitle = "Exotic Hunter (Tier III)",
                totalScore = 2100,
                spotsCount = 14,
                topCar = "Chevrolet Corvette Z06",
                isFriend = false,
                mutualFriends = 2
            )
        )
    )

    val searchResults: StateFlow<List<com.example.model.PlayerSearchResult>> = combine(
        _allPlayers,
        _playerSearchQuery
    ) { players, query ->
        if (query.isBlank()) {
            players
        } else {
            players.filter {
                it.username.contains(query, ignoreCase = true) ||
                it.handle.contains(query, ignoreCase = true) ||
                it.topCar.contains(query, ignoreCase = true)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Firebase Auth State
    val authUser: StateFlow<com.google.firebase.auth.FirebaseUser?> = authService.authStateFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), authService.currentUser)

    private val _isAuthLoading = MutableStateFlow(false)
    val isAuthLoading: StateFlow<Boolean> = _isAuthLoading.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    // Current navigation tab
    private val _currentTab = MutableStateFlow(TabDestination.SPOT)
    val currentTab: StateFlow<TabDestination> = _currentTab.asStateFlow()

    // Database flow of all cars
    val allSpots: StateFlow<List<CarSpot>> = repository.getAllSpotsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtering & Sorting state for Garage / Collection
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedBrandFilter = MutableStateFlow("All")
    val selectedBrandFilter = _selectedBrandFilter.asStateFlow()

    private val _selectedTierFilter = MutableStateFlow<RarityTier?>(null)
    val selectedTierFilter = _selectedTierFilter.asStateFlow()

    private val _selectedSortOrder = MutableStateFlow(SortOrder.POINTS_DESC)
    val selectedSortOrder = _selectedSortOrder.asStateFlow()

    // Filtered cars for Garage
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

    private val _showTrophyRevealDialog = MutableStateFlow(false)
    val showTrophyRevealDialog: StateFlow<Boolean> = _showTrophyRevealDialog.asStateFlow()

    private val _antiCheatMode = MutableStateFlow("AUTHENTIC")
    val antiCheatMode: StateFlow<String> = _antiCheatMode.asStateFlow()

    // Detail dialog inspection
    private val _inspectedCar = MutableStateFlow<CarSpot?>(null)
    val inspectedCar: StateFlow<CarSpot?> = _inspectedCar.asStateFlow()

    // User Profile & Points
    val totalScore: StateFlow<Int> = allSpots.combine(_currentTab) { spots, _ ->
        spots.sumOf { it.pointsAwarded }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1700)

    val userProfile: StateFlow<UserProfile> = combine(allSpots, totalScore, authUser) { spots, score, user ->
        val uniqueModels = spots.map { "${it.make}_${it.model}" }.distinct().size
        val rank = when {
            score >= 5000 -> "Apex Legend (Tier V)"
            score >= 3000 -> "Supercar Master (Tier IV)"
            score >= 1500 -> "Exotic Hunter (Tier III)"
            score >= 500 -> "Street Scout (Tier II)"
            else -> "Rookie Spotter (Tier I)"
        }
        val name = user?.displayName?.ifBlank { null } ?: "ApexHunter"
        val handle = if (user?.email != null) "@${user.email!!.substringBefore("@")}" else "@apex_spotter"
        UserProfile(
            username = name,
            handle = handle,
            avatarUrl = user?.photoUrl?.toString(),
            totalScore = score,
            rankTitle = rank,
            uniqueSpotsCount = uniqueModels,
            totalSpotsCount = spots.size,
            globalRank = if (score > 4000) 3 else if (score > 2000) 4 else 8
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserProfile())

    fun signInWithGoogle() {
        viewModelScope.launch {
            _isAuthLoading.value = true
            _authError.value = null
            val result = authService.signInWithGoogle()
            _isAuthLoading.value = false
            if (result.isFailure) {
                val exception = result.exceptionOrNull()
                if (exception !is androidx.credentials.exceptions.GetCredentialCancellationException) {
                    _authError.value = exception?.localizedMessage ?: "Sign-in failed"
                }
            }
        }
    }

    fun signOut() {
        authService.signOut()
    }

    // Leaderboard & Friends Feed
    val leaderboard: StateFlow<List<LeaderboardEntry>> = combine(totalScore, allSpots) { score, spots ->
        val uniqueModels = spots.map { "${it.make}_${it.model}" }.distinct().size
        repository.getLeaderboard(score, uniqueModels)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val friendsFeed: StateFlow<List<FriendActivity>> = MutableStateFlow(repository.getFriendsFeed())

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
        _allPlayers.value = _allPlayers.value.map { player ->
            if (player.id == playerId) {
                player.copy(isFriend = !player.isFriend)
            } else {
                player
            }
        }
    }

    fun setAntiCheatMode(mode: String) {
        _antiCheatMode.value = mode
    }

    fun inspectCar(car: CarSpot?) {
        _inspectedCar.value = car
    }

    fun dismissTrophyDialog() {
        _showTrophyRevealDialog.value = false
        _latestRecognitionResult.value = null
    }

    fun deleteCar(car: CarSpot) {
        viewModelScope.launch {
            repository.deleteSpot(car)
            firestoreService.deleteCarSpotFromFirestore(car.id)
            if (_inspectedCar.value?.id == car.id) {
                _inspectedCar.value = null
            }
        }
    }

    /**
     * Executes the car snapping flow:
     * 1. Multi-step HUD scan telemetry
     * 2. Anti-cheat sensor evaluation
     * 3. Vehicle match & stats calculation
     * 4. Pop-up trophy reveal
     */
    fun snapCar(photoUri: String? = null) {
        if (_isAnalyzing.value) return
        viewModelScope.launch {
            _isAnalyzing.value = true
            _scanningStepText.value = "Calibrating optical focal grid..."
            delay(400)
            _scanningStepText.value = "Scanning for screen moiré & bezel anti-cheat..."
            delay(500)
            _scanningStepText.value = "Querying automotive telemetry database..."
            delay(500)

            val result = recognitionEngine.analyzeVehicleImage(
                photoUri = photoUri,
                simulatedCheatMode = _antiCheatMode.value
            )

            _latestRecognitionResult.value = result
            _isAnalyzing.value = false
            _showTrophyRevealDialog.value = true

            // Automatically persist to user's collection if authentic
            if (result.isSuccess && result.carSpot != null) {
                repository.addSpot(result.carSpot)
                firestoreService.saveCarSpotToFirestore(result.carSpot)
            }
        }
    }
}
