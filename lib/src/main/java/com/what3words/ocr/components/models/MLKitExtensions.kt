package com.what3words.ocr.components.models

import android.graphics.Bitmap
import android.util.Log
import com.google.android.gms.common.moduleinstall.InstallStatusListener
import com.google.android.gms.common.moduleinstall.ModuleInstallClient
import com.google.android.gms.common.moduleinstall.ModuleInstallRequest
import com.google.android.gms.common.moduleinstall.ModuleInstallStatusUpdate
import com.google.mlkit.vision.text.TextRecognizer
import com.what3words.androidwrapper.What3WordsAndroidWrapper
import com.what3words.androidwrapper.helpers.DefaultDispatcherProvider
import com.what3words.androidwrapper.helpers.DispatcherProvider
import com.what3words.javawrapper.What3WordsV3
import com.what3words.javawrapper.request.AutosuggestOptions
import com.what3words.javawrapper.response.APIResponse
import com.what3words.javawrapper.response.Suggestion
import com.what3words.ocr.components.extensions.io
import kotlinx.coroutines.withContext

fun TextRecognizer.scan(
    image: Bitmap,
    dataProvider: What3WordsAndroidWrapper,
    options: AutosuggestOptions?,
    onScanning: () -> Unit,
    onDetected: () -> Unit,
    onValidating: () -> Unit,
    onFinished: (List<Suggestion>, APIResponse.What3WordsError?) -> Unit,
    dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider(),
    tag: String = this::class.java.simpleName,
) {
    Log.d(tag, "** scanning with MLKit **")
    onScanning.invoke()
    var error: APIResponse.What3WordsError? = null
    val listFound3wa = mutableListOf<Suggestion>()
    this.process(image, 0).addOnSuccessListener { visionText ->
        io(dispatcherProvider) {
            for (possible3wa in What3WordsV3.findPossible3wa(visionText.text.lowercase())) {
                Log.d(tag, "*** found possible 3wa: $possible3wa ***")
                onDetected.invoke()
                val autosuggestReq =
                    dataProvider.autosuggest(possible3wa)
                if (options != null) autosuggestReq.options(options)
                val autosuggestRes = autosuggestReq.execute()
                if (autosuggestRes.isSuccessful) {
                    //checks if at least one suggestion words matches the possible3wa from the regex,
                    //this makes our OCR more accurate and avoids getting partial what3words address while focusing the camera.
                    autosuggestRes.suggestions.firstOrNull { it.words == possible3wa }?.let {
                        Log.d(
                            tag,
                            "*** dataProvider found a full match: ${it.words} ***"
                        )
                        listFound3wa.add(it)
                    }
                } else {
                    Log.d(
                        tag,
                        "*** dataProvider autosuggest failed with error: $error ***"
                    )
                    error = autosuggestRes.error
                }
                onValidating.invoke()
            }
            withContext(dispatcherProvider.main()) {
                onFinished.invoke(if (error == null) listFound3wa else emptyList(), error)
            }
        }
    }.addOnFailureListener { e ->
        Log.e(tag, "** scanning with MLKit failed with error: ${e.message} **")
        onFinished.invoke(emptyList(), APIResponse.What3WordsError.SDK_ERROR.apply {
            message = e.message
        })
    }
}

fun TextRecognizer.isModuleInstalled(
    moduleClient: ModuleInstallClient,
    tag: String = this::class.java.simpleName,
    result: (Boolean) -> Unit
) {
    Log.d(tag, "* moduleInstalled check *")
    moduleClient
        .areModulesAvailable(this)
        .addOnSuccessListener { response ->
            Log.d(
                tag,
                "** moduleInstalled check result: ${response.areModulesAvailable()} **"
            )
            result.invoke(response.areModulesAvailable())
        }
        .addOnFailureListener {
            Log.d(
                tag,
                "** moduleInstalled check failed with exception: ${it.message} **"
            )
            result.invoke(false)
        }
}

fun TextRecognizer.installModule(
    moduleClient: ModuleInstallClient,
    tag: String = this::class.java.simpleName,
    onDownloaded: (Boolean, APIResponse.What3WordsError?) -> Unit
) {
    val listener = object : InstallStatusListener {
        override fun onInstallStatusUpdated(update: ModuleInstallStatusUpdate) {
            update.progressInfo?.let {
                val progress = (it.bytesDownloaded * 100 / it.totalBytesToDownload).toInt()
                // Set the progress for the progress bar.
                Log.d(tag, "*** Download progress: $progress ***")
            }

            if (isTerminateState(update.installState)) {
                moduleClient.unregisterListener(this)
                if (update.installState == ModuleInstallStatusUpdate.InstallState.STATE_COMPLETED) {
                    Log.d(tag, "*** Download completed ***")
                    onDownloaded.invoke(true, null)
                } else if (update.installState == ModuleInstallStatusUpdate.InstallState.STATE_FAILED) {
                    Log.d(
                        tag,
                        "*** Download failed with errorCode: ${update.errorCode} ***"
                    )
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
        .addOnSuccessListener {
            Log.d(
                tag,
                "** installModule requested success, but not downloaded yet. **"
            )
        }
        .addOnFailureListener {
            Log.d(
                tag,
                "** installModule requested failed with exception: ${it.message} **"
            )
            onDownloaded.invoke(false, APIResponse.What3WordsError.SDK_ERROR.apply {
                message = it.message
            })
        }
}
