package com.journeyapps.barcodescanner;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;

import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.Result;
import com.google.zxing.ResultPoint;
import com.google.zxing.client.android.FinishListener;
import com.google.zxing.client.android.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 */
public class BarcodeView extends ViewGroup {
  public static interface StateListener {
    public void previewReady();
  }

  public static enum DecodeMode {
    NONE,
    SINGLE,
    CONTINUOUS
  };

  private static final String TAG = BarcodeView.class.getSimpleName();

  private CameraThread.CameraInstance cameraInstance;
  private boolean hasSurface;
  private Activity activity;
  private DecoderThread decoderThread;

  private Handler resultHandler;

  private Decoder decoder;

  private DecodeMode decodeMode = DecodeMode.NONE;
  private BarcodeCallback callback = null;

  private SurfaceView surfaceView;

  private List<StateListener> stateListeners = new ArrayList<>();

  private final SurfaceHolder.Callback surfaceCallback = new SurfaceHolder.Callback() {

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
      if (holder == null) {
        Log.e(TAG, "*** WARNING *** surfaceCreated() gave us a null surface!");
      }
      if (!hasSurface) {
        hasSurface = true;
        initCamera(holder);
      }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
      hasSurface = false;
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }
  };

  public BarcodeView(Context context) {
    super(context);
    initialize();
  }

  public BarcodeView(Context context, AttributeSet attrs) {
    super(context, attrs);
    initialize();
  }

  public BarcodeView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    initialize();
  }

  /**
   * Call from UI thread only.
   *
   * The decoder's decode method will only be called from a dedicated DecoderThread.
   *
   * @param decoder the decoder used to decode barcodes.
   */
  public void setDecoder(Decoder decoder) {
    this.decoder = decoder;
    if(this.decoderThread != null) {
      this.decoderThread.setDecoder(decoder);
    }
  }

  public void addStateListener(StateListener listener) {
    stateListeners.add(listener);
  }

  private void firePreviewReady() {
    for (StateListener listener : stateListeners) {
      listener.previewReady();
    }

    decoderThread.setCropRect(previewFramingRect);
  }

  public Decoder getDecoder() {
    return decoder;
  }

  /**
   * Decode a single barcode, then stop decoding.
   *
   * The callback will only be called on the UI thread.
   *
   * @param callback called with the barcode result, as well as possible ResultPoints
   */
  public void decodeSingle(BarcodeCallback callback) {
    this.decodeMode = DecodeMode.SINGLE;
    this.callback = callback;
  }


  /**
   * Continuously decode barcodes. The same barcode may be returned multiple times per second.
   *
   * The callback will only be called on the UI thread.
   *
   * @param callback called with the barcode result, as well as possible ResultPoints
   */
  public void decodeContinuous(BarcodeCallback callback) {
    this.decodeMode = DecodeMode.CONTINUOUS;
    this.callback = callback;
  }

  public void stopDecoding() {
    this.decodeMode = DecodeMode.NONE;
    this.callback = null;
    // TODO: stop the actual decoding process
  }

  private final Handler.Callback resultCallback = new Handler.Callback() {
    @Override
    public boolean handleMessage(Message message) {
      if(message.what == R.id.zxing_decode_succeeded) {
        Result result = (Result) message.obj;

        if(result != null) {
          if(callback != null && decodeMode != DecodeMode.NONE) {
            callback.barcodeResult(result);
            if(decodeMode == DecodeMode.SINGLE) {
              stopDecoding();
            }
          }
        }
      } else if(message.what == R.id.zxing_decode_failed) {
        // Failed. Next preview is automatically tried.
      } else if(message.what == R.id.zxing_possible_result_points) {
        List<ResultPoint> resultPoints = (List<ResultPoint>) message.obj;
        if(callback != null && decodeMode != DecodeMode.NONE) {
          callback.possibleResultPoints(resultPoints);
        }
      } else if(message.what == R.id.zxing_prewiew_ready) {
        previewSized(getPreviewSize());
      }
      return false;
    }
  };

  protected Decoder createDefaultDecoder() {
    MultiFormatReader defaultReader = new MultiFormatReader();
    Map<DecodeHintType, Object> hints = new HashMap<>();
    Decoder decoder = new Decoder(defaultReader);
    hints.put(DecodeHintType.NEED_RESULT_POINT_CALLBACK, decoder);
    defaultReader.setHints(hints);
    return decoder;
  }

  private void initialize() {
    activity = (Activity) getContext();

    resultHandler = new Handler(resultCallback);

    decoder = createDefaultDecoder();

    surfaceView = new SurfaceView(getContext());
    addView(surfaceView);
  }

  private Point getPreviewSize() {
    if(cameraInstance == null) {
      return null;
    } else {
      return cameraInstance.getCameraManager().getPreviewSize();
    }
  }

  private boolean center = false;

  private Rect containerRect;
  private Point previewSize;
  private Rect surfaceRect;

  private Rect framingRect = null;
  private Rect previewFramingRect = null;

  private void calculateFrames() {
    if(containerRect == null || previewSize == null) {
      previewFramingRect = null;
      framingRect = null;
      surfaceRect = null;
      return;
    }

    int previewWidth = previewSize.x;
    int previewHeight = previewSize.y;

    int width = containerRect.width();
    int height = containerRect.height();


    if (center ^ (width * previewHeight < height * previewWidth)) {
      final int scaledChildWidth = previewWidth * height / previewHeight;
      surfaceRect = new Rect((width - scaledChildWidth) / 2, 0, (width + scaledChildWidth) / 2, height);
    } else {
      final int scaledChildHeight = previewHeight * width / previewWidth;
      surfaceRect = new Rect(0, (height - scaledChildHeight) / 2,
              width, (height + scaledChildHeight) / 2);
    }

    Rect container = new Rect(0, 0, width, height);
    framingRect = calculateFramingRect(container, surfaceRect);
    Rect frameInPreview = new Rect(framingRect);
    frameInPreview.offset(-surfaceRect.left, -surfaceRect.top);

    previewFramingRect = new Rect(frameInPreview.left * previewWidth / surfaceRect.width(),
            frameInPreview.top * previewHeight / surfaceRect.height(),
            frameInPreview.right * previewWidth / surfaceRect.width(),
            frameInPreview.bottom * previewHeight / surfaceRect.height());

    if(previewFramingRect.width() <= 0 || previewFramingRect.height() <= 0) {
      previewFramingRect = null;
      framingRect = null;
    } else {
      firePreviewReady();
    }
  }

  private void containerSized(Rect container) {
    this.containerRect = container;
    calculateFrames();
  }

  private void previewSized(Point size) {
    this.previewSize = size;
    calculateFrames();
    requestLayout();
  }



  @SuppressLint("DrawAllocation")
  @Override
  protected void onLayout(boolean changed, int l, int t, int r, int b) {
    if(changed) {
      containerSized(new Rect(0, 0, r - l, b - t));
    }

    if(surfaceRect == null) {
      // HACK
      surfaceView.layout(0, 0, 1, 1);
    } else {
      surfaceView.layout(surfaceRect.left, surfaceRect.top, surfaceRect.right, surfaceRect.bottom);
    }
  }

  public Rect getFramingRect() {
    return framingRect;
  }

  public Rect getPreviewFramingRect() {
    return previewFramingRect;
  }

  /**
   * Call from UI thread only.
   */
  public void resume() {
    Util.validateMainThread();

    SurfaceHolder surfaceHolder = surfaceView.getHolder();
    if (hasSurface) {
      // The activity was paused but not stopped, so the surface still exists. Therefore
      // surfaceCreated() won't be called, so init the camera here.
      initCamera(surfaceHolder);
    } else {
      // Install the callback and wait for surfaceCreated() to init the camera.
      surfaceHolder.addCallback(surfaceCallback);
    }
  }


  /**
   * Call from UI thread only.
   */
  public void pause() {
    Util.validateMainThread();

    if(decoderThread != null) {
      decoderThread.stop();
      decoderThread = null;
    }
    if(cameraInstance != null) {
      cameraInstance.close();
      cameraInstance = null;
    }
    if (!hasSurface) {
      SurfaceHolder surfaceHolder = surfaceView.getHolder();
      surfaceHolder.removeCallback(surfaceCallback);
    }
  }

  private void initCamera(SurfaceHolder surfaceHolder) {
    if (surfaceHolder == null) {
      throw new IllegalStateException("No SurfaceHolder provided");
    }

    if(cameraInstance != null || decoderThread != null) {
      Log.w(TAG, "initCamera called twice");
      return;
    }

    cameraInstance = CameraThread.getInstance().open(getContext(), surfaceHolder);
    cameraInstance.setReadyHandler(resultHandler);

    decoderThread = new DecoderThread(cameraInstance, decoder, resultHandler);
    decoderThread.start();
  }

  /**
   * Calculate framing rectangle, relative to the preview frame.
   *
   * @param container this container, with left = top = 0
   * @param surface the SurfaceView, relative to this container
   * @return the framing rect, relative to this container
   */
  protected Rect calculateFramingRect(Rect container, Rect surface) {
    Rect intersection = new Rect(container);
    intersection.intersect(surface);

    // margin as 10% of the smaller of width, height
    int margin = Math.min(intersection.width() / 10, intersection.height() / 10);
    intersection.inset(margin, margin);
    if(intersection.height() > intersection.width()) {
      // We don't want a frame that is taller than wide.
      intersection.inset(0, (intersection.height() - intersection.width()) / 2);
    }
    return intersection;
  }

  private void displayFrameworkBugMessageAndExit() {
    AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
    builder.setTitle(getContext().getString(R.string.zxing_app_name));
    builder.setMessage(getContext().getString(R.string.zxing_msg_camera_framework_bug));
    builder.setPositiveButton(R.string.zxing_button_ok, new FinishListener(activity));
    builder.setOnCancelListener(new FinishListener(activity));
    builder.show();
  }
}
