package com.moyettes.legacyvoicechat.audio.filter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.moyettes.legacyvoicechat.audio.utils.AudioUtils;
import com.plasmoverse.rnnoise.Denoise;
import com.plasmoverse.rnnoise.DenoiseException;

public class NoiseSuppressionFilter implements AudioFilter {
	private static final Logger LOGGER = LogManager.getLogger(NoiseSuppressionFilter.class);

    private final boolean enabled;
    private final LimiterFilter limiter;
    private Denoise rnnoiseInstance;

    public NoiseSuppressionFilter(boolean enabled) {
        this.enabled = enabled;
        this.limiter = new LimiterFilter(48000, -6.0f);

        if (enabled) {
            try {
                this.rnnoiseInstance = Denoise.create();
                LOGGER.info("Plasmo Voice RNNoise noise suppression enabled with built-in limiter");
            } catch (Exception e) {
                LOGGER.warn("Plasmo Voice RNNoise not available, noise suppression disabled: " + e.getMessage());
                this.rnnoiseInstance = null;
            }
        }
    }

    @Override
    public String getName() {
        return "noise_suppression";
    }

    @Override
    public short[] process(short[] samples) {
        limiter.process(samples);

        if (!enabled || rnnoiseInstance == null) {
            return samples;
        }

        try {
            float[] floatSamples = AudioUtils.shortsToFloatsRange(samples);

            float[] processed = rnnoiseInstance.process(floatSamples);

            return AudioUtils.floatsRangeToShort(processed);
        } catch (DenoiseException e) {
            LOGGER.warn("Noise suppression failed: " + e.getMessage());
            return samples;
        }
    }

    @Override
    public boolean isEnabled() {
        return enabled && rnnoiseInstance != null;
    }

    @Override
    public int getSupportedChannels() {
        return 1;
    }

    public void close() {
        if (rnnoiseInstance != null) {
            try {
                rnnoiseInstance.getClass().getMethod("close").invoke(rnnoiseInstance);
            } catch (Exception e) {
                LOGGER.warn("Error closing RNNoise instance: " + e.getMessage());
            }
            rnnoiseInstance = null;
        }
    }
}
