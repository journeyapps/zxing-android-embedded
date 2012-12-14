# ZXing Android Minimal

This is a port of the [http://code.google.com/p/zxing/](ZXing Android Barcode Scanner application) as a minimal Android
library project, for embedding in other Android applications. This is not affiliated with the official ZXing project.

Generally it is recommended to scan a barcode [via intents](http://code.google.com/p/zxing/wiki/ScanningViaIntent).
If however that is not possible, you can embed the barcode scanner in your application by using this library.

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
        <version>1.1.1</version>
        <type>apklib</type>
        <scope>compile</scope>
    </dependency>


### Add to your AndroidManifest.xml:

    <uses-permission android:name="android.permission.CAMERA"/>
    <uses-permission android:name="android.permission.FLASHLIGHT"/>

    <activity android:clearTaskOnLaunch="true" android:configChanges="orientation|keyboardHidden"
            android:name="com.google.zxing.client.android.CaptureActivity" android:screenOrientation="landscape"
            android:stateNotNeeded="true" android:theme="@android:style/Theme.NoTitleBar.Fullscreen"
            android:windowSoftInputMode="stateAlwaysHidden"/>
    <activity android:name="com.google.zxing.client.android.HelpActivity" android:screenOrientation="user"/>

### Alternative: Enable manifest merging in your project's pom.xml

This is a fairly new feature in the maven-android-plugin, and not documented yet.

See [https://github.com/jayway/maven-android-plugin/pull/135](https://github.com/jayway/maven-android-plugin/pull/135) for details.

### Launch the intent using the bundled IntentIntegrator:

        IntentIntegrator.initiateScan(this);

## Deploy

To deploy to your own repository, add the following to your settings.xml (typically in `~/.m2/settings.xml`):

    <profiles>
        <profile>
            <id>repository-properties</id>
            <properties>
                <repo.id>your-repo-id</repo.id>
                <repo.url>https://your/repo/url</repo.url>
                <repo.name>Your Repo Name</repo.name>
            </properties>
        </profile>
    </profiles>

    <activeProfiles>
        <activeProfile>repository-properties</activeProfile>
    </activeProfiles>

    <servers>
        <server>
            <id>your-repo-id</id><!-- must match repo.id above -->
            <username>your-username</username>
            <password>your-password</password>
        </server>
    </servers>

Then run:

    mvn clean deploy

## License

[Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0)
