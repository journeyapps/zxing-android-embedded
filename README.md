# ZXing Android Embedded

Barcode scanning library for Android, using [ZXing][2] for decoding.

The project is loosely based on the [ZXing Android Barcode Scanner application][2], but is not affiliated with the official ZXing project.

Features:

1. Can be used via Intents (little code required).
2. Can be embedded in an Activity, for advanced customization of UI and logic.
3. Scanning can be performed in landscape or portrait mode.
4. Camera is managed in a background thread, for fast startup time.

A sample application is available in [Releases](https://github.com/journeyapps/zxing-android-embedded/releases).

## Adding aar dependency with Gradle

From version 3 this is a single library, supporting Gingerbread and later versions of Android
(API level 9+). If you need support for earlier Android versions, use [version 2][4].

Add the following to your build.gradle file:

```groovy
repositories {
    jcenter()
}

dependencies {
    compile 'com.journeyapps:zxing-android-embedded:3.3.0@aar'
    compile 'com.google.zxing:core:3.2.1'
    compile 'com.android.support:appcompat-v7:23.1.0'   // Version 23+ is required
}

android {
    buildToolsVersion '23.0.2' // Older versions may give compile errors
}

```

## Usage with IntentIntegrator

Launch the intent with the default options:
```java
new IntentIntegrator(this).initiateScan(); // `this` is the current Activity


// Get the results:
@Override
protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
    if(result != null) {
        if(result.getContents() == null) {
            Toast.makeText(this, "Cancelled", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "Scanned: " + result.getContents(), Toast.LENGTH_LONG).show();
        }
    } else {
        super.onActivityResult(requestCode, resultCode, data);
    }
}
```

Use from a Fragment:
```java
IntentIntegrator.forFragment(this).initiateScan(); // `this` is the current Fragment

// If you're using the support library, use IntentIntegrator.forSupportFragment(this) instead.
```

Customize options:
```java
IntentIntegrator integrator = new IntentIntegrator(this);
integrator.setDesiredBarcodeFormats(IntentIntegrator.ONE_D_CODE_TYPES);
integrator.setPrompt("Scan a barcode");
integrator.setCameraId(0);  // Use a specific camera of the device
integrator.setBeepEnabled(false);
integrator.setBarcodeImageEnabled(true);
integrator.initiateScan();
```

See [IntentIntegrator][5] for more options.

### Changing the orientation

To change the orientation, specify the orientation in your `AndroidManifest.xml` and let the `ManifestMerger` to update the Activity's definition.

Sample:

```xml
<activity
		android:name="com.journeyapps.barcodescanner.CaptureActivity"
		android:screenOrientation="fullSensor"
		tools:replace="screenOrientation" />
```

```java
IntentIntegrator integrator = new IntentIntegrator(this);
integrator.setOrientationLocked(false);
integrator.initiateScan();
```

### Customization and advanced options

See [EMBEDDING](EMBEDDING.md).

For more advanced options, look at the [Sample Application](https://github.com/journeyapps/zxing-android-embedded/blob/master/sample/src/main/java/example/zxing/MainActivity.java),
and browse the source code of the library.

## Android Permissions

The camera permission is required for barcode scanning to function. It is automatically included as
part of the library. On Android 6 it is requested at runtime when the barcode scanner is first opened.

When using BarcodeView directly (instead of via IntentIntegrator / CaptureActivity), you have to
request the permission manually before calling `BarcodeView#resume()`, otherwise the camera will
fail to open.

## Building locally

    ./gradlew assemble

To deploy the artifacts the your local Maven repository:

    ./gradlew publishToMavenLocal

You can then use your local version by specifying in your `build.gradle` file:

    repositories {
        mavenLocal()
    }

## Sponsored by

[JourneyApps][1] - Creating business solutions with mobile apps. Fast.


## License

[Apache License 2.0][7]


[1]: http://journeyapps.com
[2]: https://github.com/zxing/zxing/
[3]: https://github.com/zxing/zxing/wiki/Scanning-Via-Intent
[4]: https://github.com/journeyapps/zxing-android-embedded/blob/2.x/README.md
[5]: zxing-android-embedded/src/com/google/zxing/integration/android/IntentIntegrator.java
[7]: http://www.apache.org/licenses/LICENSE-2.0
