package com.what3words.ocr.components.models

import android.graphics.Bitmap
import com.google.android.gms.common.moduleinstall.InstallStatusListener
import com.google.android.gms.common.moduleinstall.ModuleInstallClient
import com.google.android.gms.common.moduleinstall.ModuleInstallRequest
import com.google.android.gms.common.moduleinstall.ModuleInstallStatusUpdate
import com.google.mlkit.vision.text.TextRecognizer
import com.what3words.core.types.common.W3WError
import com.what3words.core.types.options.W3WAutosuggestOptions
import com.what3words.javawrapper.What3WordsV3.findPossible3wa
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Scan [image] [Bitmap] for one or more three word addresses using MLKit.
 *
 * @param image the [Bitmap] that the scanner should use to find possible three word addresses.
 * i.e: country clipping, check [W3WAutosuggestOptions] for possible filters/clippings.
 * @param onScanning the callback when it starts to scan image for text.
 * @param onDetected the callback when our [findPossible3wa] regex finds possible matches on the scanned text.
 * @param onError the callback with a [W3WError] in case an error was found while scanning.
 */
fun TextRecognizer.scan(
    image: Bitmap,
    onScanning: () -> Unit,
    onDetected: (List<String>) -> Unit,
    onError: (W3WError) -> Unit,
    onCompleted: () -> Unit,
    coroutineScope: CoroutineScope,
    rotation: Int = 0
) {
    require(image.width > 0 && image.height > 0) { "Invalid image dimensions" }
    require(rotation in 0..359) { "Rotation must be between 0 and 359 degrees" }

    val throttleTimeout = 250L

    onScanning.invoke()

    coroutineScope.launch {
        suspendCoroutine { continuation ->
            this@scan.process(image, rotation).addOnSuccessListener { visionText ->
                val possibleAddresses = findPossible3wa(visionText.text)
                if (possibleAddresses.isNotEmpty()) {
                    onDetected.invoke(possibleAddresses)
                }
            }.addOnFailureListener { e ->
                onError.invoke(W3WError(message = "Image processing failed: ${e.message}"))
            }.addOnCompleteListener {
                continuation.resume(Unit)
            }
        }

        delay(throttleTimeout)
    }.invokeOnCompletion { exception ->
        exception?.let { onError.invoke(W3WError(message = it.message)) }
        onCompleted.invoke()
    }
}

/**
 * Checks if all modules that this wrapper depend on are installed, will return true if no modules need to be downloaded to this specific implementation.
 *
 * @param moduleClient [ModuleInstallClient] to check against.
 * @param result a callback with a [Boolean] true if all modules are installed or no modules needed to install, false if not installed and wrapper needs to download modules.
 */
fun TextRecognizer.isModuleInstalled(
    moduleClient: ModuleInstallClient,
    result: (Boolean) -> Unit
) {
    moduleClient
        .areModulesAvailable(this)
        .addOnSuccessListener { response ->
            result.invoke(response.areModulesAvailable())
        }
        .addOnFailureListener {
            result.invoke(false)
        }
}

/**
 * Request to download modules that this wrapper depend on.
 *
 * @param moduleClient [ModuleInstallClient] to check against.
 * @param onCompleted a callback when the download of the modules was successful.
 * @param onError a callback with a [W3WError] in case an error was found while downloading the modules.
 */
fun TextRecognizer.installModule(
    moduleClient: ModuleInstallClient,
    onCompleted: () -> Unit,
    onError: (W3WError) -> Unit
) {
    val listener = object : InstallStatusListener {
        override fun onInstallStatusUpdated(update: ModuleInstallStatusUpdate) {
            if (isTerminateState(update.installState)) {
                moduleClient.unregisterListener(this)
                if (update.installState == ModuleInstallStatusUpdate.InstallState.STATE_COMPLETED) {
                    onCompleted()
                } else if (update.installState == ModuleInstallStatusUpdate.InstallState.STATE_FAILED) {
                    onError.invoke(W3WError(message = "Download failed with errorCode: ${update.errorCode}"))
                }
            }
        }

        private fun isTerminateState(@ModuleInstallStatusUpdate.InstallState state: Int): Boolean {
            return state == ModuleInstallStatusUpdate.InstallState.STATE_CANCELED || state == ModuleInstallStatusUpdate.InstallState.STATE_COMPLETED || state == ModuleInstallStatusUpdate.InstallState.STATE_FAILED
        }
    }

    val moduleInstallRequest =
        ModuleInstallRequest.newBuilder()
            .addApi(this)
            .setListener(listener)
            .build()

    moduleClient
        .installModules(moduleInstallRequest)
        .addOnFailureListener {
            onError(W3WError(message = it.message))
        }
}
