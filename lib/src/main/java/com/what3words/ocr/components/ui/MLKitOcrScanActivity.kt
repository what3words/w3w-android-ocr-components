package com.what3words.ocr.components.ui

import android.content.Context
import android.content.Intent
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
         * @param displayUnits the [DisplayUnits] that will show on the [SuggestionPicker], by default will be [DisplayUnits.SYSTEM] which will use the system Locale to determinate if Imperial or Metric system.
         * @param scanStateScanningTitle the text to be displayed when it starts scanning, default: [R.string.scan_state_scanning]
         * @param scanStateDetectedTitle the text to be displayed when it detects a possible three word address, default: [R.string.scan_state_detecting]
         * @param scanStateValidatingTitle the text to be displayed when it validates a possible three word address (API/SDK check for validation), default: [R.string.scan_state_validating]
         * @param scanStateFoundTitle the title to be displayed as the header of the list of scanned and validated three word addresses, default: [R.string.scan_state_found]
         * @param scanStateLoadingTitle the title to be displayed when it's waiting for permissions to be accepted or any kind of download needed, default: [R.string.scan_state_loading]
         * @param closeButtonContentDescription the content description of the actionable close button, default: [R.string.scan_state_loading]
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
            scanStateLoadingTitle: String = context.getString(R.string.scan_state_loading),
            closeButtonContentDescription: String = context.getString(R.string.cd_close_button)
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
                scanStateLoadingTitle,
                closeButtonContentDescription
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
         * @param displayUnits the [DisplayUnits] that will show on the [SuggestionPicker], by default will be [DisplayUnits.SYSTEM] which will use the system Locale to determinate if Imperial or Metric system.
         * @param scanStateScanningTitle the text to be displayed when it starts scanning, default: [R.string.scan_state_scanning]
         * @param scanStateDetectedTitle the text to be displayed when it detects a possible three word address, default: [R.string.scan_state_detecting]
         * @param scanStateValidatingTitle the text to be displayed when it validates a possible three word address (API/SDK check for validation), default: [R.string.scan_state_validating]
         * @param scanStateFoundTitle the title to be displayed as the header of the list of scanned and validated three word addresses, default: [R.string.scan_state_found]
         * @param scanStateLoadingTitle the title to be displayed when it's waiting for permissions to be accepted or any kind of download needed, default: [R.string.scan_state_loading]
         * @param closeButtonContentDescription the content description of the actionable close button, default: [R.string.scan_state_loading]
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
            scanStateLoadingTitle: String = context.getString(R.string.scan_state_loading),
            closeButtonContentDescription: String = context.getString(R.string.cd_close_button)
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
                scanStateLoadingTitle,
                closeButtonContentDescription
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
            scanStateLoadingTitle: String,
            closeButtonContentDescription: String
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
                this.putExtra(CLOSE_BUTTON_CD_ID, closeButtonContentDescription)
            }
        }
    }

    override val dataProvider: What3WordsAndroidWrapper by lazy {
        if (dataProviderType == W3WOcrWrapper.DataProvider.SDK) {
            What3WordsSdk(this, "")
        } else {
            What3WordsV3(apiKey!!, this)
        }
    }

    override val ocrWrapper: W3WOcrWrapper by lazy {
        when (ocrProviderType) {
            W3WOcrWrapper.OcrProvider.MLKit -> {
                buildMLKit(this)
            }

            else -> {
                throw ExceptionInInitializerError("Use private library for Hybrid and Tesseract")
            }
        }
    }

    private fun buildMLKit(
        context: Context
    ): W3WOcrMLKitWrapper {
        val textRecognizerOptions =
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
        return W3WOcrMLKitWrapper(context, textRecognizerOptions)
    }
}