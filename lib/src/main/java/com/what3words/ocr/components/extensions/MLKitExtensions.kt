package com.what3words.ocr.components.extensions

import android.graphics.Bitmap
import android.util.Log
import com.google.android.gms.common.moduleinstall.InstallStatusListener
import com.google.android.gms.common.moduleinstall.ModuleInstallClient
import com.google.android.gms.common.moduleinstall.ModuleInstallRequest
import com.google.android.gms.common.moduleinstall.ModuleInstallStatusUpdate
import com.google.mlkit.vision.text.TextRecognizer
import com.what3words.core.types.common.W3WError
import com.what3words.javawrapper.What3WordsV3.findPossible3wa
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Scan [image] [Bitmap] for one or more possible what3words addresses using MLKit.
 *
 * @param image the [Bitmap] that the scanner should use to find possible what3words addresses.
 * @param onScanning the callback when it starts to scan image for text.
 * @param onDetected the callback when our [findPossible3wa] regex finds possible matches on the scanned text.
 * @param onError the callback with a [W3WError] in case an error was found while scanning.
 * @param onCompleted the callback when the scanning process is completed.
 */
fun TextRecognizer.scan(
    image: Bitmap,
    onScanning: () -> Unit,
    onDetected: (List<String>) -> Unit,
    onError: (W3WError) -> Unit,
    onCompleted: () -> Unit,
    coroutineScope: CoroutineScope,
    isBypass3waFilter: Boolean = false,
    rotation: Int = 0,
    throttleTimeout: Long = 250L
) {
    val TAG = "MLKitExtensions" // Define a TAG for logging

    require(image.width > 0 && image.height > 0) { "Invalid image dimensions" }
    require(rotation in 0..359) { "Rotation must be between 0 and 359 degrees" }

    Log.d(TAG, "Starting scan. Image dimensions: ${image.width}x${image.height}, Rotation: $rotation")
    onScanning.invoke()

    coroutineScope.launch {
        suspendCoroutine { continuation ->
            this@scan.process(image, rotation).addOnSuccessListener { visionText ->
                Log.d(TAG, "Image processing successful. Detected text: ${visionText.text}")
                if (isBypass3waFilter) {
                    val lines = visionText.text.split("\n")
                    Log.d(TAG, "Bypassing 3wa filter. Detected lines: $lines")
                    onDetected.invoke(lines)
                } else {
                    val processedText = correctSlashesInText(visionText.text)
                    Log.d(TAG, "Processed text after slash correction: $processedText")
                    val possibleAddresses = findPossible3wa(processedText)
                    Log.d(TAG, "Found possible what3words addresses: $possibleAddresses")
                    onDetected.invoke(possibleAddresses)
                }

            }.addOnFailureListener { e ->
                Log.e(TAG, "Image processing failed", e)
                onError.invoke(W3WError(message = "Image processing failed: ${e.message}"))
            }.addOnCompleteListener {
                Log.d(TAG, "Image processing task completed.")
                continuation.resume(Unit)
            }
        }

        Log.d(TAG, "Applying throttle delay: ${throttleTimeout}ms")
        delay(throttleTimeout)
    }.invokeOnCompletion { exception ->
        if (exception != null) {
            Log.e(TAG, "Coroutine scope completed with error", exception)
            onError.invoke(W3WError(message = exception.message ?: "Unknown error in coroutine scope"))
        } else {
            Log.d(TAG, "Coroutine scope completed successfully.")
        }
        onCompleted.invoke()
    }
}

/**
 * Corrects common OCR misinterpretations of the three slashes (///) that begin what3words addresses.
 *
 * @param text The text recognized by OCR
 * @return The text with common slash misinterpretations corrected
 */
private fun correctSlashesInText(text: String): String {
    // Common patterns where slashes are misrecognized
    val patterns = listOf(
        "Ill", "IlI", "lIl", "III", "ill", "lll", "I/I", "l/l", "II/", "Iil",
        "Il/", "I//", "//I", "/ll", "l//", "//l", "I/", "Il", "ll", "lI", "II",
    )

    var processedText = text

    // Replace pattern at the beginning of a word
    for (pattern in patterns) {
        processedText = processedText.replace(Regex("\\b$pattern"), "///")
    }
    
    processedText = processedText.replace(". ", ".")
    processedText = processedText.replace(" . ", ".")
    processedText = processedText.replace(" .", ".")
    processedText = processedText.replace("  .", ".")
    processedText = processedText.replace("  .  ", ".")
    processedText = processedText.replace(".  ", ".")

    return processedText
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
