package com.what3words.ocr.components.sample

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.what3words.ocr.components.models.W3WOcrWrapper

class ComposeOcrScanSamplePopupViewModel : ViewModel() {

    var results: String? by mutableStateOf(null)
    var selectedMLKitLibrary: W3WOcrWrapper.MLKitLibraries by mutableStateOf(W3WOcrWrapper.MLKitLibraries.Latin)
    var availableMLKitLanguages: List<W3WOcrWrapper.MLKitLibraries> by mutableStateOf(
        W3WOcrWrapper.MLKitLibraries.values().toList()
    )
    var ocrWrapper: W3WOcrWrapper? by mutableStateOf(null)
}
