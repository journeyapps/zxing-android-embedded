package com.journeyapps.barcodescanner

import com.google.zxing.BarcodeFormat
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import java.util.*

/**
 * DecoderFactory that creates a MultiFormatReader with specified hints.
 */
class DefaultDecoderFactory : DecoderFactory {
    private var decodeFormats: Collection<BarcodeFormat>? = null
    private var hints: Map<DecodeHintType, *>? = null
    private var characterSet: String? = null
    private var scanType = 0

    constructor()
    constructor(decodeFormats: Collection<BarcodeFormat>) {
        this.decodeFormats = decodeFormats
    }

    constructor(decodeFormats: Collection<BarcodeFormat>, hints: Map<DecodeHintType, *>, characterSet: String, scanType: Int) {
        this.decodeFormats = decodeFormats
        this.hints = hints
        this.characterSet = characterSet
        this.scanType = scanType
    }

    override fun createDecoder(baseHints: Map<DecodeHintType, *>): Decoder {
        val hints: MutableMap<DecodeHintType, Any?> = EnumMap(DecodeHintType::class.java)
        hints.putAll(baseHints)

        this.hints?.let { hints.putAll(it) }
        this.decodeFormats?.let { hints[DecodeHintType.POSSIBLE_FORMATS] = it }
        characterSet?.let { hints[DecodeHintType.CHARACTER_SET] = it }

        val reader = MultiFormatReader()
        reader.setHints(hints)
        return when (scanType) {
            0 -> Decoder(reader)
            1 -> InvertedDecoder(reader)
            2 -> MixedDecoder(reader)
            else -> Decoder(reader)
        }
    }
}