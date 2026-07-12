package com.moyettes.legacyvoicechat.audio.codec;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.plasmoverse.opus.OpusEncoder;
import com.plasmoverse.opus.OpusException;

import java.io.IOException;
import java.util.Arrays;

public class NativeOpusEncoder {
	private static final Logger LOGGER = LogManager.getLogger(NativeOpusEncoder.class);

    private OpusEncoder encoder;
    private final int sampleRate;
    private final int channels;
    private final int mtuSize;
    private boolean initialized = false;

    public NativeOpusEncoder(int sampleRate, int mtuSize, boolean stereo, OpusMode opusMode) {
        this.sampleRate = sampleRate;
        this.channels = stereo ? 2 : 1;
        this.mtuSize = mtuSize;
    }

    public void initialize() {
        try {
            com.plasmoverse.opus.OpusMode mode = Arrays.stream(com.plasmoverse.opus.OpusMode.values())
                .filter((element) -> element.getApplication() == OpusMode.VOIP.getApplication())
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Invalid opus application mode"));

            this.encoder = OpusEncoder.create(sampleRate, channels == 2, mtuSize, mode);

            encoder.setBitrate(-1000);

            this.initialized = true;
            LOGGER.info("Native Opus encoder initialized successfully with VOIP mode");
        } catch (OpusException | IOException e) {
            LOGGER.warn("Failed to initialize native Opus encoder: " + e.getMessage());
            this.initialized = false;
        }
    }

    public byte[] encode(short[] samples) {
        if (!initialized || encoder == null) {
            return null;
        }

        try {
            return encoder.encode(samples);
        } catch (OpusException e) {
            LOGGER.warn("Native Opus encoding failed: " + e.getMessage());
            return null;
        }
    }

    public void setBitrate(int bitrate) {
        if (initialized && encoder != null) {
			encoder.setBitrate(bitrate);
		}
    }

    public int getBitrate() {
        if (initialized && encoder != null) {
            try {
                return encoder.getBitrate();
            } catch (OpusException e) {
                return 0;
            }
        }
        return 0;
    }

    public void reset() {
        if (initialized && encoder != null) {
            try {
                encoder.reset();
            } catch (Exception e) {
                LOGGER.warn("Failed to reset native Opus encoder: " + e.getMessage());
            }
        }
    }

    public void close() {
        if (encoder != null) {
            try {
                encoder.close();
            } catch (Exception e) {
                LOGGER.warn("Failed to close native Opus encoder: " + e.getMessage());
            }
            encoder = null;
            initialized = false;
        }
    }

    public boolean isInitialized() {
        return initialized;
    }
}
