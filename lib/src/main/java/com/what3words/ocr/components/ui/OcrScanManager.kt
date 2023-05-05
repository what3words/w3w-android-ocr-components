package com.what3words.ocr.components.ui

import android.content.Context
import android.graphics.Bitmap
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.common.util.concurrent.ListenableFuture
import com.what3words.javawrapper.response.APIResponse
import com.what3words.ocr.components.extensions.BitmapUtils
import com.what3words.ocr.components.models.OcrScanResult
import com.what3words.ocr.components.models.W3WOcrWrapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


@ExperimentalGetImage
internal class OcrScanManager(
    private val wrapper: W3WOcrWrapper,
    private val ocrScanResultCallback: OcrScanResultCallback
) {
    interface OcrScanResultCallback {
        fun onFinished(result: OcrScanResult)
    }

    private var cameraProvider: ProcessCameraProvider? = null
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>

    private lateinit var imageAnalyzer: ImageAnalysis

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
                            imageAnalysis
                        ) { ocrResult ->
                            CoroutineScope(Dispatchers.Main).launch {
                                //only call onFinished if isSuccessful or there's an error
                                if (ocrResult.isSuccessful() || ocrResult.error != null) {
                                    ocrScanResultCallback.onFinished(ocrResult)
                                    withContext(Dispatchers.IO) {
                                        cameraProviderFuture.get()
                                    }.unbindAll()
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
        wrapper.stop()
    }

    @ExperimentalGetImage
    private class OcrAnalyzer(
        wrapper: W3WOcrWrapper,
        private val imageAnalysis: ImageAnalysis,
        private val onResult: (OcrScanResult) -> Unit
    ) :
        ImageAnalysis.Analyzer {
        private val textRecognizer = OcrRecognizer(wrapper)

        @ExperimentalGetImage
        override fun analyze(imageProxy: ImageProxy) {
            textRecognizer.recognizeImageText(
                imageProxy,
            ) { ocrResult ->
                imageProxy.close()
                //only call clear if isSuccessful or there's an error
                if (ocrResult.isSuccessful() || ocrResult.error != null) {
                    imageAnalysis.clearAnalyzer()
                }
                onResult.invoke(ocrResult)
            }
        }
    }


    @ExperimentalGetImage
    private class OcrRecognizer(
        private val wrapper: W3WOcrWrapper
    ) {
        fun recognizeImageText(
            imageProxy: ImageProxy,
            onResult: (OcrScanResult) -> Unit
        ) {
            //These magic numbers match the percentages on the fragment_ocr_scan,
            //ImageProxy comes with full view port size, then we have crop the bitmap to match viewfinder on the fragment
            //build using guidelines with percentages to fit all screens and possible smaller views where fragment is loaded.
            BitmapUtils.getBitmap(imageProxy)?.let { bitmap ->
                val x1: Int = (bitmap.width * 0.065).toInt()
                val y1: Int = (bitmap.height * 0.34).toInt()
                val width1: Int = (bitmap.width * 0.935).toInt()
                val height1: Int = (bitmap.height * 0.22).toInt()
                try {
                    val bitmapCropped = Bitmap.createBitmap(
                        bitmap,
                        x1,
                        y1,
                        width1,
                        height1
                    )
                    wrapper.scan(bitmapCropped) { scanResult ->
                        onResult(scanResult)
                        bitmap.recycle()
                        bitmapCropped.recycle()
                    }
                } catch (e: Exception) {
                    onResult(OcrScanResult(error = APIResponse.What3WordsError.SDK_ERROR.apply {
                        this.message = e.message
                    }))
                }
            } ?: kotlin.run {
                onResult(OcrScanResult(error = APIResponse.What3WordsError.SDK_ERROR.apply {
                    this.message = "Bitmap conversion error"
                }))
            }
        }
    }
}