## Embedding BarcodeView

For more control over the UI or scanning behaviour, some components may be used directly:

* BarcodeView: Handles displaying the preview and decoding of the barcodes.
* DecoratedBarcodeView: Combines BarcodeView with a viewfinder for feedback, as well as some status /
  prompt text.
* CaptureManager: Manages the InactivityTimer, BeepManager, orientation lock, and returning of the
  barcode result.

These components can be used from any Activity or Fragment.

Samples:
* [ContinuousCaptureActivity][6]: continuously scan and display results (instead of a once-off scan).
* [ToolbarCaptureActivity][8]: Same as the normal CaptureActivity, but with a Lollipop Toolbar.


## Notes on scaling

On each Android device, the camera has a set list of available preview sizes. When embedding the
barcode scanning along with other components on an Activity, there will almost never be a preview
size that matches up exactly, so we have to pick one and scale and/or crop it.

Also affecting this is that either SurfaceView or TextureView can be used to display the preview.
SurfaceView has better performance, but does not support cropping. TextureView is more powerful,
but has some performance overhead, and is only supported on Android API 14+. We use SurfaceView by
default.

To avoid aspect ratio distortion, we can crop the preview. However, in some combinations of
SurfaceView and other components, the camera preview may end up displaying outside the SurfaceView,
and over other components. This happens especially when:

1. Placing the scanner inside a dialog, or:
2. Other components are placed before the (Decorated)BarcodeView, resulting in a lower z-order.

For these cases we have two solutions:

1. Use TextureView instead of SurfaceView. This may have a performance impact, but solves the above
   issues. Note that this is only available with Android API 14+.

2. Use either `fitCenter` or `fitXY` for scaling, instead of `centerCrop`. Note that `fitCenter` may
   result in black bars next to the preview, and `fitXY` may distort the aspect ratio.
   
The default is to:

1. Use TextureView on Android API 14+, SurfaceView on lower versions.
2. Use `centerCrop` scaling when TextureView is used.
3. Use `fitCenter` if SurfaceView is used.

You can override these options:

```xml
<com.journeyapps.barcodescanner.DecoratedBarcodeView
        android:layout_width="..."
        android:layout_height="..."
        app:zxing_use_texture_view="false" (defaults to true, only has an effect on Android API 14+)
        app:zxing_preview_scaling_strategy="centerCrop"/> (or fitCenter / fitXY)
```

For a full-screen barcode scanner with no Toolbar, the recommended options are:

```
app:zxing_use_texture_view="false"
app:zxing_preview_scaling_strategy="centerCrop"
```


[8]: sample/src/main/java/example/zxing/ToolbarCaptureActivity.java
[6]: sample/src/main/java/example/zxing/ContinuousCaptureActivity.java