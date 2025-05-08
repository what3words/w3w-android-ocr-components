package com.what3words.ocr.components.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.what3words.core.types.domain.W3WSuggestion
import com.what3words.core.types.domain.isLand
import com.what3words.core.types.geometry.km
import com.what3words.design.library.ui.components.What3wordsAddressListItem
import com.what3words.design.library.ui.components.What3wordsAddressListItemDefaults
import com.what3words.design.library.ui.models.DisplayUnits
import com.what3words.ocr.components.R
import com.what3words.ocr.components.ui.OcrScannerState.ScanningType
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun W3WOcrScannerLiveScanToggle(
    ocrScannerState: OcrScannerState,
    scannerColors: W3WOcrScannerDefaults.Colors,
    scannerStrings: W3WOcrScannerDefaults.Strings,
    scannerTextStyles: W3WOcrScannerDefaults.TextStyles,
    onToggleLiveMode: (Boolean) -> Unit
) {
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
                uncheckedThumbColor = scannerColors.liveSwitchThumbColor,
                uncheckedBorderColor = scannerColors.liveSwitchUncheckedBorderColor
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

@Composable
fun W3WOcrScannerShutterButton(
    ocrScannerState: OcrScannerState,
    scannerColors: W3WOcrScannerDefaults.Colors,
    onShutterClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .alpha(if (ocrScannerState.scanningType != ScanningType.Live) 1.0f else 0.4f)
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
                    enabled = ocrScannerState.scanningType != ScanningType.Live,
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
}

@Composable
fun W3WOcrScannerImportButton(
    scannerColors: W3WOcrScannerDefaults.Colors,
    scannerStrings: W3WOcrScannerDefaults.Strings,
    scannerTextStyles: W3WOcrScannerDefaults.TextStyles,
    onImport: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedIconButton(
            onClick = onImport,
            modifier = Modifier.size(48.dp),
            border = BorderStroke(
                width = 2.dp,
                color = scannerColors.importButtonBorderColor
            ),
            colors = IconButtonDefaults.outlinedIconButtonColors(containerColor = scannerColors.importButtonBackgroundColor)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_import),
                contentDescription = scannerStrings.importButtonLabel,
                tint = scannerColors.importButtonIconColor
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = scannerStrings.importButtonLabel,
            color = scannerColors.buttonLabelColor,
            style = scannerTextStyles.buttonLabelTextStyle
        )
    }
}

/**
 * Composable function that displays the list of found what3words addresses in the bottom sheet
 * when the scanner state is [OcrScannerState.State.Found].
 *
 * It uses a [LazyColumn] to efficiently display potentially long lists of suggestions.
 * Each suggestion is rendered using [What3wordsAddressListItem].
 *
 * This composable is intended to be used within a [ColumnScope], typically as the content
 * of the bottom sheet in the OCR scanner UI.
 *
 * @param ocrScannerState The current state of the OCR scanner, primarily used here to access the `foundItems` list.
 * @param displayUnits The unit system ([DisplayUnits.METRIC] or [DisplayUnits.IMPERIAL]) to use for displaying distances in the address list items.
 * @param scannerColors An instance of [W3WOcrScannerDefaults.Colors] defining the colors for scanner-specific elements like the list header text.
 * @param scannerStrings An instance of [W3WOcrScannerDefaults.Strings] containing localized strings, such as the title for the "Found" state (`scanStateFoundTitle`).
 * @param scannerTextStyles An instance of [W3WOcrScannerDefaults.TextStyles] defining text styles for scanner-specific elements, like the list header.
 * @param suggestionTextStyles An instance of [What3wordsAddressListItemDefaults.TextStyles] defining the text styles for the individual address items in the list.
 * @param suggestionColors An instance of [What3wordsAddressListItemDefaults.Colors] defining the colors for the individual address items in the list.
 * @param suggestionNearestPlacePrefix A string prefix to be displayed before the nearest place information (e.g., "near"). Defaults to the resource string R.string.near.
 * @param onSuggestionSelected A lambda function that is invoked when the user clicks on a specific what3words address suggestion in the list. It receives the selected [W3WSuggestion].
 */
@Composable
fun ColumnScope.OCRScannerFoundContent(
    ocrScannerState: OcrScannerState,
    displayUnits: DisplayUnits,
    scannerColors: W3WOcrScannerDefaults.Colors,
    scannerStrings: W3WOcrScannerDefaults.Strings,
    scannerTextStyles: W3WOcrScannerDefaults.TextStyles,
    suggestionTextStyles: What3wordsAddressListItemDefaults.TextStyles,
    suggestionColors: What3wordsAddressListItemDefaults.Colors,
    suggestionNearestPlacePrefix: String? = stringResource(id = R.string.near),
    onSuggestionSelected: (W3WSuggestion) -> Unit
) {
    LazyColumn(modifier = Modifier.testTag("ocrFoundList")) {
        item {
            Text(
                modifier = Modifier
                    .padding(start = 18.dp),
                style = scannerTextStyles.listHeaderTextStyle,
                color = scannerColors.listHeaderTextColor,
                text = scannerStrings.scanStateFoundTitle
            )
        }
        items(
            count = ocrScannerState.foundItems.size,
            key = { ocrScannerState.foundItems[it].w3wAddress.words },
        ) { index ->
            val item = ocrScannerState.foundItems[index]
            What3wordsAddressListItem(
                modifier = Modifier
                    .testTag("itemOCR ${item.w3wAddress.words}")
                    .animateItem(fadeInSpec = null, fadeOutSpec = null),
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
                showDivider = ocrScannerState.foundItems.lastIndex != index
            )
        }
    }
}

/**
 * Composable function that displays information about the current state of the OCR scanner.
 * It shows different titles based on the `ocrScannerState` (e.g., "Scanning...", "Validating...").
 * This composable is typically shown in the bottom sheet when the scanner is actively processing
 * but hasn't found results yet or is in an intermediate state.
 *
 * This composable is intended to be used within a [ColumnScope], as it's designed to be part of a column layout.
 *
 * @param ocrScannerState The current state of the OCR scanner, used to determine which title to display.
 * @param scannerStrings An instance of [W3WOcrScannerDefaults.Strings] containing the localized strings
 *                       for the different scanner states (e.g., scanning, detected, validating).
 * @param scannerTextStyles An instance of [W3WOcrScannerDefaults.TextStyles] defining the text style
 *                          for the state title.
 * @param scannerColors An instance of [W3WOcrScannerDefaults.Colors] defining the color of the state title text.
 */
@Composable
fun ColumnScope.OCRScannerStateInfoContent(
    ocrScannerState: OcrScannerState,
    scannerStrings: W3WOcrScannerDefaults.Strings,
    scannerTextStyles: W3WOcrScannerDefaults.TextStyles,
    scannerColors: W3WOcrScannerDefaults.Colors
) {
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
                    .testTag("ocrStateInfoText")
                    .align(Alignment.Center),
                style = scannerTextStyles.stateTextStyle,
                text = title,
                color = scannerColors.stateTextColor
            )
        }
    }
}

/**
 * Composable function that displays the content shown when no what3words address is found
 * after scanning or importing an image in the OCR scanner.
 * It typically includes a message indicating that no address was found and a button to retry
 * the scan or capture.
 *
 * This composable is intended to be used within a [ColumnScope], as it arranges its elements vertically.
 *
 * @param scannerStrings An instance of [W3WOcrScannerDefaults.Strings] containing the localized strings
 *                       to be displayed, such as the 'not found' message and the 'retry' button label.
 * @param scannerTextStyles An instance of [W3WOcrScannerDefaults.TextStyles] defining the text styles
 *                          for the message displayed.
 * @param onRetryPressed A lambda function that is invoked when the user clicks the 'Try again' button.
 *                       This typically triggers an action like navigating back to the scanning state
 *                       or allowing the user to capture a new photo.
 */
@Composable
fun ColumnScope.OCRScannerNotFoundContent(
    scannerStrings: W3WOcrScannerDefaults.Strings,
    scannerTextStyles: W3WOcrScannerDefaults.TextStyles,
    scannerColors: W3WOcrScannerDefaults.Colors,
    onRetryPressed: () -> Unit
) {
    Text(
        modifier = Modifier
            .testTag("ocrNotFoundText")
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth(),
        text = scannerStrings.notFoundMessage,
        color = scannerColors.notFoundMessageTextColor,
        style = scannerTextStyles.notFoundMessageTextStyle,
        textAlign = TextAlign.Center
    )
    Button(
        onClick = { onRetryPressed() },
        modifier = Modifier
            .testTag("ocrNotFoundRetryButton")
            .padding(start = 16.dp, end = 16.dp, top = 8.dp)
            .fillMaxWidth()
            .align(Alignment.CenterHorizontally)
    ) {
        Text(text = scannerStrings.tryAgainButtonLabel)
    }
}