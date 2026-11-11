package com.example

import com.example.model.CarSpot
import com.example.model.RarityTier
import com.example.util.SocialShareUtil
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testSocialShareMessageGeneration() {
        val spot = CarSpot(
            id = "test_1",
            make = "Porsche",
            model = "911 GT3 RS",
            year = 2024,
            trim = "Weissach Package",
            rarityTier = RarityTier.EXOTIC,
            rarityScore = 92,
            pointsAwarded = 850,
            horsepower = 518,
            zeroToSixtyMph = 3.0,
            topSpeedMph = 184,
            estimatedPriceUsd = 241300,
            engineType = "4.0L Naturally Aspirated Flat-6",
            drivetrain = "RWD • 7-Speed PDK",
            description = "Ultimate track car",
            trivia = "Downforce record",
            photoUri = null,
            locationName = "Angeles Crest, CA",
            latitude = 34.0,
            longitude = -118.0,
            timestamp = System.currentTimeMillis(),
            isAuthentic = true,
            antiCheatConfidence = 0.99f,
            antiCheatDetails = "Verified"
        )

        val message = SocialShareUtil.createShareMessage(spot)
        assertEquals("I just spotted a Porsche 911 GT3 RS with CarSpotter! #CarSpotter #CarSpotting", message)

        val detailedMessage = SocialShareUtil.createDetailedShareMessage(spot)
        assertTrue(detailedMessage.contains("518 HP"))
        assertTrue(detailedMessage.contains("Angeles Crest, CA"))
        assertTrue(detailedMessage.contains("#CarSpotter #CarSpotting"))
    }

    @Test
    fun testRarityPoints() {
        assertTrue(RarityTier.LEGENDARY.basePoints > RarityTier.EXOTIC.basePoints)
        assertTrue(RarityTier.EXOTIC.basePoints > RarityTier.RARE.basePoints)
        assertTrue(RarityTier.RARE.basePoints > RarityTier.UNCOMMON.basePoints)
        assertTrue(RarityTier.UNCOMMON.basePoints > RarityTier.COMMON.basePoints)
    }
}
