package com.example.loopark.data

import android.content.Context
import android.content.SharedPreferences
import com.example.loopark.model.ParkingSpot
import java.io.File

class ParkingStorage(private val context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("parkmate_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_HAS_SPOT = "has_spot"
        private const val KEY_LATITUDE_STR = "latitude_str"
        private const val KEY_LONGITUDE_STR = "longitude_str"
        private const val KEY_ADDRESS = "address"
        private const val KEY_SAVED_AT = "saved_timestamp"
        private const val KEY_ACCURACY = "accuracy_meters"
        private const val KEY_FLOOR = "floor_level"
        private const val KEY_PILLAR = "pillar_number"
        private const val KEY_SLOT_NUM = "slot_number"
        private const val KEY_NOTES = "notes"
        private const val KEY_PHOTO_PATH = "photo_path"
    }

    /**
     * Permanently saves the parking spot coordinates, structured notes,
     * and photo path using SharedPreferences with full 64-bit precision.
     */
    fun saveParkingSpot(spot: ParkingSpot) {
        prefs.edit().apply {
            putBoolean(KEY_HAS_SPOT, spot.isSaved)
            if (spot.latitude != null) {
                putString(KEY_LATITUDE_STR, spot.latitude.toString())
            } else {
                remove(KEY_LATITUDE_STR)
            }
            if (spot.longitude != null) {
                putString(KEY_LONGITUDE_STR, spot.longitude.toString())
            } else {
                remove(KEY_LONGITUDE_STR)
            }
            putString(KEY_ADDRESS, spot.address)
            putString(KEY_SAVED_AT, spot.savedTimestamp)
            if (spot.accuracyMeters != null) {
                putFloat(KEY_ACCURACY, spot.accuracyMeters)
            } else {
                remove(KEY_ACCURACY)
            }
            putString(KEY_FLOOR, spot.floorOrLevel)
            putString(KEY_PILLAR, spot.pillarNumber)
            putString(KEY_SLOT_NUM, spot.slotNumber)
            putString(KEY_NOTES, spot.notes)
            putString(KEY_PHOTO_PATH, spot.photoPath)
            apply()
        }
    }

    /**
     * Loads the permanently saved parking spot upon app launch.
     */
    fun loadParkingSpot(): ParkingSpot {
        val hasSpot = prefs.getBoolean(KEY_HAS_SPOT, false)
        if (!hasSpot) return ParkingSpot()

        val latStr = prefs.getString(KEY_LATITUDE_STR, null)
        val lngStr = prefs.getString(KEY_LONGITUDE_STR, null)
        val lat = latStr?.toDoubleOrNull()
        val lng = lngStr?.toDoubleOrNull()

        if (lat == null || lng == null) return ParkingSpot()

        val address = prefs.getString(KEY_ADDRESS, null)
        val savedAt = prefs.getString(KEY_SAVED_AT, null)
        val accuracy = if (prefs.contains(KEY_ACCURACY)) prefs.getFloat(KEY_ACCURACY, 0f) else null
        val floor = prefs.getString(KEY_FLOOR, "") ?: ""
        val pillar = prefs.getString(KEY_PILLAR, "") ?: ""
        val slotNum = prefs.getString(KEY_SLOT_NUM, "") ?: ""
        val notes = prefs.getString(KEY_NOTES, "") ?: ""
        val photoPath = prefs.getString(KEY_PHOTO_PATH, null)

        // Verify photo file still exists if path was saved
        val validPhotoPath = photoPath?.takeIf { File(it).exists() }

        val tags = mutableListOf<String>()
        tags.add("GPS Saved")
        accuracy?.let { tags.add("±${it.toInt()}m") }
        if (floor.isNotBlank()) tags.add(floor)
        if (pillar.isNotBlank()) tags.add("Pillar $pillar")

        return ParkingSpot(
            latitude = lat,
            longitude = lng,
            address = address,
            savedTimestamp = savedAt,
            accuracyMeters = accuracy,
            floorOrLevel = floor,
            pillarNumber = pillar,
            slotNumber = slotNum,
            notes = notes,
            photoPath = validPhotoPath,
            tags = tags
        )
    }

    /**
     * Clears all saved parking data and removes the local photo file if present.
     */
    fun clearParkingSpot() {
        val photoPath = prefs.getString(KEY_PHOTO_PATH, null)
        if (!photoPath.isNullOrBlank()) {
            try {
                val file = File(photoPath)
                if (file.exists()) {
                    file.delete()
                }
            } catch (_: Exception) { }
        }
        prefs.edit().clear().apply()
    }
}
