package com.example.model

enum class RarityTier(val label: String, val basePoints: Int, val colorHex: Long) {
    COMMON("Common", 50, 0xFF64748BL),
    UNCOMMON("Uncommon", 150, 0xFF0284C7L),
    RARE("Rare", 400, 0xFF4F46E5L),
    EXOTIC("Exotic", 850, 0xFFD97706L),
    LEGENDARY("Hypercar / Legendary", 1800, 0xFFDC2626L)
}

data class CarSpot(
    val id: String = "",
    val make: String = "",
    val model: String = "",
    val year: Int = 0,
    val trim: String = "",
    val rarityTier: RarityTier = RarityTier.COMMON,
    val rarityScore: Int = 1,
    val pointsAwarded: Int = 0,
    val horsepower: Int = 0,
    val zeroToSixtyMph: Double = 0.0,
    val topSpeedMph: Int = 0,
    val estimatedPriceUsd: Long = 0L,
    val engineType: String = "",
    val drivetrain: String = "",
    val description: String = "",
    val trivia: String = "",
    val photoUri: String? = null,
    val locationName: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val timestamp: Long = 0L,
    val isAuthentic: Boolean = true,
    val antiCheatConfidence: Float = 1.0f,
    val antiCheatDetails: String = "",
    val isFirstDiscovery: Boolean = false
)

data class AntiCheatResult(
    val isAuthentic: Boolean,
    val confidence: Float,
    val verdict: String,
    val details: String,
    val detectionFlags: List<String>
)

data class RecentSpotSummary(
    val id: String = "",
    val carName: String = "",
    val rarityTier: RarityTier = RarityTier.COMMON,
    val pointsEarned: Int = 0,
    val horsepower: Int = 0,
    val topSpeedMph: Int = 0,
    val locationName: String = "",
    val timestamp: Long = 0L,
    val isVerified: Boolean = true
)

data class SpotterProfile(
    val userId: String = "",
    val username: String = "Spotter",
    val handle: String = "@spotter",
    val avatarUrl: String? = null,
    val totalScore: Int = 0,
    val uniqueSpotsCount: Int = 0,
    val totalSpotsCount: Int = 0,
    val rankTitle: String = "Rookie Spotter (Tier I)",
    val topCar: String = "",
    val topHorsepower: Int = 0,
    val topSpeedMph: Int = 0,
    val isPublicProfile: Boolean = true,
    val recentSpots: List<RecentSpotSummary> = emptyList()
)

data class UserProfile(
    val id: String = "",
    val username: String = "Spotter",
    val handle: String = "@spotter",
    val avatarUrl: String? = null,
    val totalScore: Int = 0,
    val rankTitle: String = "Rookie Spotter (Tier I)",
    val uniqueSpotsCount: Int = 0,
    val totalSpotsCount: Int = 0,
    val globalRank: Int = 1
)

data class LeaderboardEntry(
    val userId: String = "",
    val rank: Int,
    val username: String,
    val handle: String,
    val totalScore: Int,
    val uniqueSpots: Int,
    val topCar: String,
    val isCurrentUser: Boolean = false,
    val badge: String = "Spotter"
)

data class FriendActivity(
    val id: String,
    val friendId: String,
    val username: String,
    val handle: String,
    val avatarUrl: String? = null,
    val carName: String,
    val rarityTier: RarityTier,
    val pointsEarned: Int,
    val location: String,
    val timeAgo: String,
    val friendTopCar: String = "",
    val friendTopHorsepower: Int = 0,
    val friendTopSpeedMph: Int = 0,
    val isVerified: Boolean = true
)

data class PlayerSearchResult(
    val id: String,
    val username: String,
    val handle: String,
    val avatarUrl: String? = null,
    val rankTitle: String,
    val totalScore: Int,
    val spotsCount: Int,
    val topCar: String,
    val topHorsepower: Int = 0,
    val topSpeedMph: Int = 0,
    val isFriend: Boolean = false
)

data class RadarSighting(
    val carSpot: CarSpot,
    val spotterUserId: String,
    val spotterName: String,
    val spotterHandle: String,
    val spotterAvatarUrl: String? = null,
    val isOwnSpot: Boolean
)

