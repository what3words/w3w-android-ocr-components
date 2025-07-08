package com.what3words.ocr.components.ui

import androidx.compose.runtime.Stable
import com.what3words.core.types.domain.W3WSuggestion
import com.what3words.core.types.image.W3WImage

/**
 * Represents the state of an [W3WOcrScanner], including detected what3words suggestions
 * and the current operational state of the scanner.
 *
 * @property foundItems A list of `W3WSuggestion` objects representing the detected what3words
 *                      addresses. Defaults to an empty list.
 * @property state The current operational state of the OCR scanner, represented by the nested
 *                 `State` enum. Defaults to `State.Idle`.
 */
@Stable
data class OcrScannerState(
    /**
     * List of detected what3words addresses.
     */
    val foundItems: List<W3WSuggestion> = listOf(),
    /**
     * Current operational state of the OCR scanner.
     */
    val state: State = State.Idle,
    val scanningType: ScanningType = ScanningType.Photo,
    val capturedImage: W3WImage? = null,
    /**
     * Indicates whether the captured image came from media import (true) or camera capture (false).
     */
    val isFromMedia: Boolean = false
) {

    /**
     * Represents the operational states of the OCR scanner.
     *
     * - `Idle`: The scanner is not currently active.
     * - `Scanning`: The scanner is actively capturing frames and searching for what3words addresses.
     * - `Detected`: Potential what3words addresses have been detected but are not yet validated.
     * - `Validating`: The detected addresses are being validated for accuracy.
     * - `Found`: One or more valid what3words addresses have been successfully detected.
     */
    enum class State {
        Idle,
        Scanning,
        Detected,
        Validating,
        Found,
        NotFound
    }

    enum class ScanningType {
        Live,
        Photo
    }
}
