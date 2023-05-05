package com.what3words.ocr.components.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.what3words.ocr.components.R
import com.what3words.ocr.components.extensions.serializable
import com.what3words.ocr.components.models.*

@SuppressLint("UnsafeOptInUsageError")
abstract class BaseOcrScanActivity : AppCompatActivity(), OcrScanFragment.OcrScanResultCallback {

    protected lateinit var dataProvider: W3WOcrWrapper.DataProvider
    protected lateinit var ocrProvider: W3WOcrWrapper.OcrProvider
    protected var mlKitV2Library: W3WOcrWrapper.MLKitLibraries? = null
    protected var apiKey: String? = null
    protected var languageCode: String? = null
    protected var tessDataPath: String? = null

    abstract val ocrWrapper: W3WOcrWrapper


    companion object {
        const val OCR_PROVIDER_ID = "OCR_PROVIDER"
        const val DATA_PROVIDER_ID = "DATA_PROVIDER"
        const val MLKIT_LIBRARY_ID = "MLKIT_LIBRARY"
        const val API_KEY_ID = "API_KEY"
        const val LANGUAGE_CODE_ID = "LANGUAGE_CODE"
        const val TESS_DATA_PATH_ID = "TESS_DATA_PATH"
        const val RESULT_ID = "RESULT"
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