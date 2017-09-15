package com.journeyapps.barcodescanner;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;

import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;

/**
 * DecoderFactory that creates a MultiFormatReader with specified hints.
 */
public class DefaultDecoderFactory implements DecoderFactory {
    private Collection<BarcodeFormat> decodeFormats;
    private Map<DecodeHintType, ?> hints;
    private String characterSet;
<<<<<<< HEAD
    private int scanType;
=======
    private boolean inverted;
>>>>>>> parent of cf4c0dc... Added a mixed scan mode to enable scanning inverted and normal barcodes

    public DefaultDecoderFactory() {
    }

<<<<<<< HEAD
    public DefaultDecoderFactory(Collection<BarcodeFormat> decodeFormats, Map<DecodeHintType, ?> hints, String characterSet, int scanType) {
        this.decodeFormats = decodeFormats;
        this.hints = hints;
        this.characterSet = characterSet;
        this.scanType = scanType;
=======
    public DefaultDecoderFactory(Collection<BarcodeFormat> decodeFormats, Map<DecodeHintType, ?> hints, String characterSet, boolean inverted) {
        this.decodeFormats = decodeFormats;
        this.hints = hints;
        this.characterSet = characterSet;
        this.inverted = inverted;
>>>>>>> parent of cf4c0dc... Added a mixed scan mode to enable scanning inverted and normal barcodes
    }

    @Override
    public Decoder createDecoder(Map<DecodeHintType, ?> baseHints) {
        Map<DecodeHintType, Object> hints = new EnumMap<>(DecodeHintType.class);

        hints.putAll(baseHints);

        if(this.hints != null) {
            hints.putAll(this.hints);
        }

        if(this.decodeFormats != null) {
            hints.put(DecodeHintType.POSSIBLE_FORMATS, decodeFormats);
        }

        if (characterSet != null) {
            hints.put(DecodeHintType.CHARACTER_SET, characterSet);
        }

        MultiFormatReader reader = new MultiFormatReader();
        reader.setHints(hints);

<<<<<<< HEAD
        switch (scanType){
            case 0:
                return new Decoder(reader);
            case 1:
                return new InvertedDecoder(reader);
            case 2:
                return new MixedDecoder(reader);
            default:
                return new Decoder(reader);


        }

=======
        return inverted ? new InvertedDecoder(reader) : new Decoder(reader);
>>>>>>> parent of cf4c0dc... Added a mixed scan mode to enable scanning inverted and normal barcodes

    }
}
