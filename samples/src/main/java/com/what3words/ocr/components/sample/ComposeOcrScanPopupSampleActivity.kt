package com.what3words.ocr.components.sample

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Button
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ExposedDropdownMenuBox
import androidx.compose.material.ExposedDropdownMenuDefaults
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.zIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.what3words.androidwrapper.What3WordsV3
import com.what3words.design.library.ui.components.NavigationBarScaffold
import com.what3words.design.library.ui.theme.W3WTheme
import com.what3words.javawrapper.request.AutosuggestOptions
import com.what3words.javawrapper.request.Coordinates
import com.what3words.javawrapper.response.Suggestion
import com.what3words.javawrapper.response.SuggestionWithCoordinates
import com.what3words.ocr.components.extensions.decodeBase64
import com.what3words.ocr.components.extensions.serializable
import com.what3words.ocr.components.models.W3WOcrMLKitWrapper
import com.what3words.ocr.components.models.W3WOcrWrapper
import com.what3words.ocr.components.ui.BaseOcrScanActivity
import com.what3words.ocr.components.ui.MLKitOcrScanActivity
import com.what3words.ocr.components.ui.W3WOcrScanner

class ComposeOcrScanPopupSampleActivity : ComponentActivity() {
    private val viewModel: ComposeOcrScanSamplePopupViewModel by viewModels()
    private lateinit var ocrWrapper: W3WOcrWrapper

    private val resultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                // There are no request codes
                result.data?.let {
                    val suggestion =
                        it.serializable<SuggestionWithCoordinates>(BaseOcrScanActivity.SUGGESTION_RESULT_ID)
                    if (suggestion != null) viewModel.results =
                        ("${suggestion.words}, ${suggestion.nearestPlace}, ${suggestion.country}\n${suggestion.coordinates?.lat}, ${suggestion.coordinates?.lng}")
                }
            }
        }

    override fun onDestroy() {
        super.onDestroy()
        ocrWrapper.stop()
    }

    @SuppressLint("UnsafeOptInUsageError")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            W3WTheme {
                // A surface container using the 'background' color from the theme
                NavigationBarScaffold(
                    title = "OCR SDK Sample app"
                ) {
                    var scanScreenVisible by remember { mutableStateOf(false) }
                    val options = remember {
                        AutosuggestOptions().apply {
                            focus = Coordinates(51.2, 1.2)
                        }
                    }

                    LaunchedEffect(key1 = viewModel.selectedMLKitLibrary, block = {
                        ocrWrapper = getOcrWrapper()
                    })

                    AnimatedVisibility(
                        visible = scanScreenVisible,
                        modifier = Modifier.zIndex(Float.MAX_VALUE),
                        enter = expandVertically(
                            animationSpec = tween(
                                750
                            ),
                        ),
                        exit = shrinkVertically(
                            animationSpec = tween(
                                750
                            )
                        )
                    ) {
                        W3WOcrScanner(
                            ocrWrapper,
                            options = options,
                            returnCoordinates = true,
                            onError = {
                                scanScreenVisible = false
                                viewModel.results =
                                    ("${it.key}, ${it.message}")
                            },
                            onDismiss = {
                                scanScreenVisible = false
                            },
                            onSuggestionSelected = {
                                viewModel.results =
                                    ("${it.words}, ${it.nearestPlace}, ${it.country}\n${it.coordinates?.lat}, ${it.coordinates?.lng}")
                                scanScreenVisible = false
                            })
                    }
                    MLKitLibrariesDropdownMenuBox()
                    Button(modifier = Modifier.fillMaxWidth(), onClick = {
                        scanScreenVisible = true
                    }) {
                        Text(text = "Launch OCR scanner in screen")
                    }

                    Button(modifier = Modifier.fillMaxWidth(), onClick = {
                        val intent = MLKitOcrScanActivity.newInstanceWithApi(
                            this,
                            viewModel.selectedMLKitLibrary,
                            BuildConfig.W3W_API_KEY,
                            options,
                            returnCoordinates = true
                        )
                        try {
                            resultLauncher.launch(intent)
                        } catch (e: ExceptionInInitializerError) {
                            viewModel.results = e.message
                        }
                    }) {
                        Text(text = "Launch OCR scanner as a pop up")
                    }

                    if (viewModel.results != null) Text(
                        modifier = Modifier.fillMaxWidth(), text = viewModel.results!!
                    )
                }
            }
        }
    }

    private fun getOcrWrapper(): W3WOcrWrapper {
        val textRecognizer = TextRecognition.getClient(
            when (viewModel.selectedMLKitLibrary) {
                W3WOcrWrapper.MLKitLibraries.Latin -> TextRecognizerOptions.DEFAULT_OPTIONS
                W3WOcrWrapper.MLKitLibraries.LatinAndDevanagari -> DevanagariTextRecognizerOptions.Builder()
                    .build()

                W3WOcrWrapper.MLKitLibraries.LatinAndKorean -> KoreanTextRecognizerOptions.Builder()
                    .build()

                W3WOcrWrapper.MLKitLibraries.LatinAndJapanese -> JapaneseTextRecognizerOptions.Builder()
                    .build()

                W3WOcrWrapper.MLKitLibraries.LatinAndChinese -> ChineseTextRecognizerOptions.Builder()
                    .build()
            }
        )
        return W3WOcrMLKitWrapper(
            What3WordsV3(
                BuildConfig.W3W_API_KEY,
                this@ComposeOcrScanPopupSampleActivity
            ),
            textRecognizer
        )
    }

    @OptIn(ExperimentalMaterialApi::class)
    @Composable
    fun MLKitLibrariesDropdownMenuBox() {
        var expanded by remember { mutableStateOf(false) }

        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = {
                expanded = !expanded
            }) {
                TextField(
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text("MLKit Language library")
                    },
                    value = viewModel.selectedMLKitLibrary.name,
                    onValueChange = {
                    },
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                )

                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    viewModel.availableMLKitLanguages.forEach { item ->
                        DropdownMenuItem(onClick = {
                            viewModel.selectedMLKitLibrary = item
                            expanded = false
                        }) {
                            Text(text = item.name)
                        }
                    }
                }
            }
        }
    }
}