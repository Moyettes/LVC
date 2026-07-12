package com.moyettes.legacyvoicechat.sound;

import java.util.concurrent.ConcurrentHashMap;

public class Simple3DAudio {

    private final ConcurrentHashMap<Integer, PlayerAudioSource> playerSources = new ConcurrentHashMap<>();

    public double listenerX = 0.0;
    public double listenerY = 0.0;
    public double listenerZ = 0.0;
    public float listenerYaw = 0.0f;
    public float listenerPitch = 0.0f;

    public boolean initialize() {
        System.out.println("Simple 3D audio system initialized");
        return true;
    }

    public PlayerAudioSource getOrCreatePlayerSource(Integer playerId) {
        return playerSources.computeIfAbsent(playerId, id -> {
            System.out.println("Created audio source for player " + id);
            return new PlayerAudioSource();
        });
    }

    public void updateListener(double x, double y, double z, float yaw, float pitch) {
        this.listenerX = x;
        this.listenerY = y;
        this.listenerZ = z;
        this.listenerYaw = yaw;
        this.listenerPitch = pitch;
    }

    public void playPlayerAudio(Integer playerId, byte[] audioData, double playerX, double playerY, double playerZ, float volume) {
        PlayerAudioSource source = getOrCreatePlayerSource(playerId);
        if (source != null) {
            // Calculate stereo positioning
            byte[] positionedAudio = calculateStereoPositioning(audioData, playerX, playerY, playerZ, volume);
            source.playAudio(positionedAudio);
        }
    }

    private byte[] calculateStereoPositioning(byte[] audioData, double playerX, double playerY, double playerZ, float volume) {
        double deltaX = playerX - listenerX;
        double deltaZ = playerZ - listenerZ;

        double distance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);

        double angle = Math.atan2(deltaZ, deltaX) - Math.toRadians(listenerYaw);

        while (angle > Math.PI) angle -= 2 * Math.PI;
        while (angle < -Math.PI) angle += 2 * Math.PI;

        float pan = (float) Math.sin(angle);

        float stereoWidth = (float) Math.max(0.1, Math.min(1.0, 10.0 / Math.max(1.0, distance)));
        pan *= stereoWidth;

        return applyStereoPanning(audioData, pan, volume);
    }

    private byte[] applyStereoPanning(byte[] audioData, float pan, float volume) {
        if (Math.abs(pan) < 0.01f) {
            return duplicateMonoToStereo(audioData, volume);
        }

        byte[] stereoAudio = new byte[audioData.length * 2];

        for (int i = 0; i < audioData.length; i += 2) {
            if (i + 1 < audioData.length) {
                short sample = (short) ((audioData[i + 1] << 8) | (audioData[i] & 0xFF));

                sample = (short) (sample * volume);

                float leftGain = (float) Math.sqrt((1.0f - pan) / 2.0f);
                float rightGain = (float) Math.sqrt((1.0f + pan) / 2.0f);

                short leftSample = (short) (sample * leftGain);
                short rightSample = (short) (sample * rightGain);

                int stereoIndex = (i / 2) * 4;

                stereoAudio[stereoIndex] = (byte) (leftSample & 0xFF);
                stereoAudio[stereoIndex + 1] = (byte) ((leftSample >> 8) & 0xFF);

                stereoAudio[stereoIndex + 2] = (byte) (rightSample & 0xFF);
                stereoAudio[stereoIndex + 3] = (byte) ((rightSample >> 8) & 0xFF);
            }
        }

        return stereoAudio;
    }

    private byte[] duplicateMonoToStereo(byte[] monoAudio, float volume) {
        byte[] stereoAudio = new byte[monoAudio.length * 2];

        for (int i = 0; i < monoAudio.length; i += 2) {
            if (i + 1 < monoAudio.length) {
                short sample = (short) ((monoAudio[i + 1] << 8) | (monoAudio[i] & 0xFF));

                sample = (short) (sample * volume);

                int stereoIndex = (i / 2) * 4;

                stereoAudio[stereoIndex] = (byte) (sample & 0xFF);
                stereoAudio[stereoIndex + 1] = (byte) ((sample >> 8) & 0xFF);

                stereoAudio[stereoIndex + 2] = (byte) (sample & 0xFF);
                stereoAudio[stereoIndex + 3] = (byte) ((sample >> 8) & 0xFF);
            }
        }

        return stereoAudio;
    }

    public void removePlayerSource(Integer playerId) {
        PlayerAudioSource source = playerSources.remove(playerId);
        if (source != null) {
            source.cleanup();
        }
    }

    public boolean isInitialized() {
        return true;
    }

    public void cleanup() {
        for (PlayerAudioSource source : playerSources.values()) {
            if (source != null) {
                source.cleanup();
            }
        }
        playerSources.clear();
    }

    public static class PlayerAudioSource {
        public void playAudio(byte[] audioData) {
        }

        public void cleanup() {
        }
    }
}
