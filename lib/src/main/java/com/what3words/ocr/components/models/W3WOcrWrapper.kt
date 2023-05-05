package com.what3words.ocr.components.models

import android.graphics.Bitmap
import java.util.concurrent.ExecutorService

interface W3WOcrWrapper {

    @Throws(java.lang.UnsupportedOperationException::class)
    fun language(languageCode: String)

    fun scan(
        image: Bitmap,
        onFinished: ((OcrScanResult) -> Unit)
    )

    fun getExecutor() : ExecutorService

    fun stop()

    enum class OcrProvider {
        MLKit,
        Hybrid,
        Tesseract
    }

    enum class MLKitLibraries {
        Latin,
        LatinAndDevanagari,
        LatinAndKorean,
        LatinAndJapanese,
        LatinAndChinese
    }

    enum class DataProvider {
        API,
        SDK
    }
}

