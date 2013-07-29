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
        compile 'com.embarkmobile:zxing-android-minimal:1.1.4@aar'
    }


## Adding apklib dependency with Maven

Add as a dependency to your Android project's pom.xml:

    <repositories>
        <repository>
            <id>zxing-android-minimal</id>
            <name>ZXing Android Minimal</name>
            <url>https://raw.github.com/embarkmobile/zxing-android-minimal/mvn-repo/maven-repository</url>
            <snapshots>
                <enabled>false</enabled>
            </snapshots>
            <releases>
                <enabled>true</enabled>
            </releases>
        </repository>
    </repositories>

    <dependency>
        <groupId>com.embarkmobile</groupId>
        <artifactId>zxing-android-minimal</artifactId>
        <version>1.1.4</version>
        <type>apklib</type>
        <scope>compile</scope>
    </dependency>


Make sure manifest merging in your project's pom.xml. See [https://github.com/jayway/maven-android-plugin/pull/135](https://github.com/jayway/maven-android-plugin/pull/135) for details.

Alternatively - *only if not using manifest merging* - add to your AndroidManifest.xml:

    <uses-permission android:name="android.permission.CAMERA"/>
    <uses-permission android:name="android.permission.FLASHLIGHT"/>

    <activity android:clearTaskOnLaunch="true" android:configChanges="orientation|keyboardHidden"
            android:name="com.google.zxing.client.android.CaptureActivity" android:screenOrientation="landscape"
            android:stateNotNeeded="true" android:theme="@android:style/Theme.NoTitleBar.Fullscreen"
            android:windowSoftInputMode="stateAlwaysHidden"/>
    <activity android:name="com.google.zxing.client.android.HelpActivity" android:screenOrientation="user"/>


## Usage

Launch the intent using the bundled IntentIntegrator:

    IntentIntegrator.initiateScan(this);    // `this` is the current Activity or Context

## Building locally

    ./gradlew assemble

To produce .aar artifacts:

    ./gradlew uploadArchives

## License

[Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0)
