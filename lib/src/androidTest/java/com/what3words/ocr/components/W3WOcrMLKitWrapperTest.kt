package com.what3words.ocr.components

import android.graphics.BitmapFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.mlkit.vision.text.TextRecognizerOptionsInterface.LATIN
import com.google.mlkit.vision.text.TextRecognizerOptionsInterface.LATIN_AND_DEVANAGARI
import com.google.mlkit.vision.text.TextRecognizerOptionsInterface.LATIN_AND_JAPANESE
import com.google.mlkit.vision.text.TextRecognizerOptionsInterface.LATIN_AND_KOREAN
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.what3words.androidwrapper.What3WordsAndroidWrapper
import com.what3words.ocr.components.models.W3WOcrMLKitWrapper
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class W3WOcrMLKitWrapperTest {

    private lateinit var mlKitWrapper: W3WOcrMLKitWrapper
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private val what3WordsAndroidWrapper: What3WordsAndroidWrapper = mockk()

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testValid3waSimpleLatinOcrScan() = runTest {
        //given
        every {
            what3WordsAndroidWrapper.autosuggest("filled.count.soap").execute()
        }.answers {
            TestData.filledCountSoapAutosuggestResponse
        }

        val bitmapToScan = BitmapFactory.decodeResource(
            context.resources,
            com.what3words.ocr.components.test.R.drawable.simple_filled_count_soap
        )

        mlKitWrapper = W3WOcrMLKitWrapper(context, LATIN)

        //when & wait
        mlKitWrapper.start { _, _ -> }
        val scanResult = suspendCoroutine { cont ->
            mlKitWrapper.scan(
                bitmapToScan,
                what3WordsAndroidWrapper,
                null,
                {},
                {},
                {}) { suggestions, error ->
                cont.resume(Pair(suggestions, error))
            }
        }
        mlKitWrapper.stop()

        //then
        verify(exactly = 1) { what3WordsAndroidWrapper.autosuggest("filled.count.soap") }
        assertTrue(scanResult.second == null)
        assertEquals(1, scanResult.first.size)
        assertEquals("filled.count.soap", scanResult.first[0].words)
        assertEquals("en", scanResult.first[0].language)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testValid3waSimpleLatinUppercaseOcrScan() = runTest {
        //given
        every {
            what3WordsAndroidWrapper.autosuggest("filled.count.soap").execute()
        }.answers {
            TestData.filledCountSoapAutosuggestResponse
        }

        val bitmapToScan = BitmapFactory.decodeResource(
            context.resources,
            com.what3words.ocr.components.test.R.drawable.simple_valid_english_uppercase_3wa
        )

        mlKitWrapper = W3WOcrMLKitWrapper(context, LATIN)

        //when & wait
        mlKitWrapper.start { _, _ -> }
        val scanResult = suspendCoroutine { cont ->
            mlKitWrapper.scan(
                bitmapToScan,
                what3WordsAndroidWrapper,
                null,
                {},
                {},
                {}) { suggestions, error ->
                cont.resume(Pair(suggestions, error))
            }
        }
        mlKitWrapper.stop()

        //then
        verify(exactly = 1) { what3WordsAndroidWrapper.autosuggest("filled.count.soap") }
        assertTrue(scanResult.second == null)
        assertEquals(1, scanResult.first.size)
        assertEquals("filled.count.soap", scanResult.first[0].words)
        assertEquals("en", scanResult.first[0].language)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testValid3waHindiSimpleOcrScan() = runTest {
        //given
        every {
            what3WordsAndroidWrapper.autosuggest("डोलने.पीसना.संभाला").execute()
        }.answers {
            TestData.hindiAutosuggestResponse
        }

        val bitmapToScan = BitmapFactory.decodeResource(
            context.resources,
            com.what3words.ocr.components.test.R.drawable.simple_valid_hindi_3wa
        )

        mlKitWrapper = W3WOcrMLKitWrapper(context, LATIN_AND_DEVANAGARI)

        //when & wait
        mlKitWrapper.start { _, _ -> }
        val scanResult = suspendCoroutine { cont ->
            mlKitWrapper.scan(
                bitmapToScan,
                what3WordsAndroidWrapper,
                null,
                {},
                {},
                {}) { suggestions, error ->
                cont.resume(Pair(suggestions, error))
            }
        }
        mlKitWrapper.stop()

        //then
        verify(exactly = 1) {
            what3WordsAndroidWrapper.autosuggest("डोलने.पीसना.संभाला")
        }
        assertTrue(scanResult.second == null)
        assertEquals(1, scanResult.first.size)
        assertEquals("डोलने.पीसना.संभाला", scanResult.first[0].words)
        assertEquals("hi", scanResult.first[0].language)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testValid3waJapaneseSimpleOcrScan() = runTest {
        //given
        every {
            what3WordsAndroidWrapper.autosuggest("こくさい。ていか。かざす").execute()
        }.answers {
            TestData.japaneseAutosuggestResponse
        }

        val bitmapToScan = BitmapFactory.decodeResource(
            context.resources,
            com.what3words.ocr.components.test.R.drawable.simple_valid_japanese_3wa
        )

        mlKitWrapper = W3WOcrMLKitWrapper(context, LATIN_AND_JAPANESE)

        //when & wait
        mlKitWrapper.start { _, _ -> }
        val scanResult = suspendCoroutine { cont ->
            mlKitWrapper.scan(
                bitmapToScan,
                what3WordsAndroidWrapper,
                null,
                {},
                {},
                {}) { suggestions, error ->
                cont.resume(Pair(suggestions, error))
            }
        }
        mlKitWrapper.stop()

        //then
        verify(exactly = 1) {
            what3WordsAndroidWrapper.autosuggest("こくさい。ていか。かざす")
        }
        assertTrue(scanResult.second == null)
        assertEquals(1, scanResult.first.size)
        assertEquals("こくさい。ていか。かざす", scanResult.first[0].words)
        assertEquals("ja", scanResult.first[0].language)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    // @Test
    fun testValid3waKoreanSimpleOcrScan() = runTest {
        //given
        every {
            what3WordsAndroidWrapper.autosuggest("쓸모.평소.나다").execute()
        }.answers {
            TestData.koreanAutosuggestResponse
        }

        val bitmapToScan = BitmapFactory.decodeResource(
            context.resources,
            com.what3words.ocr.components.test.R.drawable.simple_valid_korean_3wa
        )

        mlKitWrapper = W3WOcrMLKitWrapper(context, LATIN_AND_KOREAN)

        //when & wait
        mlKitWrapper.start { _, _ -> }
        val scanResult = suspendCoroutine { cont ->
            mlKitWrapper.scan(
                bitmapToScan,
                what3WordsAndroidWrapper,
                null,
                {},
                {},
                {}) { suggestions, error ->
                cont.resume(Pair(suggestions, error))
            }
        }
        mlKitWrapper.stop()

        //then
        verify(exactly = 1) {
            what3WordsAndroidWrapper.autosuggest("쓸모.평소.나다")
        }
        assertTrue(scanResult.second == null)
        assertEquals(3, scanResult.first.size)
        assertEquals("쓸모.평소.나다", scanResult.first[0].words)
        assertEquals("ko", scanResult.first[0].language)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    // @Test
    fun testValid3waChineseSimpleOcrScan() = runTest {
        //given
        every {
            what3WordsAndroidWrapper.autosuggest("产权.绝缘.墨镜").execute()
        }.answers {
            TestData.chineseAutosuggestResponse
        }

        val bitmapToScan = BitmapFactory.decodeResource(
            context.resources,
            com.what3words.ocr.components.test.R.drawable.simple_valid_chinese_3wa
        )

        mlKitWrapper = W3WOcrMLKitWrapper(context, TextRecognizerOptions.LATIN_AND_CHINESE)

        //when & wait
        mlKitWrapper.start { _, _ -> }
        val scanResult = suspendCoroutine { cont ->
            mlKitWrapper.scan(
                bitmapToScan,
                what3WordsAndroidWrapper,
                null,
                {},
                {},
                {}) { suggestions, error ->
                cont.resume(Pair(suggestions, error))
            }
        }
        mlKitWrapper.stop()

        //then
        verify(exactly = 1) {
            what3WordsAndroidWrapper.autosuggest("产权.绝缘.墨镜")
        }
        assertTrue(scanResult.second == null)
        assertEquals(3, scanResult.first.size)
        assertEquals("产权.绝缘.墨镜", scanResult.first[0].words)
        assertEquals("zh", scanResult.first[0].language)
    }
}