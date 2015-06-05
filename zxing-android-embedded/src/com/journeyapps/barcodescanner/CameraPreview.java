package com.journeyapps.barcodescanner;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup;
import android.view.WindowManager;

import com.google.zxing.client.android.R;
import com.journeyapps.barcodescanner.camera.CameraInstance;
import com.journeyapps.barcodescanner.camera.CameraSettings;
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
 * 1. resume() - initialize the camera and start the preview. Call from the Activity's onResume().
 * 2. pause() - stop the preview and release any resources. Call from the Activity's onPause().
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
    public interface StateListener {
        /**
         * Preview and frame sizes are determined.
         */
        void previewSized();

        /**
         * Preview has started.
         */
        void previewStarted();

        /**
         * Preview has stopped.
         */
        void previewStopped();

        /**
         * The camera has errored, and cannot display a preview.
         *
         * @param error the error
         */
        void cameraError(Exception error);
    }

    private static final String TAG = CameraPreview.class.getSimpleName();

    private CameraInstance cameraInstance;

    private WindowManager windowManager;

    private Handler stateHandler;

    private SurfaceView surfaceView;

    private boolean previewActive = false;

    private RotationListener rotationListener;

    private List<StateListener> stateListeners = new ArrayList<>();

    private DisplayConfiguration displayConfiguration;
    private CameraSettings cameraSettings = new CameraSettings();

    // Size of this container, non-null after layout is performed
    private Size containerSize;

    // Size of the preview resolution
    private Size previewSize;

    // Rect placing the preview surface
    private Rect surfaceRect;

    // Size of the current surface. non-null if the surface is ready
    private Size currentSurfaceSize;

    // Framing rectangle relative to this view
    private Rect framingRect = null;

    // Framing rectangle relative to the preview resolution
    private Rect previewFramingRect = null;


    private final SurfaceHolder.Callback surfaceCallback = new SurfaceHolder.Callback() {

        @Override
        public void surfaceCreated(SurfaceHolder holder) {

        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            currentSurfaceSize = null;
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            if (holder == null) {
                Log.e(TAG, "*** WARNING *** surfaceChanged() gave us a null surface!");
                return;
            }
            currentSurfaceSize = new Size(width, height);
            startPreviewIfReady();
        }
    };

    private final Handler.Callback stateCallback = new Handler.Callback() {
        @Override
        public boolean handleMessage(Message message) {
            if (message.what == R.id.zxing_prewiew_size_ready) {
                previewSized((Size) message.obj);
                return true;
            } else if (message.what == R.id.zxing_camera_error) {
                Exception error = (Exception) message.obj;

                if (isActive()) {
                    // This check prevents multiple errors from begin passed through.
                    pause();
                    fireState.cameraError(error);
                }
            }
            return false;
        }
    };

    private RotationCallback rotationCallback = new RotationCallback() {
        @Override
        public void onRotationChanged(int rotation) {
            // Make sure this is run on the main thread.
            stateHandler.post(new Runnable() {
                @Override
                public void run() {
                    rotationChanged();
                }
            });
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
        if (getBackground() == null) {
            // Default to SurfaceView colour, so that there are less changes.
            setBackgroundColor(Color.BLACK);
        }

        windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);

        stateHandler = new Handler(stateCallback);

        setupSurfaceView();

        rotationListener = new RotationListener();
    }

    private void rotationChanged() {
        pause();
        resume();
    }

    private void setupSurfaceView() {
        surfaceView = new SurfaceView(getContext());
        if (Build.VERSION.SDK_INT < 11) {
            surfaceView.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        }
        surfaceView.getHolder().addCallback(surfaceCallback);
        addView(surfaceView);
    }

    /**
     * Add a listener to be notified of changes to the preview state, as well as camera errors.
     *
     * @param listener the listener
     */
    public void addStateListener(StateListener listener) {
        stateListeners.add(listener);
    }

    private final StateListener fireState = new StateListener() {
        @Override
        public void previewSized() {
            for (StateListener listener : stateListeners) {
                listener.previewSized();
            }
        }

        @Override
        public void previewStarted() {
            for (StateListener listener : stateListeners) {
                listener.previewStarted();
            }

        }

        @Override
        public void previewStopped() {
            for (StateListener listener : stateListeners) {
                listener.previewStopped();
            }
        }

        @Override
        public void cameraError(Exception error) {
            for (StateListener listener : stateListeners) {
                listener.cameraError(error);
            }
        }
    };

    private void calculateFrames() {
        if (containerSize == null || previewSize == null || displayConfiguration == null) {
            previewFramingRect = null;
            framingRect = null;
            surfaceRect = null;
            throw new IllegalStateException("containerSize or previewSize is not set yet");
        }

        int previewWidth = previewSize.width;
        int previewHeight = previewSize.height;

        int width = containerSize.width;
        int height = containerSize.height;

        surfaceRect = displayConfiguration.scalePreview(previewSize);

        Rect container = new Rect(0, 0, width, height);
        framingRect = calculateFramingRect(container, surfaceRect);
        Rect frameInPreview = new Rect(framingRect);
        frameInPreview.offset(-surfaceRect.left, -surfaceRect.top);

        previewFramingRect = new Rect(frameInPreview.left * previewWidth / surfaceRect.width(),
                frameInPreview.top * previewHeight / surfaceRect.height(),
                frameInPreview.right * previewWidth / surfaceRect.width(),
                frameInPreview.bottom * previewHeight / surfaceRect.height());

        if (previewFramingRect.width() <= 0 || previewFramingRect.height() <= 0) {
            previewFramingRect = null;
            framingRect = null;
            Log.w(TAG, "Preview frame is too small");
        } else {
            fireState.previewSized();
        }
    }

    /**
     * Call this on the main thread, while the preview is running.
     *
     * @param on true to turn on the torch
     */
    public void setTorch(boolean on) {
        if (cameraInstance != null) {
            cameraInstance.setTorch(on);
        }
    }

    private void containerSized(Size containerSize) {
        this.containerSize = containerSize;
        if (cameraInstance != null) {
            if (cameraInstance.getDisplayConfiguration() == null) {
                displayConfiguration = new DisplayConfiguration(getDisplayRotation(), containerSize);
                cameraInstance.setDisplayConfiguration(displayConfiguration);
                cameraInstance.configureCamera();
            }
        }
    }

    private void previewSized(Size size) {
        this.previewSize = size;
        if (containerSize != null) {
            calculateFrames();
            requestLayout();
            startPreviewIfReady();
        }
    }

    private void startPreviewIfReady() {
        if (currentSurfaceSize != null && previewSize != null && surfaceRect != null) {
            if (currentSurfaceSize.equals(new Size(surfaceRect.width(), surfaceRect.height()))) {
                startCameraPreview(surfaceView.getHolder());
            } else {
                // Surface is not the correct size yet
            }
        }
    }

    @SuppressLint("DrawAllocation")
    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        containerSized(new Size(r - l, b - t));

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
     * @return the CameraSettings currently in use
     */
    public CameraSettings getCameraSettings() {
        return cameraSettings;
    }

    /**
     * Set the CameraSettings. Use this to select a different camera, change exposure and torch
     * settings, and some other options.
     *
     * This has no effect if the camera is already open.
     *
     * @param cameraSettings the new settings
     */
    public void setCameraSettings(CameraSettings cameraSettings) {
        this.cameraSettings = cameraSettings;
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
        Log.d(TAG, "resume()");

        // initCamera() does nothing if called twice, but does log a warning
        initCamera();

        if (currentSurfaceSize != null) {
            // The activity was paused but not stopped, so the surface still exists. Therefore
            // surfaceCreated() won't be called, so init the camera here.
            startPreviewIfReady();
        } else {
            // Install the callback and wait for surfaceCreated() to init the camera.
            surfaceView.getHolder().addCallback(surfaceCallback);
        }

        // To trigger surfaceSized again
        requestLayout();
        rotationListener.listen(getContext(), rotationCallback);
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
        Log.d(TAG, "pause()");

        if (cameraInstance != null) {
            cameraInstance.close();
            cameraInstance = null;
            previewActive = false;
        }
        if (currentSurfaceSize == null) {
            SurfaceHolder surfaceHolder = surfaceView.getHolder();
            surfaceHolder.removeCallback(surfaceCallback);
        }

        this.containerSize = null;
        this.previewSize = null;
        this.previewFramingRect = null;
        rotationListener.stop();

        fireState.previewStopped();
    }

    /**
     * Considered active if between resume() and pause().
     *
     * @return true if active
     */
    protected boolean isActive() {
        return cameraInstance != null;
    }

    private int getDisplayRotation() {
        return windowManager.getDefaultDisplay().getRotation();
    }

    private void initCamera() {
        if (cameraInstance != null) {
            Log.w(TAG, "initCamera called twice");
            return;
        }

        cameraInstance = new CameraInstance(getContext());
        cameraInstance.setCameraSettings(cameraSettings);

        cameraInstance.setReadyHandler(stateHandler);
        cameraInstance.open();
    }


    private void startCameraPreview(SurfaceHolder holder) {
        if (!previewActive) {
            Log.i(TAG, "Starting preview");
            cameraInstance.setSurfaceHolder(holder);
            cameraInstance.startPreview();
            previewActive = true;

            previewStarted();
            fireState.previewStarted();
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
     * @param surface   the SurfaceView, relative to this container
     * @return the framing rect, relative to this container
     */
    protected Rect calculateFramingRect(Rect container, Rect surface) {
        Rect intersection = new Rect(container);
        intersection.intersect(surface);

        // margin as 10% of the smaller of width, height
        int margin = Math.min(intersection.width() / 10, intersection.height() / 10);
        intersection.inset(margin, margin);
        if (intersection.height() > intersection.width()) {
            // We don't want a frame that is taller than wide.
            intersection.inset(0, (intersection.height() - intersection.width()) / 2);
        }
        return intersection;
    }
}
