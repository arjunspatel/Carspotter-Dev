package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.CarSpot
import com.example.model.RarityTier

@Entity(tableName = "car_spots")
data class CarSpotEntity(
    @PrimaryKey
    val id: String,
    val make: String,
    val model: String,
    val year: Int,
    val trim: String,
    val rarityTier: String,
    val rarityScore: Int,
    val pointsAwarded: Int,
    val horsepower: Int,
    val zeroToSixtyMph: Double,
    val topSpeedMph: Int,
    val estimatedPriceUsd: Long,
    val engineType: String,
    val drivetrain: String,
    val description: String,
    val trivia: String,
    val photoUri: String?,
    val locationName: String,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long,
    val isAuthentic: Boolean,
    val antiCheatConfidence: Float,
    val antiCheatDetails: String,
    val isFirstDiscovery: Boolean
) {
    fun toDomain(): CarSpot {
        val tier = try {
            RarityTier.valueOf(rarityTier)
        } catch (_: Exception) {
            RarityTier.RARE
        }
        return CarSpot(
            id = id,
            make = make,
            model = model,
            year = year,
            trim = trim,
            rarityTier = tier,
            rarityScore = rarityScore,
            pointsAwarded = pointsAwarded,
            horsepower = horsepower,
            zeroToSixtyMph = zeroToSixtyMph,
            topSpeedMph = topSpeedMph,
            estimatedPriceUsd = estimatedPriceUsd,
            engineType = engineType,
            drivetrain = drivetrain,
            description = description,
            trivia = trivia,
            photoUri = photoUri,
            locationName = locationName,
            latitude = latitude,
            longitude = longitude,
            timestamp = timestamp,
            isAuthentic = isAuthentic,
            antiCheatConfidence = antiCheatConfidence,
            antiCheatDetails = antiCheatDetails,
            isFirstDiscovery = isFirstDiscovery
        )
    }

    companion object {
        fun fromDomain(car: CarSpot): CarSpotEntity {
            return CarSpotEntity(
                id = car.id,
                make = car.make,
                model = car.model,
                year = car.year,
                trim = car.trim,
                rarityTier = car.rarityTier.name,
                rarityScore = car.rarityScore,
                pointsAwarded = car.pointsAwarded,
                horsepower = car.horsepower,
                zeroToSixtyMph = car.zeroToSixtyMph,
                topSpeedMph = car.topSpeedMph,
                estimatedPriceUsd = car.estimatedPriceUsd,
                engineType = car.engineType,
                drivetrain = car.drivetrain,
                description = car.description,
                trivia = car.trivia,
                photoUri = car.photoUri,
                locationName = car.locationName,
                latitude = car.latitude,
                longitude = car.longitude,
                timestamp = car.timestamp,
                isAuthentic = car.isAuthentic,
                antiCheatConfidence = car.antiCheatConfidence,
                antiCheatDetails = car.antiCheatDetails,
                isFirstDiscovery = car.isFirstDiscovery
            )
        }
    }
}
