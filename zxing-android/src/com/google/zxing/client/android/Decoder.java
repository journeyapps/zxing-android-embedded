package com.google.zxing.client.android;

import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.Reader;
import com.google.zxing.Result;
import com.google.zxing.client.android.camera.CameraThread;
import com.google.zxing.common.HybridBinarizer;

/**
 *
 */
public class Decoder {
  private static final String TAG = Decoder.class.getSimpleName();

  private CameraThread.CameraInstance cameraInstance;
  private HandlerThread thread;
  private Handler handler;
  private Reader reader;
  private Handler resultHandler;

  private final Handler.Callback callback = new Handler.Callback() {
    @Override
    public boolean handleMessage(Message message) {
      if (message.what == R.id.zxing_decode) {
        decode((byte[]) message.obj, message.arg1, message.arg2);
      }
      return true;
    }
  };

  public Decoder(CameraThread.CameraInstance cameraInstance, Reader reader, Handler resultHandler) {
    this.cameraInstance = cameraInstance;
    this.reader = reader;
    this.resultHandler = resultHandler;
  }

  public Reader getReader() {
    return reader;
  }

  public void setReader(Reader reader) {
    this.reader = reader;
  }

  public void start() {
    thread = new HandlerThread(TAG);
    thread.start();
    handler = new Handler(thread.getLooper(), callback);
    requestNextPreview();
  }

  public void stop() {
    thread.quit();
  }

  private void requestNextPreview() {
    cameraInstance.requestPreview(handler, R.id.zxing_decode);
  }

  private Result decode(BinaryBitmap bitmap) {
    try {
      if(reader instanceof MultiFormatReader) {
        return ((MultiFormatReader)reader).decodeWithState(bitmap);
      } else {
        return reader.decode(bitmap);
      }
    } catch (Exception e) {
      // Decode error, try again next frame
      return null;
    } finally {
      reader.reset();
    }
  }

  private void decode(byte[] data, int width, int height) {
    long start = System.currentTimeMillis();
    Result rawResult = null;
    PlanarYUVLuminanceSource source = cameraInstance.getCameraManager().buildLuminanceSource(data, width, height);
    if (source != null) {
      BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
      rawResult = decode(bitmap);
    }

    if (rawResult != null) {
      // Don't log the barcode contents for security.
      long end = System.currentTimeMillis();
      Log.d(TAG, "Found barcode in " + (end - start) + " ms");
      if (resultHandler != null) {
        Message message = Message.obtain(resultHandler, R.id.zxing_decode_succeeded, rawResult);
        Bundle bundle = new Bundle();
        message.setData(bundle);
        message.sendToTarget();
      }
    } else {
      if (resultHandler != null) {
        Message message = Message.obtain(resultHandler, R.id.zxing_decode_failed);
        message.sendToTarget();
      }
    }
    requestNextPreview();
  }


}
