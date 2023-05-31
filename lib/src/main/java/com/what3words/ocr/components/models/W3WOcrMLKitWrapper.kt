package com.what3words.ocr.components.models

import android.graphics.Bitmap
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.what3words.androidwrapper.What3WordsAndroidWrapper
import com.what3words.androidwrapper.helpers.DefaultDispatcherProvider
import com.what3words.androidwrapper.helpers.DispatcherProvider
import com.what3words.api.sdk.bridge.models.What3WordsSdk
import com.what3words.javawrapper.What3WordsV3
import com.what3words.javawrapper.request.AutosuggestOptions
import com.what3words.javawrapper.response.APIResponse
import com.what3words.javawrapper.response.Suggestion
import com.what3words.ocr.components.extensions.io
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlinx.coroutines.withContext

/**
 * Creates a new [W3WOcrWrapper] to work with MLKit V2 [TextRecognizer].
 *
 * @sample com.what3words.ocr.components.sample.ComposeOcrScanPopupSampleActivity.getOcrWrapper
 *
 * @param wrapper the [What3WordsAndroidWrapper] data provider to be used by this wrapper to validate a possible three word address,
 * could be our [What3WordsV3] for API or [What3WordsSdk] for SDK (SDK requires extra setup).
 * @param recognizer the MLKit V2 [TextRecognizer] to be used to scan text from a [Bitmap], by default will be [com.google.mlkit.vision.text.latin] check [this](https://developers.google.com/ml-kit/vision/text-recognition/v2/android).
 * @param dispatcherProvider [DispatcherProvider] to handle Coroutines threading, by default uses [DefaultDispatcherProvider]
 */
class W3WOcrMLKitWrapper(
    private val wrapper: What3WordsAndroidWrapper,
    private val recognizer: TextRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS),
    private val dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider()
) : W3WOcrWrapper {

    companion object {
        /**
         * Static method to check if this implementation of [W3WOcrWrapper] allows to set a specific language.
         *
         * @return true if allows to set language, false if this wrapper is language agnostic.
         **/
        fun allowsToSetLanguage(): Boolean {
            return false
        }

        /**
         * Static method to check if [languageCode] is supported by this [W3WOcrWrapper], it will throw [UnsupportedOperationException] if [allowsToSetLanguage] is false.
         *
         * @param languageCode the ISO- language code to check for support.
         * @return true if supports this [languageCode], false if not.
         *
         * @throws [UnsupportedOperationException] if [allowsToSetLanguage] is false.
         */
        fun supportsLanguage(languageCode: String): Boolean {
            throw java.lang.UnsupportedOperationException(
                "MLKit doesn't support language selection, , will try to scan all available languages listed here:" +
                        "https://developers.google.com/ml-kit/vision/text-recognition/languages"
            )
        }

        /**
         * Static method to get the list of languages ISO supported by this wrapper, it will throw [UnsupportedOperationException] if [allowsToSetLanguage] is false.
         *
         * @return list of ISO language codes supported by this wrapper.
         *
         * @throws [UnsupportedOperationException] if [allowsToSetLanguage] is false.
         */
        fun availableLanguages(): List<String> {
            throw java.lang.UnsupportedOperationException(
                "MLKit doesn't support language selection, will try to scan all available languages listed here:" +
                        "https://developers.google.com/ml-kit/vision/text-recognition/languages"
            )
        }
    }

    private val imageAnalyzerExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    override fun language(languageCode: String) {
        throw java.lang.UnsupportedOperationException(
            "MLKit doesn't support language selection, will try to scan all available languages listed here:" +
                    "https://developers.google.com/ml-kit/vision/text-recognition/languages"
        )
    }

    override fun scan(
        image: Bitmap,
        options: AutosuggestOptions?,
        onScanning: () -> Unit,
        onDetected: () -> Unit,
        onValidating: () -> Unit,
        onFinished: (OcrScanResult) -> Unit
    ) {
        onScanning.invoke()
        val ocrScanResult = OcrScanResult()
        val listFound3wa = mutableListOf<Suggestion>()
        recognizer.process(image, 0).addOnSuccessListener { visionText ->
            io(dispatcherProvider) {
                for (possible3wa in What3WordsV3.findPossible3wa(visionText.text)) {
                    onDetected.invoke()
                    val autosuggestReq =
                        wrapper.autosuggest(possible3wa)
                    if(options != null) autosuggestReq.options(options)
                    val autosuggestRes = autosuggestReq.execute()
                    if (autosuggestRes.isSuccessful) {
                        //checks if at least one suggestion words matches the possible3wa from the regex,
                        //this makes our OCR more accurate and avoids getting partial what3words address while focusing the camera.
                        autosuggestRes.suggestions.firstOrNull { it.words == possible3wa }?.let {
                            listFound3wa.add(it)
                        }
                    } else {
                        ocrScanResult.error = autosuggestRes.error
                    }
                    onValidating.invoke()
                }
                if (ocrScanResult.error == null) {
                    ocrScanResult.suggestions = listFound3wa
                }
                withContext(dispatcherProvider.main()) { onFinished.invoke(ocrScanResult) }
            }
        }.addOnFailureListener { e ->
            ocrScanResult.error = APIResponse.What3WordsError.SDK_ERROR.apply {
                message = e.message
            }
            onFinished.invoke(ocrScanResult)
        }
    }

    override fun getExecutor(): ExecutorService {
        return imageAnalyzerExecutor
    }

    override fun getDataProvider(): What3WordsAndroidWrapper {
        return wrapper
    }

    override fun stop() {
        recognizer.close()
    }
}