package com.what3words.ocr.components

import android.content.Context
import android.graphics.BitmapFactory
import androidx.test.platform.app.InstrumentationRegistry
import com.google.mlkit.vision.text.TextRecognizerOptionsInterface.LATIN
import com.google.mlkit.vision.text.TextRecognizerOptionsInterface.LATIN_AND_CHINESE
import com.google.mlkit.vision.text.TextRecognizerOptionsInterface.LATIN_AND_DEVANAGARI
import com.google.mlkit.vision.text.TextRecognizerOptionsInterface.LATIN_AND_JAPANESE
import com.google.mlkit.vision.text.TextRecognizerOptionsInterface.LATIN_AND_KOREAN
import com.what3words.core.types.common.W3WError
import com.what3words.core.types.image.W3WImage
import com.what3words.ocr.components.datasource.W3WMLKitImageDataSource
import com.what3words.ocr.components.test.R
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(Parameterized::class)
class W3WMLKitImageDataSourceTest(
    private val scriptType: Int,
    private val imageResource: Int,
    private val expectedText: String
) {
    private lateinit var imageDataSource: W3WMLKitImageDataSource
    private lateinit var context: Context

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
    }

    @Test
    fun testStartAndStop() = runBlocking {
        imageDataSource = W3WMLKitImageDataSource.create(context, scriptType)
        imageDataSource.isBypassed3waFilter = true
        var isReady = false
        var error: W3WError? = null
        imageDataSource.start(
            onReady = { isReady = true },
            onError = { error = it }
        )

        delay(2000)

        assertTrue("DataSource should be ready", isReady)
        assertNull("There should be no error", error)

        imageDataSource.stop()
    }

    @Test
    fun testScanWithoutStart() {
        imageDataSource = W3WMLKitImageDataSource.create(context, scriptType)
        val mockImage = loadTestImage(R.drawable.empty_3wa_scan)
        var errorCalled = false

        imageDataSource.scan(
            image = mockImage,
            onScanning = {},
            onDetected = {},
            onError = { errorCalled = true },
            onCompleted = {}
        )

        assertTrue("OnError should be called when scanning without starting", errorCalled)
    }

    @Test
    fun testScanImage() = runBlocking {
        imageDataSource = W3WMLKitImageDataSource.create(context, scriptType)
        val (detectedText, error) = performScan(imageResource)

        assertNotNull("Detected text should not be null", detectedText)
        assertNull("There should be no error", error)
        var isFound = false
        detectedText?.forEach {
            if (areStringsAtLeast80PercentEqual(it, expectedText)) {
                println("text: $it - expected: $expectedText are 80% same")
                isFound = true
            } else {
                println("text: $it - expected: $expectedText not 80% same")
            }
        }
        assert(isFound)
    }

    private fun performScan(imageResource: Int): Pair<List<String>?, W3WError?> {
        val latch = CountDownLatch(1)
        var detectedText: List<String>? = null
        var error: W3WError? = null

        imageDataSource.start(
            onReady = {},
            onError = { error = it }
        )

        latch.await(5, TimeUnit.SECONDS)

        val testImage = loadTestImage(imageResource)

        imageDataSource.scan(
            image = testImage,
            onScanning = {},
            onDetected = {
                detectedText = it
            },
            onError = {
                error = it
            },
            onCompleted = {
                latch.countDown()
            }
        )

        assertTrue("Scan should complete", latch.await(10, TimeUnit.SECONDS))
        imageDataSource.stop()

        return Pair(detectedText, error)
    }

    private fun areStringsAtLeast80PercentEqual(str1: String, str2: String): Boolean {
        val longerLength = maxOf(str1.length, str2.length)
        val levenshteinDistance = levenshteinDistance(str1, str2)
        val similarityPercentage = (longerLength - levenshteinDistance).toDouble() / longerLength
        return similarityPercentage >= 0.8
    }

    private fun levenshteinDistance(str1: String, str2: String): Int {
        val dp = Array(str1.length + 1) { IntArray(str2.length + 1) }

        for (i in 0..str1.length) {
            for (j in 0..str2.length) {
                when {
                    i == 0 -> dp[i][j] = j
                    j == 0 -> dp[i][j] = i
                    else -> dp[i][j] = minOf(
                        dp[i - 1][j] + 1,
                        dp[i][j - 1] + 1,
                        dp[i - 1][j - 1] + if (str1[i - 1] == str2[j - 1]) 0 else 1
                    )
                }
            }
        }

        return dp[str1.length][str2.length]
    }

    private fun loadTestImage(resourceId: Int): W3WImage {
        val bitmap = BitmapFactory.decodeResource(context.resources, resourceId)
        assertNotNull("Test image should be loaded", bitmap)
        return W3WImage(bitmap)
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun testParameters() = listOf(
            arrayOf(LATIN_AND_CHINESE, R.drawable.simple_valid_chinese_3wa, "美白.色系.自尊"),
            arrayOf(LATIN, R.drawable.simple_filled_count_soap, "filled.count.soap"),
            arrayOf(LATIN, R.drawable.simple_valid_english_uppercase_3wa, "FILLED.COUNT.SOAP"),
            arrayOf(LATIN_AND_DEVANAGARI, R.drawable.simple_valid_hindi_3wa, "डोलने.पीसना.संभाला"),
            arrayOf(
                LATIN_AND_JAPANESE,
                R.drawable.simple_valid_japanese_3wa,
                "こくさい。ていか。かざす"
            ),
            arrayOf(LATIN_AND_KOREAN, R.drawable.simple_valid_korean_3wa, "국기.깔끔.되었다"),
            arrayOf(LATIN, R.drawable.complex_index_home_raft, "index.home.raft"),
            arrayOf(LATIN, R.drawable.simple_portuguese_2_3wa, "refrigerando.valem.touro")
        )
    }
}