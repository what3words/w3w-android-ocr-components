package com.what3words.ocr.components.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import com.what3words.core.datasource.image.W3WImageDataSource
import com.what3words.core.datasource.text.W3WTextDataSource
import com.what3words.core.types.common.W3WError
import com.what3words.core.types.common.W3WResult
import com.what3words.core.types.domain.W3WSuggestion
import com.what3words.core.types.image.W3WImage
import com.what3words.core.types.options.W3WAutosuggestOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Manages OCR (Optical Character Recognition) scanning operations for what3words addresses
 * within [W3WOcrScanner].
 *
 * This class utilizing the [W3WImageDataSource] to scan images for possible what3words addresses, then it uses the [W3WTextDataSource] for
 * validating detected addresses.
 *
 * In most cases, this will be created via [rememberOcrScanManager].
 *
 * @property w3wImageDataSource The data source for scanning images for possible what3words addresses.
 * @property w3wTextDataSource The data source for what3words address validation operations.
 * @property options Optional [W3WAutosuggestOptions] for what3words address validation.
 */
class OcrScanManager(
    private val w3wImageDataSource: W3WImageDataSource,
    private val w3wTextDataSource: W3WTextDataSource,
    private val options: W3WAutosuggestOptions? = null
) {
    /**
     * The current state of the OCR scanner, exposed as a Compose State.
     * This allows the W3WOcrScanner Composable to reactively update based on scanner state changes.
     */
    private val _ocrScannerState = MutableStateFlow(OcrScannerState())
    val ocrScannerState: StateFlow<OcrScannerState> = _ocrScannerState.asStateFlow()
    private var isStopping: Boolean = false

    /**
     * Prepares the OCR scanner for operation. This method should be called before scanning images.
     *
     * @param onReady Callback invoked when the scanner is ready.
     * @param onError Callback invoked if an error occurs during preparation.
     */
    fun getReady(onReady: () -> Unit, onError: (W3WError) -> Unit) {
        isStopping = false
        w3wImageDataSource.start(onReady, onError)
    }

    /**
     * Stops the OCR scanner and resets its state.
     */
    fun stop() {
        isStopping = true
        w3wImageDataSource.stop()
        _ocrScannerState.value = OcrScannerState()
    }

    /**
     * Scan the image for what3words addresses.
     *
     * @param image the [W3WImage] to be scanned.
     * @param onError the callback with a [W3WError] in case an error was found while scanning.
     * @param onFound the callback with a list of [W3WSuggestion] in case a what3words address was found.
     * @param onCompleted the callback when the scanning process is completed.
     */
    fun scanImage(
        image: W3WImage,
        onError: (W3WError) -> Unit,
        onFound: (List<W3WSuggestion>) -> Unit,
        onCompleted: () -> Unit
    ) {
        // In case the scanner is stopping, we ignore the scan request.
        if (isStopping) {
            return
        }

        try {
            w3wImageDataSource.scan(
                image,
                onScanning = {
                    updateState(OcrScannerState.State.Scanning)
                },
                onDetected = { possibleAddresses ->
                    updateState(OcrScannerState.State.Detected)
                    updateState(OcrScannerState.State.Validating)
                    validateAddressesAsync(possibleAddresses) { newSuggestions ->
                        if (newSuggestions.isNotEmpty()) {
                            onFound(newSuggestions)
                            updateState(
                                OcrScannerState.State.Found,
                                (newSuggestions + _ocrScannerState.value.foundItems).distinctBy { it.w3wAddress.words }
                            )
                        }

                    }
                },
                onError = {
                    onError(it)
                },
                onCompleted = {
                    image.bitmap.recycle()
                    onCompleted.invoke()
                }
            )
        } catch (e: Exception) {
            onError(W3WError(message = e.message))
        }
    }

    private fun validateAddressesAsync(
        possibleAddresses: List<String>,
        onValidationComplete: (List<W3WSuggestion>) -> Unit
    ) {
        val filteredAddresses =
            possibleAddresses.filterNot { it in _ocrScannerState.value.foundItems.map { item -> item.w3wAddress.words } }
        if (filteredAddresses.isEmpty()) {
            onValidationComplete(emptyList())
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            val newSuggestions = filteredAddresses.mapNotNull { possible3wa ->
                when (val result = w3wTextDataSource.autosuggest(possible3wa, options)) {
                    is W3WResult.Success -> result.value.firstOrNull {
                        it.w3wAddress.words.equals(
                            possible3wa,
                            ignoreCase = true
                        )
                    }

                    is W3WResult.Failure -> null
                }
            }
            onValidationComplete(newSuggestions)
        }
    }

    private fun updateState(
        newState: OcrScannerState.State,
        newFoundItems: List<W3WSuggestion>? = null
    ) {
        _ocrScannerState.update { currentState ->
            currentState.copy(
                state = newState,
                foundItems = newFoundItems ?: currentState.foundItems
            )
        }
    }
}

/**
 * Creates a [OcrScanManager] that is remembered across compositions. It also sets up
 * a [DisposableEffect] to properly stop the manager when the composable leaves the
 * composition.
 *
 * @param w3wImageDataSource The data source for scanning images for possible what3words addresses.
 * @param w3wTextDataSource The data source for validating what3words addresses.
 * @param options Optional [W3WAutosuggestOptions] for address validation.
 *
 * @see OcrScanManager
 */
@Composable
fun rememberOcrScanManager(
    w3wImageDataSource: W3WImageDataSource,
    w3wTextDataSource: W3WTextDataSource,
    options: W3WAutosuggestOptions?,
): OcrScanManager {
    val manager = remember {
        OcrScanManager(
            w3wImageDataSource = w3wImageDataSource,
            w3wTextDataSource = w3wTextDataSource,
            options = options,
        )
    }

    DisposableEffect(key1 = Unit) {
        onDispose {
            manager.stop()
        }
    }

    return manager
}