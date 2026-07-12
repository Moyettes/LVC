package com.moyettes.legacyvoicechat.audio;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.sound.sampled.*;
import java.util.function.Consumer;

public class MicrophoneCapture implements Runnable {
	private static final Logger LOGGER = LogManager.getLogger(MicrophoneCapture.class);


    private static final float SAMPLE_RATE = 48000.0f;
    private static final int SAMPLE_SIZE_IN_BITS = 16;
    private static final int CHANNELS = 1;
    private static final boolean BIG_ENDIAN = false;

    private TargetDataLine microphone;
    private volatile boolean running;
    private Thread captureThread;
    private Consumer<byte[]> audioDataConsumer;
    private String preferredDeviceName;

    public MicrophoneCapture(Consumer<byte[]> audioDataConsumer) {
        this(audioDataConsumer, null);
    }

    public MicrophoneCapture(Consumer<byte[]> audioDataConsumer, String preferredDeviceName) {
        this.audioDataConsumer = audioDataConsumer;
        this.preferredDeviceName = preferredDeviceName;
    }

    public boolean startCapture() {
        if (running) {
            return true;
        }

        try {
            AudioFormat format = new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                SAMPLE_RATE,
                SAMPLE_SIZE_IN_BITS,
                CHANNELS,
                2,
                SAMPLE_RATE,
                BIG_ENDIAN
            );

            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
            if (!AudioSystem.isLineSupported(info)) {
                LOGGER.warn("Audio format not supported");
                return false;
            }

            if (preferredDeviceName != null && !preferredDeviceName.isEmpty() && !"Default".equals(preferredDeviceName)) {
                Mixer.Info[] mixers = AudioSystem.getMixerInfo();
                TargetDataLine line = null;
                for (Mixer.Info mixerInfo : mixers) {
                    if (preferredDeviceName.equals(mixerInfo.getName())) {
                        try {
                            Mixer mixer = AudioSystem.getMixer(mixerInfo);

                            Line.Info lineInfo = new Line.Info(TargetDataLine.class);
                            line = (TargetDataLine) mixer.getLine(lineInfo);
                            break;
                        } catch (Exception ignored) {
                        }
                    }
                }
                if (line != null) {
                    microphone = line;
                }
            }

            if (microphone == null) {
                DataLine.Info anyFormat = new DataLine.Info(TargetDataLine.class, null);
                microphone = (TargetDataLine) AudioSystem.getLine(anyFormat);
            }

            int internalBufferBytes = 960 * 2 * 32;
            microphone.open(format, internalBufferBytes);
            microphone.start();

            running = true;
            captureThread = new Thread(this, "MicrophoneCaptureThread");
            captureThread.setDaemon(true);
            captureThread.start();
            return true;
        } catch (LineUnavailableException e) {
            LOGGER.warn("Microphone line unavailable: " + e.getMessage());
            return false;
        }
    }

    public void stopCapture() {
        running = false;
        if (captureThread != null) {
            try {
                captureThread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
		LOGGER.info("Microphone capture stopped.");
    }

    public void close() {
        stopCapture();
        if (microphone != null) {
            microphone.stop();
            microphone.close();
            microphone = null;
			LOGGER.info("Microphone closed.");
        }
    }

    @Override
    public void run() {
        final int frameBytes = 960 * 2;
        byte[] frameBuffer = new byte[frameBytes];
        int frameOffset = 0;
        byte[] readBuffer = new byte[4096];

		LOGGER.info("Audio capture thread started");

        while (running) {
            try {
                int bytesRead = microphone.read(readBuffer, 0, readBuffer.length);
                if (bytesRead <= 0) {
                    continue;
                }

                int srcPos = 0;
                while (srcPos < bytesRead) {
                    int remainingInFrame = frameBytes - frameOffset;
                    int toCopy = Math.min(remainingInFrame, bytesRead - srcPos);
                    System.arraycopy(readBuffer, srcPos, frameBuffer, frameOffset, toCopy);
                    frameOffset += toCopy;
                    srcPos += toCopy;

                    if (frameOffset == frameBytes) {
                        if (audioDataConsumer != null) {
                            byte[] frameCopy = new byte[frameBytes];
                            System.arraycopy(frameBuffer, 0, frameCopy, 0, frameBytes);
                            audioDataConsumer.accept(frameCopy);
                        }

                        frameOffset = 0;
                    }
                }
            } catch (Exception e) {
                LOGGER.warn("Error in audio capture: " + e.getMessage());
                break;
            }
        }

    }

}



