package com.what3words.ocr.components.ui

import android.content.Context
import android.graphics.Bitmap
import android.util.DisplayMetrics
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
import com.what3words.javawrapper.request.AutosuggestOptions
import com.what3words.javawrapper.response.APIResponse.What3WordsError
import com.what3words.javawrapper.response.Suggestion
import com.what3words.ocr.components.extensions.BitmapUtils
import com.what3words.ocr.components.models.W3WOcrWrapper
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@ExperimentalGetImage
internal class OcrScanManager(
    private val wrapper: W3WOcrWrapper,
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
    internal var layoutCoordinates: LayoutCoordinates? = null
    internal var displayMetrics: DisplayMetrics? = null

    fun startCamera(
        context: Context,
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView
    ) {
        imageAnalyzer =
            ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setTargetAspectRatio(AspectRatio.RATIO_16_9).build().also { imageAnalysis ->
                    imageAnalysis.setAnalyzer(
                        wrapper.getExecutor(),
                        OcrAnalyzer(
                            wrapper,
                            options,
                            layoutCoordinates!!,
                            displayMetrics!!,
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

    fun stop() {
        if (::cameraProviderFuture.isInitialized) cameraProviderFuture.get().unbindAll()
        if (::imageAnalyzer.isInitialized) imageAnalyzer.clearAnalyzer()
    }

    @ExperimentalGetImage
    private class OcrAnalyzer(
        wrapper: W3WOcrWrapper,
        options: AutosuggestOptions?,
        layoutCoordinates: LayoutCoordinates,
        displayMetrics: DisplayMetrics,
        private val ocrScanResultCallback: OcrScanResultCallback,
        private val onResult: (List<Suggestion>, What3WordsError?) -> Unit
    ) :
        ImageAnalysis.Analyzer {
        private val textRecognizer =
            OcrRecognizer(wrapper, options, layoutCoordinates, displayMetrics)

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

    @ExperimentalGetImage
    private class OcrRecognizer(
        private val wrapper: W3WOcrWrapper,
        private val options: AutosuggestOptions?,
        private val layoutCoordinates: LayoutCoordinates,
        private val displayMetrics: DisplayMetrics
    ) {
        fun recognizeImageText(
            imageProxy: ImageProxy,
            ocrScanResultCallback: OcrScanResultCallback,
            onResult: (List<Suggestion>, What3WordsError?) -> Unit
        ) {
            BitmapUtils.getBitmap(imageProxy)?.let { bitmap ->
                val bitmapToBeScanned = try {
                    if (layoutCoordinates.isAttached) {
                        val x1: Float =
                            (layoutCoordinates.positionInRoot().x * bitmap.width) / displayMetrics.widthPixels
                        val y1: Float =
                            (layoutCoordinates.positionInRoot().y * bitmap.height) / displayMetrics.heightPixels
                        val width1: Int =
                            (layoutCoordinates.size.width * bitmap.width) / displayMetrics.widthPixels
                        val height1: Int =
                            (layoutCoordinates.size.height * bitmap.height) / displayMetrics.heightPixels
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
                    onResult(emptyList(), null)
                    return
                }
                try {
                    wrapper.scan(
                        bitmapToBeScanned,
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