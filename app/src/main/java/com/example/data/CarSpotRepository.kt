package com.example.data

import com.example.model.CarSpot
import com.example.model.FriendActivity
import com.example.model.LeaderboardEntry
import com.example.model.RarityTier
import com.example.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CarSpotRepository(private val dao: CarSpotDao) {

    fun getAllSpotsFlow(): Flow<List<CarSpot>> {
        return dao.getAllSpotsFlow().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun addSpot(spot: CarSpot) {
        dao.insertSpot(CarSpotEntity.fromDomain(spot))
    }

    suspend fun deleteSpot(spot: CarSpot) {
        dao.deleteSpot(CarSpotEntity.fromDomain(spot))
    }

    fun getLeaderboard(currentUserScore: Int, currentUserUniqueSpots: Int): List<LeaderboardEntry> {
        val baseEntries = mutableListOf(
            LeaderboardEntry(
                rank = 1,
                username = "SpeedDemon_99",
                handle = "@monaco_spotter",
                totalScore = 6420,
                uniqueSpots = 8,
                topCar = "Bugatti Chiron Super Sport",
                badge = "Apex Master"
            ),
            LeaderboardEntry(
                rank = 2,
                username = "VeloceRosa",
                handle = "@maranello_v6",
                totalScore = 4850,
                uniqueSpots = 6,
                topCar = "Ferrari SF90 XX",
                badge = "Grand Tourer"
            ),
            LeaderboardEntry(
                rank = 3,
                username = "TurboApex",
                handle = "@ring_king",
                totalScore = 3910,
                uniqueSpots = 5,
                topCar = "Porsche 911 GT3 RS",
                badge = "Ring Master"
            ),
            LeaderboardEntry(
                rank = 4,
                username = "You (ApexHunter)",
                handle = "@apex_spotter",
                totalScore = currentUserScore,
                uniqueSpots = currentUserUniqueSpots,
                topCar = "Porsche 911 GT3 RS",
                isCurrentUser = true,
                badge = "Rising Hunter"
            ),
            LeaderboardEntry(
                rank = 5,
                username = "KyotoMidnight",
                handle = "@shuto_gtr",
                totalScore = 2450,
                uniqueSpots = 4,
                topCar = "Nissan GT-R Nismo",
                badge = "Street Hunter"
            ),
            LeaderboardEntry(
                rank = 6,
                username = "CanyonCarver",
                handle = "@mulholland_run",
                totalScore = 1880,
                uniqueSpots = 3,
                topCar = "Chevrolet Corvette Z06",
                badge = "Spotter"
            ),
            LeaderboardEntry(
                rank = 7,
                username = "SilverstonePro",
                handle = "@brit_speed",
                totalScore = 1420,
                uniqueSpots = 2,
                topCar = "McLaren 750S",
                badge = "Rookie"
            )
        )

        // Sort by totalScore descending and assign accurate ranks
        val sorted = baseEntries.sortedByDescending { it.totalScore }
        return sorted.mapIndexed { index, entry ->
            entry.copy(rank = index + 1)
        }
    }

    fun getFriendsFeed(): List<FriendActivity> {
        return listOf(
            FriendActivity(
                id = "f_act_1",
                username = "Marcus Sterling",
                handle = "@marcus_supercars",
                carName = "McLaren 750S Spider",
                rarityTier = RarityTier.EXOTIC,
                pointsEarned = 900,
                location = "Beverly Hills, CA",
                timeAgo = "12m ago"
            ),
            FriendActivity(
                id = "f_act_2",
                username = "Elena Rostova",
                handle = "@elena_ring",
                carName = "BMW M4 CSL",
                rarityTier = RarityTier.RARE,
                pointsEarned = 650,
                location = "Nürburg, Germany",
                timeAgo = "44m ago"
            ),
            FriendActivity(
                id = "f_act_3",
                username = "Kenji Takahashi",
                handle = "@kenji_drift",
                carName = "Nissan GT-R Nismo",
                rarityTier = RarityTier.RARE,
                pointsEarned = 700,
                location = "Yokohama, Japan",
                timeAgo = "2h ago"
            ),
            FriendActivity(
                id = "f_act_4",
                username = "Chloe Dupont",
                handle = "@chloe_monaco",
                carName = "Bugatti Chiron Super Sport",
                rarityTier = RarityTier.LEGENDARY,
                pointsEarned = 2500,
                location = "Monte Carlo, Monaco",
                timeAgo = "5h ago"
            )
        )
    }
}
