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
import com.what3words.design.library.ui.models.DisplayUnits
import com.what3words.javawrapper.request.AutosuggestOptions
import com.what3words.javawrapper.response.Coordinates
import com.what3words.javawrapper.response.SuggestionWithCoordinates
import com.what3words.ocr.components.R
import com.what3words.ocr.components.models.W3WOcrMLKitWrapper
import com.what3words.ocr.components.models.W3WOcrWrapper

class MLKitOcrScanActivity : BaseOcrScanActivity() {
    companion object {
        /**
         * Creates a new [MLKitOcrScanActivity] Intent to work with our API.
         *
         * It creates a new instance of [BaseOcrScanActivity] where will have a jetpack compose
         * composable that handles the Camera using CameraX with a specified MLKit Library for text recognition.
         *
         * @param context context needed to create new Intent.
         * @param mlKitLibrary the MLKit Library that this instance should use to detect text,
         * options [W3WOcrWrapper.MLKitLibraries.Latin] (default), [W3WOcrWrapper.MLKitLibraries.LatinAndChinese],
         * [W3WOcrWrapper.MLKitLibraries.LatinAndKorean], [W3WOcrWrapper.MLKitLibraries.LatinAndDevanagari], [W3WOcrWrapper.MLKitLibraries.LatinAndJapanese].
         * @param apiKey the API key to use when querying the what3words API.
         * @param options [AutosuggestOptions] to be applied when using what3words API. (Optional)
         * @param returnCoordinates when a [SuggestionWithCoordinates] is picked if it should return [Coordinates] or not. Default false, if true, it might result in API cost charges.
         */
        fun newInstanceWithApi(
            context: Context,
            mlKitLibrary: W3WOcrWrapper.MLKitLibraries,
            apiKey: String,
            options: AutosuggestOptions? = null,
            returnCoordinates: Boolean = false,
            displayUnits: DisplayUnits = DisplayUnits.SYSTEM,
            scanStateScanningTitle: String = context.getString(R.string.scan_state_scanning),
            scanStateDetectedTitle: String = context.getString(R.string.scan_state_detecting),
            scanStateValidatingTitle: String = context.getString(R.string.scan_state_validating),
            scanStateFoundTitle: String = context.getString(R.string.scan_state_found),
            scanStateLoadingTitle: String = context.getString(R.string.scan_state_loading)
        ): Intent {
            return buildInstance(
                context,
                mlKitLibrary,
                W3WOcrWrapper.DataProvider.API,
                apiKey,
                options,
                returnCoordinates,
                displayUnits,
                scanStateScanningTitle,
                scanStateDetectedTitle,
                scanStateValidatingTitle,
                scanStateFoundTitle,
                scanStateLoadingTitle
            )
        }

        /**
         * Creates a new [MLKitOcrScanActivity] Intent to work with our SDK.
         *
         * It creates a new instance of [BaseOcrScanActivity] where will have a jetpack compose
         * composable that handles the Camera using CameraX with a specified MLKit Library for text recognition.
         *
         * @param context context needed to create new Intent.
         * @param mlKitLibrary the MLKit Library that this instance should use to detect text,
         * options [W3WOcrWrapper.MLKitLibraries.Latin] (default), [W3WOcrWrapper.MLKitLibraries.LatinAndChinese],
         * [W3WOcrWrapper.MLKitLibraries.LatinAndKorean], [W3WOcrWrapper.MLKitLibraries.LatinAndDevanagari], [W3WOcrWrapper.MLKitLibraries.LatinAndJapanese].
         * @param options [AutosuggestOptions] to be applied when using what3words SDK. (Optional)
         * @param returnCoordinates when a [SuggestionWithCoordinates] is picked if it should return [Coordinates] or not.
         */
        fun newInstanceWithSdk(
            context: Context,
            mlKitLibrary: W3WOcrWrapper.MLKitLibraries,
            options: AutosuggestOptions? = null,
            returnCoordinates: Boolean = false,
            displayUnits: DisplayUnits = DisplayUnits.SYSTEM,
            scanStateScanningTitle: String = context.getString(R.string.scan_state_scanning),
            scanStateDetectedTitle: String = context.getString(R.string.scan_state_detecting),
            scanStateValidatingTitle: String = context.getString(R.string.scan_state_validating),
            scanStateFoundTitle: String = context.getString(R.string.scan_state_found),
            scanStateLoadingTitle: String = context.getString(R.string.scan_state_loading)
        ): Intent {
            return buildInstance(
                context,
                mlKitLibrary,
                W3WOcrWrapper.DataProvider.SDK,
                null,
                options,
                returnCoordinates,
                displayUnits,
                scanStateScanningTitle,
                scanStateDetectedTitle,
                scanStateValidatingTitle,
                scanStateFoundTitle,
                scanStateLoadingTitle
            )
        }

        private fun buildInstance(
            context: Context,
            mlKitLibrary: W3WOcrWrapper.MLKitLibraries,
            dataProvider: W3WOcrWrapper.DataProvider,
            apiKey: String? = null,
            options: AutosuggestOptions? = null,
            returnCoordinates: Boolean,
            displayUnits: DisplayUnits,
            scanStateScanningTitle: String,
            scanStateDetectedTitle: String,
            scanStateValidatingTitle: String,
            scanStateFoundTitle: String,
            scanStateLoadingTitle: String
        ): Intent {
            return Intent(context, MLKitOcrScanActivity::class.java).apply {
                this.putExtra(DATA_PROVIDER_ID, dataProvider)
                this.putExtra(OCR_PROVIDER_ID, W3WOcrWrapper.OcrProvider.MLKit)
                this.putExtra(MLKIT_LIBRARY_ID, mlKitLibrary)
                this.putExtra(AUTOSUGGEST_OPTIONS_ID, options)
                this.putExtra(API_KEY_ID, apiKey)
                this.putExtra(RETURN_COORDINATES_ID, returnCoordinates)
                this.putExtra(DISPLAY_UNITS_ID, displayUnits)
                this.putExtra(SCAN_STATE_SCANNING_TITLE_ID, scanStateScanningTitle)
                this.putExtra(SCAN_STATE_DETECTED_TITLE_ID, scanStateDetectedTitle)
                this.putExtra(SCAN_STATE_VALIDATING_TITLE_ID, scanStateValidatingTitle)
                this.putExtra(SCAN_STATE_FOUND_TITLE_ID, scanStateFoundTitle)
                this.putExtra(SCAN_STATE_LOADING_TITLE_ID, scanStateLoadingTitle)
            }
        }
    }

    override val ocrWrapper: W3WOcrWrapper by lazy {
        val dataProvider: What3WordsAndroidWrapper =
            if (dataProvider == W3WOcrWrapper.DataProvider.SDK) {
                What3WordsSdk(this, "")
            } else {
                What3WordsV3(apiKey!!, this)
            }
        when (ocrProvider) {
            W3WOcrWrapper.OcrProvider.MLKit -> {
                buildMLKit(this, dataProvider)
            }

            else -> {
                throw ExceptionInInitializerError("Use private library for Hybrid and Tesseract")
            }
        }
    }

    private fun buildMLKit(context: Context, dataProvider: What3WordsAndroidWrapper): W3WOcrMLKitWrapper {
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
        return W3WOcrMLKitWrapper(context, dataProvider, textRecognizer)
    }
}