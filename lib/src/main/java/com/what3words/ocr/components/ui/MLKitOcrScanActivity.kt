package com.what3words.ocr.components.ui

import android.content.Context
import android.content.Intent
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.what3words.androidwrapper.What3WordsAndroidWrapper
import com.what3words.androidwrapper.What3WordsV3
import com.what3words.api.sdk.bridge.models.What3WordsSdk
import com.what3words.ocr.components.models.W3WOcrMLKitWrapper
import com.what3words.ocr.components.models.W3WOcrWrapper

class MLKitOcrScanActivity : BaseOcrScanActivity() {
    override val ocrWrapper: W3WOcrWrapper by lazy {
        val dataProvider: What3WordsAndroidWrapper =
            if (dataProvider == W3WOcrWrapper.DataProvider.SDK) {
                What3WordsSdk(this, "")
            } else {
                What3WordsV3(apiKey!!, this)
            }
        when (ocrProvider) {
            W3WOcrWrapper.OcrProvider.MLKit -> {
                buildMLKit(dataProvider)
            }

            else -> {
                throw ExceptionInInitializerError("Use private library for Hybrid and Tesseract")
            }
        }
    }

    class Builder() {
        private var dataProvider: W3WOcrWrapper.DataProvider? = null
        private var mlkitLibrary: W3WOcrWrapper.MLKitLibraries? = null
        private var apiKey: String? = null

        fun withAPI(apiKey: String): Builder {
            dataProvider = W3WOcrWrapper.DataProvider.API
            this.apiKey = apiKey
            return this
        }

        fun withSDK(): Builder {
            dataProvider = W3WOcrWrapper.DataProvider.SDK
            return this
        }

        fun withMLKitLibrary(mlKitLanguage: W3WOcrWrapper.MLKitLibraries): Builder {
            mlkitLibrary = mlKitLanguage
            return this
        }

        fun build(context: Context): Intent {
            if (dataProvider == null) throw java.lang.Exception("OcrScanActivity requires withAPI/withSDK and withOcrProvider to build activity intent")
            if (mlkitLibrary == null) throw ExceptionInInitializerError(
                "MLKitOcrScanActivity needs a valid MLKit Language Library"
            )
            return Intent(context, MLKitOcrScanActivity::class.java).apply {
                this.putExtra(DATA_PROVIDER_ID, dataProvider)
                this.putExtra(OCR_PROVIDER_ID, W3WOcrWrapper.OcrProvider.MLKit)
                this.putExtra(MLKIT_LIBRARY_ID, mlkitLibrary)
                this.putExtra(API_KEY_ID, apiKey)
            }
        }
    }


    private fun buildMLKit(dataProvider: What3WordsAndroidWrapper): W3WOcrMLKitWrapper {
        val textRecognizer = TextRecognition.getClient(
            when (mlKitV2Library) {
                W3WOcrWrapper.MLKitLibraries.Latin -> TextRecognizerOptions.DEFAULT_OPTIONS
                W3WOcrWrapper.MLKitLibraries.LatinAndDevanagari -> DevanagariTextRecognizerOptions.Builder()
                    .build()

                W3WOcrWrapper.MLKitLibraries.LatinAndKorean -> KoreanTextRecognizerOptions.Builder()
                    .build()

                W3WOcrWrapper.MLKitLibraries.LatinAndJapanese -> JapaneseTextRecognizerOptions.Builder()
                    .build()

                W3WOcrWrapper.MLKitLibraries.LatinAndChinese -> ChineseTextRecognizerOptions.Builder()
                    .build()

                null -> throw ExceptionInInitializerError(
                    "MLKitOcrScanActivity needs a valid MLKit Language Library"
                )
            }
        )
        return W3WOcrMLKitWrapper(dataProvider, textRecognizer)
    }
}