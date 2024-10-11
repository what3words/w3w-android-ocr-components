package com.what3words.ocr.components.datasource

import android.content.Context
import com.google.android.gms.common.moduleinstall.ModuleInstall
import com.google.android.gms.common.moduleinstall.ModuleInstallClient
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.what3words.androidwrapper.helpers.DefaultDispatcherProvider
import com.what3words.androidwrapper.helpers.DispatcherProvider
import com.what3words.core.datasource.image.W3WImageDataSource
import com.what3words.core.types.common.W3WError
import com.what3words.core.types.image.W3WImage
import com.what3words.ocr.components.extensions.installModule
import com.what3words.ocr.components.extensions.isModuleInstalled
import com.what3words.ocr.components.extensions.scan
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

/**
 * The implementation of [W3WImageDataSource] that uses MLKit to scan images for possible what3words addresses.
 *
 * @property context The context used for initializing ML Kit and module installation.
 * @property recognizerOptions The options for configuring the text recognizer. Defaults to Latin text recognition.
 */
class W3WMLKitImageDataSource internal constructor(
    private val context: Context,
    private val recognizerOptions: Int = TextRecognizerOptions.LATIN,
    dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider()
) : W3WImageDataSource {

    private var isStopped: Boolean = false
    private lateinit var mlkitRecognizer: TextRecognizer
    private val moduleClient: ModuleInstallClient by lazy {
        ModuleInstall.getClient(context)
    }
    private val scope: CoroutineScope = CoroutineScope(dispatcherProvider.io() + SupervisorJob())

    override fun start(onReady: () -> Unit, onError: (W3WError) -> Unit) {
        isStopped = false
        try {
            mlkitRecognizer = TextRecognition.getClient(
                when (recognizerOptions) {
                    TextRecognizerOptions.LATIN_AND_CHINESE -> {
                        ChineseTextRecognizerOptions.Builder().build()
                    }

                    TextRecognizerOptions.LATIN_AND_DEVANAGARI -> {
                        DevanagariTextRecognizerOptions.Builder().build()
                    }

                    TextRecognizerOptions.LATIN_AND_KOREAN -> {
                        KoreanTextRecognizerOptions.Builder().build()
                    }

                    TextRecognizerOptions.LATIN_AND_JAPANESE -> {
                        JapaneseTextRecognizerOptions.Builder().build()
                    }

                    else -> {
                        TextRecognizerOptions.Builder().build()
                    }
                }
            )

            mlkitRecognizer.isModuleInstalled(moduleClient) { isInstalled ->
                if (isInstalled) {
                    onReady.invoke()
                } else {
                    mlkitRecognizer.installModule(moduleClient,
                        onError = { error ->
                            onError(error)
                        }, onCompleted = {
                            onReady()
                        })
                }
            }
        } catch (e: Exception) {
            onError.invoke(
                W3WError(message = e.message)
            )
        }
    }

    override fun scan(
        image: W3WImage,
        onScanning: () -> Unit,
        onDetected: (List<String>) -> Unit,
        onError: (W3WError) -> Unit,
        onCompleted: () -> Unit
    ) {
        if (!::mlkitRecognizer.isInitialized || isStopped) {
            onError.invoke(
                W3WError(message = "Please call start() before scan()")
            )
            return
        }
        mlkitRecognizer.scan(
            image.bitmap,
            onScanning,
            onDetected,
            onError,
            onCompleted,
            scope
        )
    }

    override fun stop() {
        isStopped = true
        mlkitRecognizer.close()
    }

    companion object {
        @JvmStatic
        fun create(
            context: Context,
            recognizerOptions: Int,
            dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider()
        ): W3WMLKitImageDataSource {
            return W3WMLKitImageDataSource(context, recognizerOptions, dispatcherProvider)
        }
    }
}