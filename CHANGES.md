### 2.3.0 (2015-04-01)

* Fix issues with portrait orientation.
* Fix camera id used when resuming (from upstream ZXing)
* Change default result duration to 0.
* Remove more unused code.

### 2.2.0 (2015-03-28)

* Experimental setOrientation() option.

### 2.1.0 (2015-03-20)

* Update to ZXing 3.2.0.
* Removed HelpActivity and EncodeActivity. If there is a need for EncodeActivity, it can be split
  off as a separate lib. Thanks to @rehan-vanzyl.
* Remove some permissions and `<supports-screens>` section from the AndroidManifest.xml.

### 2.0.1 (2014-12-20)

* Changed group name, artifact name, GitHub organization and Maven repository.

### 2.0.0 (2014-06-03)

There are now three libraries:
* zxing-android-minimal (equivalent to Barcode Scanner 4.7.0)
* zxing-android-legacy (roughly equivalent to zxing-android-minimal 1.2.1, or Barcode Scanner 4.3.2)
* zxing-android-integration

See the readme for more details.

Other changes:
* Update to zxing core 3.0.1
* Prefix all resources with zxing or zxinglegacy
* Rewrote IntentIntegrator, based on the official IntentIntegrator 3.0.1.


### 1.2.1 (2014-05-16)

* Add support for custom capture layouts thanks to @martar.
* Add some helper methods to IntentIntegrator.
