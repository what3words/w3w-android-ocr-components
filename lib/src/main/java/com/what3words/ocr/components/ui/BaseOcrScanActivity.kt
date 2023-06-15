package com.what3words.ocr.components.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.what3words.design.library.ui.models.DisplayUnits
import com.what3words.design.library.ui.theme.W3WTheme
import com.what3words.javawrapper.request.AutosuggestOptions
import com.what3words.ocr.components.R
import com.what3words.ocr.components.extensions.serializable
import com.what3words.ocr.components.models.*

@SuppressLint("UnsafeOptInUsageError")
abstract class BaseOcrScanActivity : AppCompatActivity() {

    protected lateinit var dataProvider: W3WOcrWrapper.DataProvider
    protected lateinit var ocrProvider: W3WOcrWrapper.OcrProvider
    protected lateinit var displayUnits: DisplayUnits
    protected lateinit var scanStateScanningTitle: String
    protected lateinit var scanStateDetectedTitle: String
    protected lateinit var scanStateValidatingTitle: String
    protected lateinit var scanStateFoundTitle: String
    protected lateinit var scanStateLoadingTitle: String
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
        const val SUGGESTION_RESULT_ID = "SUGGESTION_RESULT"
        const val RETURN_COORDINATES_ID = "RETURN_COORDINATES"
        const val DISPLAY_UNITS_ID = "DISPLAY_UNITS"
        const val SCAN_STATE_SCANNING_TITLE_ID = "SCAN_STATE_SCANNING_TITLE"
        const val SCAN_STATE_DETECTED_TITLE_ID = "SCAN_STATE_DETECTED_TITLE"
        const val SCAN_STATE_VALIDATING_TITLE_ID = "SCAN_STATE_VALIDATING_TITLE"
        const val SCAN_STATE_FOUND_TITLE_ID = "SCAN_STATE_FOUND_TITLE"
        const val SCAN_STATE_LOADING_TITLE_ID = "SCAN_STATE_LOADING_TITLE"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // we can assume here that is not null due to the checks and exceptions thrown on the Builder.build()
        dataProvider = intent.serializable(DATA_PROVIDER_ID)!!
        ocrProvider = intent.serializable(OCR_PROVIDER_ID)!!
        mlKitV2Library = intent.serializable(MLKIT_LIBRARY_ID)
        apiKey = intent.getStringExtra(API_KEY_ID)
        languageCode = intent.getStringExtra(LANGUAGE_CODE_ID)
        tessDataPath = intent.getStringExtra(TESS_DATA_PATH_ID)
        autosuggestOptions = intent.serializable(AUTOSUGGEST_OPTIONS_ID)
        displayUnits = intent.serializable(DISPLAY_UNITS_ID)!!
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
        setContent {
            W3WTheme {
                val lifecycleOwner = LocalLifecycleOwner.current
                DisposableEffect(LocalLifecycleOwner.current) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_DESTROY) {
                            ocrWrapper.stop()
                        }
                    }

                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                    }
                }

                // A surface container using the 'background' color from the theme
                W3WOcrScanner(
                    ocrWrapper,
                    options = autosuggestOptions,
                    returnCoordinates = returnCoordinates,
                    displayUnits = displayUnits,
                    scanStateScanningTitle = scanStateScanningTitle,
                    scanStateDetectedTitle = scanStateDetectedTitle,
                    scanStateValidatingTitle = scanStateValidatingTitle,
                    scanStateFoundTitle = scanStateFoundTitle,
                    scanStateLoadingTitle = scanStateLoadingTitle,
                    onError = {
                        setResult(RESULT_CANCELED)
                        finish()
                    },
                    onDismiss = {
                        setResult(RESULT_CANCELED)
                        finish()
                    },
                    onSuggestionSelected = {
                        setResult(RESULT_OK, Intent().apply {
                            putExtra(SUGGESTION_RESULT_ID, it)
                        })
                        finish()
                    })
            }
        }
    }
}