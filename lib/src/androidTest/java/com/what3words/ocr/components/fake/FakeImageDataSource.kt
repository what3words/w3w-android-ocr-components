package com.what3words.ocr.components.fake

import com.what3words.core.datasource.image.W3WImageDataSource
import com.what3words.core.types.common.W3WError
import com.what3words.core.types.image.W3WImage

class FakeImageDataSource : W3WImageDataSource {

    private var isStarted = false
    private var scanResults: List<String> = emptyList()
    private var shouldSimulateError = false
    private var simulatedError: W3WError? = null
    private var shouldSimulateStartError = false

    fun setScanResults(results: List<String>) {
        scanResults = results
    }

    fun setShouldSimulateStartError(shouldSimulate: Boolean) {
        shouldSimulateStartError = shouldSimulate
    }

    fun setSimulateError(error: W3WError?) {
        shouldSimulateError = error != null
        simulatedError = error
    }

    override fun start(onReady: () -> Unit, onError: (W3WError) -> Unit) {
        if (shouldSimulateStartError) {
            isStarted = false
            onError(W3WError("Simulated error"))
        } else {
            isStarted = true
            onReady()
        }
    }

    override fun stop() {
        isStarted = false
    }

    override fun scan(
        image: W3WImage,
        onScanning: () -> Unit,
        onDetected: (List<String>) -> Unit,
        onError: (W3WError) -> Unit,
        onCompleted: () -> Unit
    ) {
        if (!isStarted) {
            onError(W3WError("Scanner not started"))
            return
        }

        onScanning()

        if (shouldSimulateError) {
            onError(simulatedError ?: W3WError("Simulated error"))
        } else {
            onDetected(scanResults)
        }

        onCompleted()
    }
}