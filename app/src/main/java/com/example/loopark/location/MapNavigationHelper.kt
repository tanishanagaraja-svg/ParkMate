package com.example.loopark.location

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

object MapNavigationHelper {

    /**
     * Opens navigation directions to the given latitude and longitude.
     * 1. Attempts to open Google Maps directly with walking navigation.
     * 2. If Google Maps is not installed, attempts to open an available map app using geo URI.
     * 3. If no map app is available, opens Google Maps directions in the web browser.
     */
    fun openDirections(
        context: Context,
        latitude: Double,
        longitude: Double,
        onError: (String) -> Unit
    ) {
        // Step 1: Open Google Maps directly with walking navigation mode
        val googleMapsUri = Uri.parse("google.navigation:q=$latitude,$longitude&mode=w")
        val googleMapsIntent = Intent(Intent.ACTION_VIEW, googleMapsUri).apply {
            setPackage("com.google.android.apps.maps")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(googleMapsIntent)
            return
        } catch (e: ActivityNotFoundException) {
            // Google Maps app is not installed, proceed to fallback
        } catch (e: Exception) {
            // Other intent handling error, proceed to fallback
        }

        // Step 2: Fallback to any installed map application via geo: URI
        val geoUri = Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude(Parked+Car)")
        val geoIntent = Intent(Intent.ACTION_VIEW, geoUri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(geoIntent)
            return
        } catch (e: ActivityNotFoundException) {
            // No native map application available, proceed to web fallback
        } catch (e: Exception) {
            // Other intent error, proceed to web fallback
        }

        // Step 3: Fallback to Google Maps directions in mobile web browser
        val webUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$latitude,$longitude&travelmode=walking")
        val browserIntent = Intent(Intent.ACTION_VIEW, webUri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(browserIntent)
        } catch (e: Exception) {
            onError("Unable to launch map or browser for navigation: ${e.localizedMessage ?: "Unknown error"}")
        }
    }
}
