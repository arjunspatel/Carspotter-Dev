package com.example.data

import android.content.Context
import android.util.Log
import com.example.R
import com.example.model.CarSpot
import com.example.model.RadarSighting
import com.example.model.RarityTier
import com.example.model.RecentSpotSummary
import com.example.model.SpotterProfile
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

private const val TAG = "FirestoreService"

class FirestoreCollectionService(
    private val firestore: FirebaseFirestore
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun userGarageCollection(userId: String) =
        firestore.collection("users").document(userId).collection("car_spots")

    private fun userFriendsCollection(userId: String) =
        firestore.collection("users").document(userId).collection("friends")

    private fun spotterDoc(userId: String) =
        firestore.collection("spotters").document(userId)

    private fun publicSightingDoc(sightingId: String) =
        firestore.collection("public_sightings").document(sightingId)

    /**
     * Real-time snapshot listener of a specific authenticated user's garage.
     */
    fun observeUserGarage(userId: String): Flow<List<CarSpot>> = callbackFlow {
        val registration = userGarageCollection(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val spots = snapshot?.documents
                    ?.mapNotNull { doc -> doc.toCarSpotOrNull() }
                    ?.sortedByDescending { it.timestamp }
                    ?: emptyList()
                trySend(spots)
            }
        awaitClose { registration.remove() }
    }

    /**
     * Real-time snapshot listener of recent public vehicle sightings across all accounts.
     */
    fun observePublicSightings(currentUserId: String, maxCount: Int = 100): Flow<List<RadarSighting>> = callbackFlow {
        val registration = firestore.collection("public_sightings")
            .whereEqualTo("isPublic", true)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val sightings = snapshot?.documents
                    ?.mapNotNull { doc -> doc.toRadarSightingOrNull(currentUserId) }
                    ?.sortedByDescending { it.carSpot.timestamp }
                    ?.take(maxCount)
                    ?: emptyList()
                trySend(sightings)
            }
        awaitClose { registration.remove() }
    }

    /**
     * Real-time snapshot listener of all public non-PII Spotter profiles.
     */
    fun observePublicSpotters(): Flow<List<SpotterProfile>> = callbackFlow {
        val registration = firestore.collection("spotters")
            .whereEqualTo("isPublicProfile", true)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val spotters = snapshot?.documents
                    ?.mapNotNull { doc -> doc.toSpotterProfileOrNull() }
                    ?.sortedByDescending { it.totalScore }
                    ?: emptyList()
                trySend(spotters)
            }
        awaitClose { registration.remove() }
    }

    /**
     * Real-time snapshot listener of the current user's added friend UIDs.
     */
    fun observeFriendIds(userId: String): Flow<Set<String>> = callbackFlow {
        val registration = userFriendsCollection(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val ids = snapshot?.documents?.map { it.id }?.toSet() ?: emptySet()
                trySend(ids)
            }
        awaitClose { registration.remove() }
    }

    /**
     * Ensures a public SpotterProfile document exists for the signed-in user, keeps display metadata fresh,
     * and backfills existing garage spots into /public_sightings so older spots appear on the Radar map.
     */
    suspend fun ensureSpotterProfile(
        userId: String,
        displayName: String?,
        avatarUrl: String?
    ) {
        val path = "spotters/$userId"
        try {
            val cleanName = sanitizeDisplayName(displayName)
            val cleanHandle = buildPublicHandle(cleanName, userId)
            val cleanAvatar = avatarUrl?.takeIf { it.isNotBlank() }?.take(512)

            val docRef = spotterDoc(userId)
            val existing = docRef.get().await()
            val currentSpots = fetchUserGarageOnce(userId)
            syncSpotterStatsInternal(
                userId = userId,
                username = cleanName,
                handle = cleanHandle,
                avatarUrl = cleanAvatar,
                spots = currentSpots,
                exists = existing.exists()
            )

            // Backfill any user garage spots missing from /public_sightings
            currentSpots.take(25).forEach { spot ->
                runCatching {
                    val pubRef = publicSightingDoc(spot.id)
                    if (!pubRef.get().await().exists()) {
                        publishSightingInternal(userId, cleanName, cleanHandle, cleanAvatar, spot)
                    }
                }
            }
        } catch (e: Exception) {
            if (e is FirebaseFirestoreException) {
                handleFirestoreError(e, OperationType.WRITE, path, userId)
            }
            Log.e(TAG, "ensureSpotterProfile failed for $userId: ${e.message}", e)
            throw e
        }
    }

    /**
     * Saves a verified vehicle spot to the user's private cloud garage, publishes it to /public_sightings
     * for the multi-account Radar map, and updates the user's public Spotter profile stats.
     */
    suspend fun saveCarSpotToFirestore(
        userId: String,
        car: CarSpot,
        displayName: String? = null,
        avatarUrl: String? = null
    ) {
        val path = "users/$userId/car_spots/${car.id}"
        try {
            val payload = mutableMapOf<String, Any?>(
                "userId" to userId,
                "make" to car.make.take(80),
                "model" to car.model.take(100),
                "year" to car.year,
                "trim" to car.trim.take(120),
                "rarityTier" to car.rarityTier.name,
                "rarityScore" to car.rarityScore.coerceIn(1, 100),
                "pointsAwarded" to car.pointsAwarded.coerceIn(0, 100000),
                "horsepower" to car.horsepower.coerceIn(0, 10000),
                "zeroToSixtyMph" to car.zeroToSixtyMph.coerceIn(0.0, 100.0),
                "topSpeedMph" to car.topSpeedMph.coerceIn(0, 1000),
                "estimatedPriceUsd" to car.estimatedPriceUsd.coerceAtLeast(0L),
                "engineType" to car.engineType.take(160),
                "drivetrain" to car.drivetrain.take(160),
                "description" to car.description.take(1500),
                "trivia" to car.trivia.take(1500),
                "photoUri" to car.photoUri?.take(512),
                "locationName" to car.locationName.take(200),
                "latitude" to car.latitude.coerceIn(-90.0, 90.0),
                "longitude" to car.longitude.coerceIn(-180.0, 180.0),
                "timestamp" to car.timestamp.coerceAtLeast(0L),
                "isAuthentic" to car.isAuthentic,
                "antiCheatConfidence" to car.antiCheatConfidence.toDouble().coerceIn(0.0, 1.0),
                "antiCheatDetails" to car.antiCheatDetails.take(1500),
                "isFirstDiscovery" to car.isFirstDiscovery,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }

            userGarageCollection(userId)
                .document(car.id)
                .set(payload)
                .await()

            val cleanName = sanitizeDisplayName(displayName)
            val cleanHandle = buildPublicHandle(cleanName, userId)
            val cleanAvatar = avatarUrl?.takeIf { it.isNotBlank() }?.take(512)
            publishSightingInternal(userId, cleanName, cleanHandle, cleanAvatar, car)

            Log.d(TAG, "Saved car spot ${car.id} to $path and published public sighting")
            syncSpotterFromGarage(userId, displayName, avatarUrl)
        } catch (e: Exception) {
            if (e is FirebaseFirestoreException) {
                handleFirestoreError(e, OperationType.CREATE, path, userId)
            }
            Log.e(TAG, "Failed saving car spot to Firestore: ${e.message}", e)
            throw e
        }
    }

    /**
     * Deletes a vehicle spot from the user's private cloud garage, removes its public radar pin,
     * and recomputes their public Spotter profile stats.
     */
    suspend fun deleteCarSpotFromFirestore(
        userId: String,
        spotId: String,
        displayName: String? = null,
        avatarUrl: String? = null
    ) {
        val path = "users/$userId/car_spots/$spotId"
        try {
            userGarageCollection(userId)
                .document(spotId)
                .delete()
                .await()
            runCatching { publicSightingDoc(spotId).delete().await() }
            syncSpotterFromGarage(userId, displayName, avatarUrl)
        } catch (e: Exception) {
            if (e is FirebaseFirestoreException) {
                handleFirestoreError(e, OperationType.DELETE, path, userId)
            }
            Log.e(TAG, "Failed deleting car spot from Firestore: ${e.message}", e)
            throw e
        }
    }

    suspend fun addFriend(userId: String, friendId: String) {
        if (userId == friendId) return
        val path = "users/$userId/friends/$friendId"
        try {
            val payload = mapOf(
                "userId" to userId,
                "friendId" to friendId,
                "createdAt" to FieldValue.serverTimestamp()
            )
            userFriendsCollection(userId).document(friendId).set(payload).await()
        } catch (e: Exception) {
            if (e is FirebaseFirestoreException) {
                handleFirestoreError(e, OperationType.CREATE, path, userId)
            }
            throw e
        }
    }

    suspend fun removeFriend(userId: String, friendId: String) {
        val path = "users/$userId/friends/$friendId"
        try {
            userFriendsCollection(userId).document(friendId).delete().await()
        } catch (e: Exception) {
            if (e is FirebaseFirestoreException) {
                handleFirestoreError(e, OperationType.DELETE, path, userId)
            }
            throw e
        }
    }

    suspend fun getUserGarageOnce(userId: String): List<CarSpot> {
        val path = "users/$userId/car_spots"
        return try {
            fetchUserGarageOnce(userId)
        } catch (e: Exception) {
            if (e is FirebaseFirestoreException) {
                handleFirestoreError(e, OperationType.LIST, path, userId)
            }
            throw e
        }
    }

    private suspend fun publishSightingInternal(
        userId: String,
        spotterName: String,
        spotterHandle: String,
        spotterAvatarUrl: String?,
        car: CarSpot
    ) {
        val publicPayload = mutableMapOf<String, Any?>(
            "userId" to userId,
            "spotterName" to spotterName.take(60),
            "spotterHandle" to spotterHandle.take(60),
            "spotterAvatarUrl" to spotterAvatarUrl?.take(512),
            "make" to car.make.take(80),
            "model" to car.model.take(100),
            "year" to car.year.coerceIn(1885, 2100),
            "trim" to car.trim.take(120),
            "rarityTier" to car.rarityTier.name,
            "rarityScore" to car.rarityScore.coerceIn(1, 100),
            "pointsAwarded" to car.pointsAwarded.coerceIn(0, 100000),
            "horsepower" to car.horsepower.coerceIn(0, 10000),
            "zeroToSixtyMph" to car.zeroToSixtyMph.coerceIn(0.0, 100.0),
            "topSpeedMph" to car.topSpeedMph.coerceIn(0, 1000),
            "estimatedPriceUsd" to car.estimatedPriceUsd.coerceAtLeast(0L),
            "engineType" to car.engineType.take(160),
            "drivetrain" to car.drivetrain.take(160),
            "description" to car.description.take(1500),
            "trivia" to car.trivia.take(1500),
            "locationName" to car.locationName.take(200),
            "latitude" to car.latitude.coerceIn(-90.0, 90.0),
            "longitude" to car.longitude.coerceIn(-180.0, 180.0),
            "timestamp" to car.timestamp.coerceAtLeast(0L),
            "isAuthentic" to car.isAuthentic,
            "antiCheatConfidence" to car.antiCheatConfidence.toDouble().coerceIn(0.0, 1.0),
            "antiCheatDetails" to car.antiCheatDetails.take(1500),
            "isPublic" to true,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        ).filterValues { it != null }

        publicSightingDoc(car.id).set(publicPayload).await()
    }

    private suspend fun fetchUserGarageOnce(userId: String): List<CarSpot> {
        val snap = userGarageCollection(userId).get().await()
        return snap.documents.mapNotNull { it.toCarSpotOrNull() }.sortedByDescending { it.timestamp }
    }

    private suspend fun syncSpotterFromGarage(
        userId: String,
        displayName: String?,
        avatarUrl: String?
    ) {
        val existing = spotterDoc(userId).get().await()
        val spots = fetchUserGarageOnce(userId)
        val existingName = existing.getString("username")
        val cleanName = sanitizeDisplayName(displayName ?: existingName)
        val cleanHandle = existing.getString("handle")?.takeIf { it.isNotBlank() }
            ?: buildPublicHandle(cleanName, userId)
        val cleanAvatar = (avatarUrl ?: existing.getString("avatarUrl"))?.takeIf { it.isNotBlank() }?.take(512)

        syncSpotterStatsInternal(
            userId = userId,
            username = cleanName,
            handle = cleanHandle,
            avatarUrl = cleanAvatar,
            spots = spots,
            exists = existing.exists()
        )
    }

    private suspend fun syncSpotterStatsInternal(
        userId: String,
        username: String,
        handle: String,
        avatarUrl: String?,
        spots: List<CarSpot>,
        exists: Boolean
    ) {
        val totalScore = spots.sumOf { it.pointsAwarded }
        val uniqueSpotsCount = spots.map { "${it.make}_${it.model}" }.distinct().size
        val topVehicle = spots.maxByOrNull { it.horsepower } ?: spots.maxByOrNull { it.pointsAwarded }
        val topCarName = topVehicle?.let { "${it.year} ${it.make} ${it.model}".trim() } ?: ""
        val topHp = topVehicle?.horsepower ?: 0
        val topSpeed = spots.maxOfOrNull { it.topSpeedMph } ?: 0
        val rankTitle = computeRankTitle(totalScore)

        val recentSummaries = spots
            .sortedByDescending { it.timestamp }
            .take(5)
            .map { spot ->
                mapOf(
                    "id" to spot.id.take(128),
                    "carName" to "${spot.year} ${spot.make} ${spot.model}".trim().take(120),
                    "rarityTier" to spot.rarityTier.name,
                    "pointsEarned" to spot.pointsAwarded.coerceIn(0, 100000),
                    "horsepower" to spot.horsepower.coerceIn(0, 10000),
                    "topSpeedMph" to spot.topSpeedMph.coerceIn(0, 1000),
                    "locationName" to spot.locationName.take(200),
                    "timestamp" to spot.timestamp.coerceAtLeast(0L),
                    "isVerified" to spot.isAuthentic
                )
            }

        val docRef = spotterDoc(userId)
        if (!exists) {
            val createPayload = mutableMapOf<String, Any?>(
                "userId" to userId,
                "username" to username,
                "handle" to handle,
                "avatarUrl" to avatarUrl,
                "totalScore" to totalScore,
                "uniqueSpotsCount" to uniqueSpotsCount,
                "totalSpotsCount" to spots.size,
                "rankTitle" to rankTitle,
                "topCar" to topCarName,
                "topHorsepower" to topHp,
                "topSpeedMph" to topSpeed,
                "isPublicProfile" to true,
                "recentSpots" to recentSummaries,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }
            docRef.set(createPayload).await()
        } else {
            val updatePayload = mutableMapOf<String, Any?>(
                "username" to username,
                "handle" to handle,
                "avatarUrl" to avatarUrl,
                "totalScore" to totalScore,
                "uniqueSpotsCount" to uniqueSpotsCount,
                "totalSpotsCount" to spots.size,
                "rankTitle" to rankTitle,
                "topCar" to topCarName,
                "topHorsepower" to topHp,
                "topSpeedMph" to topSpeed,
                "isPublicProfile" to true,
                "recentSpots" to recentSummaries,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.update(updatePayload).await()
        }
    }

    companion object {
        fun computeRankTitle(score: Int): String = when {
            score >= 5000 -> "Apex Legend (Tier V)"
            score >= 3000 -> "Supercar Master (Tier IV)"
            score >= 1500 -> "Exotic Hunter (Tier III)"
            score >= 500 -> "Street Scout (Tier II)"
            else -> "Rookie Spotter (Tier I)"
        }

        fun sanitizeDisplayName(raw: String?): String {
            val trimmed = raw?.trim().orEmpty()
            return if (trimmed.isNotEmpty()) trimmed.take(60) else "Spotter"
        }

        fun buildPublicHandle(displayName: String, userId: String): String {
            val slug = displayName.lowercase()
                .replace(Regex("[^a-z0-9]+"), "_")
                .trim('_')
                .take(18)
            val base = slug.ifEmpty { "spotter_${userId.take(6).lowercase()}" }
            return "@$base".take(60)
        }
    }
}

private fun DocumentSnapshot.toCarSpotOrNull(): CarSpot? {
    val make = getString("make") ?: return null
    val model = getString("model") ?: return null
    val tierName = getString("rarityTier") ?: RarityTier.COMMON.name
    val tier = runCatching { RarityTier.valueOf(tierName) }.getOrDefault(RarityTier.COMMON)
    return CarSpot(
        id = id,
        make = make,
        model = model,
        year = (getLong("year") ?: 0L).toInt(),
        trim = getString("trim") ?: "",
        rarityTier = tier,
        rarityScore = (getLong("rarityScore") ?: 1L).toInt(),
        pointsAwarded = (getLong("pointsAwarded") ?: 0L).toInt(),
        horsepower = (getLong("horsepower") ?: 0L).toInt(),
        zeroToSixtyMph = getDouble("zeroToSixtyMph") ?: 0.0,
        topSpeedMph = (getLong("topSpeedMph") ?: 0L).toInt(),
        estimatedPriceUsd = getLong("estimatedPriceUsd") ?: 0L,
        engineType = getString("engineType") ?: "",
        drivetrain = getString("drivetrain") ?: "",
        description = getString("description") ?: "",
        trivia = getString("trivia") ?: "",
        photoUri = getString("photoUri"),
        locationName = getString("locationName") ?: "",
        latitude = getDouble("latitude") ?: 0.0,
        longitude = getDouble("longitude") ?: 0.0,
        timestamp = getLong("timestamp") ?: 0L,
        isAuthentic = getBoolean("isAuthentic") ?: true,
        antiCheatConfidence = (getDouble("antiCheatConfidence") ?: 1.0).toFloat(),
        antiCheatDetails = getString("antiCheatDetails") ?: "",
        isFirstDiscovery = getBoolean("isFirstDiscovery") ?: false
    )
}

private fun DocumentSnapshot.toRadarSightingOrNull(currentUserId: String): RadarSighting? {
    val ownerId = getString("userId") ?: return null
    val carSpot = toCarSpotOrNull() ?: return null
    val spotterName = getString("spotterName") ?: "Spotter"
    val spotterHandle = getString("spotterHandle") ?: "@spotter"
    return RadarSighting(
        carSpot = carSpot,
        spotterUserId = ownerId,
        spotterName = spotterName,
        spotterHandle = spotterHandle,
        spotterAvatarUrl = getString("spotterAvatarUrl"),
        isOwnSpot = ownerId == currentUserId
    )
}

@Suppress("UNCHECKED_CAST")
private fun DocumentSnapshot.toSpotterProfileOrNull(): SpotterProfile? {
    val uid = getString("userId") ?: id
    val username = getString("username") ?: return null
    val handle = getString("handle") ?: "@spotter"
    val rawRecent = get("recentSpots") as? List<Map<String, Any?>> ?: emptyList()
    val recentSpots = rawRecent.mapNotNull { item ->
        val spotId = item["id"] as? String ?: return@mapNotNull null
        val carName = item["carName"] as? String ?: return@mapNotNull null
        val tierStr = item["rarityTier"] as? String ?: RarityTier.COMMON.name
        val tier = runCatching { RarityTier.valueOf(tierStr) }.getOrDefault(RarityTier.COMMON)
        RecentSpotSummary(
            id = spotId,
            carName = carName,
            rarityTier = tier,
            pointsEarned = ((item["pointsEarned"] as? Number)?.toInt()) ?: 0,
            horsepower = ((item["horsepower"] as? Number)?.toInt()) ?: 0,
            topSpeedMph = ((item["topSpeedMph"] as? Number)?.toInt()) ?: 0,
            locationName = item["locationName"] as? String ?: "",
            timestamp = ((item["timestamp"] as? Number)?.toLong()) ?: 0L,
            isVerified = (item["isVerified"] as? Boolean) ?: true
        )
    }

    return SpotterProfile(
        userId = uid,
        username = username,
        handle = handle,
        avatarUrl = getString("avatarUrl"),
        totalScore = (getLong("totalScore") ?: 0L).toInt(),
        uniqueSpotsCount = (getLong("uniqueSpotsCount") ?: 0L).toInt(),
        totalSpotsCount = (getLong("totalSpotsCount") ?: 0L).toInt(),
        rankTitle = getString("rankTitle") ?: "Rookie Spotter (Tier I)",
        topCar = getString("topCar") ?: "",
        topHorsepower = (getLong("topHorsepower") ?: 0L).toInt(),
        topSpeedMph = (getLong("topSpeedMph") ?: 0L).toInt(),
        isPublicProfile = getBoolean("isPublicProfile") ?: true,
        recentSpots = recentSpots
    )
}
