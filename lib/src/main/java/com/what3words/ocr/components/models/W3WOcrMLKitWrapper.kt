package com.what3words.ocr.components.models

import android.content.Context
import android.graphics.Bitmap
import com.google.android.gms.common.moduleinstall.ModuleInstall
import com.google.android.gms.common.moduleinstall.ModuleInstallClient
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.what3words.androidwrapper.What3WordsAndroidWrapper
import com.what3words.androidwrapper.helpers.DefaultDispatcherProvider
import com.what3words.androidwrapper.helpers.DispatcherProvider
import com.what3words.javawrapper.request.AutosuggestOptions
import com.what3words.javawrapper.response.APIResponse.What3WordsError
import com.what3words.javawrapper.response.Suggestion
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Creates a new [W3WOcrWrapper] to work with MLKit V2 [TextRecognizer].
 *
 * @sample com.what3words.ocr.components.sample.ComposeOcrScanPopupSampleActivity.getOcrWrapper
 *
 * @param recognizerOptions the MLKit V2 Library to be used to scan text from a [Bitmap], by default will be [TextRecognizerOptions.LATIN], other possibilities are:
 * [TextRecognizerOptions.LATIN_AND_CHINESE], [TextRecognizerOptions.LATIN_AND_DEVANAGARI], [TextRecognizerOptions.LATIN_AND_JAPANESE] and [TextRecognizerOptions.LATIN_AND_KOREAN]
 * @param dispatcherProvider [DispatcherProvider] to handle Coroutines threading, by default uses [DefaultDispatcherProvider]
 */
class W3WOcrMLKitWrapper(
    private val context: Context,
    private val recognizerOptions: Int = TextRecognizerOptions.LATIN,
    private val dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider()
) : W3WOcrWrapper {

    companion object {
        const val TAG = "W3WOcrMLKitWrapper"
    }

    private var isStopped: Boolean = false
    private val imageAnalyzerExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private lateinit var mlkitRecognizer: TextRecognizer

    /**
     * Starts the wrapper to start receiving images to [scan], this will check if modules are installed and if not download the needed
     * modules to be able to perform a [scan].
     *
     * @param readyListener will return true/false if started successfully and if it fails it will return a [What3WordsError] with an error message.
     **/
    override fun start(
        readyListener: (Boolean, What3WordsError?) -> Unit
    ) {
        isStopped = false
        mlkitRecognizer = TextRecognition.getClient(
            when (recognizerOptions) {
                TextRecognizerOptions.LATIN_AND_CHINESE -> {
                    ChineseTextRecognizerOptions.Builder().setExecutor(executor()).build()
                }

                TextRecognizerOptions.LATIN_AND_DEVANAGARI -> {
                    DevanagariTextRecognizerOptions.Builder().setExecutor(executor()).build()
                }

                TextRecognizerOptions.LATIN_AND_KOREAN -> {
                    KoreanTextRecognizerOptions.Builder().setExecutor(executor()).build()
                }

                TextRecognizerOptions.LATIN_AND_JAPANESE -> {
                    JapaneseTextRecognizerOptions.Builder().setExecutor(executor()).build()
                }

                else -> {
                    TextRecognizerOptions.Builder().setExecutor(executor()).build()
                }
            }
        )
        mlkitRecognizer.isModuleInstalled(moduleClient, TAG) { isInstalled ->
            if (isInstalled) {
                readyListener.invoke(true, null)
            } else {
                mlkitRecognizer.installModule(moduleClient, TAG) { isDownloaded, error ->
                    if (isDownloaded) {
                        readyListener.invoke(true, null)
                    } else {
                        readyListener.invoke(false, error)
                    }
                }
            }
        }
    }

    override fun setLanguage(languageCode: String, secondaryLanguageCode: String?) {
        throw java.lang.UnsupportedOperationException(
            "MLKit doesn't support language selection, will try to scan all available languages listed here:" +
                    "https://developers.google.com/ml-kit/vision/text-recognition/languages"
        )
    }

    override fun supportsLanguage(languageCode: String): Boolean {
        throw java.lang.UnsupportedOperationException(
            "MLKit doesn't support language selection, will try to scan all available languages listed here:" +
                    "https://developers.google.com/ml-kit/vision/text-recognition/languages"
        )
    }

    private val moduleClient: ModuleInstallClient by lazy {
        ModuleInstall.getClient(context)
    }

    override fun scan(
        image: Bitmap,
        dataProvider: What3WordsAndroidWrapper,
        options: AutosuggestOptions?,
        onScanning: () -> Unit,
        onDetected: () -> Unit,
        onValidating: () -> Unit,
        onFinished: (List<Suggestion>, What3WordsError?) -> Unit
    ) {
        if (!::mlkitRecognizer.isInitialized || isStopped) {
            onFinished.invoke(emptyList(), What3WordsError.SDK_ERROR.apply {
                message = "Please call start() before scan()"
            })
            return
        }
        imageAnalyzerExecutor.submit {
            mlkitRecognizer.scan(
                image,
                dataProvider,
                options,
                onScanning,
                onDetected,
                onValidating,
                onFinished,
                dispatcherProvider,
                TAG
            )
        }
    }

    override fun executor(): ExecutorService {
        return imageAnalyzerExecutor
    }

    /**
     * This method should be called when all the work from this wrapper is finished i.e: Activity.onDestroy
     **/
    override fun stop() {
        imageAnalyzerExecutor.submit {
            isStopped = true
            mlkitRecognizer.close()
        }
    }
}