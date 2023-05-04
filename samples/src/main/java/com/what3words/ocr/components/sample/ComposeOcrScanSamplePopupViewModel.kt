package com.what3words.ocr.components.sample

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.what3words.ocr.components.models.W3WOcrWrapper

class ComposeOcrScanSamplePopupViewModel : ViewModel() {

    var scannedImage: String? by mutableStateOf(null)

    var results: String? by mutableStateOf(null)
    var selectedMLKitLibrary: W3WOcrWrapper.MLKitLibraries by mutableStateOf(W3WOcrWrapper.MLKitLibraries.Latin)
    var availableMLKitLanguages: List<W3WOcrWrapper.MLKitLibraries> by mutableStateOf(
        W3WOcrWrapper.MLKitLibraries.values().toList()
    )
    var ocrType: W3WOcrWrapper.OcrProvider by mutableStateOf(W3WOcrWrapper.OcrProvider.MLKit)
        private set
}
