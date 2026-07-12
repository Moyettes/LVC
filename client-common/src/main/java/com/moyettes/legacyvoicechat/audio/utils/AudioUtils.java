package com.moyettes.legacyvoicechat.audio.utils;

public class AudioUtils {

    public static double getHighestAudioLevel(byte[] audioData) {
        if (audioData == null || audioData.length == 0) {
            return -127.0;
        }

        long sum = 0;
        int sampleCount = 0;

        for (int i = 0; i < audioData.length - 1; i += 2) {
            int sample = (audioData[i] & 0xFF) | ((audioData[i + 1] & 0xFF) << 8);
            if (sample > 32767) {
                sample -= 65536;
            }
            sum += sample * sample;
            sampleCount++;
        }

        if (sampleCount == 0) {
            return -127.0;
        }

        double rms = Math.sqrt((double) sum / sampleCount);
        return 20.0 * Math.log10(rms / 32768.0);
    }

    public static short getHighestAbsoluteSample(short[] samples) {
        short highest = 0;
        for (short sample : samples) {
            short abs = (short) Math.abs(sample);
            if (abs > highest) {
                highest = abs;
            }
        }
        return highest;
    }

    public static float dbToMul(float db) {
        return Float.isFinite(db) ? (float) Math.pow(10.0F, db / 20.0F) : 0.0F;
    }

    public static float mulToDb(float mul) {
        return (mul == 0.0f) ? -Float.MAX_VALUE : (float) (20.0F * Math.log10(mul));
    }

    public static float gainCoefficient(int sampleRate, float time) {
        return (float) Math.exp(-1.0f / (sampleRate * time));
    }

    public static float[] shortsToFloatsRange(short[] shorts) {
        float[] floats = new float[shorts.length];
        for (int i = 0; i < shorts.length; i++) {
            floats[i] = (float) shorts[i] / 0x8000;
        }
        return floats;
    }

    public static short[] floatsRangeToShort(float[] floats) {
        short[] shorts = new short[floats.length];
        for (int i = 0; i < floats.length; i++) {
            shorts[i] = (short) (floats[i] * 0x8000);
        }
        return shorts;
    }

    public static short[] bytesToShorts(byte[] bytes) {
        short[] shorts = new short[bytes.length / 2];
        for (int i = 0; i < shorts.length; i++) {
            shorts[i] = (short) ((bytes[i * 2] & 0xFF) | ((bytes[i * 2 + 1] & 0xFF) << 8));
        }
        return shorts;
    }

    public static byte[] shortsToBytes(short[] shorts) {
        byte[] bytes = new byte[shorts.length * 2];
        for (int i = 0; i < shorts.length; i++) {
            bytes[i * 2] = (byte) (shorts[i] & 0xFF);
            bytes[i * 2 + 1] = (byte) ((shorts[i] >> 8) & 0xFF);
        }
        return bytes;
    }

    public static float[] shortsToFloats(short[] shorts) {
        float[] floats = new float[shorts.length];
        for (int i = 0; i < shorts.length; i++) {
            floats[i] = shorts[i] / 32768.0f;
        }
        return floats;
    }

    public static short[] floatsToShorts(float[] floats) {
        short[] shorts = new short[floats.length];
        for (int i = 0; i < floats.length; i++) {
            shorts[i] = (short) Math.max(Short.MIN_VALUE,
                Math.min(Short.MAX_VALUE, floats[i] * 32768.0f));
        }
        return shorts;
    }

    /**
     * Applies a fade-in effect to audio samples to prevent pops/clicks when audio starts.
     * Used on the first audio packet from a player.
     * 
     * @param samples The audio samples (mono or stereo)
     * @param channels Number of channels (1 for mono, 2 for stereo)
     * @return Processed samples with fade-in applied
     */
    public static short[] fadeIn(short[] samples, int channels) {
        if (samples == null || samples.length == 0) {
            return samples;
        }

        int fadeInDuration = samples.length;
        short[] processed = new short[samples.length];

        for (int index = 0; index < samples.length; index += channels) {
            float fade = Math.min(index / (float) fadeInDuration, 1.0f);

            for (int channel = 0; channel < channels; channel++) {
                if (index + channel < samples.length) {
                    processed[index + channel] = (short) (samples[index + channel] * fade);
                }
            }
        }

        return processed;
    }

    /**
     * Applies a fade-out effect to audio samples to prevent pops/clicks when audio ends.
     * Used on the last audio packet from a player.
     * 
     * @param samples The audio samples (mono or stereo)
     * @param channels Number of channels (1 for mono, 2 for stereo)
     * @return Processed samples with fade-out applied
     */
    public static short[] fadeOut(short[] samples, int channels) {
        if (samples == null || samples.length == 0) {
            return samples;
        }

        int fadeOutDuration = samples.length;
        short[] processed = new short[samples.length];

        for (int index = 0; index < samples.length; index += channels) {
            float fade = Math.max((fadeOutDuration - index) / (float) fadeOutDuration, 0.0f);

            for (int channel = 0; channel < channels; channel++) {
                if (index + channel < samples.length) {
                    processed[index + channel] = (short) (samples[index + channel] * fade);
                }
            }
        }

        return processed;
    }

	public static int getActivationOffset(byte[] samples, double activationLevel) {
		int highestPos = -1;
		for (int i = 0; i < samples.length; i += 100) {
			double level = calculateAudioLevel(samples, i, Math.min(i + 100, samples.length));
			if (level >= activationLevel) {
				highestPos = i;
			}
		}
		return highestPos;
	}

	public static double calculateAudioLevel(byte[] samples, int offset, int length) {
		double rms = 0D; // root mean square (RMS) amplitude

		for (int i = offset; i < length; i += 2) {
			double sample = (double) bytesToShort(samples[i], samples[i + 1]) / Short.MAX_VALUE;
			rms += sample * sample;
		}

		int sampleCount = length / 2;

		rms = (sampleCount == 0) ? 0 : Math.sqrt(rms / sampleCount);

		double db;

		if (rms > 0D) {
			db = Math.min(Math.max(20D * Math.log10(rms), -127D), 0D);
		} else {
			db = -127D;
		}

		return db;
	}

	public static short bytesToShort(byte b1, byte b2) {
		return (short) (((b2 & 0xFF) << 8) | (b1 & 0xFF));
	}
}
