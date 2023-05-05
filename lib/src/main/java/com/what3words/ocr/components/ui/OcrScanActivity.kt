package com.what3words.ocr.components.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.what3words.androidwrapper.What3WordsAndroidWrapper
import com.what3words.androidwrapper.What3WordsV3
import com.what3words.api.sdk.bridge.models.What3WordsSdk
import com.what3words.ocr.components.R
import com.what3words.ocr.components.extensions.serializable
import com.what3words.ocr.components.models.*

@SuppressLint("UnsafeOptInUsageError")
open class OcrScanActivity : AppCompatActivity(), OcrScanFragment.OcrScanResultCallback {

    protected lateinit var dataProvider: W3WOcrWrapper.DataProvider
    protected lateinit var ocrProvider: W3WOcrWrapper.OcrProvider
    protected var mlKitV2Library: W3WOcrWrapper.MLKitLibraries? = null
    protected var apiKey: String? = null
    protected var languageCode: String? = null
    protected var tessDataPath: String? = null

    protected open val ocrWrapper: W3WOcrWrapper by lazy {
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

    protected fun buildMLKit(dataProvider: What3WordsAndroidWrapper): W3WOcrMLKitWrapper {
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
                    "OcrScanActivity using MLKitV2 OCR needs a valid MLKit Language Library"
                )
            }
        )
        return W3WOcrMLKitWrapper(dataProvider, textRecognizer)
    }

    companion object {
        const val OCR_PROVIDER_ID = "OCR_PROVIDER"
        const val DATA_PROVIDER_ID = "DATA_PROVIDER"
        const val MLKIT_LIBRARY_ID = "MLKIT_LIBRARY"
        const val API_KEY_ID = "API_KEY"
        const val LANGUAGE_CODE_ID = "LANGUAGE_CODE"
        const val TESS_DATA_PATH_ID = "TESS_DATA_PATH"
        const val RESULT_ID = "RESULT"
    }

    open class Builder() {
        protected var languageCode: String? = null
        protected var tessDataPath: String? = null
        protected var dataProvider: W3WOcrWrapper.DataProvider? = null
        protected var ocrProvider: W3WOcrWrapper.OcrProvider? = null
        protected var mlkitLibrary: W3WOcrWrapper.MLKitLibraries? = null
        protected var apiKey: String? = null

        open fun withAPI(apiKey: String): Builder {
            dataProvider = W3WOcrWrapper.DataProvider.API
            this.apiKey = apiKey
            return this
        }

        open fun withSDK(): Builder {
            dataProvider = W3WOcrWrapper.DataProvider.SDK
            return this
        }

        fun withMLKitOcrProvider(mlKitLanguage: W3WOcrWrapper.MLKitLibraries): Builder {
            ocrProvider = W3WOcrWrapper.OcrProvider.MLKit
            mlkitLibrary = mlKitLanguage
            return this
        }

        open fun build(context: Context): Intent {
            if (dataProvider == null || ocrProvider == null) throw java.lang.Exception("OcrScanActivity requires withAPI/withSDK and withOcrProvider to build activity intent")
            if (ocrProvider == W3WOcrWrapper.OcrProvider.MLKit && mlkitLibrary == null) throw ExceptionInInitializerError(
                "OcrScanActivity using MLKitV2 OCR needs a valid MLKit Language Library"
            )
            return Intent(context, OcrScanActivity::class.java).apply {
                this.putExtra(DATA_PROVIDER_ID, dataProvider)
                this.putExtra(OCR_PROVIDER_ID, ocrProvider)
                this.putExtra(MLKIT_LIBRARY_ID, mlkitLibrary)
                this.putExtra(API_KEY_ID, apiKey)
                this.putExtra(LANGUAGE_CODE_ID, languageCode)
                this.putExtra(TESS_DATA_PATH_ID, tessDataPath)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ocr_scan)
        // we can assume here that is not null due to the checks and exceptions thrown on the Builder.build()
        dataProvider = intent.serializable(DATA_PROVIDER_ID)!!
        ocrProvider = intent.serializable(OCR_PROVIDER_ID)!!
        mlKitV2Library = intent.serializable(MLKIT_LIBRARY_ID)
        apiKey = intent.getStringExtra(API_KEY_ID)
        languageCode = intent.getStringExtra(LANGUAGE_CODE_ID)
        tessDataPath = intent.getStringExtra(TESS_DATA_PATH_ID)

        if (savedInstanceState == null) {
            val fragment = OcrScanFragment.newInstance()
            fragment.ocrWrapper(ocrWrapper, this)
            supportFragmentManager.beginTransaction().replace(R.id.container, fragment).commitNow()
        }
    }

    override fun onFinished(result: OcrScanResult) {
        setResult(Activity.RESULT_OK, Intent().apply {
            putExtra(RESULT_ID, result)
        })
        finish()
    }
}