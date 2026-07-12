package com.moyettes.legacyvoicechat.audio.codec;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.plasmoverse.opus.OpusDecoder;
import com.plasmoverse.opus.OpusException;

import java.io.IOException;

public class NativeOpusDecoder {
	private static final Logger LOGGER = LogManager.getLogger(NativeOpusDecoder.class);

    private OpusDecoder decoder;
    private final int sampleRate;
    private final int channels;
    private final int frameSize;
    private boolean initialized = false;

    public NativeOpusDecoder(int sampleRate, int frameSize, boolean stereo) {
        this.sampleRate = sampleRate;
        this.channels = stereo ? 2 : 1;
        this.frameSize = frameSize;
    }

    public void initialize() {
        try {
            this.decoder = OpusDecoder.create(sampleRate, channels == 2, frameSize);
            this.initialized = true;
            LOGGER.info("Native Opus decoder initialized successfully");
        } catch (OpusException | IOException e) {
            LOGGER.warn("Failed to initialize native Opus decoder: " + e.getMessage());
            this.initialized = false;
        }
    }

    public short[] decode(byte[] opusData) {
        if (!initialized || decoder == null || opusData == null) {
            return null;
        }

        try {
            return decoder.decode(opusData);
        } catch (OpusException e) {
            LOGGER.warn("Native Opus decoding failed: " + e.getMessage());
            return null;
        }
    }

    public short[] decodeSilence() {
        if (!initialized || decoder == null) {
            return null;
        }

        try {
            return decoder.decode(null);
        } catch (OpusException e) {
            LOGGER.warn("Native Opus silence decoding failed: " + e.getMessage());
            return null;
        }
    }

    public void reset() {
        if (initialized && decoder != null) {
            try {
                decoder.reset();
            } catch (Exception e) {
                LOGGER.warn("Failed to reset native Opus decoder: " + e.getMessage());
            }
        }
    }

    public void close() {
        if (decoder != null) {
            try {
                decoder.close();
            } catch (Exception e) {
                LOGGER.warn("Failed to close native Opus decoder: " + e.getMessage());
            }
            decoder = null;
            initialized = false;
        }
    }

    public boolean isInitialized() {
        return initialized;
    }
}
