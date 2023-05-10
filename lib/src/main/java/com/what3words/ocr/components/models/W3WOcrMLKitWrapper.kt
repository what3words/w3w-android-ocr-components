package com.what3words.ocr.components.models

import android.graphics.Bitmap
import android.graphics.RectF
import android.util.Log
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.what3words.androidwrapper.What3WordsAndroidWrapper
import com.what3words.androidwrapper.helpers.DefaultDispatcherProvider
import com.what3words.androidwrapper.helpers.DispatcherProvider
import com.what3words.javawrapper.What3WordsV3
import com.what3words.javawrapper.response.APIResponse
import com.what3words.javawrapper.response.Autosuggest
import com.what3words.ocr.components.extensions.encodeToBase64
import com.what3words.ocr.components.extensions.io
import com.what3words.ocr.components.extensions.main
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class W3WOcrMLKitWrapper(
    private val wrapper: What3WordsAndroidWrapper,
    private val recognizer: TextRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS),
    private val dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider()
) : W3WOcrWrapper {

    companion object {
        fun allowsToSetLanguage(): Boolean {
            return false
        }

        fun supportsLanguage(languageCode: String): Boolean {
            throw java.lang.UnsupportedOperationException(
                "MLKit doesn't support language selection, , will try to scan all available languages listed here:" +
                        "https://developers.google.com/ml-kit/vision/text-recognition/languages"
            )
        }

        fun availableLanguages(): List<String> {
            throw java.lang.UnsupportedOperationException(
                "MLKit doesn't support language selection, , will try to scan all available languages listed here:" +
                        "https://developers.google.com/ml-kit/vision/text-recognition/languages"
            )
        }
    }

    private val imageAnalyzerExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    override fun language(languageCode: String) {
        throw java.lang.UnsupportedOperationException(
            "MLKit doesn't support language selection, , will try to scan all available languages listed here:" +
                    "https://developers.google.com/ml-kit/vision/text-recognition/languages"
        )
    }

    override fun scan(
        image: Bitmap,
        onFinished: ((OcrScanResult) -> Unit)
    ) {
        val ocrScanResult = OcrScanResult()
        recognizer.process(image, 0)
            .addOnSuccessListener { visionText ->
                io(dispatcherProvider) {
                    What3WordsV3.findPossible3wa(visionText.text).firstOrNull()
                        ?.let { possible3wa ->
                            val autosuggestRes = wrapper.autosuggest(possible3wa).execute()
                            if (autosuggestRes.isSuccessful) {
                                //checks if at least one suggestion words matches the possible3wa from the regex,
                                //this makes our OCR more accurate and avoids getting partial what3words address while focusing the camera.
                                if (autosuggestRes.suggestions.any { it.words == possible3wa }) {
                                    val info = getInfo(autosuggestRes, visionText)
                                    ocrScanResult.suggestions = autosuggestRes.suggestions
                                    ocrScanResult.info = info
                                    ocrScanResult.scannedImage = image.encodeToBase64()
                                }
                            } else {
                                ocrScanResult.error = autosuggestRes.error
                            }
                        }
                    withContext(dispatcherProvider.main()) { onFinished.invoke(ocrScanResult) }
                }
            }.addOnFailureListener { e ->
                onFinished.invoke(OcrScanResult(error = APIResponse.What3WordsError.SDK_ERROR.apply {
                    message = e.message
                }))
            }
    }

    private fun getInfo(
        autosuggestRes: Autosuggest,
        visionText: Text
    ): OcrInfo {
        val list = mutableListOf<RectF>()
        autosuggestRes.suggestions.forEach { suggestion ->
            val match =
                visionText.textBlocks.flatMap { it.lines }
                    .firstOrNull { it.text.contains(suggestion.words) }
            if (match != null && match.boundingBox != null) {
                list.add(RectF(match.boundingBox!!))
            }
        }
        return OcrInfo().apply { boxes = list }
    }

    override fun getExecutor(): ExecutorService {
        return imageAnalyzerExecutor
    }

    override fun stop() {
        recognizer.close()
    }
}