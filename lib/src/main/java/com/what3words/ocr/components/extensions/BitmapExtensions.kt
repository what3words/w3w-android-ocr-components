package com.what3words.ocr.components.extensions

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.util.Log

private const val TAG = "BitmapExtensions"

/**
 * Efficiently loads a bitmap from a Uri with automatic downsampling
 * to prevent OOM errors with large images.
 *
 * IMPORTANT: This function involves I/O and decoding, call it from a background thread.
 *
 * @param contentResolver ContentResolver to access the Uri
 * @return Downsampled bitmap or null if loading failed
 */
fun Uri.loadDownsampledBitmap(contentResolver: ContentResolver): Bitmap? {
    try {
        // 1. Get original dimensions efficiently without loading the full bitmap
        val (originalWidth, originalHeight) = this.getImageDimensions(contentResolver)

        // If dimensions are invalid, return null
        if (originalWidth <= 0 || originalHeight <= 0) {
            Log.w(TAG, "Could not get valid dimensions for URI: $this")
            return null
        }

        // 2. Determine target dimensions based on original size
        // (Consider making maxDimension configurable if needed)
        val maxDimension = when {
            originalWidth > 3000 || originalHeight > 3000 -> 1200  // Large images
            originalWidth > 1500 || originalHeight > 1500 -> 800  // Medium images
            else -> 600 // Smaller images
        }

        val (targetWidth, targetHeight) = if (originalWidth > originalHeight) {
            // Landscape or Square
            Pair(maxDimension, (originalHeight.toFloat() * maxDimension / originalWidth).toInt())
        } else {
            // Portrait
            Pair((originalWidth.toFloat() * maxDimension / originalHeight).toInt(), maxDimension)
        }

        // Ensure target dimensions are at least 1x1 to avoid decoding errors
        val finalTargetWidth = maxOf(1, targetWidth)
        val finalTargetHeight = maxOf(1, targetHeight)

        // 3. Decode the bitmap using the most efficient method for the API level
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.P -> {
                // API 28+: Use ImageDecoder with setTargetSize for efficient downsampling
                val source = ImageDecoder.createSource(contentResolver, this)
                ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                    // Prefer software allocation for consistency across devices/formats
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    // Let ImageDecoder handle efficient scaling to the target size
                    decoder.setTargetSize(finalTargetWidth, finalTargetHeight)
                    // Consider memory policy if memory pressure is an issue:
                     decoder.memorySizePolicy = ImageDecoder.MEMORY_POLICY_LOW_RAM
                }
            }
            else -> {
                // Older APIs: Use BitmapFactory with calculated inSampleSize
                // Calculate the optimal sample size (power of 2)
                val sampleSize = calculateSampleSize(originalWidth, originalHeight, finalTargetWidth, finalTargetHeight)

                val loadOptions = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                    // Use RGB_565 to reduce memory footprint (trades quality for memory)
                    inPreferredConfig = Bitmap.Config.RGB_565
                    // Ensures the bitmap is actually loaded (false is default)
                    inJustDecodeBounds = false
                }

                // Open a new input stream to decode the actual bitmap with downsampling
                contentResolver.openInputStream(this)?.use { inputStream ->
                    BitmapFactory.decodeStream(inputStream, null, loadOptions)
                }
                // Returns null if inputStream is null or decodeStream fails
            }
        }
    } catch (e: Exception) {
        Log.e(TAG, "Failed to load downsampled bitmap for URI: $this", e)
        return null // Return null on any exception during the process
    }
}

/**
 * Gets the dimensions of an image from a Uri without loading the full bitmap into memory.
 *
 * @param contentResolver ContentResolver to access the Uri
 * @return Pair of (width, height), or (0, 0) if dimensions could not be read or an error occurred.
 */
private fun Uri.getImageDimensions(contentResolver: ContentResolver): Pair<Int, Int> {
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // API 28+: Use ImageDecoder to get size info.
            val source = ImageDecoder.createSource(contentResolver, this)
            var width = 0
            var height = 0
            // This decodes only enough to get the size information.
            // It might throw exceptions for invalid/corrupt images or source issues.
            ImageDecoder.decodeDrawable(source) { decoder, info, _ ->
                width = info.size.width
                height = info.size.height
                // Set a listener that returns true to cancel the full decode immediately after getting info.
                decoder.setOnPartialImageListener { true }
            }
            Pair(width, height)
        } else {
            // Older APIs: Use BitmapFactory options with inJustDecodeBounds = true
            contentResolver.openInputStream(this)?.use { inputStream ->
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true // Only decode bounds, not the whole image
                }
                BitmapFactory.decodeStream(inputStream, null, options)
                Pair(options.outWidth, options.outHeight)
            } ?: run {
                Log.w(TAG, "Could not open InputStream for URI: $this to get dimensions.")
                Pair(0, 0) // Return 0,0 if stream is null
            }
        }
    } catch (e: Exception) {
        Log.e(TAG, "Error getting image dimensions for URI: $this", e)
        Pair(0, 0) // Return invalid dimensions on error
    }
}

/**
 * Calculates the largest power-of-2 sample size value that keeps both
 * height and width larger than the requested height and width.
 * This is the standard Android method for calculating inSampleSize.
 *
 * @param width Original width
 * @param height Original height
 * @param targetWidth Target width
 * @param targetHeight Target height
 * @return Calculated sample size (power of 2, minimum 1)
 */
private fun calculateSampleSize(width: Int, height: Int, targetWidth: Int, targetHeight: Int): Int {
    var sampleSize = 1

    // Ensure target dimensions are positive
    if (targetWidth <= 0 || targetHeight <= 0 || width <= 0 || height <= 0) {
        return sampleSize // Return 1 if dimensions are invalid
    }

    if (height > targetHeight || width > targetWidth) {
        val halfHeight: Int = height / 2
        val halfWidth: Int = width / 2

        // Calculate the largest inSampleSize value that is a power of 2 and keeps both
        // height and width larger than or equal to the requested height and width.
        while ((halfHeight / sampleSize) >= targetHeight && (halfWidth / sampleSize) >= targetWidth) {
            sampleSize *= 2
        }
    }

    return sampleSize
}