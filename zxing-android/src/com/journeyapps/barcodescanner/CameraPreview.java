package com.journeyapps.barcodescanner;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup;

import com.google.zxing.client.android.R;
import com.journeyapps.barcodescanner.camera.CameraInstance;
import com.journeyapps.barcodescanner.camera.DisplayConfiguration;

import java.util.ArrayList;
import java.util.List;

/**
 * CameraPreview is a view that handles displaying of a camera preview on a SurfaceView. It is
 * intended to be used as a base for realtime processing of camera images, e.g. barcode decoding
 * or OCR, although none of this happens in CameraPreview itself.
 *
 * The camera is managed on a separate thread, using CameraInstance.
 *
 * Two methods MUST be called on CameraPreview to manage its state:
 *   1. resume() - initialize the camera and start the preview. Call from the Activity's onResume().
 *   2. pause() - stop the preview and release any resources. Call from the Activity's onPause().
 *
 * Startup sequence:
 *
 * 1. Create SurfaceView.
 * 2. open camera.
 * 2. layout this container, to get size
 * 3. set display config, according to the container size
 * 4. configure()
 * 5. wait for preview size to be ready
 * 6. set surface size according to preview size
 * 7. set surface and start preview
 */
public class CameraPreview extends ViewGroup {
  public static interface StateListener {
    public void previewReady();
  }

  public static enum PreviewScaleMode {
    /**
     * Center the preview inside the BarcodeView. It may contain black bars above and below, or
     * left and right.
     */
    CENTER,

    /**
     * Drop the preview inside the BarcodeView (default). It will fill all available space, but sections may
     * be cut off.
     */
    CROP
  };

  private static final String TAG = CameraPreview.class.getSimpleName();

  private CameraInstance cameraInstance;
  private boolean hasSurface;
  private Activity activity;

  private Handler stateHandler;

  private SurfaceView surfaceView;

  private boolean previewActive = false;

  private RotationListener rotationListener;

  private List<StateListener> stateListeners = new ArrayList<>();

  private PreviewScaleMode previewScaleMode = PreviewScaleMode.CROP;

  private Rect containerRect;
  private Point previewSize;
  private Rect surfaceRect;

  private Rect framingRect = null;
  private Rect previewFramingRect = null;

  private final SurfaceHolder.Callback surfaceCallback = new SurfaceHolder.Callback() {

    @Override
    public void surfaceCreated(SurfaceHolder holder) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
      hasSurface = false;
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
      if (holder == null) {
        Log.e(TAG, "*** WARNING *** surfaceChanged() gave us a null surface!");
        return;
      }
      if(!hasSurface && surfaceRect != null) {
        if(surfaceRect.width() == width && surfaceRect.height() == height) {
          // We're only ready if the surface has the correct size
          hasSurface = true;
          startPreviewIfReady();
        }
      }
    }
  };

  private final Handler.Callback stateCallback = new Handler.Callback() {
    @Override
    public boolean handleMessage(Message message) {
      if(message.what == R.id.zxing_prewiew_size_ready) {
        previewSized((Point)message.obj);
        return true;
      }
      return false;
    }
  };

  public CameraPreview(Context context) {
    super(context);
    initialize(context, null, 0, 0);
  }

  public CameraPreview(Context context, AttributeSet attrs) {
    super(context, attrs);
    initialize(context, attrs, 0, 0);
  }

  public CameraPreview(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    initialize(context, attrs, defStyleAttr, 0);
  }


  private void initialize(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
    if(getBackground() == null) {
      // Default to SurfaceView colour, so that there are less changes.
      setBackgroundColor(Color.BLACK);
    }

    activity = (Activity) context;

    stateHandler = new Handler(stateCallback);

    setupSurfaceView();

    rotationListener = new RotationListener(context) {
      @Override
      public void onRotationChanged(int rotation) {
        rotationChanged();
      }
    };
  }

  private void rotationChanged() {
    pause();
    resume();
  }


  private void setupSurfaceView() {
    surfaceView = new SurfaceView(getContext());
    surfaceView.getHolder().addCallback(surfaceCallback);
    addView(surfaceView);
  }

  public void addStateListener(StateListener listener) {
    stateListeners.add(listener);
  }

  private void firePreviewReady() {
    for (StateListener listener : stateListeners) {
      listener.previewReady();
    }
  }
  public PreviewScaleMode getPreviewScaleMode() {
    return previewScaleMode;
  }

  /**
   * Set the scale mode of the preview, when the aspect ratio is different from the BarcodeView.
   *
   * @param previewScaleMode PreviewScaleMode.CROP or PreviewScaleMode.CENTER
   */
  public void setPreviewScaleMode(PreviewScaleMode previewScaleMode) {
    this.previewScaleMode = previewScaleMode;
  }


  private void calculateFrames() {
    if(containerRect == null || previewSize == null) {
      previewFramingRect = null;
      framingRect = null;
      surfaceRect = null;
      throw new IllegalStateException("containerRect or previewSize is not set yet");
    }

    int previewWidth = previewSize.x;
    int previewHeight = previewSize.y;

    int width = containerRect.width();
    int height = containerRect.height();

    // Either crop or center the SurfaceView.
    boolean center = (previewScaleMode == PreviewScaleMode.CENTER);
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
      Log.w(TAG, "Preview frame is too small");
    } else {
      firePreviewReady();
    }
  }

  private void containerSized(Rect container) {
    this.containerRect = container;
    if(cameraInstance != null) {
      if (cameraInstance.getDisplayConfiguration() == null) {
        cameraInstance.setDisplayConfiguration(new DisplayConfiguration(getDisplayRotation(), new Point(container.width(), container.height())));
        cameraInstance.configureCamera();
      }
    }
  }

  private void previewSized(Point size) {
    this.previewSize = size;
    if(containerRect != null) {
      calculateFrames();
      requestLayout();
      startPreviewIfReady();
    }
  }

  private void startPreviewIfReady() {
    if(hasSurface && previewSize != null) {
      startCameraPreview(surfaceView.getHolder());
    }
  }

  @SuppressLint("DrawAllocation")
  @Override
  protected void onLayout(boolean changed, int l, int t, int r, int b) {
    containerSized(new Rect(0, 0, r - l, b - t));

    if (surfaceRect == null) {
      // Match the container, to reduce the risk of issues. The preview should never be drawn
      // while the surface has this size.
      surfaceView.layout(0, 0, getWidth(), getHeight());
    } else {
      surfaceView.layout(surfaceRect.left, surfaceRect.top, surfaceRect.right, surfaceRect.bottom);
    }
  }

  /**
   * The framing rectangle, relative to this view. Use to draw the rectangle.
   *
   * Will never be null while the preview is active.
   *
   * @return the framing rect, or null
   * @see #isPreviewActive()
   */
  public Rect getFramingRect() {
    return framingRect;
  }

  /**
   * The framing rect, relative to the camera preview resolution.
   *
   * Will never be null while the preview is active.
   *
   * @return the preview rect, or null
   * @see #isPreviewActive()
   */
  public Rect getPreviewFramingRect() {
    return previewFramingRect;
  }

  /**
   * Start the camera preview and decoding. Typically this should be called from the Activity's
   * onResume() method.
   *
   * Call from UI thread only.
   */
  public void resume() {
    // This must be safe to call multiple times
    Util.validateMainThread();

    // initCamera() does nothing if called twice, but does log a warning
    initCamera();

    if (hasSurface) {
      // The activity was paused but not stopped, so the surface still exists. Therefore
      // surfaceCreated() won't be called, so init the camera here.
      startPreviewIfReady();
    } else {
      // Install the callback and wait for surfaceCreated() to init the camera.
      surfaceView.getHolder().addCallback(surfaceCallback);
    }

    // To trigger surfaceSized again
    requestLayout();
    rotationListener.enable();
  }


  /**
   * Pause scanning and the camera preview. Typically this should be called from the Activity's
   * onPause() method.
   *
   * Call from UI thread only.
   */
  public void pause() {
    // This must be safe to call multiple times.
    Util.validateMainThread();

    if(cameraInstance != null) {
      cameraInstance.close();
      cameraInstance = null;
      previewActive = false;
    }
    if (!hasSurface) {
      SurfaceHolder surfaceHolder = surfaceView.getHolder();
      surfaceHolder.removeCallback(surfaceCallback);
    }

    this.containerRect = null;
    this.previewSize = null;
    this.previewFramingRect = null;
    rotationListener.disable();
  }

  private int getDisplayRotation() {
    return activity.getWindowManager().getDefaultDisplay().getRotation();
  }

  private void initCamera() {
    if(cameraInstance != null) {
      Log.w(TAG, "initCamera called twice");
      return;
    }

    cameraInstance = new CameraInstance(getContext());

    cameraInstance.setReadyHandler(stateHandler);
    cameraInstance.open();
  }


  private void startCameraPreview(SurfaceHolder holder) {
    if(!previewActive) {
      Log.i(TAG, "Starting preview");
      cameraInstance.setSurfaceHolder(holder);
      cameraInstance.startPreview();
      previewActive = true;

      previewStarted();
    }
  }

  /**
   * Called when the preview is started. Override this to start decoding work.
   */
  protected void previewStarted() {

  }

  /**
   * Get the current CameraInstance. This may be null, and may change when
   * pausing / resuming the preview.
   *
   * While the preview is active, getCameraInstance() will never be null.
   *
   * @return the current CameraInstance
   * @see #isPreviewActive()
   */
  public CameraInstance getCameraInstance() {
    return cameraInstance;
  }

  /**
   * The preview typically starts being active a while after calling resume(), and stops
   * when calling pause().
   *
   * @return true if the preview is active
   */
  public boolean isPreviewActive() {
    return previewActive;
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
}
