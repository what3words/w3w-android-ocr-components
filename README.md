# <img src="https://what3words.com/assets/images/w3w_square_red.png" width="64" height="64" alt="what3words">&nbsp;w3w-android-ocr-components

[![Maven Central](https://img.shields.io/maven-central/v/com.what3words/w3w-android-ocr-components.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22com.what3words%22%20AND%20a:%22w3w-android-ocr-components%22)

### Android minimum SDK support
[![Generic badge](https://img.shields.io/badge/minSdk-24-green.svg)](https://developer.android.com/about/versions/marshmallow/android-7.0/)

An Android library to scan what3words address using [MLKit V2](https://developers.google.com/ml-kit/vision/text-recognition/v2/android).

<img src="https://github.com/what3words/w3w-android-ocr-components/blob/main/assets/ocr-component-demo.gif" width=40% height=40%>

To obtain an API key, please visit [https://what3words.com/select-plan](https://what3words.com/select-plan) and sign up for an account.

### Gradle

```
implementation 'com.what3words:w3w-android-ocr-components:$latest'
```

## Sample app

This repository includes a [sample app](https://github.com/what3words/w3w-android-samples/tree/main/ocr-sample) demonstrating the usage of the what3words OCR component.

## Usages

Adding a `W3WOcrScanner` to your app looks like the following:

```kotlin
W3WOcrScanner(
    ocrScanManager = ocrScanManager,
    onDismiss = {},
    onSuggestionSelected = {},
    onError = {},
    onSuggestionFound = {}
)
```
<details>
  <summary>Creating a OcrScanManager</summary>

### Creating a OcrScanManager

OcrScanManager encapsulates the scanner’s state and logic within it. It uses the `W3WImageDataSource` to scan images for possible what3words addresses, and the `W3WTextDataSource` to validate detected addresses.

```kotlin
val w3WImageDataSource = W3WMLKitImageDataSource.create(
    context = context,
    recognizerOptions = TextRecognizerOptionsInterface.LATIN
)

val w3WTextDataSource = W3WApiTextDataSource.create(context, W3W_API_KEY)

val ocrScanManager = rememberOcrScanManager(
    w3wImageDataSource = w3WImageDataSource,
    w3wTextDataSource = w3WTextDataSource,
)

```

</details>

<details>
 <summary>Using OcrScannerState</summary>

### Using OcrScannerState

If you prefer more control over the scanner's state, you can use `OcrScannerState` directly instead of using `OcrScanManager`. This allows you to manage the state externally and integrate the component into your existing architecture.

#### Example Usage
```kotlin
val ocrScannerState = remember { OcrScannerState() }

W3WOcrScanner(
    ocrScannerState = ocrScannerState,
    onError = { error ->
        // Handle the error
    },
    onDismiss = {
        // Handle scanner dismissal
    },
    onFrameCaptured = { image ->
        // Scanner has captured a frame, you will need to implement your own functions to detect the what3words addresses in the frame and then update the ocrScannerState

        CompletableDeferred<Unit>().apply {
            // Signal completion when processing is done
            complete(Unit)
        }
    },
    onSuggestionSelected = { suggestion ->
        // Handle address selection
    },
)
```

#### Comparison Between using OcrScanManager and OcrScannerState directly
| Feature/Aspect                | **OcrScanManager**                                              | **OcrScannerState**                                       |
|-------------------------------|-----------------------------------------------------------------|----------------------------------------------------------|
| **Ease of Use**               | Encapsulates scanner state and logic, easier to use out of the box. | Provides more granular control but requires extra setup. |
| **State Management**          | Internal state management, ideal for simple integrations.      | External state management, better for complex apps.      |
| **Customizability**           | Limited to predefined functionalities.                         | High, allows deep customization and integration.         |
| **Use Case**                  | Quick implementation without managing internal details.        | Advanced use cases needing dynamic or external control.  |
| **Flexibility**               | Limited flexibility.        

</details>

<details>
  <summary>Styling W3WOcrScanner</summary>

### Styling W3WOcrScanner

You can customize the appearance of the `W3WOcrScanner` by providing parameters for strings, colors, and text styles.

```kotlin
W3WOcrScanner(
    ...
    scannerStrings = W3WOcrScannerDefaults.defaultStrings(
        scanStateFoundTitle = stringResource(id = R.string.scan_state_found),
    ),
    scannerColors = W3WOcrScannerDefaults.defaultColors(
        bottomDrawerBackground = W3WTheme.colors.background
    ),
    scannerTextStyles = W3WOcrScannerDefaults.defaultTextStyles(
        stateTextStyle = W3WTheme.typography.headline
    ),
    suggestionColors = SuggestionWhat3wordsDefaults.defaultColors(
        background = W3WTheme.colors.background
    ),
    suggestionTextStyles = SuggestionWhat3wordsDefaults.defaultTextStyles(
        wordsTextStyle = W3WTheme.typography.headline
    )
)
```

</details>

<details>
  <summary>Camera Permission</summary>

### Camera Permission

The OCR component requires camera permission to function. Add the following permission to your app’s `AndroidManifest.xml` file:

```xml
<uses-permission android:name="android.permission.CAMERA" />
```

</details>
