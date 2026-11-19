package com.example.loopark.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PhotoHelper {

    private const val PHOTO_FILE_NAME = "parkmate_spot_photo.jpg"

    /**
     * Creates a temporary image file in the cache directory and returns
     * its content URI using FileProvider for the camera intent.
     */
    fun createCameraTempUri(context: Context): Pair<Uri, File> {
        val cacheDir = context.cacheDir
        val tempFile = File.createTempFile("camera_temp_", ".jpg", cacheDir)
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            tempFile
        )
        return Pair(uri, tempFile)
    }

    /**
     * Saves a camera-captured file or gallery image stream permanently into
     * the app's internal files directory so it persists across app restarts.
     */
    fun saveFileToInternalStorage(context: Context, sourceFile: File): String? {
        return try {
            val destination = File(context.filesDir, PHOTO_FILE_NAME)
            sourceFile.inputStream().use { input ->
                FileOutputStream(destination).use { output ->
                    input.copyTo(output)
                }
            }
            destination.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Copies a gallery Uri to internal storage.
     */
    fun saveUriToInternalStorage(context: Context, sourceUri: Uri): String? {
        return try {
            val destination = File(context.filesDir, PHOTO_FILE_NAME)
            val inputStream: InputStream? = context.contentResolver.openInputStream(sourceUri)
            inputStream?.use { input ->
                FileOutputStream(destination).use { output ->
                    input.copyTo(output)
                }
            }
            destination.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Efficiently loads and rotates (EXIF) a bitmap from file path to avoid OOM.
     */
    fun loadRotatedBitmap(path: String, maxDim: Int = 1024): Bitmap? {
        return try {
            val file = File(path)
            if (!file.exists()) return null

            // 1. Decode bounds
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(path, options)

            // 2. Calculate inSampleSize
            var inSampleSize = 1
            val height = options.outHeight
            val width = options.outWidth

            if (height > maxDim || width > maxDim) {
                val halfHeight = height / 2
                val halfWidth = width / 2
                while ((halfHeight / inSampleSize) >= maxDim && (halfWidth / inSampleSize) >= maxDim) {
                    inSampleSize *= 2
                }
            }

            options.inJustDecodeBounds = false
            options.inSampleSize = inSampleSize

            val decodedBitmap = BitmapFactory.decodeFile(path, options) ?: return null

            // 3. Fix EXIF orientation (common on Samsung, Pixel camera photos)
            val exif = ExifInterface(path)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )

            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                else -> return decodedBitmap
            }

            Bitmap.createBitmap(
                decodedBitmap,
                0,
                0,
                decodedBitmap.width,
                decodedBitmap.height,
                matrix,
                true
            )
        } catch (e: Exception) {
            null
        }
    }
}
