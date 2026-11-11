package com.example.data

import android.content.Context
import android.util.Log
import com.example.R
import com.example.model.CarSpot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

private const val TAG = "FirestoreService"

class FirestoreCollectionService(context: Context) {

    private val firestore: FirebaseFirestore by lazy {
        val databaseId = try {
            val resId = context.resources.getIdentifier("firestore_database_id", "string", context.packageName)
            if (resId != 0) context.getString(resId) else null
        } catch (_: Exception) {
            null
        }

        if (!databaseId.isNullOrBlank()) {
            Log.d(TAG, "Initializing Firestore with named database ID: $databaseId")
            FirebaseFirestore.getInstance(databaseId)
        } else {
            Log.d(TAG, "Initializing default Firestore instance")
            FirebaseFirestore.getInstance()
        }
    }

    /**
     * Stores a vehicle spot in the cloud Firestore collection
     */
    suspend fun saveCarSpotToFirestore(car: CarSpot): Boolean {
        return try {
            val spotMap = hashMapOf(
                "id" to car.id,
                "make" to car.make,
                "model" to car.model,
                "year" to car.year,
                "trim" to car.trim,
                "rarityTier" to car.rarityTier.name,
                "rarityScore" to car.rarityScore,
                "pointsAwarded" to car.pointsAwarded,
                "horsepower" to car.horsepower,
                "zeroToSixtyMph" to car.zeroToSixtyMph,
                "topSpeedMph" to car.topSpeedMph,
                "estimatedPriceUsd" to car.estimatedPriceUsd,
                "engineType" to car.engineType,
                "drivetrain" to car.drivetrain,
                "description" to car.description,
                "trivia" to car.trivia,
                "locationName" to car.locationName,
                "latitude" to car.latitude,
                "longitude" to car.longitude,
                "timestamp" to car.timestamp,
                "isAuthentic" to car.isAuthentic,
                "antiCheatConfidence" to car.antiCheatConfidence,
                "antiCheatDetails" to car.antiCheatDetails
            )

            firestore.collection("car_spots")
                .document(car.id)
                .set(spotMap)
                .await()

            Log.d(TAG, "Successfully saved car spot ${car.id} to Firestore")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed saving car spot to Firestore: ${e.message}", e)
            false
        }
    }

    /**
     * Delete car spot from cloud Firestore
     */
    suspend fun deleteCarSpotFromFirestore(spotId: String): Boolean {
        return try {
            firestore.collection("car_spots")
                .document(spotId)
                .delete()
                .await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed deleting car spot from Firestore: ${e.message}", e)
            false
        }
    }
}
