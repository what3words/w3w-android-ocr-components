package com.what3words.ocr.components.ui

import android.content.Context
import android.graphics.Bitmap
import android.util.DisplayMetrics
import android.util.Log
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionInRoot
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.common.util.concurrent.ListenableFuture
import com.what3words.androidwrapper.What3WordsAndroidWrapper
import com.what3words.api.sdk.bridge.models.What3WordsSdk
import com.what3words.javawrapper.What3WordsV3
import com.what3words.javawrapper.request.AutosuggestOptions
import com.what3words.javawrapper.response.APIResponse.What3WordsError
import com.what3words.javawrapper.response.Suggestion
import com.what3words.ocr.components.extensions.BitmapUtils
import com.what3words.ocr.components.models.W3WOcrWrapper
import com.what3words.ocr.components.ui.OcrScanManager.OcrScanResultCallback
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Manager that uses [androidx.camera.view.PreviewView] and [androidx.camera.core.ImageAnalysis] to analise captured frames
 * from devices camera, creating a queue of frames to be cropped and sent to the [wrapper] provided for Text scan, working as an helper
 * to be used internally by our [W3WOcrScanner], for separation of concerns.
 *
 * @param wrapper the [What3WordsAndroidWrapper] data provider to be used by this wrapper to validate a possible three word address,
 * could be our [What3WordsV3] for API or [What3WordsSdk] for SDK (SDK requires extra setup).
 * @param options optional [AutosuggestOptions] to filter OCR scan results.
 * @param ocrScanResultCallback a [OcrScanResultCallback] called in different stages of the OCR scanning process.
 */
@ExperimentalGetImage
internal class OcrScanManager(
    private val wrapper: W3WOcrWrapper,
    private val dataProvider: What3WordsAndroidWrapper,
    private val options: AutosuggestOptions? = null,
    private val ocrScanResultCallback: OcrScanResultCallback
) {
    interface OcrScanResultCallback {
        fun onScanning()
        fun onDetected()
        fun onValidating()
        fun onError(error: What3WordsError)
        fun onFound(result: List<Suggestion>)
    }

    private var cameraProvider: ProcessCameraProvider? = null
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>

    private lateinit var imageAnalyzer: ImageAnalysis
    internal var cropLayoutCoordinates: LayoutCoordinates? = null
    internal var cameraLayoutCoordinates: LayoutCoordinates? = null

    /**
     * This method will start all the camera logic, bind [PreviewView] to [LifecycleOwner] provided and create a [UseCaseGroup] using [PreviewView.getViewPort]
     * At this point permissions should have been handled already.
     *
     * @param context the [Context] where the [PreviewView] will be running.
     * @param lifecycleOwner the lifecycle owner of the [PreviewView] to allows us to bind a [UseCaseGroup] it.
     * @param previewView the [PreviewView] that will be used to capture frames to be scanned.
     */
    fun startCamera(
        context: Context,
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView
    ) {
        imageAnalyzer =
            ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setTargetAspectRatio(AspectRatio.RATIO_16_9).build().also { imageAnalysis ->
                    imageAnalysis.setAnalyzer(
                        wrapper.executor(),
                        OcrAnalyzer(
                            wrapper,
                            dataProvider,
                            options,
                            cropLayoutCoordinates!!,
                            cameraLayoutCoordinates!!,
                            ocrScanResultCallback
                        ) { suggestions, error ->
                            CoroutineScope(Dispatchers.Main).launch {
                                //only call onFinished if isSuccessful or there's an error
                                if (error == null && suggestions.isNotEmpty()) {
                                    ocrScanResultCallback.onFound(suggestions)
                                } else if (error != null) {
                                    ocrScanResultCallback.onError(error)
                                }
                            }
                        }
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
    }

    /**
     * [OcrAnalyzer] is a custom [ImageAnalysis.Analyzer] that will get the frames from [imageAnalyzer] and will send them to [OcrRecognizer]
     * which will compute the [ImageProxy] returned by [ImageAnalysis.Analyzer.analyze] and call [onResult] when [OcrRecognizer] finishes.
     *
     * @param wrapper the [What3WordsAndroidWrapper] data provider to be used by this wrapper to validate a possible three word address,
     * could be our [What3WordsV3] for API or [What3WordsSdk] for SDK (SDK requires extra setup).
     * @param options optional [AutosuggestOptions] to filter OCR scan results.
     * @param cropLayoutCoordinates the [LayoutCoordinates] of the camera shutter set on top of [PreviewView] inside [W3WOcrScanner], this is set dynamically to allow different form factors.
     * @param cameraLayoutCoordinates the [LayoutCoordinates] of the camera shutter set on top of [PreviewView] inside [W3WOcrScanner], this is set dynamically to allow different form factors.
     * @param displayMetrics the [DisplayMetrics] of the device to gives necessary information to crop the [ImageProxy] to the desired camera shutter size.
     * @param ocrScanResultCallback a [OcrScanResultCallback] called in different stages of the OCR scanning process, provided by the [W3WOcrScanner].
     * @param onResult a callback that will be called when a result was found, which is either [List] of [Suggestion] or, in case of an error, a [What3WordsError] with all error details.
     */
    @ExperimentalGetImage
    private class OcrAnalyzer(
        wrapper: W3WOcrWrapper,
        dataProvider: What3WordsAndroidWrapper,
        options: AutosuggestOptions?,
        cropLayoutCoordinates: LayoutCoordinates,
        cameraLayoutCoordinates: LayoutCoordinates,
        private val ocrScanResultCallback: OcrScanResultCallback,
        private val onResult: (List<Suggestion>, What3WordsError?) -> Unit
    ) :
        ImageAnalysis.Analyzer {
        private val textRecognizer =
            OcrRecognizer(wrapper, dataProvider, options, cropLayoutCoordinates, cameraLayoutCoordinates)

        @ExperimentalGetImage
        override fun analyze(imageProxy: ImageProxy) {
            textRecognizer.recognizeImageText(
                imageProxy,
                ocrScanResultCallback
            ) { suggestions, error ->
                imageProxy.close()
                onResult.invoke(suggestions, error)
            }
        }
    }

    /**
     * [OcrRecognizer] will get the [ImageProxy] convert it to a [Bitmap], crop the bitmap with [LayoutCoordinates] and [DisplayMetrics] information
     * and send the cropped [Bitmap] to our [wrapper].
     *
     * @param wrapper the [What3WordsAndroidWrapper] data provider to be used by this wrapper to validate a possible three word address,
     * could be our [What3WordsV3] for API or [What3WordsSdk] for SDK (SDK requires extra setup).
     * @param options optional [AutosuggestOptions] to filter OCR scan results.
     * @param cropLayoutCoordinates the [LayoutCoordinates] of the camera shutter set on top of [PreviewView] inside [W3WOcrScanner], this is set dynamicallyto allow different form factors.
     * @param cameraLayoutCoordinates the [LayoutCoordinates] of the camera shutter set on top of [PreviewView] inside [W3WOcrScanner], this is set dynamicallyto allow different form factors.
     */
    @ExperimentalGetImage
    private class OcrRecognizer(
        private val wrapper: W3WOcrWrapper,
        private val dataProvider: What3WordsAndroidWrapper,
        private val options: AutosuggestOptions?,
        private val cropLayoutCoordinates: LayoutCoordinates,
        private val cameraLayoutCoordinates: LayoutCoordinates
    ) {
        companion object {
            const val TAG = "OcrRecognizer"
        }

        fun recognizeImageText(
            imageProxy: ImageProxy,
            ocrScanResultCallback: OcrScanResultCallback,
            onResult: (List<Suggestion>, What3WordsError?) -> Unit
        ) {
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
                    Log.e(TAG, e.message ?: "")
                    //ignore frame if any cropping issues.
                    onResult(emptyList(), null)
                    return
                }
                try {
                    wrapper.scan(
                        bitmapToBeScanned,
                        dataProvider,
                        options,
                        onScanning = { ocrScanResultCallback.onScanning() },
                        onDetected = { ocrScanResultCallback.onDetected() },
                        onValidating = { ocrScanResultCallback.onValidating() }
                    ) { suggestions, error ->
                        onResult(suggestions, error)
                        bitmap.recycle()
                        bitmapToBeScanned.recycle()
                    }
                } catch (e: Exception) {
                    onResult(emptyList(), What3WordsError.SDK_ERROR.apply {
                        this.message = e.message
                    })
                }
            } ?: kotlin.run {
                onResult(emptyList(), What3WordsError.SDK_ERROR.apply {
                    this.message = "Bitmap conversion error"
                })
            }
        }
    }
}