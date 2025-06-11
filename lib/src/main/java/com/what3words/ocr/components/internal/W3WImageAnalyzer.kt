package com.what3words.ocr.components.internal

import android.graphics.Bitmap
import android.util.Log
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionInRoot
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
            // Use CameraX's built-in method to convert ImageProxy to Bitmap
            // With targetRotation set, this should be properly oriented
            val bitmap = imageProxy.toBitmap()

            val bitmapToBeScanned =
                if (cropLayoutCoordinates.isAttached && cameraLayoutCoordinates.isAttached) {
                    val x1: Float =
                        (cropLayoutCoordinates.positionInRoot().x * bitmap.width) / cameraLayoutCoordinates.size.width
                    val y1: Float =
                        (cropLayoutCoordinates.positionInRoot().y * bitmap.height) / cameraLayoutCoordinates.size.height
                    val width1: Int =
                        (cropLayoutCoordinates.size.width * bitmap.width) / cameraLayoutCoordinates.size.width
                    val height1: Int =
                        (cropLayoutCoordinates.size.height * bitmap.height) / cameraLayoutCoordinates.size.height
                    Bitmap.createBitmap(
                        bitmap,
                        x1.roundToInt(),
                        y1.roundToInt(),
                        width1,
                        height1
                    )
                } else {
                    bitmap
                }

            val deferred = onFrameCaptured.invoke(W3WImage(bitmapToBeScanned))
            deferred.invokeOnCompletion {
                if (bitmap == bitmapToBeScanned) bitmap.recycle()
                else {
                    bitmap.recycle()
                    bitmapToBeScanned.recycle()
                }

                imageProxy.close()
            }
        } catch (e: Exception) {
            Log.e("W3WImageAnalyzer", "Error processing image: ${e.message}", e)
            onError.invoke(W3WError(message = "Image processing failed: ${e.message}"))
            imageProxy.close()
        }
    }
}
