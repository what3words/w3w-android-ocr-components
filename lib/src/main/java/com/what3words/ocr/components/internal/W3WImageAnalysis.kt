package com.what3words.ocr.components.internal

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.compose.ui.layout.LayoutCoordinates
import com.what3words.core.types.common.W3WError
import com.what3words.core.types.image.W3WImage
import kotlinx.coroutines.CompletableDeferred
import java.util.concurrent.ExecutorService

/**
 * Builds an [ImageAnalysis] instance with a [W3WImageAnalyzer] for processing images.
 *
 * @param imageAnalyzerExecutor An [ExecutorService] to execute the image analysis on.
 * @param cropLayoutCoordinates The [LayoutCoordinates] of the crop area view.
 * @param cameraLayoutCoordinates The [LayoutCoordinates] of the camera view.
 * @param aspectRatioStrategy The [AspectRatioStrategy] to use for setting the camera resolution.
 * @param targetRotation The rotation to apply to the captured images.
 * @param onFrameCaptured A callback invoked when a frame is captured by the camera.
 * @param onError A callback invoked when an error occurs.
 */
internal fun buildW3WImageAnalysis(
    imageAnalyzerExecutor: ExecutorService,
    cropLayoutCoordinates: LayoutCoordinates,
    cameraLayoutCoordinates: LayoutCoordinates,
    aspectRatioStrategy: AspectRatioStrategy = AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY,
    targetRotation: Int,
    onFrameCaptured: ((W3WImage) -> CompletableDeferred<Unit>),
    onError: (W3WError) -> Unit,
): ImageAnalysis {

    val w3WImageAnalyzer = W3WImageAnalyzer(
        onFrameCaptured = onFrameCaptured,
        onError = onError,
        cropLayoutCoordinates = cropLayoutCoordinates,
        cameraLayoutCoordinates = cameraLayoutCoordinates,
    )
    return ImageAnalysis.Builder()
        .setTargetRotation(targetRotation)
        .setOutputImageRotationEnabled(true)
        .setResolutionSelector(
            ResolutionSelector.Builder()
                .setResolutionStrategy(ResolutionStrategy.HIGHEST_AVAILABLE_STRATEGY)
                .setAspectRatioStrategy(aspectRatioStrategy)
                .build()
        ).setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
        .also { imageAnalysis ->
            imageAnalysis.setAnalyzer(
                imageAnalyzerExecutor,
                w3WImageAnalyzer
            )
        }
}
