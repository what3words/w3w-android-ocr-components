package com.what3words.ocr.components.ui

import android.Manifest
import android.content.res.Configuration
import android.view.ViewGroup
import androidx.camera.view.PreviewView
import androidx.compose.animation.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.ui.text.TextStyle
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
import com.what3words.design.library.ui.components.SuggestionWhat3wordsDefaults
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
import kotlinx.coroutines.withContext


object W3WOcrScannerDefaults {
    data class Colors(
        val bottomDrawerBackground: Color,
        val overlayBackground: Color,
        val stateTextColor: Color,
        val listHeaderTextColor: Color,
        val gripColor: Color,
        val closeIconColor: Color,
        val logoColor: Color,
        val shutterInactiveColor: Color,
        val shutterActiveColor: Color
    )

    data class TextStyles(
        val stateTextStyle: TextStyle,
        val listHeaderTextStyle: TextStyle
    )

    data class Strings(
        val scanStateScanningTitle: String,
        val scanStateDetectedTitle: String,
        val scanStateValidatingTitle: String,
        val scanStateFoundTitle: String,
        val scanStateLoadingTitle: String,
        val closeButtonContentDescription: String
    )

    /**
     * Creates [W3WOcrScannerDefaults.Colors] to be applied to [W3WOcrScanner],
     * allowing to override any [Color] on [W3WOcrScanner] composable for customization.
     *
     * @param bottomDrawerBackground set the background [Color] of the [W3WOcrScanner] [BottomSheetScaffold] sheetContent.
     * @param overlayBackground set the background [Color] of the camera shutter overlay.
     * @param stateTextColor set text [Color] of the [W3WOcrScanner] current state [Text] field.
     * @param listHeaderTextColor set text [Color] of the [W3WOcrScanner] scanned suggestions list header.
     * @param gripColor set text [Color] of the [W3WOcrScanner] [BottomSheetScaffold] sheetContent grip.
     * @param closeIconColor the [Color] tint of the [W3WOcrScanner] close button.
     * @param logoColor the [Color] tint of the [W3WOcrScanner] what3words logo.
     * @param shutterInactiveColor the [Color] of the camera shutter 4 corners when state is different to found.
     * @param shutterActiveColor the [Color] of the camera shutter 4 corners when state is found.
     *
     * @return [W3WOcrScannerDefaults.Colors] that will be applied to the [W3WOcrScanner] composable.
     */
    @Composable
    fun defaultColors(
        bottomDrawerBackground: Color = W3WTheme.colors.background,
        overlayBackground: Color = Color(0x990A3049),
        stateTextColor: Color = W3WTheme.colors.textPrimary,
        listHeaderTextColor: Color = W3WTheme.colors.textPlaceholder,
        gripColor: Color = Color.LightGray,
        closeIconColor: Color = Color.White,
        logoColor: Color = Color.White,
        shutterInactiveColor: Color = Color.White,
        shutterActiveColor: Color = Color.Green
    ): Colors {
        return Colors(
            bottomDrawerBackground = bottomDrawerBackground,
            overlayBackground = overlayBackground,
            stateTextColor = stateTextColor,
            listHeaderTextColor = listHeaderTextColor,
            gripColor = gripColor,
            closeIconColor = closeIconColor,
            logoColor = logoColor,
            shutterActiveColor = shutterActiveColor,
            shutterInactiveColor = shutterInactiveColor
        )
    }

    /**
     * Creates [W3WOcrScannerDefaults.TextStyles] to be applied to [W3WOcrScanner],
     * allowing to override any [TextStyle] on [W3WOcrScanner] composable for customization.
     *
     * @param stateTextStyle set [TextStyle] of the [W3WOcrScanner] current state.
     * @param listHeaderTextStyle set [TextStyle] of the [W3WOcrScanner] scanned list header.
     *
     * @return [W3WOcrScannerDefaults.TextStyles] that will be applied to the [W3WOcrScanner] composable.
     */
    @Composable
    fun defaultTextStyles(
        stateTextStyle: TextStyle = W3WTheme.typography.headline,
        listHeaderTextStyle: TextStyle = W3WTheme.typography.caption2
    ): TextStyles {
        return TextStyles(
            stateTextStyle = stateTextStyle,
            listHeaderTextStyle = listHeaderTextStyle
        )
    }

    /**
     * Creates [W3WOcrScannerDefaults.Strings] to be used on [W3WOcrScanner],
     * allowing localisation and accessibility to be controlled by developers.
     *
     * @param scanStateScanningTitle the text to be displayed when it starts scanning, default: [R.string.scan_state_scanning]
     * @param scanStateDetectedTitle the text to be displayed when it detects a possible three word address, default: [R.string.scan_state_detecting]
     * @param scanStateValidatingTitle the text to be displayed when it validates a possible three word address (API/SDK check for validation), default: [R.string.scan_state_validating]
     * @param scanStateFoundTitle the title to be displayed as the header of the list of scanned and validated three word addresses, default: [R.string.scan_state_found]
     * @param scanStateLoadingTitle the title to be displayed when it's waiting for permissions to be accepted or any kind of download needed, default: [R.string.scan_state_loading]
     * @param closeButtonContentDescription the content description of the actionable close button, default: [R.string.scan_state_loading]
     *
     * @return [W3WOcrScannerDefaults.Strings] that will be used on [W3WOcrScanner] composable.
     */
    @Composable
    fun defaultStrings(
        scanStateScanningTitle: String = stringResource(id = R.string.scan_state_scanning),
        scanStateDetectedTitle: String = stringResource(id = R.string.scan_state_detecting),
        scanStateValidatingTitle: String = stringResource(id = R.string.scan_state_validating),
        scanStateFoundTitle: String = stringResource(id = R.string.scan_state_found),
        scanStateLoadingTitle: String = stringResource(id = R.string.scan_state_loading),
        closeButtonContentDescription: String = stringResource(id = R.string.cd_close_button),
    ): Strings {
        return Strings(
            scanStateScanningTitle = scanStateScanningTitle,
            scanStateDetectedTitle = scanStateDetectedTitle,
            scanStateValidatingTitle = scanStateValidatingTitle,
            scanStateFoundTitle = scanStateFoundTitle,
            scanStateLoadingTitle = scanStateLoadingTitle,
            closeButtonContentDescription = closeButtonContentDescription
        )
    }
}


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
 * @param scannerColors the [W3WOcrScannerDefaults.Colors] that will be applied to the [W3WOcrScanner], Default colors are set here [W3WOcrScannerDefaults.defaultColors] and can all be overridden.
 * @param scannerTextStyles the [W3WOcrScannerDefaults.TextStyles] that will be applied to the [W3WOcrScanner], Default text styles are set here [W3WOcrScannerDefaults.defaultTextStyles] and can all be overridden.
 * @param scannerStrings the [W3WOcrScannerDefaults.Strings] that will be applied to the [W3WOcrScanner] to allow localisation and accessibility customisation. Default strings are set here [W3WOcrScannerDefaults.defaultStrings] and can all be overridden.
 * @param suggestionTextStyles the [SuggestionWhat3wordsDefaults.TextStyles] that will be applied to the [W3WOcrScanner] list of scanned three word address. Default text styles are set here [SuggestionWhat3wordsDefaults.defaultTextStyles] and can all be overridden.
 * @param suggestionColors the [SuggestionWhat3wordsDefaults.Colors] that will be applied to the [W3WOcrScanner] list of scanned three word address. Default colors are set here [SuggestionWhat3wordsDefaults.defaultColors] and can all be overridden.
 * @param suggestionNearestPlacePrefix the prefix to [SuggestionWhat3words] nearest place. Default prefix is [com.what3words.design.library.R.string.near]
 * @param onSuggestionSelected the callback when a [SuggestionWithCoordinates] is selected from the [SuggestionPicker].
 * @param onSuggestionFound the callback when a [SuggestionWithCoordinates] is detected and validated and displayed in the [SuggestionPicker].
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
    scannerColors: W3WOcrScannerDefaults.Colors = W3WOcrScannerDefaults.defaultColors(),
    scannerTextStyles: W3WOcrScannerDefaults.TextStyles = W3WOcrScannerDefaults.defaultTextStyles(),
    scannerStrings: W3WOcrScannerDefaults.Strings = W3WOcrScannerDefaults.defaultStrings(),
    suggestionTextStyles: SuggestionWhat3wordsDefaults.TextStyles = SuggestionWhat3wordsDefaults.defaultTextStyles(),
    suggestionColors: SuggestionWhat3wordsDefaults.Colors = SuggestionWhat3wordsDefaults.defaultColors(),
    suggestionNearestPlacePrefix: String? = stringResource(id = com.what3words.design.library.R.string.near),
    onSuggestionSelected: ((SuggestionWithCoordinates) -> Unit),
    onSuggestionFound: ((SuggestionWithCoordinates) -> Unit)?,
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
                if (onSuggestionFound != null) {
                    result.forEach { onSuggestionFound.invoke(SuggestionWithCoordinates(it)) }
                }
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
                scannerStrings = scannerStrings,
                suggestionTextStyles = suggestionTextStyles,
                suggestionColors = suggestionColors,
                scannerColors = scannerColors,
                scannerTextStyles = scannerTextStyles,
                suggestionNearestPlacePrefix = suggestionNearestPlacePrefix
            ) {
                manager.stop()
                if (returnCoordinates) {
                    CoroutineScope(Dispatchers.Main).launch {
                        val res = withContext(Dispatchers.IO) {
                            wrapper.getDataProvider().convertToCoordinates(it.words).execute()
                        }
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
        ScanArea(
            previewView,
            scanResultState,
            scannerStrings.closeButtonContentDescription,
            scannerColors,
            {
                if (scanResultState.state == ScanResultState.State.Idle) {
                    manager.layoutCoordinates = it
                    manager.displayMetrics = context.resources.displayMetrics
                }
                val newHeight =
                    ((context.resources.displayMetrics.heightPixels / context.resources.displayMetrics.density) - (it.boundsInRoot().bottom / context.resources.displayMetrics.density)).dp - 60.dp
                if (heightSheet != newHeight) {
                    heightSheet = newHeight
                }
            },
            {
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
    scannerStrings: W3WOcrScannerDefaults.Strings,
    scannerTextStyles: W3WOcrScannerDefaults.TextStyles,
    scannerColors: W3WOcrScannerDefaults.Colors,
    suggestionTextStyles: SuggestionWhat3wordsDefaults.TextStyles,
    suggestionColors: SuggestionWhat3wordsDefaults.Colors,
    suggestionNearestPlacePrefix: String?,
    onSuggestionSelected: (Suggestion) -> Unit
) {
    Column(
        modifier = Modifier
            .clip(shape = RoundedCornerShape(8.dp, 8.dp, 0.dp, 0.dp))
            .heightIn(min = 100.dp, max = maxHeight)
            .background(scannerColors.bottomDrawerBackground),
    ) {
        Icon(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .align(CenterHorizontally),
            painter = painterResource(id = R.drawable.grip),
            contentDescription = stringResource(R.string.grip_content_description),
            tint = scannerColors.gripColor
        )
        if (scanResultState.state != ScanResultState.State.Found && scanResultState.foundItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                val title: String? = when (scanResultState.state) {
                    ScanResultState.State.Detected -> scannerStrings.scanStateDetectedTitle
                    is ScanResultState.State.Error -> null
                    ScanResultState.State.Found -> null
                    ScanResultState.State.Scanning -> scannerStrings.scanStateScanningTitle
                    ScanResultState.State.Validating -> scannerStrings.scanStateValidatingTitle
                    ScanResultState.State.Idle -> scannerStrings.scanStateLoadingTitle
                }
                if (!title.isNullOrEmpty()) {
                    Text(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(top = 4.dp),
                        style = scannerTextStyles.stateTextStyle,
                        text = title,
                        color = scannerColors.stateTextColor
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
                        style = scannerTextStyles.listHeaderTextStyle,
                        color = scannerColors.listHeaderTextColor,
                        text = scannerStrings.scanStateFoundTitle
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
                    textStyles = suggestionTextStyles,
                    colors = suggestionColors,
                    nearestPlacePrefix = suggestionNearestPlacePrefix,
                    onClick = {
                        onSuggestionSelected.invoke(item)
                    },
                )
            }
        }
    }
}

@Composable
private fun ScanArea(
    previewView: PreviewView,
    scanResultState: ScanResultState,
    closeButtonContentDescription: String,
    scannerColors: W3WOcrScannerDefaults.Colors,
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
                .background(scannerColors.overlayBackground)
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
                .background(scannerColors.overlayBackground)
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
                .background(scannerColors.overlayBackground)
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
                .background(scannerColors.overlayBackground)
        )
        Icon(
            modifier = Modifier.constrainAs(logo) {
                top.linkTo(parent.top)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
                bottom.linkTo(cropArea.top)
                width = Dimension.wrapContent
                height = Dimension.wrapContent
            },
            painter = painterResource(id = R.drawable.ic_logo_with_letters),
            contentDescription = null,
            tint = scannerColors.logoColor
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
            textColor = scannerColors.closeIconColor,
            iconContentDescription = closeButtonContentDescription
        )
        val margin = (-2).dp
        val color = remember { Animatable(scannerColors.shutterInactiveColor) }

        if (scanResultState.lastAdded != null) {
            LaunchedEffect(scanResultState.lastAdded) {
                color.animateTo(scannerColors.shutterActiveColor, animationSpec = tween(500))
                color.animateTo(scannerColors.shutterInactiveColor, animationSpec = tween(500))
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
            "",
            W3WOcrScannerDefaults.defaultColors(),
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
            "",
            W3WOcrScannerDefaults.defaultColors(),
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
            "",
            W3WOcrScannerDefaults.defaultColors(),
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
            "",
            W3WOcrScannerDefaults.defaultColors(),
            {},
            {})
    }
}