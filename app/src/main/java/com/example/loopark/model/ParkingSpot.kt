package com.example.loopark.model

import java.util.Locale

data class ParkingSpot(
    val latitude: Double? = null,
    val longitude: Double? = null,
    val address: String? = null,
    val savedTimestamp: String? = null,
    val accuracyMeters: Float? = null,
    val floorOrLevel: String = "",
    val pillarNumber: String = "",
    val slotNumber: String = "",
    val notes: String = "",
    val photoPath: String? = null,
    val tags: List<String> = emptyList()
) {
    val isSaved: Boolean
        get() = latitude != null && longitude != null

    val formattedCoordinates: String
        get() = if (latitude != null && longitude != null) {
            String.format(Locale.US, "%.5f°, %.5f°", latitude, longitude)
        } else {
            "GPS Coordinates Not Set"
        }

    val displayTitle: String
        get() = when {
            !address.isNullOrBlank() -> address
            isSaved -> "Spot at $formattedCoordinates"
            else -> "No Spot Saved"
        }

    val hasDetails: Boolean
        get() = floorOrLevel.isNotBlank() || pillarNumber.isNotBlank() || slotNumber.isNotBlank() || notes.isNotBlank() || !photoPath.isNullOrBlank()
}
