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
     * Change current scanning language of the wrapper using a ISO 639-1 two letter code language code.
     *
     * @param languageCode the ISO 639-1 two letter code language code to change the wrapper too (it will ignore scanned three word address in other languages)
     *
     * @throws [UnsupportedOperationException] if [languageCode] is not supported by this wrapper implementation or this wrapper is language agnostic, i.e [W3WOcrMLKitWrapper].
     */
    @Throws(java.lang.UnsupportedOperationException::class)
    fun changeLanguage(languageCode: String)

    /**
     * Get current ISO 639-1 two letter code language code.
     *
     * @return ISO 639-1 two letter code language code.
     * @throws [UnsupportedOperationException] if this wrapper is language agnostic, i.e [W3WOcrMLKitWrapper].
     */
    @Throws(java.lang.UnsupportedOperationException::class)
    fun currentLanguage() : String


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
    fun supportsLanguage(languageCode: String) : Boolean


    /**
     * Checks if all modules that this wrapper depend on are installed, will return true if no modules need to be downloaded to this specific implementation.
     *
     * @return a callback with a [Boolean] true if all modules are installed or no modules needed to install, false if not installed and wrapper needs to download modules.
     */
    fun moduleInstalled(result: ((Boolean) -> Unit))


    /**
     * Request to download modules that this wrapper depend on.
     *
     * @return a callback with a [Boolean] or [What3WordsError], if successful true, if some issues were found while trying to download dependency will be false,
     * if false the [What3WordsError] should contain the information related to the download error.
     */
    fun installModule(
        onDownloaded: ((Boolean, What3WordsError?) -> Unit)
    )

    /**
     * Scan [image] [Bitmap] for one or more three word addresses.
     *
     * @param image the [Bitmap] that the scanner should use to find possible three word addresses.
     * @param options the [AutosuggestOptions] to be applied when validating a possible three word address,
     * i.e: country clipping, check [AutosuggestOptions] for possible filters/clippings.
     * @param onScanning the callback when it starts to scan image for text.
     * @param onDetected the callback when our [findPossible3wa] regex finds possible matches on the scanned text.
     * @param onValidating the callback when we start validating the results of [findPossible3wa] against our API/SDK to check if valid (it will take into account [options] if provided).
     * @param onFinished the callback with the [OcrScanResult] that can contain a list of [Suggestion] or a [What3WordsError]
     * @throws [UnsupportedOperationException] if [languageCode] is not supported by this wrapper implementation or this wrapper is language agnostic, i.e [W3WOcrMLKitWrapper].
     */
    fun scan(
        image: Bitmap,
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
     * Get the [What3WordsAndroidWrapper] that this wrapper will use to validate a three word address could be [What3WordsV3] or [What3WordsSdk].
     *
     * @return [ExecutorService] this wrapper runs on.
     */
    fun dataProvider(): What3WordsAndroidWrapper

    /**
     * This method should be called when all the work from this wrapper is finished i.e: Activity.onDestroy
     **/
    fun stop()

    enum class OcrProvider {
        MLKit,
        Hybrid,
        Tesseract
    }

    enum class MLKitLibraries {
        Latin,
        LatinAndDevanagari,
        LatinAndKorean,
        LatinAndJapanese,
        LatinAndChinese
    }

    enum class DataProvider {
        API,
        SDK
    }
}

