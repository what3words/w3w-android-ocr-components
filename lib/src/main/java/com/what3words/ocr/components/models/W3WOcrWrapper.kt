package com.what3words.ocr.components.models

import android.graphics.Bitmap
import com.what3words.androidwrapper.What3WordsAndroidWrapper
import com.what3words.api.sdk.bridge.models.What3WordsSdk
import com.what3words.javawrapper.What3WordsV3
import com.what3words.javawrapper.What3WordsV3.findPossible3wa
import com.what3words.javawrapper.request.AutosuggestOptions
import com.what3words.javawrapper.response.APIResponse.What3WordsError
import com.what3words.javawrapper.response.Suggestion
import java.util.concurrent.ExecutorService

interface W3WOcrWrapper {

    /**
     * Check if ISO 639-1 two letter code language code is supported by this wrapper implementation.
     *
     * @param languageCode the ISO 639-1 two letter code language to check.
     *
     * @return [Boolean] true if supported, false if not.
     *
     * @throws [UnsupportedOperationException] if this wrapper is language agnostic, i.e [W3WOcrMLKitWrapper].
     */
    @Throws(java.lang.UnsupportedOperationException::class)
    fun supportsLanguage(languageCode: String): Boolean

    /**
     * Scan [image] [Bitmap] for one or more three word addresses.
     *
     * @param image the [Bitmap] that the scanner should use to find possible three word addresses.
     * @param dataProvider the [What3WordsAndroidWrapper] that this wrapper will use to validate a three word address could be [What3WordsV3] or [What3WordsSdk].
     * @param options the [AutosuggestOptions] to be applied when validating a possible three word address,
     * i.e: country clipping, check [AutosuggestOptions] for possible filters/clippings.
     * @param onScanning the callback when it starts to scan image for text.
     * @param onDetected the callback when our [findPossible3wa] regex finds possible matches on the scanned text.
     * @param onValidating the callback when we start validating the results of [findPossible3wa] against our API/SDK to check if valid (it will take into account [options] if provided).
     * @param onFinished the callback with a [List] of [Suggestion] or a [What3WordsError] in case an error was found while scanning.
     */
    fun scan(
        image: Bitmap,
        dataProvider: What3WordsAndroidWrapper,
        options: AutosuggestOptions? = null,
        onScanning: (() -> Unit),
        onDetected: (() -> Unit),
        onValidating: (() -> Unit),
        onFinished: ((List<Suggestion>, What3WordsError?) -> Unit)
    )

    /**
     * Get the [ExecutorService] that this wrapper will be running on.
     *
     * @return [ExecutorService] this wrapper runs on.
     */
    fun executor(): ExecutorService

    /**
     * Starts the wrapper to start receiving images to [scan], this will check if modules are installed and if not download the needed
     * modules to be able to perform a [scan].
     *
     * @param readyListener will return true/false if started successfully and if it fails it will return a [What3WordsError] with an error message.
     **/
    fun start(
        readyListener: (Boolean, What3WordsError?) -> Unit
    )

    /**
     * Set ISO 639-1 two letter code language code to be used by the wrapper before [start], this is only supported by
     * wrappers that support ISO 639-1 [languageCode]
     *
     * @param languageCode the ISO 639-1 two letter code language to check.
     * @param secondaryLanguageCode the ISO 639-1 two letter code language to check.
     *
     * @return [Boolean] true if supported, false if not.
     *
     * @throws [UnsupportedOperationException] if this wrapper is language agnostic, i.e [W3WOcrMLKitWrapper].
     */
    fun setLanguage(
        languageCode: String,
        secondaryLanguageCode: String? = null
    )

    /**
     * This method should be called when all the work from this wrapper is finished i.e: Activity.onDestroy
     **/
    fun stop()

    enum class OcrProvider {
        MLKit,
        Hybrid,
        Tesseract
    }

    enum class DataProvider {
        API,
        SDK
    }
}

