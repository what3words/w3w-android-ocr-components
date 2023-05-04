package com.what3words.ocr.components.sample

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatSpinner
import androidx.camera.core.ExperimentalGetImage
import com.what3words.androidwrapper.What3WordsV3
import com.what3words.ocr.components.models.OcrScanResult
import com.what3words.ocr.components.models.W3WOcrMLKitWrapper
import com.what3words.ocr.components.models.decodeBase64
import com.what3words.ocr.components.ui.OcrScanFragment


@ExperimentalGetImage
class OcrScanFragmentSampleActivity : AppCompatActivity(), OcrScanFragment.OcrScanResultCallback {

    private lateinit var scannedImage: ImageView
    private lateinit var results: TextView
    private lateinit var restartButton: Button
    private lateinit var fragment: OcrScanFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val spinner = findViewById<AppCompatSpinner>(R.id.language_spinner)
        results = findViewById(R.id.results_text)
        restartButton = findViewById(R.id.restart_button)
        scannedImage = findViewById(R.id.scanned_image)

        val wrapper = W3WOcrMLKitWrapper(
            What3WordsV3("YOUR_API_KEY", this)
        )

        if (savedInstanceState == null) {
            fragment = OcrScanFragment.newInstance()
            fragment.ocrWrapper(wrapper, this)
            supportFragmentManager.beginTransaction().replace(R.id.camera_container, fragment)
                .commitNow()
        }

        restartButton.setOnClickListener {
            scannedImage.setImageResource(android.R.color.transparent)
            results.visibility = View.GONE
            results.text = ""
            fragment.initializeScan()
            restartButton.visibility = View.GONE
        }
    }

    override fun onFinished(result: OcrScanResult) {
        if (result.isSuccessful()) {
            result.scannedImage?.let { base64 ->
                scannedImage.setImageBitmap(decodeBase64(base64))
            }
            var resultText = ""
            result.suggestions.forEach { suggestion ->
                resultText =
                    resultText.plus("${suggestion.words}, ${suggestion.nearestPlace}\n")
            }
            results.text = resultText
        } else {
            results.text = result.error?.message
        }
        results.visibility = View.VISIBLE
        restartButton.visibility = View.VISIBLE
    }
}