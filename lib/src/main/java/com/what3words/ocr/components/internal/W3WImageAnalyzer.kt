package com.what3words.ocr.components.internal

import android.graphics.Bitmap
import android.util.Log
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import com.what3words.core.types.common.W3WError
import com.what3words.core.types.image.W3WImage
import kotlinx.coroutines.CompletableDeferred
import kotlin.math.roundToInt

/**
 * An [ImageAnalysis.Analyzer] that captures frames from the camera and processes them.
 */
internal class W3WImageAnalyzer(
    private val cropLayoutCoordinates: LayoutCoordinates,
    private val cameraLayoutCoordinates: LayoutCoordinates,
    private val onFrameCaptured: ((W3WImage) -> CompletableDeferred<Unit>),
    private val onError: (W3WError) -> Unit
) : ImageAnalysis.Analyzer {

    @ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
        try {
            val originalBitmap = imageProxy.toBitmap()

            val finalBitmap = if (cropLayoutCoordinates.isAttached && cameraLayoutCoordinates.isAttached) {
                val cameraXCropRect = imageProxy.cropRect
                val cropBounds = cropLayoutCoordinates.boundsInRoot()
                val cameraBounds = cameraLayoutCoordinates.boundsInRoot()

                // Calculate combined crop area
                val relativeX = (cropBounds.left - cameraBounds.left) / cameraBounds.width
                val relativeY = (cropBounds.top - cameraBounds.top) / cameraBounds.height
                val relativeWidth = cropBounds.width / cameraBounds.width
                val relativeHeight = cropBounds.height / cameraBounds.height

                val cropX = (cameraXCropRect.left + relativeX * cameraXCropRect.width()).roundToInt()
                val cropY = (cameraXCropRect.top + relativeY * cameraXCropRect.height()).roundToInt()
                val cropWidth = (relativeWidth * cameraXCropRect.width()).roundToInt()
                val cropHeight = (relativeHeight * cameraXCropRect.height()).roundToInt()

                Bitmap.createBitmap(originalBitmap, cropX, cropY, cropWidth, cropHeight)
            } else {
                val cropRect = imageProxy.cropRect
                Bitmap.createBitmap(originalBitmap, cropRect.left, cropRect.top, cropRect.width(), cropRect.height())
            }

            val deferred = onFrameCaptured.invoke(W3WImage(finalBitmap))
            deferred.invokeOnCompletion {
                originalBitmap.recycle()
                finalBitmap.recycle()
                imageProxy.close()
            }
        } catch (e: Exception) {
            Log.e("W3WImageAnalyzer", "Error processing image: ${e.message}", e)
            onError.invoke(W3WError(message = "Image processing failed: ${e.message}"))
            imageProxy.close()
        }
    }
}