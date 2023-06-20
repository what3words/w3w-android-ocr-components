# <img src="https://what3words.com/assets/images/w3w_square_red.png" width="64" height="64" alt="what3words">&nbsp;w3w-android-ocr-components

An Android library to scan what3words address using [MLKit V2](https://developers.google.com/ml-kit/vision/text-recognition/v2/android).

<img src="https://github.com/what3words/w3w-android-ocr-components/blob/documentation/assets/ocr-component-demo.gif" width=40% height=40%>

To obtain an API key, please visit [https://what3words.com/select-plan](https://what3words.com/select-plan) and sign up for an account.

## Installation

The artifact is available through [![Maven Central](https://img.shields.io/maven-central/v/com.what3words/w3w-android-ocr-components.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22com.what3words%22%20AND%20a:%22w3w-android-ocr-components%22)

### Android minumum SDK support
[![Generic badge](https://img.shields.io/badge/minSdk-24-green.svg)](https://developer.android.com/about/versions/marshmallow/android-7.0/)

### Gradle

```
implementation 'com.what3words:w3w-android-ocr-components:1.0.0'
```

## Documentation

Before implementing our MLKit OCR Component, you must add the MLKit libraries to our app. To do that, please follow this [MLKit Android setup steps](https://developers.google.com/ml-kit/vision/text-recognition/v2/android#before_you_begin).

Add the following permissions to your AndroidManifest.xml

```XML
<manifest>
     ...
     <uses-feature android:name="android.hardware.camera.any" />
     <uses-permission android:name="android.permission.CAMERA" />
     <uses-permission android:name="android.permission.INTERNET" />

     <application ...
```

There are two ways to use our MLKit OCR Component:

1. As an Activity, **MLKitOcrScanActivity**, that should be used as an activity for result, which have minimum setup but doesn't allow style customisation. Our library handles all lifecycle and scan flow and will return the selected scanned three word address. Custom localisation and accessibility are available.

2. Using our Jetpack Compose Composable **W3WOcrScanner**, will allow all the above, but the results are returned as a callback (selection and errors) and will enable styling customisation, allowing to override all styles used on our composable with just a couple of extra steps to setup.

### Using MLKitOcrScanActivity (#1)

```Kotlin
class MainActivity : AppCompatActivity() {

     private val resultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            when {
                //registerForActivityResult success with result
                result.resultCode == Activity.RESULT_OK && result.data?.hasExtra(BaseOcrScanActivity.SUCCESS_RESULT_ID) == true -> {
                    val suggestion = result.data!!.serializable<SuggestionWithCoordinates>(BaseOcrScanActivity.SUCCESS_RESULT_ID)
                    if (suggestion != null) {
                        //TODO: Use scanned three word address info
                    }
                }
                //registerForActivityResult canceled with error
                result.resultCode == Activity.RESULT_CANCELED && result.data?.hasExtra(BaseOcrScanActivity.ERROR_RESULT_ID) == true -> {
                    val error = result.data!!.getStringExtra(BaseOcrScanActivity.ERROR_RESULT_ID)
                    if(error != null) {
                        //TODO: Handle error.
                    }
                }
                //registerForActivityResult canceled by user.
                else -> {
                    //TODO: Dismissed by user.
                }
            }
        
    override fun onCreate(savedInstanceState: Bundle?) {
        ...
        val mlKitLibrary = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        //Options to filter the OCR scanning or like this example providing current location for more accurate results/distances to three word addresses.
        val options = AutosuggestOptions().apply { 
            this.focus = Coordinates(51.23, 0.1)
        }
        
        //Per default the scanned three word address will not return coordinate information, if you set returnCoordinates to true when instanciating a new MLKitOcrScanActivity, it will return coordinates and this might results in charge against your API Key.
        val returnCoordinates = true
        
        //MLKitOcrScanActivity.newInstanceWithApi allows to provide all strings to be used internally for localisation and accessibility propuses. 
        val intent = MLKitOcrScanActivity.newInstanceWithApi(
            this,
            W3WOcrWrapper.MLKitLibraries.Latin,
            "YOUR_API_KEY_HERE",
            options,
            returnCoordinates,
            scanStateFoundTitle = "YOUR_STRING_HERE"
        )
        try {
            resultLauncher.launch(intent)
        } catch (e: ExceptionInInitializerError) {
            //TODO: Handle error.
        }
    }
}
```

### Using W3WOcrScanner Composable (#2)

```Kotlin
class MainActivity : ComponentActivity() {
    private lateinit var ocrWrapper: W3WOcrWrapper

    override fun onDestroy() {
        super.onDestroy()
        if (::ocrWrapper.isInitialized) ocrWrapper.stop()
    }
        
    override fun onCreate(savedInstanceState: Bundle?) {
        //This example uses Latin MLKit library, check MLKit documentation of how to instanciate other libraries like Korean, Japanese, Devanagari or Chinese.
        val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        //Options to filter the OCR scanning or like this example providing current location for more accurate results/distances to three word addresses.
        val options = AutosuggestOptions().apply { 
            this.focus = Coordinates(51.23, 0.1)
        }

        //Per default the scanned three word address will not return coordinate information, if you set returnCoordinates to true when instanciating a new MLKitOcrScanActivity, it will return coordinates and this might results in charge against your API Key.
        val returnCoordinates = true

        val dataProvider = What3WordsV3("YOUR_API_KEY_HERE", this)
        ocrWrapper = W3WOcrMLKitWrapper(this, dataProvider, textRecognizer)

        setContent { 
            YourTheme {
                W3WOcrScanner(
                    ocrWrapper,
                    options = options,
                    returnCoordinates = returnCoordinates,
                    onError = { error ->
                        //TODO: Handle error
                    },
                    onDismiss = {
                        //TODO: Dismissed by user.
                    },
                    onSuggestionSelected = { scannedSuggestion ->
                        //TODO: Use scanned three word address info
                    }) 
            }
        }
    }
}
```

### Styling W3WOcrScanner (#2)

```Kotlin
W3WOcrScanner(
    ...
    //optional if you want to override any string of the scanner composable, to allow localisation and accessibility.
    scannerStrings = W3WOcrScannerDefaults.defaultStrings(
        scanStateFoundTitle = stringResource(id = R.string.scan_state_found),
    ),
    //optional if you want to override any colors of the scanner composable.
    scannerColors = W3WOcrScannerDefaults.defaultColors(
        bottomDrawerBackground = W3WTheme.colors.background
    ),
    //optional if you want to override any text styles.
    scannerTextStyles = W3WOcrScannerDefaults.defaultTextStyles(
        stateTextStyle = W3WTheme.typography.headline
    ),
    //optional if you want to override any colors of the scanned list item composable.
    suggestionColors = SuggestionWhat3wordsDefaults.defaultColors(
        background = W3WTheme.colors.background
    ),
    //optional if you want to override any text styles of the scanned list item composable.
    suggestionTextStyles = SuggestionWhat3wordsDefaults.defaultTextStyles(
        wordsTextStyle = W3WTheme.typography.headline
    )
) 
```