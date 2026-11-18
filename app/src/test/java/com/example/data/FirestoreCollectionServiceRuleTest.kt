package com.example.data

import com.example.base.FirestoreEmulatorTestBase
import com.example.model.CarSpot
import com.example.model.RarityTier
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class FirestoreCollectionServiceRuleTest : FirestoreEmulatorTestBase() {

    private fun sampleCar(id: String = "spot_${UUID.randomUUID()}"): CarSpot {
        return CarSpot(
            id = id,
            make = "Porsche",
            model = "911 GT3 RS",
            year = 2024,
            trim = "Weissach",
            rarityTier = RarityTier.EXOTIC,
            rarityScore = 94,
            pointsAwarded = 850,
            horsepower = 518,
            zeroToSixtyMph = 3.0,
            topSpeedMph = 184,
            estimatedPriceUsd = 241300L,
            engineType = "4.0L Flat-6",
            drivetrain = "RWD",
            description = "Track coupe",
            trivia = "Generates 1895 lbs downforce",
            photoUri = null,
            locationName = "Los Angeles, CA",
            latitude = 34.05,
            longitude = -118.24,
            timestamp = System.currentTimeMillis(),
            isAuthentic = true,
            antiCheatConfidence = 0.98f,
            antiCheatDetails = "Verified real capture",
            isFirstDiscovery = true
        )
    }

    @Test
    fun saveAndObserveGarage_authenticatedOwner_persistsIsolatedToAccountAndSyncsSpotter() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val service = FirestoreCollectionService(firestore)
        val spotId = "spot_${UUID.randomUUID()}"
        val car = sampleCar(spotId)

        withTimeout(DEFAULT_TIMEOUT_MS) {
            service.ensureSpotterProfile(aliceUid, "Alice Racer", null)
            service.saveCarSpotToFirestore(aliceUid, car, "Alice Racer", null)
        }

        val emittedAliceSpots = withTimeout(FLOW_TIMEOUT_MS) {
            service.observeUserGarage(aliceUid).first { list -> list.any { it.id == spotId } }
        }
        assertTrue(emittedAliceSpots.any { it.id == spotId && it.make == "Porsche" })

        val publicSpotters = withTimeout(FLOW_TIMEOUT_MS) {
            service.observePublicSpotters().first { list -> list.any { it.userId == aliceUid } }
        }
        val aliceProfile = publicSpotters.first { it.userId == aliceUid }
        assertTrue(aliceProfile.totalScore >= 850)
        assertEquals("2024 Porsche 911 GT3 RS", aliceProfile.topCar)

        // Switching to Bob must NOT return Alice's car spots
        val bobUid = signInTestUser(BOB_EMAIL)
        val bobSpots = withTimeout(FLOW_TIMEOUT_MS) {
            service.observeUserGarage(bobUid).first()
        }
        assertFalse(bobSpots.any { it.id == spotId })

        // Clean up test doc as Alice
        signInTestUser(ALICE_EMAIL)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            service.deleteCarSpotFromFirestore(aliceUid, spotId, "Alice Racer", null)
        }
    }

    @Test
    fun crossUserAccess_bobCannotReadOrMutateAliceGarage() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val service = FirestoreCollectionService(firestore)
        val spotId = "spot_${UUID.randomUUID()}"

        withTimeout(DEFAULT_TIMEOUT_MS) {
            service.saveCarSpotToFirestore(aliceUid, sampleCar(spotId), "Alice Racer", null)
        }

        signInTestUser(BOB_EMAIL)

        var readException: Exception? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                service.observeUserGarage(aliceUid).first()
            }
        } catch (e: Exception) {
            readException = e
        }
        assertNotNull("Expected PERMISSION_DENIED reading Alice garage as Bob", readException)
        val firestoreEx = checkNotNull(readException?.unwrapFirestoreException()) {
            "Expected FirebaseFirestoreException, got $readException"
        }
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx.code)

        var writeException: Exception? = null
        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                service.deleteCarSpotFromFirestore(aliceUid, spotId)
            }
        } catch (e: Exception) {
            writeException = e
        }
        assertNotNull("Expected PERMISSION_DENIED deleting Alice spot as Bob", writeException)
        assertEquals(
            FirebaseFirestoreException.Code.PERMISSION_DENIED,
            writeException?.unwrapFirestoreException()?.code
        )
    }

    @Test
    fun friends_ownerCanAddAndRemoveRealFriend_unauthenticatedDenied() = runBlocking {
        val bobUid = signInTestUser(BOB_EMAIL)
        val service = FirestoreCollectionService(firestore)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            service.ensureSpotterProfile(bobUid, "Bob Driver", null)
        }

        val aliceUid = signInTestUser(ALICE_EMAIL)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            service.ensureSpotterProfile(aliceUid, "Alice Racer", null)
            service.addFriend(aliceUid, bobUid)
        }

        val friendsAfterAdd = withTimeout(FLOW_TIMEOUT_MS) {
            service.observeFriendIds(aliceUid).first { it.contains(bobUid) }
        }
        assertTrue(friendsAfterAdd.contains(bobUid))

        withTimeout(DEFAULT_TIMEOUT_MS) {
            service.removeFriend(aliceUid, bobUid)
        }
        val friendsAfterRemove = withTimeout(FLOW_TIMEOUT_MS) {
            service.observeFriendIds(aliceUid).first { !it.contains(bobUid) }
        }
        assertFalse(friendsAfterRemove.contains(bobUid))

        // Unauthenticated listener fails with PERMISSION_DENIED
        auth.signOut()
        var unauthError: Exception? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                service.observeUserGarage(aliceUid).first()
            }
        } catch (e: Exception) {
            unauthError = e
        }
        assertNotNull("Expected PERMISSION_DENIED when signed out", unauthError)
        assertEquals(
            FirebaseFirestoreException.Code.PERMISSION_DENIED,
            unauthError?.unwrapFirestoreException()?.code
        )
    }

    @Test
    fun publicSightings_multiAccountVisibleToSignedInUsers_ownerOnlyWriteDelete() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val service = FirestoreCollectionService(firestore)
        val spotId = "radar_spot_${UUID.randomUUID()}"
        val car = sampleCar(spotId)

        withTimeout(DEFAULT_TIMEOUT_MS) {
            service.ensureSpotterProfile(aliceUid, "Alice Racer", null)
            service.saveCarSpotToFirestore(aliceUid, car, "Alice Racer", null)
        }

        // Bob can observe Alice's sighting on the public Radar feed
        val bobUid = signInTestUser(BOB_EMAIL)
        val radarForBob = withTimeout(FLOW_TIMEOUT_MS) {
            service.observePublicSightings(bobUid).first { list -> list.any { it.carSpot.id == spotId } }
        }
        val spottedByAlice = radarForBob.first { it.carSpot.id == spotId }
        assertFalse(spottedByAlice.isOwnSpot)
        assertEquals(aliceUid, spottedByAlice.spotterUserId)
        assertEquals("Alice Racer", spottedByAlice.spotterName)

        // Bob is denied deleting Alice's public_sighting directly
        var bobDeleteException: Exception? = null
        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                firestore.collection("public_sightings").document(spotId).delete()
                    .let { com.google.android.gms.tasks.Tasks.await(it) }
            }
        } catch (e: Exception) {
            bobDeleteException = e
        }
        assertNotNull("Expected PERMISSION_DENIED deleting Alice's public_sighting as Bob", bobDeleteException)
        assertEquals(
            FirebaseFirestoreException.Code.PERMISSION_DENIED,
            bobDeleteException?.unwrapFirestoreException()?.code
        )

        // Alice can delete her own car spot, which also removes it from /public_sightings
        signInTestUser(ALICE_EMAIL)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            service.deleteCarSpotFromFirestore(aliceUid, spotId, "Alice Racer", null)
        }
        val radarAfterDelete = withTimeout(FLOW_TIMEOUT_MS) {
            service.observePublicSightings(aliceUid).first { list -> list.none { it.carSpot.id == spotId } }
        }
        assertFalse(radarAfterDelete.any { it.carSpot.id == spotId })
    }

    private companion object {
        const val ALICE_EMAIL = "alice_spotter@test.com"
        const val BOB_EMAIL = "bob_spotter@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
