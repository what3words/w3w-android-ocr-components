package com.what3words.ocr.components.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.what3words.design.library.ui.models.DisplayUnits
import com.what3words.design.library.ui.theme.W3WTheme
import com.what3words.javawrapper.request.AutosuggestOptions
import com.what3words.ocr.components.R
import com.what3words.ocr.components.extensions.serializable
import com.what3words.ocr.components.models.W3WOcrWrapper

abstract class BaseOcrScanActivity : ComponentActivity() {

    protected lateinit var dataProvider: W3WOcrWrapper.DataProvider
    protected lateinit var ocrProvider: W3WOcrWrapper.OcrProvider
    protected lateinit var displayUnits: DisplayUnits
    protected lateinit var scanStateScanningTitle: String
    protected lateinit var scanStateDetectedTitle: String
    protected lateinit var scanStateValidatingTitle: String
    protected lateinit var scanStateFoundTitle: String
    protected lateinit var scanStateLoadingTitle: String
    protected lateinit var closeButtonContentDescription: String
    protected var mlKitV2Library: W3WOcrWrapper.MLKitLibraries? = null
    protected var apiKey: String? = null
    protected var languageCode: String? = null
    protected var tessDataPath: String? = null
    protected var autosuggestOptions: AutosuggestOptions? = null
    protected var returnCoordinates: Boolean = false

    abstract val ocrWrapper: W3WOcrWrapper

    companion object {
        const val OCR_PROVIDER_ID = "OCR_PROVIDER"
        const val DATA_PROVIDER_ID = "DATA_PROVIDER"
        const val MLKIT_LIBRARY_ID = "MLKIT_LIBRARY"
        const val AUTOSUGGEST_OPTIONS_ID = "AUTOSUGGEST_OPTIONS"
        const val API_KEY_ID = "API_KEY"
        const val LANGUAGE_CODE_ID = "LANGUAGE_CODE"
        const val TESS_DATA_PATH_ID = "TESS_DATA_PATH"
        const val SUCCESS_RESULT_ID = "SUCCESS_RESULT"
        const val ERROR_RESULT_ID = "ERROR_RESULT"
        const val RETURN_COORDINATES_ID = "RETURN_COORDINATES"
        const val DISPLAY_UNITS_ID = "DISPLAY_UNITS"
        const val SCAN_STATE_SCANNING_TITLE_ID = "SCAN_STATE_SCANNING_TITLE"
        const val SCAN_STATE_DETECTED_TITLE_ID = "SCAN_STATE_DETECTED_TITLE"
        const val SCAN_STATE_VALIDATING_TITLE_ID = "SCAN_STATE_VALIDATING_TITLE"
        const val SCAN_STATE_FOUND_TITLE_ID = "SCAN_STATE_FOUND_TITLE"
        const val SCAN_STATE_LOADING_TITLE_ID = "SCAN_STATE_LOADING_TITLE"
        const val CLOSE_BUTTON_CD_ID = "CLOSE_BUTTON_CD"
    }

    override fun onDestroy() {
        super.onDestroy()
        ocrWrapper.stop()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // we can assume here that is not null due to the checks and exceptions thrown on the Builder.build()
        if (!intent.hasExtra(DATA_PROVIDER_ID) || !intent.hasExtra(OCR_PROVIDER_ID)) {
            throw IllegalAccessException("Missing data provider or ocr provider, please use newInstanceWithApi or newInstanceWithSdk to create a new a specific instance of our OCR Activities.")
        }
        dataProvider = intent.serializable(DATA_PROVIDER_ID)!!
        ocrProvider = intent.serializable(OCR_PROVIDER_ID)!!
        mlKitV2Library = intent.serializable(MLKIT_LIBRARY_ID)
        apiKey = intent.getStringExtra(API_KEY_ID)
        languageCode = intent.getStringExtra(LANGUAGE_CODE_ID)
        tessDataPath = intent.getStringExtra(TESS_DATA_PATH_ID)
        autosuggestOptions = intent.serializable(AUTOSUGGEST_OPTIONS_ID)
        displayUnits = intent.serializable(DISPLAY_UNITS_ID) ?: DisplayUnits.SYSTEM
        scanStateScanningTitle = intent.getStringExtra(SCAN_STATE_SCANNING_TITLE_ID)
            ?: getString(R.string.scan_state_scanning)
        scanStateDetectedTitle = intent.getStringExtra(SCAN_STATE_DETECTED_TITLE_ID)
            ?: getString(R.string.scan_state_detecting)
        scanStateValidatingTitle = intent.getStringExtra(SCAN_STATE_VALIDATING_TITLE_ID)
            ?: getString(R.string.scan_state_validating)
        scanStateFoundTitle = intent.getStringExtra(SCAN_STATE_FOUND_TITLE_ID)
            ?: getString(R.string.scan_state_found)
        scanStateLoadingTitle = intent.getStringExtra(SCAN_STATE_LOADING_TITLE_ID)
            ?: getString(R.string.scan_state_loading)
        closeButtonContentDescription = intent.getStringExtra(CLOSE_BUTTON_CD_ID)
            ?: getString(R.string.cd_close_button)
        ocrWrapper.start()
        setContent {
            W3WTheme {
                // A surface container using the 'background' color from the theme
                W3WOcrScanner(
                    ocrWrapper,
                    options = autosuggestOptions,
                    returnCoordinates = returnCoordinates,
                    displayUnits = displayUnits,
                    scannerStrings = W3WOcrScannerDefaults.defaultStrings(
                        scanStateScanningTitle = scanStateScanningTitle,
                        scanStateDetectedTitle = scanStateDetectedTitle,
                        scanStateValidatingTitle = scanStateValidatingTitle,
                        scanStateFoundTitle = scanStateFoundTitle,
                        scanStateLoadingTitle = scanStateLoadingTitle,
                        closeButtonContentDescription = closeButtonContentDescription
                    ),
                    onError = {
                        setResult(RESULT_CANCELED, Intent().apply {
                            putExtra(ERROR_RESULT_ID, it.message)
                        })
                        finish()
                    },
                    onDismiss = {
                        setResult(RESULT_CANCELED)
                        finish()
                    },
                    onSuggestionSelected = {
                        setResult(RESULT_OK, Intent().apply {
                            putExtra(SUCCESS_RESULT_ID, it)
                        })
                        finish()
                    })
            }
        }
    }
}