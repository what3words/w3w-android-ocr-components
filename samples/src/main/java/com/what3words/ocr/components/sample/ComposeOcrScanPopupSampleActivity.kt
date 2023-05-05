package com.what3words.ocr.components.sample

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import com.what3words.design.library.ui.components.NavigationBarScaffold
import com.what3words.design.library.ui.theme.W3WTheme
import com.what3words.ocr.components.extensions.parcelable
import com.what3words.ocr.components.models.OcrScanResult
import com.what3words.ocr.components.models.W3WOcrWrapper
import com.what3words.ocr.components.models.decodeBase64
import com.what3words.ocr.components.ui.BaseOcrScanActivity
import com.what3words.ocr.components.ui.MLKitOcrScanActivity

class ComposeOcrScanPopupSampleActivity : ComponentActivity() {
    private val viewModel: ComposeOcrScanSamplePopupViewModel by viewModels()

    private val resultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                // There are no request codes
                result.data?.let {
                    val scanResult = it.parcelable<OcrScanResult>(BaseOcrScanActivity.RESULT_ID)
                    if (scanResult != null && scanResult.isSuccessful()) {
                        scanResult.scannedImage?.let { base64 ->
                            viewModel.scannedImage = base64
                        }
                        var resultText = ""
                        scanResult.suggestions.forEach { suggestion ->
                            resultText =
                                resultText.plus("${suggestion.words}, ${suggestion.nearestPlace}\n")
                        }
                        viewModel.results = resultText
                    }
                    else if (scanResult != null && !scanResult.isSuccessful()) {
                        viewModel.results = scanResult.error?.message
                    }
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            W3WTheme {
                // A surface container using the 'background' color from the theme
                NavigationBarScaffold(
                    title = "OCR SDK Sample app"
                ) {

                    MLKitLibrariesDropdownMenuBox()

                    Button(modifier = Modifier.fillMaxWidth(), onClick = {
                        val activityIntentBuilder = MLKitOcrScanActivity.Builder().withAPI(BuildConfig.W3W_API_KEY)
                        when (viewModel.ocrType) {
                            W3WOcrWrapper.OcrProvider.MLKit -> {
                                activityIntentBuilder
                                    .withMLKitLibrary(viewModel.selectedMLKitLibrary)
                            }
                            else -> {}
                        }
                        try {
                            resultLauncher.launch(activityIntentBuilder.build(this))
                        }
                        catch (e: ExceptionInInitializerError) {
                            viewModel.results = e.message
                        }
                    }) {
                        Text(text = "Launch OCR scanner")
                    }

                    if (viewModel.results != null) Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = viewModel.results!!
                    )
                    if (viewModel.scannedImage != null) Image(
                        bitmap = decodeBase64(viewModel.scannedImage!!).asImageBitmap(),
                        contentDescription = "scanned image"
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalMaterialApi::class)
    @Composable
    fun MLKitLibrariesDropdownMenuBox() {
        var expanded by remember { mutableStateOf(false) }

        Box(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = {
                    expanded = !expanded
                }
            ) {
                TextField(
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text("MLKit Language library")
                    },
                    value = viewModel.selectedMLKitLibrary.name,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    viewModel.availableMLKitLanguages.forEach { item ->
                        DropdownMenuItem(
                            onClick = {
                                viewModel.selectedMLKitLibrary = item
                                expanded = false
                            }
                        ) {
                            Text(text = item.name)
                        }
                    }
                }
            }
        }
    }
}