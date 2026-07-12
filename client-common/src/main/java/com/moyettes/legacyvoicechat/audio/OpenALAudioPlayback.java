package com.moyettes.legacyvoicechat.audio;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.BufferUtils;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.AL10;
import org.lwjgl.LWJGLException;
import java.nio.ByteBuffer;

public class OpenALAudioPlayback {
	private static final Logger LOGGER = LogManager.getLogger(OpenALAudioPlayback.class);


	private static final int NUM_BUFFERS = 32;
	private static final int SAMPLE_RATE = 48000;

	private int source;
	private int[] bufferIds;
	private int[] freeBuffers;
	private int freeBufferCount;

	private volatile boolean initialized = false;

	public OpenALAudioPlayback() {
		this.bufferIds = new int[NUM_BUFFERS];
		this.freeBuffers = new int[NUM_BUFFERS];
		this.freeBufferCount = 0;
	}

	public boolean initialize() {
		if (initialized) {
			return true;
		}

        try {
            // Make OpenAL context current on this thread if not already
            if (!AL.isCreated()) {
                try {
                    AL.create();
                    LOGGER.info("OpenAL context created for playback thread");
                } catch (LWJGLException e) {
                    LOGGER.warn("Failed to create OpenAL context on playback thread: " + e.getMessage());
                    return false;
                }
            }

			source = AL10.alGenSources();
			if (source == 0) {
				LOGGER.warn("Failed to create OpenAL source - alGenSources returned 0");
				return false;
			}

			int error = AL10.alGetError();
			if (error != AL10.AL_NO_ERROR) {
				LOGGER.warn("Failed to create OpenAL source - AL error: " + getErrorString(error) + " (0x" + Integer.toHexString(error) + ")");
				return false;
			}

			AL10.alSourcei(source, AL10.AL_LOOPING, AL10.AL_FALSE);
			AL10.alSourcef(source, AL10.AL_PITCH, 1.0f);
			AL10.alSourcef(source, AL10.AL_GAIN, 1.0f);
			AL10.alSource3f(source, AL10.AL_POSITION, 0, 0, 0);
			AL10.alSource3f(source, AL10.AL_VELOCITY, 0, 0, 0);
			AL10.alSourcei(source, AL10.AL_SOURCE_RELATIVE, AL10.AL_TRUE);

			error = AL10.alGetError();
			if (error != AL10.AL_NO_ERROR) {
				cleanup();
				LOGGER.warn("Failed to configure OpenAL source - AL error: " + getErrorString(error) + " (0x" + Integer.toHexString(error) + ")");
				return false;
			}

			for (int i = 0; i < NUM_BUFFERS; i++) {
				bufferIds[i] = AL10.alGenBuffers();
			}

			error = AL10.alGetError();
			if (error != AL10.AL_NO_ERROR) {
				cleanup();
				LOGGER.warn("Failed to create OpenAL buffers - AL error: " + getErrorString(error) + " (0x" + Integer.toHexString(error) + ")");
				return false;
			}

			for (int i = 0; i < NUM_BUFFERS; i++) {
				freeBuffers[i] = bufferIds[i];
			}
			freeBufferCount = NUM_BUFFERS;

			initialized = true;
			LOGGER.info("OpenAL audio playback initialized successfully");
			return true;

		} catch (Exception e) {
			LOGGER.warn("Failed to initialize OpenAL: " + e.getMessage());
			e.printStackTrace();
			cleanup();
			return false;
		}
	}

	private String getErrorString(int error) {
		switch (error) {
			case AL10.AL_INVALID_NAME: return "AL_INVALID_NAME";
			case AL10.AL_INVALID_ENUM: return "AL_INVALID_ENUM";
			case AL10.AL_INVALID_VALUE: return "AL_INVALID_VALUE";
			case AL10.AL_INVALID_OPERATION: return "AL_INVALID_OPERATION";
			case AL10.AL_OUT_OF_MEMORY: return "AL_OUT_OF_MEMORY";
			default: return "UNKNOWN_ERROR";
		}
	}

	public void writeAudioDirect(byte[] audioData) {
		if (!initialized || audioData == null || audioData.length == 0) {
			return;
		}

		removeProcessedBuffers();

		int state = AL10.alGetSourcei(source, AL10.AL_SOURCE_STATE);
		int queued = AL10.alGetSourcei(source, AL10.AL_BUFFERS_QUEUED);
		int processed = AL10.alGetSourcei(source, AL10.AL_BUFFERS_PROCESSED);
		int active = queued - processed;

		boolean needsPreBuffer = (state == AL10.AL_INITIAL || state == AL10.AL_STOPPED || active <= 1);
		if (needsPreBuffer) {
			byte[] silentFrame = new byte[audioData.length];
			for (int i = 0; i < 8; i++) {
				int silentBufferId = getFreeBuffer();
				if (silentBufferId == 0) {
					removeProcessedBuffers();
					silentBufferId = getFreeBuffer();
					if (silentBufferId == 0) break;
				}

				ByteBuffer silentBuffer = BufferUtils.createByteBuffer(silentFrame.length);
				silentBuffer.put(silentFrame);
				silentBuffer.flip();

				AL10.alBufferData(silentBufferId, AL10.AL_FORMAT_STEREO16, silentBuffer, SAMPLE_RATE);
				if (AL10.alGetError() == AL10.AL_NO_ERROR) {
					AL10.alSourceQueueBuffers(source, silentBufferId);
				} else {
					returnFreeBuffer(silentBufferId);
				}
			}
		}

		queued = AL10.alGetSourcei(source, AL10.AL_BUFFERS_QUEUED);
		if (queued >= NUM_BUFFERS) {
			LOGGER.warn("Buffer overflow detected: " + queued + "/" + NUM_BUFFERS + " - skipping packets");
			removeProcessedBuffers();
			if (getFreeBuffer() == 0) {
				return;
			}
		}

		int bufferId = getFreeBuffer();
		if (bufferId == 0) {
			removeProcessedBuffers();
			bufferId = getFreeBuffer();
			if (bufferId == 0) {
				return;
			}
		}

		ByteBuffer audioBuffer = BufferUtils.createByteBuffer(audioData.length);
		audioBuffer.put(audioData);
		audioBuffer.flip();

		AL10.alBufferData(bufferId, AL10.AL_FORMAT_STEREO16, audioBuffer, SAMPLE_RATE);

		int error = AL10.alGetError();
		if (error != AL10.AL_NO_ERROR) {
			LOGGER.warn("Failed to buffer audio data: " + getErrorString(error));
			returnFreeBuffer(bufferId);
			return;
		}

		AL10.alSourceQueueBuffers(source, bufferId);

		error = AL10.alGetError();
		if (error != AL10.AL_NO_ERROR) {
			LOGGER.warn("Failed to queue buffer: " + getErrorString(error));
			return;
		}

		state = AL10.alGetSourcei(source, AL10.AL_SOURCE_STATE);
		if (state != AL10.AL_PLAYING) {
			AL10.alSourcePlay(source);
		}
	}

	private int getFreeBuffer() {
		if (freeBufferCount > 0) {
			return freeBuffers[--freeBufferCount];
		}
		return 0;
	}

	private void returnFreeBuffer(int bufferId) {
		if (freeBufferCount < NUM_BUFFERS) {
			freeBuffers[freeBufferCount++] = bufferId;
		}
	}

	private void removeProcessedBuffers() {
		int processed = AL10.alGetSourcei(source, AL10.AL_BUFFERS_PROCESSED);

		while (processed > 0) {
			int bufferId = AL10.alSourceUnqueueBuffers(source);
			returnFreeBuffer(bufferId);
			processed--;
		}
	}

	public void close() {
		cleanup();
	}

	private void cleanup() {
		if (source != 0) {
			try {
				AL10.alSourceStop(source);
				removeProcessedBuffers();
				AL10.alDeleteSources(source);
			} catch (Exception e) {
				LOGGER.warn("Error during OpenAL source cleanup: " + e.getMessage());
			}
			source = 0;
		}

		if (bufferIds != null) {
			for (int i = 0; i < bufferIds.length; i++) {
				if (bufferIds[i] != 0) {
					try {
						AL10.alDeleteBuffers(bufferIds[i]);
					} catch (Exception e) {
						LOGGER.warn("Error deleting OpenAL buffer " + i + ": " + e.getMessage());
					}
					bufferIds[i] = 0;
				}
			}
		}

		// Clean up OpenAL context on this thread
		if (AL.isCreated()) {
			try {
				AL.destroy();
				LOGGER.info("OpenAL context destroyed for playback thread");
			} catch (Exception e) {
				LOGGER.warn("Error destroying OpenAL context: " + e.getMessage());
			}
		}

		initialized = false;
		LOGGER.info("OpenAL audio playback closed");
	}

	public boolean isInitialized() {
		return initialized;
	}
}


