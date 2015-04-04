package com.journeyapps.barcodescanner;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Point;
import android.os.Bundle;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.Result;
import com.google.zxing.ResultMetadataType;
import com.google.zxing.ResultPoint;
import com.google.zxing.client.android.DecodeFormatManager;
import com.google.zxing.client.android.DecodeHintManager;
import com.google.zxing.client.android.Intents;
import com.google.zxing.client.android.R;
import com.journeyapps.barcodescanner.camera.CameraSettings;

import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 */
public class CaptureActivity extends Activity {
  private static final String TAG = CaptureActivity.class.getSimpleName();

  private BarcodeView barcodeView;
  private ViewfinderView viewFinder;
  private TextView statusView;

  private BarcodeCallback callback = new BarcodeCallback() {
    @Override
    public void barcodeResult(Result result) {
      barcodeView.pause();

      returnResult(result);
    }

    @Override
    public void possibleResultPoints(List<ResultPoint> resultPoints) {
      for (ResultPoint point : resultPoints) {
        viewFinder.addPossibleResultPoint(point);
      }
    }
  };

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    Window window = getWindow();
    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

    setContentView(R.layout.zxing_capture2);

    barcodeView = (BarcodeView) findViewById(R.id.zxing_barcode_surface);
    barcodeView.decodeSingle(callback);

    viewFinder = (ViewfinderView)findViewById(R.id.zxing_viewfinder_view);
    viewFinder.setCameraPreview(barcodeView);

    statusView = (TextView) findViewById(R.id.zxing_status_view);

    if(getIntent() != null && Intents.Scan.ACTION.equals(getIntent().getAction())) {
      initializeFromIntent(getIntent());
    }
  }

  private void initializeFromIntent(Intent intent) {
    // Scan the formats the intent requested, and return the result to the calling activity.
    Set<BarcodeFormat> decodeFormats = DecodeFormatManager.parseDecodeFormats(intent);
    Map<DecodeHintType, Object> decodeHints = DecodeHintManager.parseDecodeHints(intent);

    CameraSettings settings = new CameraSettings();

    int orientation = intent.getIntExtra(Intents.Scan.ORIENTATION, ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
    if(orientation == ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) {
      // Lock to landscape or reverse landscape
      //noinspection ResourceType
      setRequestedOrientation(getCurrentLandscapeOrientation());
    } else {
      //noinspection ResourceType
      setRequestedOrientation(orientation);
    }

    if (intent.hasExtra(Intents.Scan.CAMERA_ID)) {
      int cameraId = intent.getIntExtra(Intents.Scan.CAMERA_ID, -1);
      if (cameraId >= 0) {
        settings.setRequestedCameraId(cameraId);
      }
    }

    String customPromptMessage = intent.getStringExtra(Intents.Scan.PROMPT_MESSAGE);
    if (customPromptMessage != null) {
      statusView.setText(customPromptMessage);
    }

    String characterSet = intent.getStringExtra(Intents.Scan.CHARACTER_SET);

    MultiFormatReader reader = new MultiFormatReader();
    reader.setHints(decodeHints);

    barcodeView.setCameraSettings(settings);
    barcodeView.setDecoder(createDecoder(decodeFormats, decodeHints, characterSet));
  }

  public static Decoder createDecoder(Collection<BarcodeFormat> decodeFormats, Map<DecodeHintType,?> baseHints, String characterSet) {
    Map<DecodeHintType,Object> hints = new EnumMap<>(DecodeHintType.class);
    if (baseHints != null) {
      hints.putAll(baseHints);
    }

    hints.put(DecodeHintType.POSSIBLE_FORMATS, decodeFormats);

    if (characterSet != null) {
      hints.put(DecodeHintType.CHARACTER_SET, characterSet);
    }

    MultiFormatReader reader = new MultiFormatReader();

    Decoder decoder = new Decoder(reader);
    hints.put(DecodeHintType.NEED_RESULT_POINT_CALLBACK, decoder);
    reader.setHints(hints);
    return decoder;
  }


  private int getCurrentLandscapeOrientation() {
    int rotation = getWindowManager().getDefaultDisplay().getRotation();
    switch (rotation) {
      case Surface.ROTATION_0:
      case Surface.ROTATION_90:
        return ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
      default:
        return ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
    }
  }

  @Override
  protected void onResume() {
    super.onResume();

    barcodeView.resume();
  }

  @Override
  protected void onPause() {
    super.onPause();

    barcodeView.pause();
  }


  @Override
  public boolean onKeyDown(int keyCode, KeyEvent event) {
    switch (keyCode) {
      case KeyEvent.KEYCODE_FOCUS:
      case KeyEvent.KEYCODE_CAMERA:
        // Handle these events so they don't launch the Camera app
        return true;
      // Use volume up/down to turn on light
      case KeyEvent.KEYCODE_VOLUME_DOWN:
        barcodeView.setTorch(false);
        return true;
      case KeyEvent.KEYCODE_VOLUME_UP:
        barcodeView.setTorch(true);
        return true;
    }
    return super.onKeyDown(keyCode, event);
  }

  public static Intent resultIntent(Result rawResult) {
    Intent intent = new Intent(Intents.Scan.ACTION);
    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
    intent.putExtra(Intents.Scan.RESULT, rawResult.toString());
    intent.putExtra(Intents.Scan.RESULT_FORMAT, rawResult.getBarcodeFormat().toString());
    byte[] rawBytes = rawResult.getRawBytes();
    if (rawBytes != null && rawBytes.length > 0) {
      intent.putExtra(Intents.Scan.RESULT_BYTES, rawBytes);
    }
    Map<ResultMetadataType,?> metadata = rawResult.getResultMetadata();
    if (metadata != null) {
      if (metadata.containsKey(ResultMetadataType.UPC_EAN_EXTENSION)) {
        intent.putExtra(Intents.Scan.RESULT_UPC_EAN_EXTENSION,
                metadata.get(ResultMetadataType.UPC_EAN_EXTENSION).toString());
      }
      Number orientation = (Number) metadata.get(ResultMetadataType.ORIENTATION);
      if (orientation != null) {
        intent.putExtra(Intents.Scan.RESULT_ORIENTATION, orientation.intValue());
      }
      String ecLevel = (String) metadata.get(ResultMetadataType.ERROR_CORRECTION_LEVEL);
      if (ecLevel != null) {
        intent.putExtra(Intents.Scan.RESULT_ERROR_CORRECTION_LEVEL, ecLevel);
      }
      @SuppressWarnings("unchecked")
      Iterable<byte[]> byteSegments = (Iterable<byte[]>) metadata.get(ResultMetadataType.BYTE_SEGMENTS);
      if (byteSegments != null) {
        int i = 0;
        for (byte[] byteSegment : byteSegments) {
          intent.putExtra(Intents.Scan.RESULT_BYTE_SEGMENTS_PREFIX + i, byteSegment);
          i++;
        }
      }
    }
    return intent;
  }

  protected void returnResult(Result rawResult) {
    Intent intent = resultIntent(rawResult);
    setResult(RESULT_OK, intent);
    finish();
  }
}
