# ZXing Android Minimal

This is a port of the [ZXing Android Barcode Scanner application](http://code.google.com/p/zxing/) as a minimal Android
library project, for embedding in other Android applications. This is not affiliated with the official ZXing project.

Generally it is recommended to scan a barcode [via intents](http://code.google.com/p/zxing/wiki/ScanningViaIntent).
If however that is not possible, you can embed the barcode scanner in your application by using this library.

## Adding aar dependency with Gradle

Add the following to your build.gradle file:

    repositories {
        mavenCentral()

        maven {
            url "https://raw.github.com/embarkmobile/zxing-android-minimal/mvn-repo/maven-repository/"
        }
    }

    dependencies {
        compile 'com.google.zxing:core:2.2'
        compile 'com.embarkmobile:zxing-android-minimal:1.1.4@aar'
    }


## Adding apklib dependency with Maven

Support for Maven apklib is dropped in version 1.2.0.

Use the [1.1.x branch](https://github.com/embarkmobile/zxing-android-minimal/tree/1.1.x) if you need to use this from a Maven project.

## Usage

Launch the intent using the bundled IntentIntegrator:

    IntentIntegrator.initiateScan(this);    // `this` is the current Activity or Context

## Building locally

    ./gradlew assemble

To produce .aar artifacts:

    ./gradlew uploadArchives

## License

[Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0)
