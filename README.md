# ZXing Android Minimal

This is a port of the ZXing Android application as a minimal Android library project, for embedding in other Android
applications.

## Usage

Currently the only supported use of this project is through Maven. However, it is not currently published to any
public Maven repository. It is assumed that you are familiar with Android projects on Maven.

### Download and build the project

    git clone git@github.com:embarkmobile/zxing-android-minimal.git
    cd zxing-android-minimal
    mvn clean install

### Add as a dependency to your Android project's pom.xml:

    <dependency>
        <groupId>com.embarkmobile</groupId>
        <artifactId>zxing-android-minimal</artifactId>
        <version>2.2-SNAPSHOT</version>
        <type>apklib</type>
        <scope>compile</scope>
    </dependency>

### Add to your AndroidManifest.xml:

    <activity android:clearTaskOnLaunch="true" android:configChanges="orientation|keyboardHidden"
            android:name="com.google.zxing.client.android.CaptureActivity" android:screenOrientation="landscape"
            android:stateNotNeeded="true" android:theme="@android:style/Theme.NoTitleBar.Fullscreen"
            android:windowSoftInputMode="stateAlwaysHidden"
            android:taskAffinity="com.embarkmobile.zxing"/>
    <activity android:name="com.google.zxing.client.android.HelpActivity" android:screenOrientation="user"
            android:taskAffinity="com.embarkmobile.zxing"/>

### Launch the intent:

        // We explicitly specify the class, so the packaged class is always used, even if BarcodeScanner is also
        // installed.
        Intent intent = new Intent(this, com.google.zxing.client.android.CaptureActivity.class);
        // We still need to explicitly define the action, so CaptureActivity sees us as an external source.
        intent.setAction(com.google.zxing.client.android.Intents.Scan.ACTION);
        startActivity(intent);
