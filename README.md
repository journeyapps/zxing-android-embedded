# ZXing Android Embedded

Barcode scanning library for Android, using [ZXing][2] for decoding.

The project is loosely based on the [ZXing Android Barcode Scanner application][2], but is not affiliated with the official ZXing project.

Features:

1. Can be used via Intents (little code required).
2. Can be embedded in an Activity, for advanced customization of UI and logic.
3. Scanning can be performed in landscape or portrait mode.
4. Camera is managed in a background thread, for fast startup time.

A sample application is available in [Releases](https://github.com/journeyapps/zxing-android-embedded/releases).

By default, Android SDK 24+ is required because of `zxing:core` 3.4.0. To support SDK 14+, see [Older SDK versions](#older-sdk-versions). 

## Adding aar dependency with Gradle

From version 4.x, only Android SDK 24+ is supported by default, and androidx is required.

Add the following to your `build.gradle` file:

```groovy
repositories {
    jcenter()
}

dependencies {
    implementation 'com.journeyapps:zxing-android-embedded:4.1.0'
    implementation 'androidx.appcompat:appcompat:1.0.2'
}

android {
    buildToolsVersion '28.0.3' // Older versions may give compile errors
}

```

## Older SDK versions

For Android SDK versions < 24, you can downgrade `zxing:core` to 3.3.0 or earlier for Android 14+ support:

```groovy
repositories {
    jcenter()
}

dependencies {
    implementation('com.journeyapps:zxing-android-embedded:4.1.0') { transitive = false }
    implementation 'androidx.appcompat:appcompat:1.0.2'
    implementation 'com.google.zxing:core:3.3.0'
}

android {
    buildToolsVersion '28.0.3'
}

```
You'll also need this in your Android manifest:

```xml
<uses-sdk tools:overrideLibrary="com.google.zxing.client.android" />
```

No guarantees are made on support for older SDK versions - you'll have to test to make sure it's compatible.

## Hardware Acceleration

Hardware acceleration is required since TextureView is used.

Make sure it is enabled in your manifest file:

```xml
    <application android:hardwareAccelerated="true" ... >
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

### Generate Barcode example

While this is not the primary purpose of this library, it does include basic support for
generating some barcode types:

```java
try {
  BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
  Bitmap bitmap = barcodeEncoder.encodeBitmap("content", BarcodeFormat.QR_CODE, 400, 400);
  ImageView imageViewQrCode = (ImageView) findViewById(R.id.qrCode);
  imageViewQrCode.setImageBitmap(bitmap);
} catch(Exception e) {

}

No customization of the image is currently supported, including changing colors or padding. If you
require more customization, copy and modify the source for the encoder.

```

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

This is considered advanced usage, and is not well-documented or supported.

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

Licensed under the [Apache License 2.0][7]

	Copyright (C) 2012-2018 ZXing authors, Journey Mobile

	Licensed under the Apache License, Version 2.0 (the "License");
	you may not use this file except in compliance with the License.
	You may obtain a copy of the License at

	    http://www.apache.org/licenses/LICENSE-2.0

	Unless required by applicable law or agreed to in writing, software
	distributed under the License is distributed on an "AS IS" BASIS,
	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
	See the License for the specific language governing permissions and
	limitations under the License.



[1]: http://journeyapps.com
[2]: https://github.com/zxing/zxing/
[3]: https://github.com/zxing/zxing/wiki/Scanning-Via-Intent
[4]: https://github.com/journeyapps/zxing-android-embedded/blob/2.x/README.md
[5]: zxing-android-embedded/src/com/google/zxing/integration/android/IntentIntegrator.java
[7]: http://www.apache.org/licenses/LICENSE-2.0
