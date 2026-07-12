package com.moyettes.legacyvoicechat.audio.filter;

import com.moyettes.legacyvoicechat.audio.utils.AudioUtils;

public class GainFilter implements AudioFilter {
    private float volume;

    public GainFilter(float volume) {
        this.volume = volume;
    }

    @Override
    public String getName() {
        return "gain";
    }

    @Override
    public short[] process(short[] samples) {
        if (volume == 1.0f) {
            return samples;
        }

        short highestValue = AudioUtils.getHighestAbsoluteSample(samples);
        if (highestValue == 0) {
            return samples;
        }

        // Prevent clipping
        float maxPossibleMultiplier = (float) (Short.MAX_VALUE - 1) / (float) highestValue;
        float actualVolume = Math.min(volume, maxPossibleMultiplier);

        short[] result = new short[samples.length];
        for (int i = 0; i < samples.length; i++) {
            result[i] = (short) (samples[i] * actualVolume);
        }

        return result;
    }

    @Override
    public boolean isEnabled() {
        return volume != 1.0f;
    }

    @Override
    public int getSupportedChannels() {
        return 2; // Supports both mono and stereo
    }

    public void setVolume(float volume) {
        this.volume = volume;
    }

    public float getVolume() {
        return volume;
    }
}
