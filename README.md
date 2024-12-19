# <img src="https://what3words.com/assets/images/w3w_square_red.png" width="64" height="64" alt="what3words">&nbsp;w3w-android-ocr-components

[![Maven Central](https://img.shields.io/maven-central/v/com.what3words/w3w-android-ocr-components.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22com.what3words%22%20AND%20a:%22w3w-android-ocr-components%22)

An Android library to scan what3words address using [MLKit V2](https://developers.google.com/ml-kit/vision/text-recognition/v2/android).

<img src="https://github.com/what3words/w3w-android-ocr-components/blob/main/assets/ocr-component-demo.gif" width=40% height=40%>

To obtain an API key, please [sign up here](https://what3words.com/select-plan).

### Android minimum SDK support
[![Generic badge](https://img.shields.io/badge/minSdk-24-green.svg)](https://developer.android.com/about/versions/marshmallow/android-7.0/)

### Gradle Dependency
```
implementation 'com.what3words:w3w-android-ocr-components:$latest'
```

## Sample App

This repository includes a [sample app](https://github.com/what3words/w3w-android-samples/tree/main/ocr-sample) demonstrating the usage of the what3words OCR component.

## Usage

Adding a `W3WOcrScanner` to your app:

```kotlin
W3WOcrScanner(
    ocrScanManager = ocrScanManager,
    onDismiss = {
        // Handle scanner dismissal
    },
    onSuggestionSelected = { suggestion ->
        // Handle address selection
    },
    onError = { error ->
        // Handle the error
    },
)
```


<details>
  <summary>Creating an OcrScanManager</summary>

OcrScanManager encapsulates the scanner’s state and logic leveraging `W3WImageDataSource` and `W3WTextDataSource` for scanning and validating addresses.

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
**Setup Options**

One of the significant enhancements in v2.0 is the flexibility to choose and configure different data sources for the OCR component.

- **Online Setup**: Use MLKit for scanning and W3WApiTextDataSource for online validation.
- **Offline Setup**: Replace W3WMLKitImageDataSource with W3WTesseractImageDataSource for offline scanning and W3WSdkTextDataSource for offline validation.
> (Offline setup requires additional licensing; contact us for details.)
</details>

<details>
 <summary>Using OcrScannerState</summary>

If you prefer more control over the scanner's state, you can use `OcrScannerState` directly, instead of using `OcrScanManager`. This allows you to manage the state externally and integrate the component into your existing architecture.

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
        // Scanner has captured a frame, you will need to implement your functions to detect the what3words addresses in the frame and then update the ocrScannerState

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

#### Feature Comparison: OcrScanManager vs. OcrScannerState
| Feature/Aspect                | **OcrScanManager**                                              | **OcrScannerState**                                       |
|-------------------------------|-----------------------------------------------------------------|----------------------------------------------------------|
| **Ease of Use**               | Encapsulates scanner state and logic easier to use out of the box. | Provides more granular control but requires extra setup. |
| **State Management**          | Internal state management is ideal for simple integrations.      | External state management is better for complex apps.      |
| **Customizability**           | Limited to predefined functionalities.                         | High, allows deep customization and integration.         |
| **Use Case**                  | Quick implementation without managing internal details.        | Advanced use cases needing dynamic or external control.  |
| **Flexibility**               | Limited flexibility.        

</details>

<details>
  <summary>Styling W3WOcrScanner</summary>

You can customise the appearance of the `W3WOcrScanner` by providing parameters for strings, colours, and text styles.

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

The OCR component requires camera permission to function. Add the following permission to your app’s `AndroidManifest.xml` file:

```xml
<uses-permission android:name="android.permission.CAMERA" />
```

</details>

## Migrating to v2.0

For guidance on upgrading from v1.x to v2.0, see the migration steps below:

---

### Introduction to the Core Library

The [Core Library](https://github.com/what3words/w3w-core-library) introduces essential models and interfaces that ensure consistency across various what3words libraries. In v2.0 of the OCR component, many existing models have been replaced by those from the Core Library. For more details on the new models, please refer to this [README](https://github.com/what3words/w3w-android-wrapper?tab=readme-ov-file#introduce-of-the-core-library).

---

### Migration steps

<details>
  <summary><b>Step 1: Update Dependencies</b></summary>

Update your app's dependencies to use the latest version of the what3words OCR components:

```gradle
implementation 'com.what3words:w3w-android-ocr-components:$latest'
```

Ensure your build file is synced and the dependencies are updated to avoid conflicts.

</details>

<details> 
    <summary><b>Step 2: Replace Deprecated Models</b></summary>

In v2.0, many models from v1.x have been deprecated in favour of the new models provided by the Core Library. Unfortunately, there is no automated process for this migration. You'll need to manually replace the old models with their counterparts from the Core Library.

</details>

<details>
    <summary><b>Step 3: Replace <code>W3WOcrMLKitWrapper</code> with <code>OcrScanManager</code></b></summary>

In v1.x, you may have used the following setup:

```kotlin
val dataProvider = What3WordsV3(W3W_API_KEY, context)
ocrWrapper = W3WOcrMLKitWrapper(context, textRecognizer)

W3WOcrScanner(
    ocrWrapper,
    dataProvider = dataProvider,
    ....
) 
```

In v2.0, this has been simplified with the introduction of OcrScanManager. Here’s the updated implementation:

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

W3WOcrScanner(
    ocrScanManager = ocrScanManager,
    ...
)
```

The OcrScanManager encapsulates scanning logic and state management, providing a more streamlined API.

</details>

<details>
    <summary><b>Step 4: Removal of <code>MLKitOcrScanActivity</code></b></summary>

MLKitOcrScanActivity has been deprecated. For standalone activity support, leverage Jetpack Compose interoperability. Learn more in [this tutorial](https://developer.android.com/develop/ui/compose/migrate/interoperability-apis/compose-in-views).
</details>