package com.what3words.ocr.components.internal

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.compose.ui.layout.LayoutCoordinates
import com.what3words.core.types.common.W3WError
import com.what3words.core.types.image.W3WImage
import java.util.concurrent.ExecutorService

/**
 * Builds an [ImageAnalysis] instance with a [W3WImageAnalyzer] for processing images.
 */
internal fun buildW3WImageAnalysis(
    imageAnalyzerExecutor: ExecutorService,
    cropLayoutCoordinates: LayoutCoordinates,
    cameraLayoutCoordinates: LayoutCoordinates,
    aspectRatioStrategy: AspectRatioStrategy = AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY,
    onFrameCaptured: (W3WImage) -> Unit,
    onError: (W3WError) -> Unit,
): ImageAnalysis {

    val w3WImageAnalyzer = W3WImageAnalyzer(
        onFrameCaptured = onFrameCaptured,
        onError = onError,
        cropLayoutCoordinates = cropLayoutCoordinates,
        cameraLayoutCoordinates = cameraLayoutCoordinates,
    )
    return ImageAnalysis.Builder().setResolutionSelector(
        ResolutionSelector.Builder()
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