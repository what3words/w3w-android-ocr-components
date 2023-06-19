# <img src="https://what3words.com/assets/images/w3w_square_red.png" width="64" height="64" alt="what3words">&nbsp;w3w-android-ocr-components

An Android library to scan what3words address using [MLKit V2](https://developers.google.com/ml-kit/vision/text-recognition/v2/android).

<img src="https://github.com/what3words/w3w-android-components/blob/master/assets/components-1-new.gif" width=40% height=40%>

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

Before starting to implement our MLKit OCR Component you will need to add the MLKit libraries to our app, to do that please follow this toturial on [MLKit Android before you being steps](https://developers.google.com/ml-kit/vision/text-recognition/v2/android#before_you_begin).


There are two ways to use our MLKit OCR Component:

1. as an Activity, **MLKitOcrScanActivity** to use as activity for result, which have mininum setup but all lifecycle and scan flow is handled by our library and will return the selected scanned three word address, will allow custom localisation and accessibility but is limited on styling customisation, out of the box uses our what3words design system.

2. using our Jetpack Compose Composable **W3WOcrScanner**, this way will allow all the above, but the results are return as a callback (selection and errors) and will allow styling customisation, allowing to override all styles used on our composable with just a couple of extra steps to setup.

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
        //This example uses Latin MLKit library, check MLKit documentation of how to instanciate other libraries like Korean, Japanese, Devanagari or Chinese.
        val mlKitLibrary = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        //Options to filter the OCR scanning or like this example providing current location for more accurate results/distances to three word addresses.
        val options = AutosuggestOptions().apply { 
            this.focus = Coordinates(51.23, 0.1)
        }
        
        //Per default the scanned three word address will not return coordinate information, if you set returnCoordinates to true when instanciating a new MLKitOcrScanActivity, it will return coordinates and this might results in charge against your API Key.
        val returnCoordinates = true
        
        //This can be called on a button click for example. MLKitOcrScanActivity.newInstanceWithApi allows to provide all strings to be used internally for localisation and accessibility propuses. 
        val intent = MLKitOcrScanActivity.newInstanceWithApi(
                this,
                mlKitLibrary,
                "YOUR_WHAT3WORDS_API_KEY_HERE",
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
        val mlKitLibrary = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

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
                //This example uses Latin MLKit library, check MLKit documentation of how to instanciate other libraries like Korean, Japanese, Devanagari or Chinese.

                W3WOcrScanner(
                    ocrWrapper,
                    options = options,
                    returnCoordinates = returnCoordinates,
                    //optional if you want to override any string of the scanner composable, to allow localisation and accessibility.
                    scannerStrings = W3WOcrScannerDefaults.defaultStrings(
                        scanStateFoundTitle = "YOUR_STRING_HERE",
                    ),
                    //optional if you want to override any colors of the scanner composable.
                    scannerColors = W3WOcrScannerDefaults.defaultColors(
                        bottomDrawerBackground = Color.White
                    ),
                    //optional if you want to override any text styles.
                    scannerTextStyles = W3WOcrScannerDefaults.defaultTextStyles(
                        stateTextStyle = TextStyle.Default
                    ),
                    //optional if you want to override any colors of the scanned list item composable.
                    suggestionColors = SuggestionWhat3wordsDefaults.defaultColors(
                        background = Color.White
                    ),
                    //optional if you want to override any text styles of the scanned list item composable.
                    suggestionTextStyles = SuggestionWhat3wordsDefaults.defaultTextStyles(
                        wordsTextStyle = TextStyle.Default
                    ),
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