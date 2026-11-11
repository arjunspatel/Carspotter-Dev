package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.example.model.CarSpot
import java.net.URLEncoder

object SocialShareUtil {

    fun createShareMessage(car: CarSpot): String {
        return "I just spotted a ${car.make} ${car.model} with CarSpotter! #CarSpotter #CarSpotting"
    }

    fun createDetailedShareMessage(car: CarSpot): String {
        return """
            🏆 I just spotted a ${car.year} ${car.make} ${car.model} (${car.trim}) with CarSpotter!
            ⭐ Rarity: ${car.rarityTier.label} (${car.rarityScore}/100)
            🔥 Specs: ${car.horsepower} HP • 0-60 in ${car.zeroToSixtyMph}s • Top Speed ${car.topSpeedMph} MPH
            📍 Spotted: ${car.locationName}
            #CarSpotter #CarSpotting #${car.make.replace(" ", "")}
        """.trimIndent()
    }

    /**
     * Share directly to Twitter / X with pre-populated message and hashtags
     */
    fun shareToTwitter(context: Context, car: CarSpot) {
        val message = createShareMessage(car)
        val tweetUrl = "https://twitter.com/intent/tweet?text=" + URLEncoder.encode(message, "UTF-8")
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(tweetUrl)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            shareGeneric(context, car)
        }
    }

    /**
     * Share to Facebook
     */
    fun shareToFacebook(context: Context, car: CarSpot) {
        val message = createShareMessage(car)
        val fbUrl = "https://www.facebook.com/sharer/sharer.php?quote=" + URLEncoder.encode(message, "UTF-8")
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(fbUrl)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            shareGeneric(context, car)
        }
    }

    /**
     * Share to Instagram (Direct intent or system fallback)
     */
    fun shareToInstagram(context: Context, car: CarSpot) {
        val instagramIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            setPackage("com.instagram.android")
            putExtra(Intent.EXTRA_TEXT, createShareMessage(car))
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(instagramIntent)
        } catch (e: Exception) {
            // If Instagram is not installed, open standard chooser with helpful toast
            Toast.makeText(context, "Instagram app not found, opening share options", Toast.LENGTH_SHORT).show()
            shareGeneric(context, car)
        }
    }

    /**
     * System chooser (supports all installed apps: Messages, WhatsApp, Telegram, etc.)
     */
    fun shareGeneric(context: Context, car: CarSpot) {
        val detailedText = createDetailedShareMessage(car)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "CarSpotter: Spotted a ${car.make} ${car.model}!")
            putExtra(Intent.EXTRA_TEXT, detailedText)
            if (car.photoUri != null && !car.photoUri.startsWith("res:")) {
                try {
                    putExtra(Intent.EXTRA_STREAM, Uri.parse(car.photoUri))
                    type = "image/*"
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } catch (_: Exception) {}
            }
        }
        val chooser = Intent.createChooser(shareIntent, "Share with CarSpotters via")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
}
