package com.example.loopark.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.roundToInt

object LocationHelper {

    fun hasLocationPermission(context: Context): Boolean {
        val fineLocationGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseLocationGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return fineLocationGranted || coarseLocationGranted
    }

    fun isGpsEnabled(context: Context): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return false
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    /**
     * Single-shot high-accuracy current location request via FusedLocationProviderClient.
     */
    @SuppressLint("MissingPermission")
    fun getCurrentLocation(
        context: Context,
        onSuccess: (Location) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!hasLocationPermission(context)) {
            onError("Location permission is not granted. Please allow location access.")
            return
        }

        if (!isGpsEnabled(context)) {
            onError("GPS is disabled on your device. Please turn on Location services in Settings.")
            return
        }

        val fusedClient = LocationServices.getFusedLocationProviderClient(context)
        val cancellationTokenSource = CancellationTokenSource()

        fusedClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            cancellationTokenSource.token
        ).addOnSuccessListener { location ->
            if (location != null) {
                onSuccess(location)
            } else {
                fusedClient.lastLocation.addOnSuccessListener { lastLoc ->
                    if (lastLoc != null) {
                        onSuccess(lastLoc)
                    } else {
                        onError("Unable to acquire GPS fix. Please ensure you have a clear view of the sky.")
                    }
                }.addOnFailureListener {
                    onError("Failed to acquire GPS location. Please try again.")
                }
            }
        }.addOnFailureListener { exception ->
            onError(exception.localizedMessage ?: "Failed to get GPS location.")
        }
    }

    /**
     * Starts continuous real-time GPS location updates.
     * Fires onLocationUpdate whenever the user's location changes.
     */
    @SuppressLint("MissingPermission")
    fun startLocationUpdates(
        context: Context,
        onLocationUpdate: (Location) -> Unit
    ): LocationCallback? {
        if (!hasLocationPermission(context) || !isGpsEnabled(context)) {
            return null
        }

        val fusedClient = LocationServices.getFusedLocationProviderClient(context)
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2500L)
            .setMinUpdateIntervalMillis(1500L)
            .setMinUpdateDistanceMeters(1f) // update when user moves 1 meter
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return
                onLocationUpdate(location)
            }
        }

        fusedClient.requestLocationUpdates(locationRequest, callback, Looper.getMainLooper())
        return callback
    }

    /**
     * Stops continuous location updates to save battery when screen or lifecycle ends.
     */
    fun stopLocationUpdates(context: Context, callback: LocationCallback?) {
        if (callback != null) {
            val fusedClient = LocationServices.getFusedLocationProviderClient(context)
            fusedClient.removeLocationUpdates(callback)
        }
    }

    /**
     * Calculates actual distance between current GPS location and saved parking location
     * using Android Location.distanceTo().
     */
    fun calculateDistance(
        currentLocation: Location,
        savedLatitude: Double,
        savedLongitude: Double
    ): Float {
        val savedLocation = Location("saved_parking_spot").apply {
            latitude = savedLatitude
            longitude = savedLongitude
        }
        return currentLocation.distanceTo(savedLocation)
    }

    /**
     * Formats distance in meters or kilometers.
     */
    fun formatDistance(meters: Float): String {
        return when {
            meters < 5f -> "At vehicle (< 5 m)"
            meters < 1000f -> "${meters.roundToInt()} m"
            else -> String.format(Locale.US, "%.2f km", meters / 1000f)
        }
    }

    fun estimateWalkMinutes(meters: Float): Int {
        // Average walking speed ~ 80 meters per minute
        return (meters / 80f).roundToInt().coerceAtLeast(1)
    }

    @Suppress("DEPRECATION")
    suspend fun getAddressFromCoordinates(
        context: Context,
        latitude: Double,
        longitude: Double
    ): String? = withContext(Dispatchers.IO) {
        try {
            val geocoder = Geocoder(context, Locale.getDefault())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                val address = addresses?.firstOrNull()
                address?.let {
                    val line = it.getAddressLine(0)
                    if (!line.isNullOrBlank()) {
                        line
                    } else {
                        val parts = listOfNotNull(it.thoroughfare, it.subLocality, it.locality)
                        if (parts.isNotEmpty()) parts.joinToString(", ") else null
                    }
                }
            } else {
                val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                val address = addresses?.firstOrNull()
                address?.let {
                    val line = it.getAddressLine(0)
                    if (!line.isNullOrBlank()) {
                        line
                    } else {
                        val parts = listOfNotNull(it.thoroughfare, it.subLocality, it.locality)
                        if (parts.isNotEmpty()) parts.joinToString(", ") else null
                    }
                }
            }
        } catch (e: Exception) {
            null
        }
    }
}
