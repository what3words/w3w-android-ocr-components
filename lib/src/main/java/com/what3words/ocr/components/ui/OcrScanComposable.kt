package com.what3words.ocr.components.ui

import android.Manifest
import android.content.res.Configuration
import android.content.res.Configuration.ORIENTATION_PORTRAIT
import android.view.ViewGroup
import androidx.camera.view.PreviewView
import androidx.compose.animation.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
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
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import com.what3words.core.types.common.W3WError
import com.what3words.core.types.domain.W3WSuggestion
import com.what3words.core.types.domain.isLand
import com.what3words.core.types.geometry.km
import com.what3words.design.library.ui.components.What3wordsAddressListItem
import com.what3words.design.library.ui.components.What3wordsAddressListItemDefaults
import com.what3words.design.library.ui.models.DisplayUnits
import com.what3words.design.library.ui.theme.W3WTheme
import com.what3words.design.library.ui.theme.w3wColorScheme
import com.what3words.javawrapper.response.APIResponse.What3WordsError
import com.what3words.javawrapper.response.SuggestionWithCoordinates
import com.what3words.ocr.components.R
import com.what3words.ocr.components.models.ScanResultState
import com.what3words.ocr.components.ui.OcrScanManager.OcrScanResultCallback
import kotlin.math.roundToInt

private const val ANIMATION_DURATION = 500 //ms
private const val SHEET_PEEK_HEIGHT = 90 //dp

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
     * Factory function for creating a default [Colors] instance for [W3WOcrScanner].
     * This function allows customization of various color properties used in the OCR scanner UI components.
     *
     * @param bottomDrawerBackground The background color of the bottom sheet in the OCR scanner.
     * @param overlayBackground The background color of the camera shutter overlay.
     * @param stateTextColor Color for the text displaying the current scanner state.
     * @param listHeaderTextColor Color for the text in the header of the scanned suggestions list.
     * @param gripColor Color of the grip in the bottom sheet content.
     * @param closeIconColor Color of the close icon.
     * @param logoColor Color of the what3words logo.
     * @param shutterInactiveColor Color of the camera shutter corners when the state is scanning.
     * @param shutterActiveColor Color of the camera shutter corners when the state is 'found'.
     * @return A [Colors] object with the specified or default color properties.
     */
    @Composable
    fun defaultColors(
        bottomDrawerBackground: Color = MaterialTheme.colorScheme.surfaceContainerLowest,
        overlayBackground: Color = Color(0x990A3049),
        stateTextColor: Color = MaterialTheme.colorScheme.onSecondaryContainer,
        listHeaderTextColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
        gripColor: Color = MaterialTheme.colorScheme.outline,
        closeIconColor: Color = Color.White,
        logoColor: Color = Color.White,
        shutterInactiveColor: Color = Color.White,
        shutterActiveColor: Color = MaterialTheme.w3wColorScheme.success,
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
     * Factory function for creating a default [TextStyles] instance for [W3WOcrScanner].
     * This function allows customization of various text style properties used in the OCR scanner UI components.
     *
     * @param stateTextStyle The text style for the text displaying the current scanner state.
     * @param listHeaderTextStyle The text style for the header of the scanned suggestions list.
     * @return A [TextStyles] object with the specified or default text style properties.
     */
    @Composable
    fun defaultTextStyles(
        stateTextStyle: TextStyle = MaterialTheme.typography.titleMedium,
        listHeaderTextStyle: TextStyle = MaterialTheme.typography.titleSmall
    ): TextStyles {
        return TextStyles(
            stateTextStyle = stateTextStyle,
            listHeaderTextStyle = listHeaderTextStyle
        )
    }

    /**
     * Factory function for creating a default [Strings] instance for [W3WOcrScanner].
     * This function allows customization and localization of various text string properties used in the OCR scanner UI components.
     *
     * @param scanStateScanningTitle The title text displayed when scanning starts.
     * @param scanStateDetectedTitle The title text displayed when a possible three-word address is detected.
     * @param scanStateValidatingTitle The title text displayed during validation of a detected what3words address.
     * @param scanStateFoundTitle The title text displayed for the header of the list of validated what3words addresses.
     * @param scanStateLoadingTitle The title text displayed during waiting periods, such as permission acceptance or downloading.
     * @param closeButtonContentDescription The content description for the close button, supporting accessibility features.
     * @return A [Strings] object with the specified or default string properties.
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
 * Creates a new [W3WOcrScanner] Composable to utilize CameraX and a [OcrScanManager] for scanning what3words addresses using text recognition.
 * This component integrates camera functionality with OCR (Optical Character Recognition) to detect and validate three-word addresses in real-time.
 *
 * @param ocrScanManager The [OcrScanManager] instance used for scanning what3words addresses.
 * @param modifier An optional [Modifier] for customizing the appearance and layout of the root [BottomSheetScaffold].
 * @param displayUnits The unit system ([DisplayUnits]) for displaying distances. Defaults to [DisplayUnits.SYSTEM],
 *                     which uses the system's locale to determine whether to use the Imperial or Metric system.
 * @param scannerColors The color scheme ([W3WOcrScannerDefaults.Colors]) applied to the [W3WOcrScanner].
 *                      Defaults are provided by [W3WOcrScannerDefaults.defaultColors] and can be overridden.
 * @param scannerTextStyles The text styles ([W3WOcrScannerDefaults.TextStyles]) applied to the [W3WOcrScanner].
 *                          Defaults are provided by [W3WOcrScannerDefaults.defaultTextStyles] and can be overridden.
 * @param scannerStrings Localized strings ([W3WOcrScannerDefaults.Strings]) used in the [W3WOcrScanner] for customization
 *                       and accessibility. Defaults are provided by [W3WOcrScannerDefaults.defaultStrings] and can be overridden.
 * @param suggestionTextStyles Text styles ([What3wordsAddressListItemDefaults.TextStyles]) applied to the list of scanned
 *                             three-word addresses. Defaults are set by [What3wordsAddressListItemDefaults.defaultTextStyles]
 *                             and can be overridden.
 * @param suggestionColors Color scheme ([What3wordsAddressListItemDefaults.Colors]) applied to the list of scanned
 *                         three-word addresses. Defaults are set by [What3wordsAddressListItemDefaults.defaultColors]
 *                         and can be overridden.
 * @param suggestionNearestPlacePrefix The prefix for displaying the nearest place in [What3wordsAddressListItem]. Defaults to the resource string [com.what3words.design.library.R.string.near].
 * @param onSuggestionSelected Callback invoked when a [SuggestionWithCoordinates] is selected from the [SuggestionPicker]
 * @param onSuggestionFound Callback invoked when a [SuggestionWithCoordinates] is detected, validated, and displayed in the [SuggestionPicker].
 * @param onError Callback invoked when an error occurs within this composable, providing a [What3WordsError].
 * @param onDismiss Callback invoked when this composable is closed using the close button, indicating a user dismissal without an error or a selected suggestion.
 */
@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
@Composable
fun W3WOcrScanner(
    ocrScanManager: OcrScanManager,
    modifier: Modifier = Modifier,
    displayUnits: DisplayUnits = DisplayUnits.SYSTEM,
    scannerColors: W3WOcrScannerDefaults.Colors = W3WOcrScannerDefaults.defaultColors(),
    scannerTextStyles: W3WOcrScannerDefaults.TextStyles = W3WOcrScannerDefaults.defaultTextStyles(),
    scannerStrings: W3WOcrScannerDefaults.Strings = W3WOcrScannerDefaults.defaultStrings(),
    suggestionTextStyles: What3wordsAddressListItemDefaults.TextStyles = What3wordsAddressListItemDefaults.defaultTextStyles(),
    suggestionColors: What3wordsAddressListItemDefaults.Colors = What3wordsAddressListItemDefaults.defaultColors(),
    suggestionNearestPlacePrefix: String? = stringResource(id = R.string.near),
    onSuggestionSelected: ((W3WSuggestion) -> Unit),
    onError: ((W3WError) -> Unit)?,
    onDismiss: (() -> Unit)?,
    onSuggestionFound: ((W3WSuggestion) -> Unit)? = null
) {
    val context = LocalContext.current.applicationContext
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView by remember {
        mutableStateOf(PreviewView(context))
    }
    val scanResultState = remember { ScanResultState() }

    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(SheetValue.PartiallyExpanded)
    )

    var heightSheet by remember { mutableStateOf(SHEET_PEEK_HEIGHT.dp) }
    var heightSheetPeek by remember { mutableStateOf(SHEET_PEEK_HEIGHT.dp) }
    val cameraPermissionState = rememberPermissionState(
        Manifest.permission.CAMERA
    ) { granted ->
        if (granted) {
            ocrScanManager.scanWithCamera(context, lifecycleOwner, previewView)
        } else {
            onError?.invoke(
                W3WError(
                    message = "Ocr scanner needs camera permissions"
                )
            )
        }
    }

    LaunchedEffect(key1 = Unit) {
        ocrScanManager.setOcrScanResultCallback(object : OcrScanResultCallback {
            override fun onScanning() {
                scanResultState.scanning()
            }

            override fun onDetected() {
                scanResultState.detected()
            }

            override fun onValidating() {
                scanResultState.validating()
            }

            override fun onError(error: W3WError) {
                onError?.invoke(error)
            }

            override fun onFound(result: List<W3WSuggestion>) {
                result.forEach { onSuggestionFound?.invoke(it) }
                scanResultState.found(result)
            }
        })

        ocrScanManager.getReady(
            onReady = {
                cameraPermissionState.launchPermissionRequest()
            },
            onError = {
                onError?.invoke(it)
            }
        )
    }

    /** This [LaunchedEffect] will run when a new scanned what3words address is added to [SuggestionPicker],
     * and if list is not empty change the size of the peek and set it to expanded.
     */
    LaunchedEffect(key1 = scanResultState.lastAdded) {
        if (scanResultState.foundItems.size > 0 && scaffoldState.bottomSheetState.hasPartiallyExpandedState) {
            scaffoldState.bottomSheetState.expand()
            heightSheetPeek = 100.dp
        }
    }

    BottomSheetScaffold(
        modifier = modifier,
        scaffoldState = scaffoldState,
        sheetPeekHeight = heightSheetPeek,
        sheetDragHandle = {
            BottomSheetDefaults.DragHandle(
                color = scannerColors.gripColor
            )
        },
        sheetContainerColor = scannerColors.bottomDrawerBackground,
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
                ocrScanManager.stop()
                onSuggestionSelected.invoke(it)
            }
        }
    ) {
        // app UI
        ScanArea(
            previewView,
            scanResultState,
            scannerStrings.closeButtonContentDescription,
            scannerColors,
            cropAreaReady = {
                if (scanResultState.state == ScanResultState.State.Idle) {
                    ocrScanManager.cropLayoutCoordinates = it
                }
            },
            bottomAreaReady = {
                val newHeight =
                    (it.size.height / context.resources.displayMetrics.density).dp - SHEET_PEEK_HEIGHT.dp
                if (heightSheet != newHeight) {
                    heightSheet = newHeight
                }
            },
            previewAreaReady = {
                if (scanResultState.state == ScanResultState.State.Idle) {
                    ocrScanManager.cameraLayoutCoordinates = it
                }
            },
            onDismiss = {
                onDismiss?.invoke()
            })
    }
}

@Composable
private fun SuggestionPicker(
    scanResultState: ScanResultState,
    maxHeight: Dp,
    displayUnits: DisplayUnits,
    scannerStrings: W3WOcrScannerDefaults.Strings,
    scannerTextStyles: W3WOcrScannerDefaults.TextStyles,
    scannerColors: W3WOcrScannerDefaults.Colors,
    suggestionTextStyles: What3wordsAddressListItemDefaults.TextStyles,
    suggestionColors: What3wordsAddressListItemDefaults.Colors,
    suggestionNearestPlacePrefix: String?,
    onSuggestionSelected: (W3WSuggestion) -> Unit
) {
    Column(
        modifier = Modifier
            .heightIn(min = 90.dp, max = maxHeight),
    ) {
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
                            .align(Alignment.Center),
                        style = scannerTextStyles.stateTextStyle,
                        text = title,
                        color = scannerColors.stateTextColor
                    )
                }
            }
        }
        LazyColumn(modifier = Modifier) {
            // the first item that is visible
            item {
                if (scanResultState.foundItems.isNotEmpty()) {
                    Text(
                        modifier = Modifier
                            .padding(start = 18.dp),
                        style = scannerTextStyles.listHeaderTextStyle,
                        color = scannerColors.listHeaderTextColor,
                        text = scannerStrings.scanStateFoundTitle
                    )
                }
            }
            items(
                count = scanResultState.foundItems.size,
                key = { scanResultState.foundItems[it].w3wAddress.words },
            ) {
                val item = scanResultState.foundItems[it]
                Modifier
                    .testTag("itemOCR ${item.w3wAddress.words}")
                What3wordsAddressListItem(
                    modifier = Modifier.animateItem(fadeInSpec = null, fadeOutSpec = null),
                    words = item.w3wAddress.words,
                    nearestPlace = item.w3wAddress.nearestPlace,
                    distance = item.distanceToFocus?.km()?.roundToInt(),
                    displayUnits = displayUnits,
                    textStyles = suggestionTextStyles,
                    colors = suggestionColors,
                    isLand = item.w3wAddress.country.isLand(),
                    nearestPlacePrefix = suggestionNearestPlacePrefix,
                    onClick = {
                        onSuggestionSelected.invoke(item)
                    },
                    showDivider = scanResultState.foundItems.lastIndex != it
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
    bottomAreaReady: (LayoutCoordinates) -> Unit,
    previewAreaReady: (LayoutCoordinates) -> Unit,
    onDismiss: (() -> Unit)?
) {
    val orientation = LocalConfiguration.current.orientation
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
                }
                .onGloballyPositioned {
                    previewAreaReady.invoke(it)
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
                    height =
                        Dimension.ratio(if (orientation == ORIENTATION_PORTRAIT) "1:1" else "3.1")
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
                .onGloballyPositioned {
                    bottomAreaReady.invoke(it)
                }
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
        IconButton(
            modifier = Modifier.constrainAs(buttonClose) {
                top.linkTo(parent.top)
                end.linkTo(parent.end)
                bottom.linkTo(cropArea.top)
                width = Dimension.wrapContent
                height = Dimension.wrapContent
            },
            onClick = {
                onDismiss?.invoke()
            }
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                tint = scannerColors.closeIconColor,
                contentDescription = closeButtonContentDescription
            )
        }
        val margin = (-2).dp
        val color = remember { Animatable(scannerColors.shutterInactiveColor) }

        if (scanResultState.lastAdded != null) {
            LaunchedEffect(scanResultState.lastAdded) {
                color.animateTo(
                    scannerColors.shutterActiveColor, animationSpec = tween(
                        ANIMATION_DURATION
                    )
                )
                color.animateTo(
                    scannerColors.shutterInactiveColor, animationSpec = tween(
                        ANIMATION_DURATION
                    )
                )
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
            {},
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
            {},
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
            {},
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
            {},
            {},
            {})
    }
}