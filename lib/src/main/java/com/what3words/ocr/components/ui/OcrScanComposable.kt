package com.what3words.ocr.components.ui

import android.Manifest
import android.content.Context
import android.content.res.Configuration.ORIENTATION_PORTRAIT
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
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
import com.what3words.design.library.ui.theme.colors_blue_20
import com.what3words.design.library.ui.theme.w3wColorScheme
import com.what3words.ocr.components.R
import com.what3words.ocr.components.extensions.loadDownsampledBitmap
import com.what3words.ocr.components.internal.buildW3WImageAnalysis
import com.what3words.ocr.components.ui.OcrScannerState.ScanningType
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import kotlin.math.roundToInt

private const val ANIMATION_DURATION = 500 //ms
private const val SHEET_PEEK_HEIGHT = 72 //dp
private const val BUTTON_CONTROL_HEIGHT = 72 //dp
enum class SheetState { PEEK, CONTENT, FULL }

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
        val shutterActiveColor: Color,
        val buttonLabelColor: Color,
        val liveSwitchCheckedTrackColor: Color,
        val liveSwitchUncheckedTrackColor: Color,
        val liveSwitchThumbColor: Color,
        val scanningTipColor: Color
    )

    /**
     * The default text styles used by W3WOcrScanner.
     */
    data class TextStyles(
        val stateTextStyle: TextStyle,
        val listHeaderTextStyle: TextStyle,
        val buttonLabelTextStyle: TextStyle,
        val scanningTipTextStyle: TextStyle
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
        val closeButtonContentDescription: String,
        val importButtonLabel: String = "Import",
        val shutterButtonContentDescription: String = "Take photo",
        val liveScanLabel: String = "Live Scan",
        val scanningTip: String = "Position the what3words addresses within frame and capture"
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
        overlayBackground: Color = colors_blue_20.copy(0.6f),
        stateTextColor: Color = MaterialTheme.colorScheme.onSecondaryContainer,
        listHeaderTextColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
        gripColor: Color = MaterialTheme.colorScheme.outline,
        closeIconColor: Color = MaterialTheme.w3wColorScheme.onSurfaceWhite,
        logoColor: Color = MaterialTheme.w3wColorScheme.onSurfaceWhite,
        shutterInactiveColor: Color = MaterialTheme.w3wColorScheme.onSurfaceWhite,
        shutterActiveColor: Color = MaterialTheme.w3wColorScheme.success,
        buttonLabelColor: Color = MaterialTheme.w3wColorScheme.onSurfaceWhite,
        liveSwitchCheckedTrackColor: Color = MaterialTheme.w3wColorScheme.brand,
        liveSwitchUncheckedTrackColor: Color = MaterialTheme.colorScheme.primaryContainer,
        liveSwitchThumbColor: Color = MaterialTheme.w3wColorScheme.onSurfaceWhite,
        scanningTipColor: Color = MaterialTheme.colorScheme.surfaceContainerLowest
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
            shutterInactiveColor = shutterInactiveColor,
            buttonLabelColor = buttonLabelColor,
            liveSwitchCheckedTrackColor = liveSwitchCheckedTrackColor,
            liveSwitchUncheckedTrackColor = liveSwitchUncheckedTrackColor,
            liveSwitchThumbColor = liveSwitchThumbColor,
            scanningTipColor = scanningTipColor
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
        listHeaderTextStyle: TextStyle = MaterialTheme.typography.titleSmall,
        buttonLabelTextStyle: TextStyle = MaterialTheme.typography.bodyMedium,
        scanningTipTextStyle: TextStyle = MaterialTheme.typography.bodyLarge
    ): TextStyles {
        return TextStyles(
            stateTextStyle = stateTextStyle,
            listHeaderTextStyle = listHeaderTextStyle,
            buttonLabelTextStyle = buttonLabelTextStyle,
            scanningTipTextStyle = scanningTipTextStyle
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
        closeButtonContentDescription: String = stringResource(id = R.string.cd_close_button)
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
    onToggleLiveMode: ((Boolean) -> Unit),
    onShutterClick: (() -> Unit),
    onBackPressed: (() -> Unit),
    onDismiss: (() -> Unit),
    onImport: (() -> Unit),
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
                onToggleLiveMode = onToggleLiveMode,
                onShutterClick = onShutterClick,
                onImport = onImport,
                onBackPressed = onBackPressed
            )
        }

        cameraPermissionState.status is PermissionStatus.Denied -> {
            if (cameraPermissionState.status.shouldShowRationale) {
                onError(W3WError(message = "Ocr scanner needs camera permissions"))
            }
        }
    }
}

// Helper function to calculate appropriate sample size
private fun calculateSampleSize(width: Int, height: Int, targetWidth: Int, targetHeight: Int): Int {
    var sampleSize = 1

    if (width > targetWidth || height > targetHeight) {
        val halfWidth = width / 2
        val halfHeight = height / 2

        // Calculate the largest inSampleSize value that is a power of 2 and keeps both
        // height and width larger than the requested height and width
        while ((halfWidth / sampleSize) >= targetWidth && (halfHeight / sampleSize) >= targetHeight) {
            sampleSize *= 2
        }
    }

    return sampleSize
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

    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    // Register for activity result to get single image
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            scope.launch(Dispatchers.IO) {
                // Get image dimensions first before loading full bitmap
                uri.loadDownsampledBitmap(context.contentResolver)?.let {
                    // Create a W3WImage from the bitmap
                    val w3wImage = W3WImage(it)

                    ocrScanManager.scanImage(
                        image = w3wImage,
                        onError = onError,
                        onFound = { suggestions ->
                            suggestions.forEach { suggestion ->
                                onSuggestionFound?.invoke(suggestion)
                            }
                        },
                        onCompleted = {},
                        isFromMedia = true
                    )
                }
            }
        }
    }

    // Define the permission request based on Android version
    val photoLibraryPermission = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> {
            // Android 14+ - Use the limited photos select API
            Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
        }

        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
            // Android 13
            Manifest.permission.READ_MEDIA_IMAGES
        }

        else -> {
            // Android 12 and below
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
    }

    // Create the permission state
    val photoLibraryPermissionState = rememberPermissionState(
        permission = photoLibraryPermission
    ) { granted ->
        if (granted) {
            imagePicker.launch("image/*")
        } else {
            onError.invoke(W3WError(message = "OCR scanner needs media permissions"))
        }
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
                onToggleLiveMode = { isLiveMode ->
                    ocrScanManager.toggleLiveMode(isLiveMode)
                },
                onShutterClick = {
                    ocrScanManager.captureNextFrame()
                },
                onImport = {
                    if (photoLibraryPermissionState.status.isGranted ||
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
                    ) {
                        // For Android 14+ we can always launch the picker as it will handle permission itself
                        imagePicker.launch("image/*")
                    } else {
                        photoLibraryPermissionState.launchPermissionRequest()
                    }
                },
                onBackPressed = {
                    ocrScanManager.onBackPressed()
                },
            )
        }

        cameraPermissionState.status is PermissionStatus.Denied -> {
            if (cameraPermissionState.status.shouldShowRationale) {
                onError(W3WError(message = "Ocr scanner needs camera permissions"))
            }
        }
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
    onToggleLiveMode: (Boolean) -> Unit,
    onImport: () -> Unit,
    onShutterClick: () -> Unit,
    onBackPressed: () -> Unit,
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
    val orientation = LocalConfiguration.current.orientation

    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.PERFORMANCE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    var cropLayoutCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var cameraLayoutCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }

    val imageAnalyzerExecutor = remember { Executors.newSingleThreadExecutor() }

    // Add a state to track if the camera is bound
    var isCameraBound by remember { mutableStateOf(false) }

    // Function to bind the camera use cases
    fun bindCamera(cameraProvider: ProcessCameraProvider) {
        try {
            val preview = Preview.Builder().build()
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            val imageAnalysis = buildW3WImageAnalysis(
                onFrameCaptured = {
                    onFrameCaptured(it)
                },
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
    var parentHeight by remember { mutableStateOf(0f) }
    var maxBottomSheetHeight by remember { mutableStateOf(52.dp) }

    ConstraintLayout(modifier = modifier.onGloballyPositioned { coordinates ->
        parentHeight = coordinates.size.height.toFloat()
    }) {
        val (preview, startBackground, endBackground, topBackground, cropArea, bottomBackground, logo, buttonClose, topLeftCropImage, topRightCropImage, bottomLeftCropImage, bottomRightCropImage, controlBar, instructionText, bottomSheet) = createRefs()
        val (picturePreviewBackground, topAppBar, picturePreviewImage) = createRefs()
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
                    if (ocrScannerState.state == OcrScannerState.State.Idle) {
                        cameraLayoutCoordinates = it
                    }
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
        var cropAreaBottom by remember { mutableStateOf(0f) }

        Box(
            modifier = Modifier
                .constrainAs(cropArea) {
                    start.linkTo(startBackground.end)
                    end.linkTo(endBackground.start)
                    top.linkTo(topBackground.bottom)
                    width = Dimension.fillToConstraints
                    height =
                        Dimension.ratio(if (orientation == ORIENTATION_PORTRAIT) "2:1.5" else "3.1")
                }
                .onGloballyPositioned {
                    cropAreaBottom = it.boundsInParent().bottom
                    if (ocrScannerState.state == OcrScannerState.State.Idle) {
                        cropLayoutCoordinates = it
                    }
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
                contentDescription = scannerStrings.closeButtonContentDescription
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

        val density = LocalDensity.current

        // Calculate maximum allowed height for bottom sheet
        maxBottomSheetHeight =
            remember(cropAreaBottom, ocrScannerState.capturedImage) {
                with(density) {
                    val parentHeightDp = parentHeight.toDp()
                    val controlBarHeight = BUTTON_CONTROL_HEIGHT.dp
                    val instructionBottomDp = cropAreaBottom.toDp()
                    parentHeightDp - instructionBottomDp - controlBarHeight - 48.dp
                }
            }
        // Add bottom control bar
        // Track the control bar visibility

        // State for the bottom sheet
        val peekHeight = SHEET_PEEK_HEIGHT.dp
        val sheetState = remember { mutableStateOf(SheetState.PEEK) }
        val fullScreenHeight = with(density) { parentHeight.toDp() }

// Track whether we're currently dragging
        var isDragging by remember { mutableStateOf(false) }
        var dragOffset by remember { mutableStateOf(0f) }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .constrainAs(controlBar) {
                    if (sheetState.value == SheetState.PEEK) {
                        bottom.linkTo(bottomSheet.top, margin = 24.dp)
                    } else {
                        top.linkTo(cropArea.bottom, margin = 24.dp)
                    }
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    height = Dimension.value(BUTTON_CONTROL_HEIGHT.dp)
                }
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                OutlinedIconButton(
                    onClick = onImport,
                    modifier = Modifier.size(48.dp),
                    border = BorderStroke(
                        width = 1.dp,
                        color = scannerColors.logoColor
                    ),
                    colors = IconButtonDefaults.outlinedIconButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_import),
                        contentDescription = scannerStrings.importButtonLabel,
                        tint = scannerColors.logoColor
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = scannerStrings.importButtonLabel,
                    color = scannerColors.buttonLabelColor,
                    style = scannerTextStyles.buttonLabelTextStyle
                )
            }

            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .border(3.dp, scannerColors.shutterInactiveColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                var isPressed by remember { mutableStateOf(false) }

                val animatedSize by animateFloatAsState(
                    targetValue = if (isPressed) 48.dp.value else 60.dp.value,
                    animationSpec = tween(durationMillis = 150),
                    label = "shutter-button-animation"
                )

                Box(
                    modifier = Modifier
                        .size(animatedSize.dp)
                        .clip(CircleShape)
                        .background(scannerColors.shutterInactiveColor)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            isPressed = true
                            onShutterClick()
                            // Reset after animation
                            MainScope().launch {
                                delay(150)
                                isPressed = false
                            }
                        }
                )
            }

            // Live scan toggle
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Switch(
                    checked = ocrScannerState.scanningType == ScanningType.Live,
                    onCheckedChange = onToggleLiveMode,
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = scannerColors.liveSwitchCheckedTrackColor,
                        uncheckedTrackColor = scannerColors.liveSwitchUncheckedTrackColor,
                        checkedThumbColor = scannerColors.liveSwitchThumbColor,
                        uncheckedThumbColor = scannerColors.liveSwitchThumbColor
                    )
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = scannerStrings.liveScanLabel,
                    color = scannerColors.buttonLabelColor,
                    style = scannerTextStyles.buttonLabelTextStyle
                )
            }
        }
        if (ocrScannerState.capturedImage != null) {
            Surface(
                modifier = Modifier
                    .constrainAs(picturePreviewBackground) {
                        top.linkTo(parent.top)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                        bottom.linkTo(parent.bottom)
                    },
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                // Empty box covering the entire screen as background for the preview
                Box(modifier = Modifier.fillMaxSize())
            }
            TopAppBar(
                modifier = Modifier
                    .constrainAs(topAppBar) {
                        top.linkTo(parent.top)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                    },
                windowInsets = WindowInsets(0.dp),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    actionIconContentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ),
                title = {
                    Text(
                        text = "Results"
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackPressed
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Default.ArrowBack,
                            contentDescription = null
                        )
                    }
                }
            )
            Image(
                modifier = Modifier
                    .constrainAs(picturePreviewImage) {
                        top.linkTo(topAppBar.bottom)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                        bottom.linkTo(bottomSheet.top)
                        width = Dimension.fillToConstraints
                        height =Dimension.fillToConstraints
                    },
                painter = BitmapPainter(image = ocrScannerState.capturedImage.toImageBitmap()),
                contentDescription = null,
                contentScale = ContentScale.Fit
            )
        }

// Calculate the target height based on state and drag
        val targetHeight = when {
            isDragging -> when (sheetState.value) {
                SheetState.CONTENT -> {
                    // When dragging from CONTENT state, start at maxBottomSheetHeight and add positive offset (moving toward fullScreenHeight)
                    val base = maxBottomSheetHeight.value
                    val target = fullScreenHeight.value
                    val current = (base + (target - base) * (-dragOffset / 500f)).coerceIn(base, target)
                    current.dp
                }
                SheetState.FULL -> {
                    // When dragging from FULL state, start at fullScreenHeight and add negative offset (moving toward maxBottomSheetHeight)
                    val base = fullScreenHeight.value
                    val target = maxBottomSheetHeight.value
                    val current = (base - (base - target) * (dragOffset / 500f)).coerceIn(target, base)
                    current.dp
                }
                else -> peekHeight // No dragging allowed in PEEK state
            }
            else -> when (sheetState.value) {
                SheetState.PEEK -> peekHeight
                SheetState.CONTENT -> maxBottomSheetHeight
                SheetState.FULL -> fullScreenHeight
            }
        }

// Animate the height
        val animatedHeight by animateFloatAsState(
            targetValue = targetHeight.value,
            animationSpec = spring(stiffness = 300f, dampingRatio = 0.8f),
            label = "bottomSheetHeight"
        )
        Box(
            modifier = Modifier
                .constrainAs(bottomSheet) {
                    bottom.linkTo(parent.bottom)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    width = Dimension.fillToConstraints
                }
                .height(animatedHeight.dp)
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(scannerColors.bottomDrawerBackground)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = {
                            if (sheetState.value != SheetState.PEEK) {
                                isDragging = true
                                dragOffset = 0f
                            }
                        },
                        onDragEnd = {
                            isDragging = false

                            // Calculate which state to snap to based on current height and drag direction
                            if (sheetState.value == SheetState.CONTENT && dragOffset < -200) {
                                sheetState.value = SheetState.FULL
                            }
                            else if (sheetState.value == SheetState.FULL && dragOffset > 200) {
                                sheetState.value = SheetState.CONTENT
                            }

                            dragOffset = 0f
                        },
                        onDragCancel = {
                            isDragging = false
                            dragOffset = 0f
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()

                            // Only allow dragging in specific states
                            when (sheetState.value) {
                                SheetState.CONTENT -> {
                                    // Only allow dragging up from CONTENT
                                    if (dragAmount.y < 0) {
                                        // Accumulate negative offset when dragging up
                                        dragOffset += dragAmount.y
                                    }
                                }
                                SheetState.FULL -> {
                                    // Only allow dragging down from FULL
                                    if (dragAmount.y > 0) {
                                        // Accumulate positive offset when dragging down
                                        dragOffset += dragAmount.y
                                    }
                                }
                                else -> { /* No dragging in PEEK state */ }
                            }
                        }
                    )
                }
        ) {
            Column(
                modifier = Modifier.padding(top = 16.dp)
                    .onGloballyPositioned { coordinates ->
                        // Measure content height whenever content changes
                        val contentHeight = with(density) { coordinates.size.height.toDp() }

                        // Auto-expand from PEEK to CONTENT if content grows or state changes
                        if (sheetState.value == SheetState.PEEK &&
                            (ocrScannerState.state == OcrScannerState.State.Found ||
                                    ocrScannerState.state == OcrScannerState.State.NotFound ||
                                    contentHeight > peekHeight + 20.dp)) {
                            sheetState.value = SheetState.CONTENT
                        }
                    }
            ) {
                // Drag handle
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        Modifier
                            .width(40.dp)
                            .height(4.dp)
                            .background(
                                scannerColors.gripColor,
                                RoundedCornerShape(2.dp)
                            )
                    )
                }

                if (ocrScannerState.state != OcrScannerState.State.Found && ocrScannerState.state != OcrScannerState.State.NotFound && ocrScannerState.foundItems.isEmpty()) {
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
                if (ocrScannerState.state == OcrScannerState.State.NotFound) {
                    Text(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .fillMaxWidth(),
                        text = "Sorry, we could not detect any what3words address in the photo.",
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center
                    )
                    Button(
                        onClick = { onBackPressed() },
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .fillMaxWidth()
                            .align(Alignment.CenterHorizontally)
                    ) {
                        Text(text = "Try again")
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
    }
}