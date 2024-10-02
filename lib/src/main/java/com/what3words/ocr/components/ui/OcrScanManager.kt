package com.what3words.ocr.components.ui

import android.content.Context
import android.graphics.Bitmap
import android.util.DisplayMetrics
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionInRoot
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.common.util.concurrent.ListenableFuture
import com.what3words.core.datasource.image.W3WImageDataSource
import com.what3words.core.datasource.text.W3WTextDataSource
import com.what3words.core.types.common.W3WError
import com.what3words.core.types.common.W3WResult
import com.what3words.core.types.domain.W3WSuggestion
import com.what3words.core.types.image.W3WImage
import com.what3words.core.types.options.W3WAutosuggestOptions
import com.what3words.javawrapper.response.APIResponse.What3WordsError
import com.what3words.javawrapper.response.Suggestion
import com.what3words.ocr.components.extensions.BitmapUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.roundToInt

/**
 * Manager that
 */
@ExperimentalGetImage
class OcrScanManager(
    private val w3wImageDataSource: W3WImageDataSource,
    private val w3WTextDataSource: W3WTextDataSource,
    private val imageAnalyzerExecutor: ExecutorService,
    private var options: W3WAutosuggestOptions? = null
) {
    interface OcrScanResultCallback {
        fun onScanning()
        fun onDetected()
        fun onValidating()
        fun onError(error: W3WError)
        fun onFound(result: List<W3WSuggestion>)
    }

    private var ocrScanResultCallback: OcrScanResultCallback? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>

    private lateinit var imageAnalyzer: ImageAnalysis
    internal var cropLayoutCoordinates: LayoutCoordinates? = null
    internal var cameraLayoutCoordinates: LayoutCoordinates? = null

    /**
     * This method will initialize the [W3WImageDataSource] and [W3WTextDataSource] to be used by the [OcrScanManager].
     */
    fun getReady(onReady: () -> Unit, onError: (W3WError) -> Unit) {
        w3wImageDataSource.start(onReady, onError)
    }

    fun setOcrScanResultCallback(callback: OcrScanResultCallback) {
        ocrScanResultCallback = callback
    }

    /**
     * This method will start all the camera logic, bind [PreviewView] to [LifecycleOwner] provided and create a [UseCaseGroup] using [PreviewView.getViewPort]
     * At this point permissions should have been handled already.
     *
     * @param context the [Context] where the [PreviewView] will be running.
     * @param lifecycleOwner the lifecycle owner of the [PreviewView] to allows us to bind a [UseCaseGroup] it.
     * @param previewView the [PreviewView] that will be used to capture frames to be scanned.
     */
    fun scanWithCamera(
        context: Context,
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView
    ) {
        imageAnalyzer =
            ImageAnalysis.Builder().setResolutionSelector(
                ResolutionSelector.Builder()
                    .setAspectRatioStrategy(AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY)
                    .build()
            ).setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
                .also { imageAnalysis ->
                    imageAnalysis.setAnalyzer(
                        imageAnalyzerExecutor,
                        OcrAnalyzer(
                            w3wImageDataSource,
                            w3WTextDataSource,
                            options,
                            cropLayoutCoordinates!!,
                            cameraLayoutCoordinates!!,
                            onScanning = {
                                ocrScanResultCallback?.onScanning()
                            },
                            onDetected = {
                                ocrScanResultCallback?.onDetected()
                            },
                            onValidating = {
                                ocrScanResultCallback?.onValidating()
                            },
                            onResult = {
                                ocrScanResultCallback?.onFound(it)
                            },
                            onError = {
                                ocrScanResultCallback?.onError(it)
                            }
                        )
                    )
                }
        cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val runnable = Runnable {
            val preview = Preview.Builder().build()
                .also { it.setSurfaceProvider(previewView.surfaceProvider) }

            //use preview ViewPort to get cropRect to make sure WYSIWYG
            val useCaseGroup = UseCaseGroup.Builder()
                .setViewPort(previewView.viewPort!!)
                .addUseCase(preview)
                .addUseCase(imageAnalyzer)
                .build()

            with(cameraProviderFuture.get()) {
                cameraProvider = this
                unbindAll()
                bindToLifecycle(
                    lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, useCaseGroup
                )
            }
        }
        cameraProviderFuture.addListener(runnable, ContextCompat.getMainExecutor(context))
    }

    /**
     * This method will stop all the camera logic, unbind [PreviewView] to [LifecycleOwner] provided and clear [imageAnalyzer] frames that are currently in the queue.
     */
    fun stop() {
        if (::cameraProviderFuture.isInitialized) cameraProviderFuture.get().unbindAll()
        if (::imageAnalyzer.isInitialized) imageAnalyzer.clearAnalyzer()
        w3wImageDataSource.stop()
        imageAnalyzerExecutor.shutdownNow()
    }

    fun scanImage(
        image: W3WImage,
    ) {
        val textRecognizer =
            OcrRecognizer(
                w3wImageDataSource,
                w3WTextDataSource,
                options,
            )

        textRecognizer.recognizeImageText(
            bitmap = image.bitmap,
            onScanning = {
                ocrScanResultCallback?.onScanning()
            },
            onDetected = {
                ocrScanResultCallback?.onDetected()
            },
            onValidating = {
                ocrScanResultCallback?.onValidating()
            },
            onResult = {
                ocrScanResultCallback?.onFound(it)
            },
            onError = {
                ocrScanResultCallback?.onError(it)
            },
            onCompleted = {}
        )
    }

    /**
     * [OcrAnalyzer] is a custom [ImageAnalysis.Analyzer] that will get the frames from [imageAnalyzer] and will send them to [OcrRecognizer]
     * which will compute the [ImageProxy] returned by [ImageAnalysis.Analyzer.analyze] and call [onResult] when [OcrRecognizer] finishes.
     *
     * @param w3wImageDataSource the [W3WImageDataSource] that will be used to scan the cropped [Bitmap] from the [ImageProxy].
     * @param w3WTextDataSource the [W3WTextDataSource] that will be used to validate the possible addresses found by the OCR scan.
     * @param options optional [W3WAutosuggestOptions] to filter OCR scan results.
     * @param cropLayoutCoordinates the [LayoutCoordinates] of the camera shutter set on top of [PreviewView] inside [W3WOcrScanner], this is set dynamically to allow different form factors.
     * @param cameraLayoutCoordinates the [LayoutCoordinates] of the camera shutter set on top of [PreviewView] inside [W3WOcrScanner], this is set dynamically to allow different form factors.
     * @param onResult a callback that will be called when a result was found, which is either [List] of [Suggestion] or, in case of an error, a [What3WordsError] with all error details.
     * @param onError a callback that will be called when an error was found, which is either a [W3WError] with all error details.
     */
    private class OcrAnalyzer(
        w3wImageDataSource: W3WImageDataSource,
        w3WTextDataSource: W3WTextDataSource,
        options: W3WAutosuggestOptions?,
        private val cropLayoutCoordinates: LayoutCoordinates,
        private val cameraLayoutCoordinates: LayoutCoordinates,
        private val onScanning: () -> Unit,
        private val onDetected: () -> Unit,
        private val onValidating: () -> Unit,
        private val onResult: (List<W3WSuggestion>) -> Unit,
        private val onError: (W3WError) -> Unit
    ) : ImageAnalysis.Analyzer {

        private val textRecognizer =
            OcrRecognizer(
                w3wImageDataSource,
                w3WTextDataSource,
                options,
            )

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
                    //ignore frame if any cropping issues.
                    return
                }

                textRecognizer.recognizeImageText(
                    bitmapToBeScanned,
                    onScanning = {
                        onScanning.invoke()
                    },
                    onDetected = {
                        onDetected.invoke()
                    },
                    onValidating = {
                        onValidating.invoke()
                    },
                    onResult = {
                        onResult.invoke(it)
                    },
                    onError = {
                        onError.invoke(it)
                    },
                    onCompleted = {
                        bitmap.recycle()
                        bitmap.recycle()
                        imageProxy.close()
                    }
                )

            } ?: kotlin.run {
                onError(W3WError(message = "Bitmap conversion error"))
            }
        }
    }

    /**
     * [OcrRecognizer] will get the [ImageProxy] convert it to a [Bitmap], crop the bitmap with [LayoutCoordinates] and [DisplayMetrics] information
     * and send the cropped [Bitmap] to our [W3WImageDataSource].
     *
     * @param options optional [W3WAutosuggestOptions] to filter OCR scan results.
     */
    private class OcrRecognizer(
        private val w3wImageDataSource: W3WImageDataSource,
        private val w3WTextDataSource: W3WTextDataSource,
        private val options: W3WAutosuggestOptions?,
    ) {
        fun recognizeImageText(
            bitmap: Bitmap,
            onScanning: () -> Unit,
            onDetected: () -> Unit,
            onValidating: () -> Unit,
            onResult: (List<W3WSuggestion>) -> Unit,
            onError: (W3WError) -> Unit,
            onCompleted: () -> Unit,
        ) {
            try {
                w3wImageDataSource.scan(
                    W3WImage(bitmap),
                    onScanning = { onScanning.invoke() },
                    onDetected = { possibleAddresses ->
                        onDetected.invoke()
                        onValidating.invoke()
                        validateAddresses(possibleAddresses) {
                            onResult(it)
                        }
                    },
                    onError = {
                        onError(it)
                    },
                    onCompleted = {
                        onCompleted.invoke()
                    }
                )
            } catch (e: Exception) {
                onError(W3WError(message = e.message))
            }
        }

        private fun validateAddresses(
            possibleAddresses: List<String>,
            onResult: (List<W3WSuggestion>) -> Unit
        ) {
            val listFound3wa = CopyOnWriteArrayList<W3WSuggestion>()
            CoroutineScope(Dispatchers.IO).launch {
                val deferredResults = possibleAddresses.map { possible3wa ->
                    async {
                        val result = w3WTextDataSource.autosuggest(possible3wa, options)
                        possible3wa to result
                    }
                }
                deferredResults.awaitAll().forEach { (possible3wa, result) ->
                    when (result) {
                        is W3WResult.Success -> {
                            result.value.firstOrNull {
                                it.w3wAddress.words.equals(possible3wa, ignoreCase = true)
                            }?.let {
                                listFound3wa.add(it)
                            }
                        }

                        is W3WResult.Failure -> {
                            // ignore
                        }
                    }
                }
                onResult(listFound3wa)
            }
        }
    }
}

@androidx.annotation.OptIn(ExperimentalGetImage::class)
@Composable
fun rememberOcrScanManager(
    w3wImageDataSource: W3WImageDataSource,
    w3wTextDataSource: W3WTextDataSource,
    options: W3WAutosuggestOptions?,
): OcrScanManager {
    val manager = remember {
        val imageAnalyzerExecutor = Executors.newSingleThreadExecutor()
        OcrScanManager(
            w3wImageDataSource = w3wImageDataSource,
            w3WTextDataSource = w3wTextDataSource,
            options = options,
            imageAnalyzerExecutor = imageAnalyzerExecutor,
        )
    }

    DisposableEffect(key1 = Unit) {
        onDispose {
            manager.stop()
        }
    }

    return manager
}