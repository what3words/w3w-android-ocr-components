package com.what3words.ocr.components.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import com.what3words.design.library.ui.theme.colors_blue_20
import com.what3words.design.library.ui.theme.w3wColorScheme
import com.what3words.ocr.components.R

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
        val liveSwitchUncheckedBorderColor: Color,
        val importButtonBackgroundColor: Color,
        val importButtonIconColor: Color,
        val importButtonBorderColor: Color,
        val imagePreviewBackgroundColor: Color,
        val resultsTopAppBarContainerColor: Color,
        val resultsTopAppBarNavigationIconContentColor: Color,
        val resultsTopAppBarNavigationTitleContentColor: Color
    )

    /**
     * The default text styles used by W3WOcrScanner.
     */
    data class TextStyles(
        val stateTextStyle: TextStyle,
        val listHeaderTextStyle: TextStyle,
        val buttonLabelTextStyle: TextStyle,
        val scanningTipTextStyle: TextStyle,
        val notFoundMessageTextStyle: TextStyle
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
        val importButtonLabel: String,
        val shutterButtonContentDescription: String,
        val liveScanLabel: String,
        val notFoundMessage: String,
        val tryAgainButtonLabel: String,
        val resultsTitle: String
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
     * @param buttonLabelColor Color for the text labels of the scanner control buttons.
     * @param liveSwitchCheckedTrackColor Color for the live scanning switch track when enabled.
     * @param liveSwitchUncheckedTrackColor Color for the live scanning switch track when disabled.
     * @param liveSwitchThumbColor Color for the live scanning switch thumb.
     * @param liveSwitchUncheckedBorderColor Color for the border of the live scanning switch when disabled.
     * @param importButtonBackgroundColor Color for the image import button's background.
     * @param importButtonIconColor Color for the image import button's icon.
     * @param importButtonBorderColor Color for the border of the image import button.
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
        liveSwitchUncheckedTrackColor: Color = colors_blue_20,
        liveSwitchThumbColor: Color = MaterialTheme.w3wColorScheme.onSurfaceWhite,
        liveSwitchUncheckedBorderColor: Color = MaterialTheme.w3wColorScheme.onSurfaceWhite,
        importButtonBackgroundColor: Color = colors_blue_20,
        importButtonIconColor: Color = MaterialTheme.w3wColorScheme.onSurfaceWhite,
        importButtonBorderColor: Color = MaterialTheme.w3wColorScheme.onSurfaceWhite,
        imagePreviewBackgroundColor: Color = colors_blue_20,
        resultsTopAppBarContainerColor: Color = MaterialTheme.colorScheme.secondaryContainer,
        resultsTopNavigationIconContentColor: Color = MaterialTheme.colorScheme.onSecondaryContainer,
        resultsTopNavigationTitleContentColor: Color = MaterialTheme.colorScheme.onSecondaryContainer,
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
            liveSwitchUncheckedBorderColor = liveSwitchUncheckedBorderColor,
            importButtonBackgroundColor = importButtonBackgroundColor,
            importButtonIconColor = importButtonIconColor,
            importButtonBorderColor = importButtonBorderColor,
            imagePreviewBackgroundColor = imagePreviewBackgroundColor,
            resultsTopAppBarContainerColor = resultsTopAppBarContainerColor,
            resultsTopAppBarNavigationIconContentColor = resultsTopNavigationIconContentColor,
            resultsTopAppBarNavigationTitleContentColor = resultsTopNavigationTitleContentColor
        )
    }

    /**
     * Factory function for creating a default [TextStyles] instance for [W3WOcrScanner].
     * This function allows customization of various text style properties used in the OCR scanner UI components.
     *
     * @param stateTextStyle The text style for the text displaying the current scanner state.
     * @param listHeaderTextStyle The text style for the header of the scanned suggestions list.
     * @param buttonLabelTextStyle The text style for labels beneath scanner control buttons.
     * @param scanningTipTextStyle The text style for scanning tips and guidance text.
     * @param notFoundMessageTextStyle The text style for the message displayed when no what3words address is found.
     * @return A [TextStyles] object with the specified or default text style properties.
     */
    @Composable
    fun defaultTextStyles(
        stateTextStyle: TextStyle = MaterialTheme.typography.titleMedium,
        listHeaderTextStyle: TextStyle = MaterialTheme.typography.titleSmall,
        buttonLabelTextStyle: TextStyle = MaterialTheme.typography.bodyMedium,
        scanningTipTextStyle: TextStyle = MaterialTheme.typography.bodyLarge,
        notFoundMessageTextStyle: TextStyle = MaterialTheme.typography.titleMedium
    ): TextStyles {
        return TextStyles(
            stateTextStyle = stateTextStyle,
            listHeaderTextStyle = listHeaderTextStyle,
            buttonLabelTextStyle = buttonLabelTextStyle,
            scanningTipTextStyle = scanningTipTextStyle,
            notFoundMessageTextStyle = notFoundMessageTextStyle
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
     * @param importButtonLabel The label text for the import button that allows users to import images.
     * @param shutterButtonContentDescription The content description for the shutter button, supporting accessibility features.
     * @param liveScanLabel The label text for the toggle that enables/disables live scanning mode.
     * @param notFoundMessage The message displayed when no what3words address is found in the scan.
     * @param tryAgainButtonLabel The label text for the button that allows users to retry scanning.
     * @param resultsTitle The title text displayed in the results screen after capturing an image.
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
        importButtonLabel: String = stringResource(R.string.import_button_label),
        shutterButtonContentDescription: String = stringResource(R.string.cd_shutter_button),
        liveScanLabel: String = stringResource(R.string.live_scan_label),
        notFoundMessage: String = stringResource(R.string.scan_state_not_found),
        tryAgainButtonLabel: String = stringResource(R.string.retry_button_label),
        resultsTitle: String = stringResource(R.string.results_title)
    ): Strings {
        return Strings(
            scanStateScanningTitle = scanStateScanningTitle,
            scanStateDetectedTitle = scanStateDetectedTitle,
            scanStateValidatingTitle = scanStateValidatingTitle,
            scanStateFoundTitle = scanStateFoundTitle,
            scanStateLoadingTitle = scanStateLoadingTitle,
            closeButtonContentDescription = closeButtonContentDescription,
            importButtonLabel = importButtonLabel,
            shutterButtonContentDescription = shutterButtonContentDescription,
            liveScanLabel = liveScanLabel,
            notFoundMessage = notFoundMessage,
            tryAgainButtonLabel = tryAgainButtonLabel,
            resultsTitle = resultsTitle
        )
    }
}