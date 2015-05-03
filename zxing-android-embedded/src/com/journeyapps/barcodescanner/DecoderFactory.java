package com.journeyapps.barcodescanner;

import com.google.zxing.DecodeHintType;

import java.util.Map;

/**
 * Factory to create Decoder instances. Typically one instance will be created per DecoderThread.
 *
 * @see DefaultDecoderFactory
 */
public interface DecoderFactory {

    /**
     * Create a new Decoder.
     *
     * While this method will only be called from a single thread, the created Decoder will
     * be used from a different thread. Each decoder will only be used from a single thread.
     *
     * @param baseHints default hints. Typically specifies DecodeHintType.NEED_RESULT_POINT_CALLBACK.
     * @return a new Decoder
     */
    Decoder createDecoder(Map<DecodeHintType, ?> baseHints);
}
