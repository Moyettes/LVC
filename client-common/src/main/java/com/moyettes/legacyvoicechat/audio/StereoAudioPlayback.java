package com.moyettes.legacyvoicechat.audio;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.sound.sampled.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class StereoAudioPlayback implements Runnable {
	private static final Logger LOGGER = LogManager.getLogger(StereoAudioPlayback.class);


    private static final int SAMPLE_SIZE_IN_BITS = 16;
    private static final int CHANNELS = 2;
    private static final boolean BIG_ENDIAN = false;

    private int sampleRate = 48000;
    private int stereoFrameBytes = 960 * 4;
    private int bufferSize = stereoFrameBytes * 16;

    private SourceDataLine speaker;
    private volatile boolean running;
    private Thread playbackThread;
    private BlockingQueue<byte[]> audioQueue;
    private AtomicBoolean initialized;
    private String outputDeviceName;

    public StereoAudioPlayback() {
        this(null);
    }

    public StereoAudioPlayback(String outputDeviceName) {
        this.audioQueue = new LinkedBlockingQueue<>(100);
        this.initialized = new AtomicBoolean(false);
        this.outputDeviceName = outputDeviceName;
    }

    public boolean initialize() {
        if (initialized.get()) {
            return true;
        }

        try {
            try {
                sampleRate = com.moyettes.legacyvoicechat.audio.AudioProcessor.getSampleRate();
            } catch (Throwable ignored) {
                sampleRate = 48000;
            }
            stereoFrameBytes = (sampleRate / 1000) * 2 /*bytes*/ * 20 /*ms*/ * CHANNELS;

            bufferSize = stereoFrameBytes * 16;

            AudioFormat format = new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                (float) sampleRate,
                SAMPLE_SIZE_IN_BITS,
                CHANNELS,
                (SAMPLE_SIZE_IN_BITS / 8) * CHANNELS,
                (float) sampleRate,
                BIG_ENDIAN
            );

                DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
                if (!AudioSystem.isLineSupported(info)) {
                    LOGGER.warn("Stereo audio playback format not supported by system");
                    return false;
                }

                if (outputDeviceName != null && !outputDeviceName.equals("Default")) {
                    Mixer.Info[] mixers = AudioSystem.getMixerInfo();
                    for (Mixer.Info mixerInfo : mixers) {
                        if (mixerInfo.getName().equals(outputDeviceName)) {
                            Mixer mixer = AudioSystem.getMixer(mixerInfo);
                            if (mixer.isLineSupported(info)) {
                                speaker = (SourceDataLine) mixer.getLine(info);
                                speaker.open(format, bufferSize);
                                LOGGER.info("Using output device: " + outputDeviceName);
                                break;
                            }
                        }
                    }
                }

                if (speaker == null) {
                    speaker = (SourceDataLine) AudioSystem.getLine(info);
                    speaker.open(format, bufferSize);
                    if (outputDeviceName != null && !outputDeviceName.equals("Default")) {
                        LOGGER.warn("Failed to use output device '" + outputDeviceName + "', falling back to default");
                    }
                }

            if (speaker != null) {
                initialized.set(true);
                return true;
            } else {
                LOGGER.warn("Failed to initialize any audio output device");
                return false;
            }
        } catch (LineUnavailableException e) {
            LOGGER.warn("Stereo speaker line unavailable: " + e.getMessage());
            return false;
        }
    }

    public boolean startPlayback() {
        if (!initialized.get()) {
            if (!initialize()) {
                return false;
            }
        }

        if (running) {
            return true;
        }

        if (speaker != null && !speaker.isRunning()) {
            try {
                speaker.start();
            } catch (Exception e) {
                LOGGER.warn("Failed to start speaker line: " + e.getMessage());
            }
        }

        running = true;
        playbackThread = new Thread(this, "StereoAudioPlaybackThread");
        playbackThread.setDaemon(true);
        playbackThread.start();

        return true;
    }

    public void stopPlayback() {
        running = false;
        if (playbackThread != null) {
            try {
                playbackThread.join(100);
                if (playbackThread.isAlive()) {
                    LOGGER.warn("Audio playback thread did not stop gracefully");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        if (speaker != null && speaker.isRunning()) {
            try {
                speaker.stop();
                speaker.flush();
            } catch (Exception e) {
                LOGGER.warn("Error stopping speaker: " + e.getMessage());
            }
        }
    }

    public void close() {
        stopPlayback();
        if (speaker != null) {
            try {
                if (speaker.isRunning()) {
                    speaker.stop();
                }
                speaker.flush();
                speaker.close();
            } catch (Exception e) {
                String errorMsg = e.getMessage();
                if (errorMsg == null) {
                    errorMsg = e.getClass().getSimpleName();
                }
                LOGGER.warn("Error closing speaker line: " + errorMsg);
            } finally {
                speaker = null;
                initialized.set(false);
            }
        } else {
            initialized.set(false);
        }

        if (audioQueue != null) {
            audioQueue.clear();
        }
    }

    public void clearQueue() {
            if (audioQueue != null) {
                audioQueue.clear();
            }
    }

    public void flushAudioBuffer() {
        if (speaker != null && speaker.isRunning()) {
            speaker.flush();
        }
    }

    public void queueAudio(byte[] audioData) {
        if (running && audioData != null && audioData.length > 0) {
            try {
                if (audioQueue.size() > 90) {
                    audioQueue.poll();
                }

                if (!audioQueue.offer(audioData)) {
                    LOGGER.warn("Stereo audio queue full, dropping packet");
                }
            } catch (Exception e) {
                LOGGER.warn("Failed to queue stereo audio data: " + e.getMessage());
            }
        }
    }

    @Override
    public void run() {
        while (running) {
            try {
                byte[] audioData = audioQueue.poll();

                if (speaker != null && speaker.isOpen()) {
                    if (!speaker.isRunning()) {
                        try {
                            speaker.start();
                        } catch (Exception ignored) { }
                    }

                    if (audioData != null && audioData.length > 0) {
                        speaker.write(audioData, 0, audioData.length);
                    }
                }

                if (audioData == null) {
                    Thread.sleep(5);
                }

            } catch (InterruptedException e) {
                break;
            } catch (Exception e) {
                if (running) {
                    LOGGER.warn("Error during stereo audio playback: " + e.getMessage());
                }
            }
        }
    }

    public boolean isInitialized() {
        return initialized.get();
    }

    public boolean isRunning() {
        return running;
    }
}
