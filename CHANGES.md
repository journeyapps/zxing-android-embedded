### 2.2.0

* Experimental setOrientation() option.

### 2.1.0

* Update to ZXing 3.2.0.
* Removed HelpActivity and EncodeActivity. If there is a need for EncodeActivity, it can be split
  off as a separate lib. Thanks to @rehan-vanzyl.
* Remove some permissions and `<supports-screens>` section from the AndroidManifest.xml.

### 2.0.1

* Changed group name, artifact name, GitHub organization and Maven repository.

### 2.0.0

There are now three libraries:
* zxing-android-minimal (equivalent to Barcode Scanner 4.7.0)
* zxing-android-legacy (roughly equivalent to zxing-android-minimal 1.2.1, or Barcode Scanner 4.3.2)
* zxing-android-integration

See the readme for more details.

Other changes:
* Update to zxing core 3.0.1
* Prefix all resources with zxing or zxinglegacy
* Rewrote IntentIntegrator, based on the official IntentIntegrator 3.0.1.


### 1.2.1

* Add support for custom capture layouts thanks to @martar.
* Add some helper methods to IntentIntegrator.
