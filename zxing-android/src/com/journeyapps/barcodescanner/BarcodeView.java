package com.journeyapps.barcodescanner;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;

import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.Result;
import com.google.zxing.ResultPoint;
import com.google.zxing.client.android.R;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A view for scanning barcodes.
 *
 * Start decoding with decodeSingle() or decodeContinuous(). Stop decoding with stopDecoding().
 *
 * @see CameraPreview for details on the preview lifecycle.
 */
public class BarcodeView extends CameraPreview {

  public static enum DecodeMode {
    NONE,
    SINGLE,
    CONTINUOUS
  };


  private DecodeMode decodeMode = DecodeMode.NONE;
  private BarcodeCallback callback = null;
  private DecoderThread decoderThread;

  private Decoder decoder;


  private Handler resultHandler;

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
        return true;
      } else if(message.what == R.id.zxing_decode_failed) {
        // Failed. Next preview is automatically tried.
        return true;
      } else if(message.what == R.id.zxing_possible_result_points) {
        List<ResultPoint> resultPoints = (List<ResultPoint>) message.obj;
        if(callback != null && decodeMode != DecodeMode.NONE) {
          callback.possibleResultPoints(resultPoints);
        }
        return true;
      }
      return false;
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


  private void initialize() {
    decoder = createDefaultDecoder();
    resultHandler = new Handler(resultCallback);
  }


  /**
   * Set the Decoder to use. Use this for more advanced customization of the decoding process,
   * when Decoder#setReader() is not enough.
   *
   * The decoder's decode method will only be called from a dedicated DecoderThread.
   *
   * Call this from UI thread only.
   *
   * @param decoder the decoder used to decode barcodes.
   */
  public void setDecoder(Decoder decoder) {
    Util.validateMainThread();

    this.decoder = decoder;
    if(this.decoderThread != null) {
      this.decoderThread.setDecoder(decoder);
    }
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
    startDecoderThread();
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
    startDecoderThread();
  }

  /**
   * Stop decoding, but do not stop the preview.
   */
  public void stopDecoding() {
    this.decodeMode = DecodeMode.NONE;
    this.callback = null;
    stopDecoderThread();
  }

  protected Decoder createDefaultDecoder() {
    MultiFormatReader defaultReader = new MultiFormatReader();
    Map<DecodeHintType, Object> hints = new HashMap<>();
    Decoder decoder = new Decoder(defaultReader);
    hints.put(DecodeHintType.NEED_RESULT_POINT_CALLBACK, decoder);
    defaultReader.setHints(hints);
    return decoder;
  }

  private void startDecoderThread() {
    stopDecoderThread(); // To be safe

    if(decodeMode != DecodeMode.NONE && isPreviewActive()) {
      // We only start the thread if both:
      // 1. decoding was requested
      // 2. the preview is active
      decoderThread = new DecoderThread(getCameraInstance(), decoder, resultHandler);
      decoderThread.setCropRect(getPreviewFramingRect());
      decoderThread.start();
    }
  }

  @Override
  protected void previewStarted() {
    super.previewStarted();

    startDecoderThread();
  }

  private void stopDecoderThread() {
    if(decoderThread != null) {
      decoderThread.stop();
      decoderThread = null;
    }
  }

  @Override
  public void pause() {
    stopDecoderThread();

    super.pause();
  }
}
