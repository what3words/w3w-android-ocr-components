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
import com.what3words.ocr.components.extensions.BitmapUtils
import kotlin.math.roundToInt

/**
 * An [ImageAnalysis.Analyzer] that captures frames from the camera and processes them.
 */
internal class W3WImageAnalyzer(
    private val cropLayoutCoordinates: LayoutCoordinates,
    private val cameraLayoutCoordinates: LayoutCoordinates,
    private val onFrameCaptured: (W3WImage) -> Unit,
    private val onError: (W3WError) -> Unit
) : ImageAnalysis.Analyzer {

    @ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
        BitmapUtils.getBitmap(imageProxy)?.let { bitmap ->
            val bitmapToBeScanned = try {
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
            } catch (e: Exception) {
                Log.e("W3WImageAnalyzer", "Bitmap cropping error: ${e.message}")
                //ignore frame if any cropping issues.
                imageProxy.close()
                return
            }

            onFrameCaptured.invoke(W3WImage(bitmapToBeScanned))
            imageProxy.close()
        } ?: run {
            onError.invoke(W3WError(message = "ImageProxy to Bitmap conversion failed"))
            imageProxy.close()
        }
    }
}