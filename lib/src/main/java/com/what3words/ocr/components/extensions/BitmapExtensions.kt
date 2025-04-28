package com.what3words.ocr.components.extensions

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build

/**
 * Efficiently loads a bitmap from a Uri with automatic downsampling
 * to prevent OOM errors with large images
 *
 * @param contentResolver ContentResolver to access the Uri
 * @return Downsampled bitmap or null if loading failed
 */
fun Uri.loadDownsampledBitmap(contentResolver: ContentResolver): Bitmap? {
    return try {
        // Read original dimensions
        val (originalWidth, originalHeight) = this.getImageDimensions(contentResolver)

        // If dimensions are invalid, return null
        if (originalWidth <= 0 || originalHeight <= 0) {
            return null
        }

        // Calculate scale based on original dimensions
        // Target maximum dimension - based on how large the original is
        val maxDimension = when {
            originalWidth > 3000 || originalHeight > 3000 -> 1280 // ~25% for very large images
            originalWidth > 1500 || originalHeight > 1500 -> 800  // ~50% for medium images
            else -> 600  // Keep smaller images at reasonable size
        }

        // Calculate target dimensions maintaining aspect ratio
        val (targetWidth, targetHeight) = if (originalWidth > originalHeight) {
            // Landscape
            Pair(maxDimension, (originalHeight.toFloat() * maxDimension / originalWidth).toInt())
        } else {
            // Portrait
            Pair((originalWidth.toFloat() * maxDimension / originalHeight).toInt(), maxDimension)
        }

        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.P -> {
                val source = ImageDecoder.createSource(contentResolver, this)
                ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    // Set target size while maintaining aspect ratio
                    decoder.setTargetSize(targetWidth, targetHeight)
                }
            }
            else -> {
                // For older Android versions
                val sampleSize = calculateSampleSize(originalWidth, originalHeight, targetWidth, targetHeight)

                val loadOptions = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                    inPreferredConfig = Bitmap.Config.RGB_565 // Use less memory
                }

                contentResolver.openInputStream(this)?.use { inputStream ->
                    BitmapFactory.decodeStream(inputStream, null, loadOptions)
                }
            }
        }
    } catch (e: Exception) {
        // Log error or handle appropriately
        null
    }
}

/**
 * Gets the dimensions of an image without loading the full bitmap into memory
 *
 * @param contentResolver ContentResolver to access the Uri
 * @return Pair of width and height, or (0, 0) if dimensions could not be read
 */
private fun Uri.getImageDimensions(contentResolver: ContentResolver): Pair<Int, Int> {
    val options = BitmapFactory.Options().apply {
        inJustDecodeBounds = true
    }

    try {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            contentResolver.openInputStream(this)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream, null, options)
            }
        } else {
            val source = ImageDecoder.createSource(contentResolver, this)
            // For API 28+, we need to use ImageDecoder to get dimensions
            try {
                ImageDecoder.decodeDrawable(source) { decoder, info, _ ->
                    options.outWidth = info.size.width
                    options.outHeight = info.size.height
                    // Cancel the decode after getting dimensions
                    decoder.setOnPartialImageListener { true } // Return true to cancel
                }
            } catch (e: Exception) {
                // Check if the exception is due to canceling the decode, which is expected
                if (e !is ImageDecoder.DecodeException || e.error != ImageDecoder.DecodeException.SOURCE_EXCEPTION) {
                   // Log or handle unexpected exceptions
                }
                // Proceed even if cancelled, dimensions should be set
            }
        }
    } catch (e: Exception) {
        // Log error or handle appropriately
        return Pair(0, 0) // Return invalid dimensions on error
    }


    return Pair(options.outWidth, options.outHeight)
}

/**
 * Calculates the appropriate sample size for downsampling based on target dimensions
 *
 * @param width Original width
 * @param height Original height
 * @param targetWidth Target width
 * @param targetHeight Target height
 * @return Sample size (power of 2)
 */
private fun calculateSampleSize(width: Int, height: Int, targetWidth: Int, targetHeight: Int): Int {
    var sampleSize = 1

    // Ensure target dimensions are positive
    if (targetWidth <= 0 || targetHeight <= 0) return sampleSize

    if (width > targetWidth || height > targetHeight) {
        val halfWidth = width / 2
        val halfHeight = height / 2

        // Calculate the largest inSampleSize value that is a power of 2 and keeps both
        // height and width larger than the requested height and width
        while ((halfWidth / sampleSize) >= targetWidth && (halfHeight / sampleSize) >= targetHeight) {
            sampleSize *= 2
        }
    }

    return sampleSize
}