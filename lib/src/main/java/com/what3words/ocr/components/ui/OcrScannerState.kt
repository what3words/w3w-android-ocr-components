package com.what3words.ocr.components.ui

import androidx.collection.ArraySet
import androidx.collection.arraySetOf
import androidx.compose.runtime.Stable
import com.what3words.core.types.domain.W3WSuggestion

@Stable
data class OcrScannerState(
    val foundItems: List<W3WSuggestion> = listOf(),
    val state: State = State.Idle
) {

    enum class State {
        Idle,
        Scanning,
        Detected,
        Validating,
        Found
    }
}