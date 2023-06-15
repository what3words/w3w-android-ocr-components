package com.what3words.ocr.components.ui

import android.Manifest
import android.content.res.Configuration
import android.view.ViewGroup
import androidx.camera.view.PreviewView
import androidx.compose.animation.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.BottomSheetScaffold
import androidx.compose.material.BottomSheetValue
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.rememberBottomSheetScaffoldState
import androidx.compose.material.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import com.what3words.design.library.ui.components.IconButtonSize
import com.what3words.design.library.ui.components.OutlinedIconButton
import com.what3words.design.library.ui.components.SuggestionWhat3words
import com.what3words.design.library.ui.models.DisplayUnits
import com.what3words.design.library.ui.theme.W3WTheme
import com.what3words.javawrapper.request.AutosuggestOptions
import com.what3words.javawrapper.response.APIResponse.What3WordsError
import com.what3words.javawrapper.response.Coordinates
import com.what3words.javawrapper.response.Suggestion
import com.what3words.javawrapper.response.SuggestionWithCoordinates
import com.what3words.ocr.components.R
import com.what3words.ocr.components.models.ScanResultState
import com.what3words.ocr.components.models.W3WOcrMLKitWrapper
import com.what3words.ocr.components.models.W3WOcrWrapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Creates a new [W3WOcrScanner] Composable to use CameraX and a [W3WOcrWrapper] to scan what3words address using text recognition.
 *
 * @sample com.what3words.ocr.components.sample.ComposeOcrScanPopupSampleActivity.onCreate
 *
 * @param wrapper the [W3WOcrWrapper] to be used when scanning for text, i.e: [W3WOcrMLKitWrapper].
 * @param modifier an optional [Modifier] for the root [BottomSheetScaffold].
 * @param options [AutosuggestOptions] to be applied when using what3words API. (Optional)
 * @param returnCoordinates when a [SuggestionWithCoordinates] is picked if it should return [Coordinates] or not. Default false, if true, it might result in API cost charges.
 * @param displayUnits the [DisplayUnits] that will show on the [SuggestionPicker], by default will be [DisplayUnits.SYSTEM] which will use the system Locale to determinate if Imperial or Metric system.
 * @param scanStateScanningTitle the text to be displayed when it starts scanning, default: [R.string.scan_state_scanning]
 * @param scanStateDetectedTitle the text to be displayed when it detects a possible three word address, default: [R.string.scan_state_detecting]
 * @param scanStateValidatingTitle the text to be displayed when it validates a possible three word address (API/SDK check for validation), default: [R.string.scan_state_validating]
 * @param scanStateFoundTitle the title to be displayed as the header of the list of scanned and validated three word addresses, default: [R.string.scan_state_found]
 * @param onSuggestionSelected the callback when a [SuggestionWithCoordinates] is selected from the [SuggestionPicker].
 * @param onError the callback when an error occurs in this composable, expect a [What3WordsError].
 * @param onDismiss when this composable is closed using the close button, meaning no [onError] or [onSuggestionSelected], it was dismissed by the user.
 */
@OptIn(ExperimentalMaterialApi::class, ExperimentalPermissionsApi::class)
@androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
@Composable
fun W3WOcrScanner(
    wrapper: W3WOcrWrapper,
    modifier: Modifier = Modifier,
    options: AutosuggestOptions? = null,
    returnCoordinates: Boolean = false,
    displayUnits: DisplayUnits = DisplayUnits.SYSTEM,
    scanStateScanningTitle: String = stringResource(id = R.string.scan_state_scanning),
    scanStateDetectedTitle: String = stringResource(id = R.string.scan_state_detecting),
    scanStateValidatingTitle: String = stringResource(id = R.string.scan_state_validating),
    scanStateFoundTitle: String = stringResource(id = R.string.scan_state_found),
    scanStateLoadingTitle: String = stringResource(id = R.string.scan_state_loading),
    onSuggestionSelected: ((SuggestionWithCoordinates) -> Unit),
    onError: ((What3WordsError) -> Unit)?,
    onDismiss: (() -> Unit)?,
) {
    val context = LocalContext.current.applicationContext
    val lifecycleOwner = LocalLifecycleOwner.current

    val previewView = remember { PreviewView(context) }
    val scanResultState = remember { ScanResultState() }
    val manager = remember {
        OcrScanManager(wrapper, options, object : OcrScanManager.OcrScanResultCallback {
            override fun onScanning() {
                scanResultState.scanning()
            }

            override fun onDetected() {
                scanResultState.detected()
            }

            override fun onValidating() {
                scanResultState.validating()
            }

            override fun onError(error: What3WordsError) {
                onError?.invoke(error)
            }

            override fun onFound(result: List<Suggestion>) {
                scanResultState.found(result)
            }
        })
    }

    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberBottomSheetState(BottomSheetValue.Collapsed)
    )

    var heightSheet by remember { mutableStateOf(78.dp) }
    var heightSheetPeek by remember { mutableStateOf(78.dp) }
    val cameraPermissionState = rememberPermissionState(
        Manifest.permission.CAMERA
    ) {
        if (it) {
            manager.startCamera(context, lifecycleOwner, previewView)
        } else {
            onError?.invoke(What3WordsError.UNKNOWN_ERROR.apply {
                message = "Ocr scanner needs camera permissions"
            })
        }
    }

    LaunchedEffect(key1 = true, block = {
        wrapper.moduleInstalled {
            if (it) {
                cameraPermissionState.launchPermissionRequest()
            } else {
                wrapper.installModule(onDownloaded = { installed, error ->
                    if (installed) {
                        cameraPermissionState.launchPermissionRequest()
                    } else {
                        onError?.invoke(error ?: What3WordsError.SDK_ERROR.apply {
                            message =
                                "Error installing MLKit modules, check if you have Google Play Services in your device"
                        })
                    }
                })
            }
        }
    })

    LaunchedEffect(key1 = scanResultState.lastAdded, block = {
        if (scanResultState.foundItems.size > 0 && scaffoldState.bottomSheetState.isCollapsed) {
            scaffoldState.bottomSheetState.expand()
            heightSheetPeek = 100.dp
        }
    })

    BottomSheetScaffold(
        modifier = modifier,
        scaffoldState = scaffoldState,
        sheetPeekHeight = heightSheetPeek,
        sheetBackgroundColor = Color.Transparent,
        sheetContent = {
            SuggestionPicker(
                scanResultState,
                heightSheet,
                displayUnits,
                scanStateScanningTitle = scanStateScanningTitle,
                scanStateDetectedTitle = scanStateDetectedTitle,
                scanStateValidatingTitle = scanStateValidatingTitle,
                scanStateFoundTitle = scanStateFoundTitle,
                scanStateLoadingTitle = scanStateLoadingTitle
            ) {
                manager.stop()
                if (returnCoordinates) {
                    CoroutineScope(Dispatchers.IO).launch {
                        val res =
                            wrapper.getDataProvider().convertToCoordinates(it.words).execute()
                        if (res.isSuccessful) {
                            onSuggestionSelected.invoke(
                                SuggestionWithCoordinates(
                                    it,
                                    res.coordinates
                                )
                            )
                        } else {
                            onSuggestionSelected.invoke(SuggestionWithCoordinates(it))
                        }
                    }
                } else {
                    onSuggestionSelected.invoke(SuggestionWithCoordinates(it))
                }
            }
        }
    ) {
        // app UI
        ScanArea(previewView, scanResultState, {
            if (scanResultState.state == ScanResultState.State.Idle) {
                manager.layoutCoordinates = it
                manager.displayMetrics = context.resources.displayMetrics
            }
            val newHeight =
                ((context.resources.displayMetrics.heightPixels / context.resources.displayMetrics.density) - (it.boundsInRoot().bottom / context.resources.displayMetrics.density)).dp - 60.dp
            if (heightSheet != newHeight) {
                heightSheet = newHeight
            }
        }, {
            manager.stop()
            onDismiss?.invoke()
        })
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SuggestionPicker(
    scanResultState: ScanResultState,
    maxHeight: Dp,
    displayUnits: DisplayUnits,
    scanStateScanningTitle: String,
    scanStateDetectedTitle: String,
    scanStateValidatingTitle: String,
    scanStateFoundTitle: String,
    scanStateLoadingTitle: String,
    onSuggestionSelected: (Suggestion) -> Unit
) {
    Column(
        modifier = Modifier
            .clip(shape = RoundedCornerShape(8.dp, 8.dp, 0.dp, 0.dp))
            .heightIn(min = 100.dp, max = maxHeight)
            .background(W3WTheme.colors.background),
    ) {
        Image(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .align(CenterHorizontally),
            painter = painterResource(id = R.drawable.grip),
            contentDescription = stringResource(R.string.grip_content_description)
        )
        if (scanResultState.state != ScanResultState.State.Found && scanResultState.foundItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                val title: String? = when (scanResultState.state) {
                    ScanResultState.State.Detected -> scanStateDetectedTitle
                    is ScanResultState.State.Error -> null
                    ScanResultState.State.Found -> null
                    ScanResultState.State.Scanning -> scanStateScanningTitle
                    ScanResultState.State.Validating -> scanStateValidatingTitle
                    ScanResultState.State.Idle -> scanStateLoadingTitle
                }
                if (!title.isNullOrEmpty()) {
                    Text(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(top = 4.dp),
                        style = W3WTheme.typography.headline,
                        text = title,
                        color = W3WTheme.colors.textPrimary
                    )
                }
            }
        }
        LazyColumn(modifier = Modifier.padding(8.dp)) {
            // the first item that is visible
            item {
                if (scanResultState.foundItems.isNotEmpty()) {
                    Text(
                        modifier = Modifier
                            .padding(start = 12.dp),
                        style = W3WTheme.typography.caption2,
                        color = W3WTheme.colors.textPlaceholder,
                        text = scanStateFoundTitle
                    )
                }
            }
            items(
                count = scanResultState.foundItems.size,
                key = { scanResultState.foundItems[it].words },
            ) {
                val item = scanResultState.foundItems[it]
                SuggestionWhat3words(
                    modifier = Modifier.animateItemPlacement(),
                    words = item.words,
                    nearestPlace = item.nearestPlace,
                    distance = item.distanceToFocusKm,
                    displayUnits = displayUnits,
                    onClick = {
                        onSuggestionSelected.invoke(item)
                    }
                )
            }
        }
    }
}

@Composable
private fun ScanArea(
    previewView: PreviewView,
    scanResultState: ScanResultState,
    cropAreaReady: (LayoutCoordinates) -> Unit,
    onDismiss: (() -> Unit)?
) {
    ConstraintLayout(
        modifier = Modifier.fillMaxSize()
    ) {
        val (preview, startBackground, endBackground, topBackground, cropArea, bottomBackground, logo, buttonClose, topLeftCropImage, topRightCropImage, bottomLeftCropImage, bottomRightCropImage) = createRefs()
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .constrainAs(preview) {
                    top.linkTo(parent.top)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    bottom.linkTo(parent.bottom)
                },
            factory = {
                previewView.apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            }
        )
        Box(
            modifier = Modifier
                .constrainAs(startBackground) {
                    top.linkTo(parent.top)
                    start.linkTo(parent.start)
                    bottom.linkTo(parent.bottom)
                    height = Dimension.fillToConstraints
                    width = Dimension.value(24.dp)
                }
                .background(Color(0x990A3049))
        )
        Box(
            modifier = Modifier
                .constrainAs(endBackground) {
                    top.linkTo(parent.top)
                    end.linkTo(parent.end)
                    bottom.linkTo(parent.bottom)
                    height = Dimension.fillToConstraints
                    width = Dimension.value(24.dp)
                }
                .background(Color(0x990A3049))
        )
        Box(
            modifier = Modifier
                .constrainAs(topBackground) {
                    start.linkTo(startBackground.end)
                    end.linkTo(endBackground.start)
                    top.linkTo(parent.top)
                    width = Dimension.fillToConstraints
                    height = Dimension.value(60.dp)
                }
                .background(Color(0x990A3049))
        )

        Box(
            modifier = Modifier
                .constrainAs(cropArea) {
                    start.linkTo(startBackground.end)
                    end.linkTo(endBackground.start)
                    top.linkTo(topBackground.bottom)
                    width = Dimension.fillToConstraints
                    height = Dimension.ratio("1:1")
                }
                .onGloballyPositioned {
                    cropAreaReady.invoke(it)
                }
        )
        Box(
            modifier = Modifier
                .constrainAs(bottomBackground) {
                    top.linkTo(cropArea.bottom)
                    start.linkTo(startBackground.end)
                    end.linkTo(endBackground.start)
                    bottom.linkTo(parent.bottom)
                    width = Dimension.fillToConstraints
                    height = Dimension.fillToConstraints
                }
                .background(Color(0x990A3049))
        )
        Image(
            modifier = Modifier.constrainAs(logo) {
                top.linkTo(parent.top)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
                bottom.linkTo(cropArea.top)
                width = Dimension.wrapContent
                height = Dimension.wrapContent
            },
            painter = painterResource(id = R.drawable.ic_logo_with_letters),
            contentDescription = null
        )
        OutlinedIconButton(
            modifier = Modifier.constrainAs(buttonClose) {
                top.linkTo(parent.top)
                end.linkTo(parent.end)
                bottom.linkTo(cropArea.top)
                width = Dimension.wrapContent
                height = Dimension.wrapContent
            },
            icon = rememberVectorPainter(image = Icons.Default.Close),
            buttonSize = IconButtonSize.Medium,
            onClick = {
                onDismiss?.invoke()
            },
            textColor = Color.White
        )
        val margin = (-2).dp
        val color = remember { Animatable(Color.White) }

        if (scanResultState.lastAdded != null) {
            LaunchedEffect(scanResultState.lastAdded) {
                color.animateTo(Color.Green, animationSpec = tween(500))
                color.animateTo(Color.White, animationSpec = tween(500))
            }
        }
        Icon(
            modifier = Modifier.constrainAs(topLeftCropImage) {
                top.linkTo(cropArea.top, margin)
                absoluteLeft.linkTo(cropArea.absoluteLeft, margin)
            },
            imageVector = ImageVector.vectorResource(id = R.drawable.top_left_corner),
            contentDescription = null,
            tint = color.value
        )
        Icon(
            modifier = Modifier.constrainAs(bottomLeftCropImage) {
                bottom.linkTo(cropArea.bottom, margin)
                absoluteLeft.linkTo(cropArea.absoluteLeft, margin)
            },
            painter = painterResource(id = R.drawable.bottom_left_corner),
            contentDescription = null,
            tint = color.value
        )
        Icon(
            modifier = Modifier.constrainAs(topRightCropImage) {
                top.linkTo(cropArea.top, margin)
                absoluteRight.linkTo(cropArea.absoluteRight, margin)
            },
            painter = painterResource(id = R.drawable.top_right_corner),
            contentDescription = null,
            tint = color.value
        )
        Icon(
            modifier = Modifier.constrainAs(bottomRightCropImage) {
                bottom.linkTo(cropArea.bottom, margin)
                absoluteRight.linkTo(cropArea.absoluteRight, margin)
            },
            painter = painterResource(id = R.drawable.bottom_right_corner),
            contentDescription = null,
            tint = color.value
        )
    }
}

@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true
)
@Composable
fun ScanAreaScanningMode() {
    W3WTheme {
        ScanArea(
            PreviewView(LocalContext.current.applicationContext),
            ScanResultState().apply { scanning() },
            {},
            {})
    }
}

@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true
)
@Composable
fun ScanAreaDetectedMode() {
    W3WTheme {
        ScanArea(
            PreviewView(LocalContext.current.applicationContext),
            ScanResultState().apply { detected() },
            {},
            {})
    }
}

@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true
)
@Composable
fun ScanAreaValidatingMode() {
    W3WTheme {
        ScanArea(
            PreviewView(LocalContext.current.applicationContext),
            ScanResultState().apply { validating() },
            {},
            {})
    }
}

@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true
)
@Composable
fun ScanAreaFoundMode() {
    W3WTheme {
        ScanArea(
            PreviewView(LocalContext.current.applicationContext),
            ScanResultState().apply { found(emptyList()) },
            {},
            {})
    }
}



