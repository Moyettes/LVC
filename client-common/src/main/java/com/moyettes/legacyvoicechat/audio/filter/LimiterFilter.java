package com.moyettes.legacyvoicechat.audio.filter;

import com.moyettes.legacyvoicechat.audio.utils.AudioUtils;

public class LimiterFilter implements AudioFilter {

    private static final float SLOPE = 1F;
    private static final float OUTPUT_GAIN = AudioUtils.dbToMul(0F);
    private static final float DEFAULT_THRESHOLD = -6F;

    private final int sampleRate;
    private final float threshold;
    private float[] envelopeBuf = new float[0];
    private float envelope;

    public LimiterFilter(int sampleRate, float thresholdDb) {
        this.sampleRate = sampleRate;
        this.threshold = thresholdDb;
    }

    @Override
    public String getName() {
        return "limiter";
    }

    @Override
    public short[] process(short[] samples) {
        float[] floatSamples = AudioUtils.shortsToFloatsRange(samples);

        analyzeEnvelope(floatSamples);
        limit(floatSamples);

        return AudioUtils.floatsRangeToShort(floatSamples);
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public int getSupportedChannels() {
        return 1;
    }

    private synchronized void limit(float[] samples) {
        for (int i = 0; i < samples.length; i++) {
            float envDB = AudioUtils.mulToDb(this.envelopeBuf[i]);

            float limiterGain = SLOPE * (threshold - envDB);
            limiterGain = AudioUtils.dbToMul(Math.min(0, limiterGain));

            samples[i] *= limiterGain * OUTPUT_GAIN;
        }
    }

    private synchronized void analyzeEnvelope(float[] samples) {
        this.envelopeBuf = new float[samples.length];

        float attackGain = AudioUtils.gainCoefficient(sampleRate, 0.001F / 1000F);
        float releaseGain = AudioUtils.gainCoefficient(sampleRate, 60F / 1000F);

        float env = this.envelope;
        for (int i = 0; i < samples.length; i++) {
            float envIn = Math.abs(samples[i]);
            if (env < envIn) {
                env = envIn + attackGain * (env - envIn);
            } else {
                env = envIn + releaseGain * (env - envIn);
            }

            this.envelopeBuf[i] = Math.max(this.envelopeBuf[i], env);
        }
        this.envelope = envelopeBuf[samples.length - 1];
    }
}
