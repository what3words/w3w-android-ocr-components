package com.what3words.ocr.components.ui

import android.content.Context
import android.content.Intent
import com.what3words.androidwrapper.datasource.text.W3WApiTextDataSource
import com.what3words.core.datasource.image.W3WImageDataSource
import com.what3words.core.datasource.text.W3WTextDataSource
import com.what3words.core.types.options.W3WAutosuggestOptions
import com.what3words.datasource.text.W3WSDKTextDataSource
import com.what3words.design.library.ui.models.DisplayUnits
import com.what3words.javasdk.W3wEngines
import com.what3words.javawrapper.request.AutosuggestOptions
import com.what3words.javawrapper.response.Coordinates
import com.what3words.javawrapper.response.SuggestionWithCoordinates
import com.what3words.ocr.components.R
import com.what3words.ocr.components.models.W3WMLKitImageDataSource
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

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
            mlKitLibrary: Int,
            apiKey: String,
            options: W3WAutosuggestOptions? = null,
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
                BaseOcrScanActivity.Companion.DataProvider.API,
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
            mlKitLibrary: Int,
            options: W3WAutosuggestOptions? = null,
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
                BaseOcrScanActivity.Companion.DataProvider.SDK,
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
            mlKitLibrary: Int,
            dataProvider: BaseOcrScanActivity.Companion.DataProvider,
            apiKey: String? = null,
            options: W3WAutosuggestOptions? = null,
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
                this.putExtra(MLKIT_LIBRARY_ID, mlKitLibrary)
                this.putExtra(AUTOSUGGEST_OPTIONS_ID, Json.encodeToString(options))
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

    override val w3WTextDataSource: W3WTextDataSource by lazy {
        if (dataProviderType == BaseOcrScanActivity.Companion.DataProvider.SDK) {
            val iEngine = W3wEngines.newDeviceEngine(this)
            W3WSDKTextDataSource.create(iEngine)
        } else {
            W3WApiTextDataSource.create(this, apiKey!!)
        }
    }

    override val w3WImageDataSource: W3WImageDataSource by lazy {
        buildMLKit(this)
    }

    private fun buildMLKit(
        context: Context
    ): W3WMLKitImageDataSource {
        if (mlKitV2Library == null) throw ExceptionInInitializerError(
            "MLKitOcrScanActivity needs a valid MLKit Language Library"
        )
        return W3WMLKitImageDataSource.create(context, mlKitV2Library!!)
    }
}