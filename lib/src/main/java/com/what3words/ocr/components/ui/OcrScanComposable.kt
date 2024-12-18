package com.what3words.ocr.components.ui

import android.Manifest
import android.content.Context
import android.content.res.Configuration
import android.content.res.Configuration.ORIENTATION_PORTRAIT
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.what3words.core.types.common.W3WError
import com.what3words.core.types.domain.W3WSuggestion
import com.what3words.core.types.domain.isLand
import com.what3words.core.types.geometry.km
import com.what3words.core.types.image.W3WImage
import com.what3words.design.library.ui.components.What3wordsAddressListItem
import com.what3words.design.library.ui.components.What3wordsAddressListItemDefaults
import com.what3words.design.library.ui.models.DisplayUnits
import com.what3words.design.library.ui.theme.W3WTheme
import com.what3words.design.library.ui.theme.w3wColorScheme
import com.what3words.ocr.components.R
import com.what3words.ocr.components.internal.buildW3WImageAnalysis
import kotlinx.coroutines.CompletableDeferred
import java.util.concurrent.Executors
import kotlin.math.roundToInt

private const val ANIMATION_DURATION = 500 //ms
private const val SHEET_PEEK_HEIGHT = 90 //dp

/**
 * Contains the default values used by W3WOcrScanner.
 */
object W3WOcrScannerDefaults {

    /**
     * The default colors used by W3WOcrScanner.
     */
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

    /**
     * The default text styles used by W3WOcrScanner.
     */
    data class TextStyles(
        val stateTextStyle: TextStyle,
        val listHeaderTextStyle: TextStyle
    )

    /**
     * The default strings used by W3WOcrScanner.
     */
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
 * A composable that provides an OCR scanner interface for detecting and processing what3words addresses.
 * This version allows fine-grained control by using an external [OcrScannerState] parameter, making it
 * suitable for integration into existing architectures.
 *
 * This version is ideal for applications requiring high customization and tight integration with
 * existing state management systems.
 *
 * This component includes:
 * - Camera preview.
 * - Status display.
 * - List of detected addresses.
 *
 * @param modifier Modifier to be applied to the root BottomSheetScaffold.
 * @param ocrScannerState An external state object that controls the scanner's behavior and lifecycle.
 *                        This allows the client to manage the scanner state independently.
 * @param displayUnits The unit system for displaying distances:
 *                     - `DisplayUnits.SYSTEM` (default): Uses the system locale (Imperial/Metric).
 *                     - `DisplayUnits.IMPERIAL`: Forces Imperial units.
 *                     - `DisplayUnits.METRIC`: Forces Metric units.
 * @param scannerColors Customizable color scheme for the scanner's interface.
 * @param scannerTextStyles Customizable text styles for UI elements.
 * @param scannerStrings Localized strings for scanner UI and accessibility.
 * @param suggestionTextStyles Text styles for the list of detected what3words addresses.
 * @param suggestionColors Color scheme for the list of detected addresses.
 * @param suggestionNearestPlacePrefix A prefix for displaying the nearest place, e.g., "near".
 * @param onFrameCaptured Callback triggered when a camera frame is captured for processing.
 *                        - Params: `W3WImage` - The captured image frame.
 *                        - Returns: `CompletableDeferred<Unit>` - Signals when processing is complete.
 * @param onSuggestionSelected Callback triggered when a user selects a three-word address.
 *                             - Params: `W3WSuggestion` - The selected address.
 * @param onError Callback triggered when an error occurs.
 *                - Params: `W3WError` - The error details.
 * @param onDismiss Callback triggered when the scanner is manually dismissed by the user.
 *
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun W3WOcrScanner(
    modifier: Modifier = Modifier,
    ocrScannerState: OcrScannerState,
    displayUnits: DisplayUnits = DisplayUnits.SYSTEM,
    scannerColors: W3WOcrScannerDefaults.Colors = W3WOcrScannerDefaults.defaultColors(),
    scannerTextStyles: W3WOcrScannerDefaults.TextStyles = W3WOcrScannerDefaults.defaultTextStyles(),
    scannerStrings: W3WOcrScannerDefaults.Strings = W3WOcrScannerDefaults.defaultStrings(),
    suggestionTextStyles: What3wordsAddressListItemDefaults.TextStyles = What3wordsAddressListItemDefaults.defaultTextStyles(),
    suggestionColors: What3wordsAddressListItemDefaults.Colors = What3wordsAddressListItemDefaults.defaultColors(),
    suggestionNearestPlacePrefix: String? = stringResource(id = R.string.near),
    onFrameCaptured: ((W3WImage) -> CompletableDeferred<Unit>),
    onSuggestionSelected: ((W3WSuggestion) -> Unit),
    onError: ((W3WError) -> Unit),
    onDismiss: (() -> Unit),
) {
    val cameraPermissionState = rememberPermissionState(
        Manifest.permission.CAMERA
    )

    LaunchedEffect(Unit) {
        cameraPermissionState.launchPermissionRequest()
    }

    when {
        cameraPermissionState.status.isGranted -> {
            ScannerContent(
                modifier = modifier,
                context = LocalContext.current,
                lifecycleOwner = LocalLifecycleOwner.current,
                displayUnits = displayUnits,
                ocrScannerState = ocrScannerState,
                onFrameCaptured = onFrameCaptured,
                onDismiss = onDismiss,
                onError = onError,
                scannerColors = scannerColors,
                scannerStrings = scannerStrings,
                scannerTextStyles = scannerTextStyles,
                suggestionTextStyles = suggestionTextStyles,
                suggestionColors = suggestionColors,
                suggestionNearestPlacePrefix = suggestionNearestPlacePrefix,
                onSuggestionSelected = onSuggestionSelected,
            )
        }

        cameraPermissionState.status is PermissionStatus.Denied -> {
            if (cameraPermissionState.status.shouldShowRationale) {
                onError(W3WError(message = "Ocr scanner needs camera permissions"))
            }
        }
    }
}

/**
 * A composable that provides an OCR scanner interface for detecting and processing what3words addresses.
 * This version simplifies integration by internally managing the scanner's state with [OcrScanManager],
 * making it easy to use in projects without the need for external state management.
 *
 * This version is ideal for projects requiring quick and easy integration without extensive
 * customization or state management.
 *
 * This component includes:
 * - Camera preview.
 * - Status display.
 * - List of detected addresses.
 *
 * @param modifier Modifier to be applied to the root BottomSheetScaffold.
 * @param ocrScanManager A manager object that handles the scanner's state and processing automatically.
 * @param displayUnits The unit system for displaying distances:
 *                     - `DisplayUnits.SYSTEM` (default): Uses the system locale (Imperial/Metric).
 *                     - `DisplayUnits.IMPERIAL`: Forces Imperial units.
 *                     - `DisplayUnits.METRIC`: Forces Metric units.
 * @param scannerColors Customizable color scheme for the scanner's interface.
 * @param scannerTextStyles Customizable text styles for UI elements.
 * @param scannerStrings Localized strings for scanner UI and accessibility.
 * @param suggestionTextStyles Text styles for the list of detected three-word addresses.
 * @param suggestionColors Color scheme for the list of detected addresses.
 * @param suggestionNearestPlacePrefix A prefix for displaying the nearest place, e.g., "near".
 * @param onSuggestionSelected Callback triggered when a user selects a three-word address.
 *                             - Params: `W3WSuggestion` - The selected address.
 * @param onSuggestionFound (Optional) Callback triggered when a suggestion is detected in real-time.
 *                          - Params: `W3WSuggestion` - The detected suggestion.
 * @param onError Callback triggered when an error occurs.
 *                - Params: `W3WError` - The error details.
 * @param onDismiss Callback triggered when the scanner is manually dismissed by the user.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun W3WOcrScanner(
    modifier: Modifier = Modifier,
    ocrScanManager: OcrScanManager,
    displayUnits: DisplayUnits = DisplayUnits.SYSTEM,
    scannerColors: W3WOcrScannerDefaults.Colors = W3WOcrScannerDefaults.defaultColors(),
    scannerTextStyles: W3WOcrScannerDefaults.TextStyles = W3WOcrScannerDefaults.defaultTextStyles(),
    scannerStrings: W3WOcrScannerDefaults.Strings = W3WOcrScannerDefaults.defaultStrings(),
    suggestionTextStyles: What3wordsAddressListItemDefaults.TextStyles = What3wordsAddressListItemDefaults.defaultTextStyles(),
    suggestionColors: What3wordsAddressListItemDefaults.Colors = What3wordsAddressListItemDefaults.defaultColors(),
    suggestionNearestPlacePrefix: String? = stringResource(id = R.string.near),
    onSuggestionSelected: ((W3WSuggestion) -> Unit),
    onError: ((W3WError) -> Unit),
    onDismiss: (() -> Unit),
    onSuggestionFound: ((W3WSuggestion) -> Unit)? = null,
) {
    var isScannerReady by remember { mutableStateOf(false) }
    val cameraPermissionState = rememberPermissionState(
        Manifest.permission.CAMERA
    ) { granted ->
        if (granted) {
            ocrScanManager.getReady(
                onReady = {
                    isScannerReady = true
                },
                onError = {
                    onError.invoke(it)
                }
            )
        } else {
            onError.invoke(W3WError(message = "Ocr scanner needs camera permissions"))
        }
    }

    LaunchedEffect(key1 = Unit) {
        cameraPermissionState.launchPermissionRequest()
    }

    val ocrScannerState by ocrScanManager.ocrScannerState.collectAsState()

    when {
        cameraPermissionState.status.isGranted -> {
            ScannerContent(
                modifier = modifier,
                context = LocalContext.current,
                lifecycleOwner = LocalLifecycleOwner.current,
                displayUnits = displayUnits,
                ocrScannerState = ocrScannerState,
                onFrameCaptured = { image ->
                    val deferred = CompletableDeferred<Unit>()
                    if (isScannerReady) {
                        ocrScanManager.scanImage(
                            image = image,
                            onError = onError,
                            onFound = { suggestions ->
                                suggestions.forEach { suggestion ->
                                    onSuggestionFound?.invoke(suggestion)
                                }
                            },
                            onCompleted = {
                                deferred.complete(Unit)
                            }
                        )
                    } else {
                        deferred.complete(Unit)
                    }

                    return@ScannerContent deferred
                },
                onDismiss = onDismiss,
                onError = onError,
                scannerColors = scannerColors,
                scannerStrings = scannerStrings,
                scannerTextStyles = scannerTextStyles,
                suggestionTextStyles = suggestionTextStyles,
                suggestionColors = suggestionColors,
                suggestionNearestPlacePrefix = suggestionNearestPlacePrefix,
                onSuggestionSelected = onSuggestionSelected,
            )
        }

        cameraPermissionState.status is PermissionStatus.Denied -> {
            if (cameraPermissionState.status.shouldShowRationale) {
                onError(W3WError(message = "Ocr scanner needs camera permissions"))
            }
        }
    }
}

@Composable
private fun SuggestionPicker(
    ocrScannerState: OcrScannerState,
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
        if (ocrScannerState.state != OcrScannerState.State.Found && ocrScannerState.foundItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                val title: String? = when (ocrScannerState.state) {
                    OcrScannerState.State.Idle -> scannerStrings.scanStateLoadingTitle
                    OcrScannerState.State.Detected -> scannerStrings.scanStateDetectedTitle
                    OcrScannerState.State.Scanning -> scannerStrings.scanStateScanningTitle
                    OcrScannerState.State.Validating -> scannerStrings.scanStateValidatingTitle
                    else -> null
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
                if (ocrScannerState.foundItems.isNotEmpty()) {
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
                count = ocrScannerState.foundItems.size,
                key = { ocrScannerState.foundItems[it].w3wAddress.words },
            ) {
                val item = ocrScannerState.foundItems[it]
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
                    showDivider = ocrScannerState.foundItems.lastIndex != it
                )
            }
        }
    }
}

@Composable
private fun ScanArea(
    ocrScannerState: OcrScannerState,
    previewView: PreviewView,
    closeButtonContentDescription: String,
    scannerColors: W3WOcrScannerDefaults.Colors,
    cropAreaReady: (LayoutCoordinates) -> Unit,
    bottomAreaReady: (LayoutCoordinates) -> Unit,
    previewAreaReady: (LayoutCoordinates) -> Unit,
    onDismiss: (() -> Unit)?,
) {
    val orientation = LocalConfiguration.current.orientation

    ConstraintLayout(
        modifier = Modifier.fillMaxSize()
    ) {
        val (preview, startBackground, endBackground, topBackground, cropArea, bottomBackground, logo, buttonClose, topLeftCropImage, topRightCropImage, bottomLeftCropImage, bottomRightCropImage) = createRefs()

        // Use the provided PreviewView
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
            factory = { previewView }
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

        LaunchedEffect(ocrScannerState.foundItems.size) {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScannerContent(
    modifier: Modifier = Modifier,
    ocrScannerState: OcrScannerState,
    context: Context,
    lifecycleOwner: LifecycleOwner,
    displayUnits: DisplayUnits,
    onFrameCaptured: ((W3WImage) -> CompletableDeferred<Unit>),
    onDismiss: (() -> Unit)?,
    onError: ((W3WError) -> Unit)?,
    scannerColors: W3WOcrScannerDefaults.Colors,
    scannerStrings: W3WOcrScannerDefaults.Strings,
    scannerTextStyles: W3WOcrScannerDefaults.TextStyles = W3WOcrScannerDefaults.defaultTextStyles(),
    suggestionTextStyles: What3wordsAddressListItemDefaults.TextStyles = What3wordsAddressListItemDefaults.defaultTextStyles(),
    suggestionColors: What3wordsAddressListItemDefaults.Colors = What3wordsAddressListItemDefaults.defaultColors(),
    suggestionNearestPlacePrefix: String? = stringResource(id = R.string.near),
    onSuggestionSelected: ((W3WSuggestion) -> Unit),
) {
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.PERFORMANCE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }
    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(SheetValue.PartiallyExpanded)
    )

    var heightSheet by remember { mutableStateOf(SHEET_PEEK_HEIGHT.dp) }
    var heightSheetPeek by remember { mutableStateOf(SHEET_PEEK_HEIGHT.dp) }

    var cropLayoutCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var cameraLayoutCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }

    val imageAnalyzerExecutor = remember { Executors.newSingleThreadExecutor() }

    /** This [LaunchedEffect] will run when a new scanned what3words address is added to [SuggestionPicker],
     * and if list is not empty change the size of the peek and set it to expanded.
     */
    LaunchedEffect(
        ocrScannerState.foundItems,
        scaffoldState.bottomSheetState.hasPartiallyExpandedState
    ) {
        val shouldExpandSheet =
            ocrScannerState.foundItems.isNotEmpty() && scaffoldState.bottomSheetState.hasPartiallyExpandedState
        if (shouldExpandSheet) {
            scaffoldState.bottomSheetState.expand()
            heightSheetPeek = 100.dp
        }
    }

    // Add a state to track if the camera is bound
    var isCameraBound by remember { mutableStateOf(false) }

    // Function to bind the camera use cases
    fun bindCamera(cameraProvider: ProcessCameraProvider) {
        try {
            val preview = Preview.Builder().build()
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            val imageAnalysis = buildW3WImageAnalysis(
                onFrameCaptured = onFrameCaptured,
                onError = onError ?: { /* Handle error */ },
                imageAnalyzerExecutor = imageAnalyzerExecutor,
                cropLayoutCoordinates = cropLayoutCoordinates ?: return,
                cameraLayoutCoordinates = cameraLayoutCoordinates ?: return,
            )

            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageAnalysis
            )

            preview.setSurfaceProvider(previewView.surfaceProvider)
            isCameraBound = true
        } catch (e: Exception) {
            onError?.invoke(W3WError(message = "Camera initialization failed: ${e.message}"))
        }
    }

    // Function to unbind the camera use cases
    fun unbindCamera(cameraProvider: ProcessCameraProvider) {
        cameraProvider.unbindAll()
        isCameraBound = false
    }

    DisposableEffect(lifecycleOwner) {
        val cameraProvider = cameraProviderFuture.get()

        onDispose {
            if (isCameraBound) {
                unbindCamera(cameraProvider)
            }
            imageAnalyzerExecutor.shutdown()
        }
    }

    LaunchedEffect(cropLayoutCoordinates, cameraLayoutCoordinates) {
        if (cropLayoutCoordinates != null && cameraLayoutCoordinates != null && !isCameraBound) {
            val cameraProvider = cameraProviderFuture.get()
            bindCamera(cameraProvider)
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
                ocrScannerState = ocrScannerState,
                maxHeight = heightSheet,
                displayUnits = displayUnits,
                scannerStrings = scannerStrings,
                suggestionTextStyles = suggestionTextStyles,
                suggestionColors = suggestionColors,
                scannerColors = scannerColors,
                scannerTextStyles = scannerTextStyles,
                suggestionNearestPlacePrefix = suggestionNearestPlacePrefix,
                onSuggestionSelected = onSuggestionSelected
            )
        }
    ) {
        ScanArea(
            ocrScannerState = ocrScannerState,
            previewView = previewView,
            closeButtonContentDescription = scannerStrings.closeButtonContentDescription,
            scannerColors = scannerColors,
            cropAreaReady = { coordinates ->
                if (ocrScannerState.state == OcrScannerState.State.Idle) {
                    cropLayoutCoordinates = coordinates
                }
            },
            bottomAreaReady = {
                val newHeight =
                    (it.size.height / context.resources.displayMetrics.density).dp - SHEET_PEEK_HEIGHT.dp
                if (heightSheet != newHeight) {
                    heightSheet = newHeight
                }
            },
            previewAreaReady = { coordinates ->
                if (ocrScannerState.state == OcrScannerState.State.Idle) {
                    cameraLayoutCoordinates = coordinates
                }
            },
            onDismiss = onDismiss
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun ScanAreaDetectedMode() {
    W3WTheme {
        ScanArea(
            OcrScannerState(emptyList(), OcrScannerState.State.Detected),
            PreviewView(LocalContext.current),
            "",
            W3WOcrScannerDefaults.defaultColors(),
            {},
            {},
            {},
            {}
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(
    uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true
)
@Composable
fun ScanAreaValidatingMode() {
    W3WTheme {
        ScanArea(
            OcrScannerState(emptyList(), OcrScannerState.State.Validating),
            PreviewView(LocalContext.current),
            "",
            W3WOcrScannerDefaults.defaultColors(),
            {},
            {},
            {},
            {})
    }
}

@androidx.compose.ui.tooling.preview.Preview(
    uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true
)
@Composable
fun ScanAreaFoundMode() {
    W3WTheme {
        ScanArea(
            OcrScannerState(emptyList(), OcrScannerState.State.Found),
            PreviewView(LocalContext.current),
            "",
            W3WOcrScannerDefaults.defaultColors(),
            {},
            {},
            {},
            {})
    }
}

@androidx.compose.ui.tooling.preview.Preview(
    uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true
)
@Composable
fun ScanAreaScanningMode() {
    W3WTheme {
        ScanArea(
            OcrScannerState(emptyList(), OcrScannerState.State.Scanning),
            PreviewView(LocalContext.current),
            "",
            W3WOcrScannerDefaults.defaultColors(),
            {},
            {},
            {},
            {})
    }
}