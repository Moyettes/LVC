package com.moyettes.legacyvoicechat.audio;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.moyettes.legacyvoicechat.audio.filter.*;
import com.moyettes.legacyvoicechat.audio.utils.AudioUtils;
import com.moyettes.legacyvoicechat.audio.codec.NativeOpusEncoder;
import com.moyettes.legacyvoicechat.audio.codec.NativeOpusDecoder;
import com.moyettes.legacyvoicechat.audio.codec.OpusMode;

import java.util.ArrayList;
import java.util.List;

public class AudioProcessor {
	private static final Logger LOGGER = LogManager.getLogger(AudioProcessor.class);


    private static final int SAMPLE_RATE = 48000;
    private static final int FRAME_SIZE_SAMPLES = 960;
    private static final int FRAME_SIZE = FRAME_SIZE_SAMPLES * 2;
    private static final int MAX_PAYLOAD_SIZE = 1024;

    private final byte[] encodeBuffer = new byte[FRAME_SIZE * 2];
    private final byte[] decodeBuffer = new byte[FRAME_SIZE * 2];

    private NativeOpusEncoder encoder;
    private NativeOpusDecoder decoder;
    private boolean initialized;

    private final Object encoderLock = new Object();
    private final Object decoderLock = new Object();

    private final List<AudioFilter> audioFilters = new ArrayList<>();
    private GainFilter gainFilter;
    private NoiseSuppressionFilter noiseFilter;

    public AudioProcessor() {
        try {
            encoder = new NativeOpusEncoder(SAMPLE_RATE, MAX_PAYLOAD_SIZE, false, OpusMode.VOIP);
            decoder = new NativeOpusDecoder(SAMPLE_RATE, FRAME_SIZE_SAMPLES, false);

            encoder.initialize();
            decoder.initialize();

            initializeAudioFilters();

            initialized = encoder.isInitialized() && decoder.isInitialized();

            if (initialized) {
                LOGGER.info("Audio processor initialized");
            } else {
                LOGGER.warn("Failed to initialize native Opus libraries");
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to initialize audio processor: " + e.getMessage());
            encoder = null;
            decoder = null;
            initialized = false;
        }
    }

    private void initializeAudioFilters() {
        this.gainFilter = new GainFilter(1.0f);
        this.noiseFilter = new NoiseSuppressionFilter(false);

        audioFilters.add(gainFilter);
        //audioFilters.add(noiseFilter);
    }

    public byte[] encodeAudio(byte[] rawAudio) {
        if (rawAudio == null || rawAudio.length == 0) {
            return null;
        }

        if (rawAudio.length != FRAME_SIZE && rawAudio.length % 2 != 0) {
            LOGGER.warn("Invalid frame size for encoding: " + rawAudio.length + " bytes (not even, expected " + FRAME_SIZE + ")");
            return null;
        }

        if (!initialized || encoder == null || !encoder.isInitialized()) {
            LOGGER.warn("Native Opus encoder not available");
            return null;
        }

        synchronized (encoderLock) {
            try {
                short[] samples = AudioUtils.bytesToShorts(rawAudio);

                samples = processAudioPipeline(samples);

                byte[] encoded = encoder.encode(samples);
                if (encoded == null || encoded.length == 0) {
                    LOGGER.warn("Native Opus encoding returned null/empty - NOT sending audio");
                    return null;
                }
                return encoded;
            } catch (Exception e) {
                LOGGER.warn("Failed to encode audio with native Opus: " + e.getMessage());
                return null;
            }
        }
    }

    private short[] processAudioPipeline(short[] samples) {
        short[] processed = samples;

        for (AudioFilter filter : audioFilters) {
            if (filter.isEnabled()) {
                processed = filter.process(processed);
            }
        }

        return processed;
    }

    public byte[] decodeAudio(byte[] opusData) {
        if (!initialized || decoder == null || !decoder.isInitialized()) {
            LOGGER.warn("Native Opus decoder not available - NOT playing audio");
            return null;
        }

        synchronized (decoderLock) {
            try {
                short[] decodedSamples = decoder.decode(opusData);
                if (decodedSamples == null || decodedSamples.length == 0) {
                    LOGGER.warn("Native Opus decoding returned null/empty - NOT playing audio");
                    return null;
                }

                return AudioUtils.shortsToBytes(decodedSamples);
            } catch (Exception e) {
                LOGGER.warn("Failed to decode audio with native Opus: " + e.getMessage());
                return null;
            }
        }
    }

    public byte[] decodeSilence() {
        if (!initialized || decoder == null || !decoder.isInitialized()) {
            return null;
        }

        synchronized (decoderLock) {
            try {
                short[] silenceSamples = decoder.decodeSilence();
                if (silenceSamples == null || silenceSamples.length == 0) {
                    return null;
                }

                return AudioUtils.shortsToBytes(silenceSamples);
            } catch (Exception e) {
                LOGGER.warn("Failed to decode silence with native Opus: " + e.getMessage());
                return null;
            }
        }
    }

    public void setMicrophoneGain(float gain) {
        if (gainFilter != null) {
            gainFilter.setVolume(gain);
        }
    }

    public void setNoiseSuppression(boolean enabled) {
        if (noiseFilter != null) {
            if (!enabled) {
                noiseFilter.close();
            }
        }
    }

    public boolean isNoiseSuppressionEnabled() {
        return noiseFilter != null && noiseFilter.isEnabled();
    }

    public double getAudioLevel(byte[] audioData) {
        if (audioData == null || audioData.length == 0) {
            return -127.0;
        }

        return AudioUtils.getHighestAudioLevel(audioData);
    }

    public void reset() {
        if (initialized) {
            synchronized (encoderLock) {
                if (encoder != null) encoder.reset();
            }
            synchronized (decoderLock) {
                if (decoder != null) decoder.reset();
            }
        }
    }

    public void close() {
        if (initialized) {
            synchronized (encoderLock) {
                if (encoder != null) encoder.close();
            }
            synchronized (decoderLock) {
                if (decoder != null) decoder.close();
            }

            // Clean up audio filters
            if (noiseFilter != null) {
                noiseFilter.close();
            }

            initialized = false;
        }
    }

    public void resetEncoder() {
        if (initialized && encoder != null) {
            synchronized (encoderLock) {
                encoder.reset();
            }
        }
    }

    public void resetDecoder() {
        if (initialized && decoder != null) {
            synchronized (decoderLock) {
                decoder.reset();
            }
        }
    }

    public boolean isInitialized() {
        return initialized;
    }

    public static int getSampleRate() {
        return SAMPLE_RATE;
    }

    public static int getFrameSize() {
        return FRAME_SIZE;
    }
}
