package com.what3words.ocr.components

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.what3words.core.types.common.W3WError
import com.what3words.core.types.domain.W3WAddress
import com.what3words.core.types.domain.W3WCountry
import com.what3words.core.types.domain.W3WSuggestion
import com.what3words.core.types.image.W3WImage
import com.what3words.core.types.language.W3WRFC5646Language
import com.what3words.ocr.components.fake.FakeImageDataSource
import com.what3words.ocr.components.fake.FakeTextDataSource
import com.what3words.ocr.components.ui.OcrScanManager
import com.what3words.ocr.components.ui.OcrScannerState
import io.mockk.every
import io.mockk.mockk
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import junit.framework.TestCase.fail
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class OcrScanManagerTest {
    private lateinit var ocrScanManager: OcrScanManager
    private lateinit var fakeImageDataSource: FakeImageDataSource
    private lateinit var fakeTextDataSource: FakeTextDataSource

    @Before
    fun setup() {
        fakeImageDataSource = FakeImageDataSource()
        fakeTextDataSource = FakeTextDataSource()
        ocrScanManager = OcrScanManager(fakeImageDataSource, fakeTextDataSource)
    }

    @Test
    fun testGetReadySuccess() {
        var isReady = false
        var error: W3WError? = null

        fakeImageDataSource.setShouldSimulateStartError(false)

        val onReady = {
            isReady = true
        }

        val onError: ((W3WError) -> Unit) = {
            error = it
        }

        ocrScanManager.getReady(onReady, onError)

        assertTrue(isReady)
        assertNull(error)
    }

    @Test
    fun testGetReadyFailed() {
        var isReady = false
        var error: W3WError? = null

        fakeImageDataSource.setShouldSimulateStartError(true)

        val onReady = {
            isReady = true
        }

        val onError: ((W3WError) -> Unit) = {
            error = it
        }

        ocrScanManager.getReady(onReady, onError)

        assertFalse(isReady)
        assertNotNull(error)
    }

    @Test
    fun testScanImageFromCamera() = runTest {
        fakeImageDataSource.setScanResults(listOf("index.home.raft"))
        fakeTextDataSource.setSuggested3was(
            listOf(
                W3WSuggestion(
                    w3wAddress = W3WAddress(
                        "index.home.raft",
                        null,
                        null,
                        W3WRFC5646Language.EN_GB,
                        W3WCountry("en"),
                        "Bayswater, London"
                    ),
                    rank = 0,
                    distanceToFocus = null
                )
            )
        )

        val mockImage: W3WImage = mockk()
        every { mockImage.bitmap } returns mockk(relaxed = true)

        var foundSuggestions: List<W3WSuggestion>? = null
        var completed = false
        ocrScanManager.toggleLiveMode(true)
        assertEquals(OcrScannerState.ScanningType.Live, ocrScanManager.ocrScannerState.value.scanningType)
        ocrScanManager.getReady(
            onReady = {

            },
            onError = {
                fail("Error when ocrScanManager getReady")
            }
        )
        ocrScanManager.scanImage(
            mockImage,
            onError = {
                println("Error: $it")
                fail("Unexpected error")
            },
            onFound = { foundSuggestions = it },
            onCompleted = { completed = true }
        )

        // Assert the results
        assertNotNull(foundSuggestions)
        assertTrue(completed)
    }

    @Test
    fun testOcrScannerState() = runTest {
        // Initial state
        ocrScanManager.toggleLiveMode(true)
        assertEquals(OcrScannerState.ScanningType.Live, ocrScanManager.ocrScannerState.value.scanningType)
        assertEquals(OcrScannerState.State.Idle, ocrScanManager.ocrScannerState.value.state)
        assertTrue(ocrScanManager.ocrScannerState.value.foundItems.isEmpty())

        // Simulate scanning
        val mockImage: W3WImage = mockk()
        every { mockImage.bitmap } returns mockk(relaxed = true)

        fakeImageDataSource.setScanResults(listOf("index.home.raft"))
        fakeTextDataSource.setSuggested3was(
            listOf(
                W3WSuggestion(
                    w3wAddress = W3WAddress(
                        "index.home.raft",
                        null,
                        null,
                        W3WRFC5646Language.EN_GB,
                        W3WCountry("en"),
                        "Bayswater, London"
                    ),
                    rank = 0,
                    distanceToFocus = null
                )
            )
        )

        ocrScanManager.getReady(
            onReady = {

            },
            onError = {
                fail("Error when ocrScanManager getReady")
            }
        )
        ocrScanManager.scanImage(
            mockImage,
            onError = {
                fail("Unexpected error")
            },
            onFound = {

            },
            onCompleted = {

            }
        )

        // Verify the final state
        assertEquals(OcrScannerState.State.Found, ocrScanManager.ocrScannerState.value.state)
        assertEquals(1, ocrScanManager.ocrScannerState.value.foundItems.size)
        assertEquals(
            "index.home.raft",
            ocrScanManager.ocrScannerState.value.foundItems[0].w3wAddress.words
        )
    }
}