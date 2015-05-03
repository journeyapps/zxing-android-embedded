# ZXing Android Embedded

This is an Android library based on the [ZXing Android Barcode Scanner application][2]
for embedding in Android applications. This is not affiliated with the official ZXing project.

Generally it is recommended to scan a barcode [via intents][3].
There are however some cases in which it is not feasible:

* Your users cannot install the Barcode Scanner application.
* You need to customise the barcode scanning logic.
* You need to customise the UI.

In these cases, this library may be more suitable.

## Version 3

Where [version 2][4] was essentially just a stripped-down version of the [Barcode Scanner application][2],
version 3 is a rewrite of a large part of the codebase, making it more versatile and customizable.

With the rewrite, many APIs for UI customization were removed. Instead, it is now recommended
to create a custom Activity using BarcodeView instead.

Other notable changes:
* The camera is now loaded in a background thread, making the activity start faster.
* The camera preview and decoding now functions correctly in any orientation.

## Adding aar dependency with Gradle

From version 3 this is a single library, supporting Gingerbread and later versions of Android
(API level 9+). If you need support for earlier Android versions, use [version 2][4].

Add the following to your build.gradle file:

```groovy
repositories {
    mavenCentral()

    maven {
        url "https://dl.bintray.com/journeyapps/maven"
    }
}

dependencies {
    compile 'com.journeyapps:zxing-android-embedded:3.0.0-beta4@aar'
    compile 'com.google.zxing:core:3.2.0'
}
```

## Usage with Maven

Support for Maven apklib is dropped in version 1.2.0. If you manage to get this working with the
aar in Maven, please create an issue or pull request with instructions.

## Usage

Launch the intent with the default options:
```java
new IntentIntegrator(this).initiateScan(); // `this` is the current Activity
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
integrator.initiateScan();
```

See [IntentIntegrator][6] for more options.

### Changing the orientation

To change the orientation, create a new Activity extending CaptureActivity, and specify the
orientation in your `AndroidManifest.xml`.

Sample:

```java
public class CaptureActivityAnyOrientation extends CaptureActivity {

}
```

```xml
<activity android:name=".CaptureActivityAnyOrientation"
          android:screenOrientation="fullSensor"
          android:stateNotNeeded="true"
          android:theme="@style/zxing_CaptureTheme"
          android:windowSoftInputMode="stateAlwaysHidden">

</activity>
```

```java
IntentIntegrator integrator = new IntentIntegrator(this);
integrator.setCaptureActivity(CaptureActivityAnyOrientation.class);
integrator.setOrientationLocked(false);
integrator.initiateScan();
```

The previous API for `integrator.setOrientation()` was removed. It caused the Activity to be created
in landscape orientation, then destroyed and re-created in the requested orientation, which creates
a bad user experience. The only way around this is to specify the orientation in the manifest.

### Customize the UI

The core of the barcode scanning happens in the BarcodeView component. You can include this in
any Activity. See [CustomCaptureActivity][6] in the sample application for an example.

The API is not stable or documented yet, and will likely change in future releases.

## Building locally

    ./gradlew assemble

To deploy the artifacts the your local Maven repository:

    ./gradlew publishToMavenLocal

You can then use your local version by specifying in your `build.gradle` file:

    repositories {
        mavenLocal()
    }

## Sponsored by

[Journey][1] - Build enterprise mobile apps for iOS and Android. Work in the cloud, code in JavaScript and forget about back-end development.


## License

[Apache License 2.0][7]

[1]: http://journeyapps.com
[2]: https://github.com/zxing/zxing/
[3]: https://github.com/zxing/zxing/wiki/Scanning-Via-Intent
[4]: https://github.com/journeyapps/zxing-android-embedded/blob/v2.3.0/README.md
[5]: zxing-android-embedded/src/com/google/zxing/integration/android/IntentIntegrator.java
[6]: sample/src/main/java/example/zxing/CustomCaptureActivity.java
[7]: http://www.apache.org/licenses/LICENSE-2.0
