### 4.1.0 (2020-01-07)

* Ability to hide the laser in ViewfinderView (#503).
* Make possibleResultPoints method in BarcodeCallback optional (#504).
* Ability to customize or disable the permission error dialog (#505).

### 4.0.2 (2019-09-07)

* Use androidx.
* Use zxing:core 3.4.0 by default.
* Minimum SDK version 24.
* Fix ArithmeticException.
* Fix ResultPoint locations when camera is mirrored.

### 4.0.0 / 4.0.1 (2019-09-07)

* Broken release - use 4.0.2.

### 3.6.0 (2018-03-04)

* Use zxing:core 3.3.2 by default (#360).
* Minimum SDK version 19, or 14 by using zxing:core 3.3.0.
* Fix preview race condition (#324).
* Request code can now specified per Intent, instead of globally (#287).
* More helpers to specify barcode formats.
* Allow scanning both inverted and non-inverted barcodes at the same time (alternating) (#326).
* More examples.

### 3.5.0 (2017-03-20)

* Allow changing the REQUEST_CODE value (#234).
* Add support for inverted scans (#235).
* Use zxing:core 3.3.0 by default (#265).

Fixes:

* Fix memory leak when using scan timeout (#283).
* Better handling of various camera errors (#241, #268, #270)

### 3.4.0 (2016-10-16)

Changes:

* Beep on scan is now controlled only by the media volume, and still plays
  even if the device is in "silent mode", as long as the media volume is not muted.
* The 150ms delay after scanning is removed.

Fixes:

* An issue where the beep sometimes played twice is fixed (#221).
* Fix rare crash (#209)
* Fix orientation lock issue (#181)
* Fix race condition with TextureView (#204)


### 3.3.0 (2016-06-05)

* Add an optional timeout to cancel scanning. (#161)
* Rename CompoundBarcodeView to DecoratedBarcodeView.
* Add more internal documentation (comments).

### 3.2.0 (2016-02-06)

* Improved preview scaling strategies, configurable between centerCrop, fitCenter, fitXY (#135)
* Fix issues with Android 6 permission support (#123)
* Fix camera initialization issues, specifically related to orientation changes (#133)
* More control over focus mode (#112)
* Keep drawing viewfinder frame after scanning / pausing (#134)
* More control over torch state, and save the state on orientation change (#136)

### 3.1.0 (2015-12-29)

* Add support for Android 6 runtime permissions (Camera only).
* Experimental support for using TextureView instead of SurfaceView.
* Fix build issues with custom attributes.
* Support library version 23+ is now a requirement.


### 3.1.0 (2015-12-29)

* Initial Android 6 permission supoprt

### 3.0.3 (2015-08-16)

* Fix for preview on Google Glass.
* Make ViewfinderView extensible. (#75)
* Add option to return image of barcode via Intents. (#72)

### 3.0.2 (2015-07-21)

* Add helper class for encoding barcodes.
* Support custom layouts for CompoundBarcodeView.

### 3.0.1 (2015-06-10)

* Fix auto-focus stopping when enabling the torch.
* Fix rendering of BarcodeView / CompoundBarcodeView in layout preview of Android Studio.
* Add option to disable beep via IntentIntegrator.
* Fix some memory leaks with RotationListener (affected some devices only).

### 3.0.0 (2015-05-17)

* First stable release of 3.0.

### 3.0.0-beta4 (2015-05-03)

* Extract functionality out of CaptureActivity into CompoundBarcodeView and CaptureManager.
* Add sample with Lollipop Toolbar/Actionbar (using appcompat library).

### 3.0.0-beta3 (2015-05-03)

* Add preview of scanned image to the result.
* For some warnings for "sending message to a Handler on a dead thread"

### 3.0.0-beta2 (2015-04-10)

* Use DecoderFactory instead of Decoder.
* Fix some orientation issues.

### 3.0.0-beta1 (2015-04-05)

* Major rewrite, allowing the library to be embedded in other Activities. See the new README for
  details.

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
