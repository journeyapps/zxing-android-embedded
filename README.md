# ZXing Android Minimal

This is a port of the [ZXing Android Barcode Scanner application](https://github.com/zxing/zxing/) as a minimal Android
library project, for embedding in other Android applications. This is not affiliated with the official ZXing project.

Generally it is recommended to scan a barcode [via intents](http://code.google.com/p/zxing/wiki/ScanningViaIntent).
There are however some cases in which it is not feasible:

* Your users cannot install the Barcode Scanner application.
* You need to customise the barcode scanning logic.
* You need to customise the UI.

In these cases, this library may be more suitable.

## Adding aar dependency with Gradle

Add the following to your build.gradle file:

```groovy
repositories {
    mavenCentral()

    maven {
        url "https://raw.github.com/embarkmobile/zxing-android-minimal/mvn-repo/maven-repository/"
    }
}

dependencies {
    // Supports Android 4.0.3 and later (API level 15)
    compile 'com.embarkmobile:zxing-android-minimal:2.0.0@aar'

    // Supports Android 2.1 and later (API level 7), but not optimal for later Android versions.
    // If you only plan on supporting Android 4.0.3 and up, you don't need to include this.
    compile 'com.embarkmobile:zxing-android-legacy:2.0.0@aar'

    // Convenience library to launch the scanning and encoding Activities.
    // It automatically picks the best scanning library from the above two, depending on the
    // Android version and what is available.
    compile 'com.embarkmobile:zxing-android-integration:2.0.0@aar'

    // Version 3.0.x of zxing core contains some code that is not compatible on Android 2.2 and earlier.
    // This mostly affects encoding, but you should test if you plan to support these versions.
    // Older versions e.g. 2.2 may also work if you need support for older Android versions.
    compile 'com.google.zxing:core:3.0.1'
}
```

### What's the difference between zxing-android-minimal and zxing-android-legacy?

The official ZXing Barcode Scanner application dropped support for older Android versions, and newer
releases only support Android 4.0.3 and newer. If you install it on an older Android version from
the Play Store, you will get an older version of the Barcode Scanner.

In order to keep up to date with the latest changes and improvements from the official project, we
need to do the same. However, many applications still need to support older Android versions.

To cater for these applications, we have two libraries:

1. zxing-android-minimal - This corresponds to the latest Barcode Scanner version.
2. zxing-android-legacy - This corresponds to version 2.2 of the ZXing project -
   the latest version that supported Android 2.2.

`zxing-android-minimal` uses a `zxing` prefix for all its resources, while `zxing-android-legacy`
uses a `zxinglegacy` prefix. It is therefore possible to include both of these in your application.
If you include both, `IntentIntegrator` will automatically choose the best one.

If you only need to support Android 4.0.3 and newer, you only need the `zxing-android-minimal`
library.

It is also possible to only use the `zxing-android-legacy` library, but it does not include the
latest updates and bugfixes from the ZXing project.


## Adding apklib dependency with Maven

Support for Maven apklib is dropped in version 1.2.0.

Use the [1.1.x branch](https://github.com/embarkmobile/zxing-android-minimal/tree/1.1.x) if you need to use this from a Maven project.

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
integrator.setResultDisplayDuration(0);
integrator.setWide();  // Wide scanning rectangle, may work better for 1D barcodes
integrator.setCameraId(0);  // Use a specific camera of the device
integrator.initiateScan();
```

See [IntentIntegrator](integration/src/main/java/com/google/zxing/integration/android/IntentIntegrator.java) for more options.

### Custom Layout

You can provide a custom layout for the capture activity. Note that you'll need to provide two
different layouts if you are using both zxing-android-minimal and zxing-android-legacy.

See [sample/src/main/res/layout/custom_capture_layout.xml](custom_capture_layout.xml) and
[sample/src/main/res/layout/custom_capture_layout.xml](custom_legacy_capture_layout.xml) for
examples.

```java
IntentIntegrator integrator = new IntentIntegrator(this);
integrator.setCaptureLayout(R.layout.custom_layout);
integrator.setLegacyCaptureLayout(R.layout.custom_legacy_layout);
integrator.initiateScan();
```

For a cancel/back button, use the ids `@id/zxing_back_button` for zxing-android-minimal and
 `@id/zxinglegacy_back_button` for zxing-android-legacy.

## Building locally

    ./gradlew assemble

To produce .aar artifacts for a Maven repository:

    ./gradlew uploadArchives


## Sponsored by

[Journey][1] - Build enterprise mobile apps for iOS and Android. Work in the cloud, code in JavaScript and forget about back-end development.


## License

[Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0)

[1]: http://journeyapps.com
