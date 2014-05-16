# ZXing Android Minimal

This is a port of the [ZXing Android Barcode Scanner application](http://code.google.com/p/zxing/) as a minimal Android
library project, for embedding in other Android applications. This is not affiliated with the official ZXing project.

Generally it is recommended to scan a barcode [via intents](http://code.google.com/p/zxing/wiki/ScanningViaIntent).
If however that is not possible, you can embed the barcode scanner in your application by using this library.

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
    compile 'com.google.zxing:core:2.2'
    compile 'com.embarkmobile:zxing-android-minimal:1.2.0@aar'
}
```


## Adding apklib dependency with Maven

Support for Maven apklib is dropped in version 1.2.0.

Use the [1.1.x branch](https://github.com/embarkmobile/zxing-android-minimal/tree/1.1.x) if you need to use this from a Maven project.

## Usage

Launch the intent using the bundled IntentIntegrator:
```java
IntentIntegrator.initiateScan(this);    // `this` is the current Activity
```

Use a custom layout:
```java
Intent intent = IntentIntegrator.createScanIntent(this);
IntentIntegrator.setCaptureLayout(intent, layoutResourceId);
IntentIntegrator.startIntent(intent, this);
```

See [doc/custom_capture_layout.xml](doc/custom_capture_layout.xml) for an example of a custom
layout with a cancel button.

Use from a fragment:
```java
Intent intent = IntentIntegrator.createScanIntent(this);    // `this` is the current Fragment
IntentIntegrator.startIntent(intent, this);
```

See [IntentIntegrator](src/com/google/zxing/integration/android/IntentIntegrator.java) for more options.

## Building locally

    ./gradlew assemble

To produce .aar artifacts:

    ./gradlew uploadArchives


## Sponsored by

[Journey][1] - Build enterprise mobile apps for iOS and Android. Work in the cloud, code in JavaScript and forget about back-end development.


## License

[Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0)

[1]: http://journeyapps.com
