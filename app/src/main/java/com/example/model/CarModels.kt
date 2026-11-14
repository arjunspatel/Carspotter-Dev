package com.example.model

enum class RarityTier(val label: String, val basePoints: Int, val colorHex: Long) {
    COMMON("Common", 50, 0xFF64748BL),
    UNCOMMON("Uncommon", 150, 0xFF0284C7L),
    RARE("Rare", 400, 0xFF4F46E5L),
    EXOTIC("Exotic", 850, 0xFFD97706L),
    LEGENDARY("Hypercar / Legendary", 1800, 0xFFDC2626L)
}

data class CarSpot(
    val id: String,
    val make: String,
    val model: String,
    val year: Int,
    val trim: String,
    val rarityTier: RarityTier,
    val rarityScore: Int, // 1 to 100
    val pointsAwarded: Int,
    val horsepower: Int,
    val zeroToSixtyMph: Double,
    val topSpeedMph: Int,
    val estimatedPriceUsd: Long,
    val engineType: String,
    val drivetrain: String,
    val description: String,
    val trivia: String,
    val photoUri: String?, // "res:car_porsche", "res:car_ferrari", or file/content URI
    val locationName: String,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long,
    val isAuthentic: Boolean,
    val antiCheatConfidence: Float,
    val antiCheatDetails: String,
    val isFirstDiscovery: Boolean = false
)

data class AntiCheatResult(
    val isAuthentic: Boolean,
    val confidence: Float,
    val verdict: String,
    val details: String,
    val detectionFlags: List<String>
)

data class UserProfile(
    val id: String = "user_current",
    val username: String = "ApexHunter",
    val handle: String = "@apex_spotter",
    val avatarUrl: String? = null,
    val totalScore: Int = 1700,
    val rankTitle: String = "Supercar Scout (Tier III)",
    val uniqueSpotsCount: Int = 2,
    val totalSpotsCount: Int = 2,
    val globalRank: Int = 14
)

data class LeaderboardEntry(
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
    val username: String,
    val handle: String,
    val carName: String,
    val rarityTier: RarityTier,
    val pointsEarned: Int,
    val location: String,
    val timeAgo: String,
    val isVerified: Boolean = true
)

data class PlayerSearchResult(
    val id: String,
    val username: String,
    val handle: String,
    val rankTitle: String,
    val totalScore: Int,
    val spotsCount: Int,
    val topCar: String,
    val isFriend: Boolean = false,
    val mutualFriends: Int = 0
)
