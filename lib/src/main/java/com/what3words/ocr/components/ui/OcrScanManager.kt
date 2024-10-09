package com.what3words.ocr.components.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
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
import kotlinx.coroutines.launch
import java.util.concurrent.CountDownLatch

class OcrScanManager(
    private val w3wImageDataSource: W3WImageDataSource,
    private val w3wTextDataSource: W3WTextDataSource,
    private val options: W3WAutosuggestOptions? = null
) {
    private val _ocrScannerState = mutableStateOf(OcrScannerState())
    val ocrScannerState: State<OcrScannerState> = _ocrScannerState

    fun getReady(onReady: () -> Unit, onError: (W3WError) -> Unit) {
        w3wImageDataSource.start(onReady, onError)
    }

    fun stop() {
        w3wImageDataSource.stop()
        _ocrScannerState.value = OcrScannerState()
    }

    /**
     * Scan the image for what3words addresses. This function is blocking the current thread until the scan is completed.
     *
     * @param image the [W3WImage] to be scanned.
     * @param onError the callback with a [W3WError] in case an error was found while scanning.
     * @param onFound the callback with a list of [W3WSuggestion] in case a what3words address was found.
     */
    fun scanImageBlocking(
        image: W3WImage,
        onError: (W3WError) -> Unit,
        onFound: (List<W3WSuggestion>) -> Unit,
    ) {
        val latch = CountDownLatch(1)
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
                    latch.countDown()
                },
                onCompleted = { latch.countDown() }
            )
        } catch (e: Exception) {
            onError(W3WError(message = e.message))
            latch.countDown()
        } finally {
            latch.await()
            image.bitmap.recycle()
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
        _ocrScannerState.value = _ocrScannerState.value.copy(
            state = newState,
            foundItems = newFoundItems ?: _ocrScannerState.value.foundItems
        )
    }
}

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