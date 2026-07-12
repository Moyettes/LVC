package com.moyettes.legacyvoicechat.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class ClientConfig {

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    @SerializedName("microphone_gain")
    private float microphoneGain = 1.0f;

    @SerializedName("master_volume")
    private float masterVolume = 1.0f;

    @SerializedName("activation_threshold")
    private double activationThreshold = -60.0;

    @SerializedName("push_to_talk")
    private boolean pushToTalk = false;

    @SerializedName("input_device")
    private String inputDevice = null;

    @SerializedName("output_device")
    private String outputDevice = null;

    @SerializedName("player_volumes")
    private Map<String, Float> playerVolumes = new HashMap<>();

    private static final float MIN_VOLUME = 0.0f;
    private static final float MAX_VOLUME = 2.0f;
    private static final double MIN_THRESHOLD = -127.0;
    private static final double MAX_THRESHOLD = 0.0;

    public ClientConfig() {

    }

    public float getMicrophoneGain() {
        return microphoneGain;
    }

    public void setMicrophoneGain(float microphoneGain) {
        this.microphoneGain = Math.max(MIN_VOLUME, Math.min(MAX_VOLUME, microphoneGain));
    }

    public float getMasterVolume() {
        return masterVolume;
    }

    public void setMasterVolume(float masterVolume) {
        this.masterVolume = Math.max(MIN_VOLUME, Math.min(MAX_VOLUME, masterVolume));
    }

    public double getActivationThreshold() {
        return activationThreshold;
    }

    public void setActivationThreshold(double activationThreshold) {
        this.activationThreshold = Math.max(MIN_THRESHOLD, Math.min(MAX_THRESHOLD, activationThreshold));
    }

    public boolean isPushToTalk() {
        return pushToTalk;
    }

    public void setPushToTalk(boolean pushToTalk) {
        this.pushToTalk = pushToTalk;
    }

    public String getInputDevice() {
        return inputDevice;
    }

    public void setInputDevice(String inputDevice) {
        this.inputDevice = (inputDevice != null && inputDevice.trim().isEmpty()) ? null : inputDevice;
    }

    public String getOutputDevice() {
        return outputDevice;
    }

    public void setOutputDevice(String outputDevice) {
        this.outputDevice = (outputDevice != null && outputDevice.trim().isEmpty()) ? null : outputDevice;
    }

    public Map<String, Float> getPlayerVolumes() {
        return playerVolumes;
    }

    public void setPlayerVolumes(Map<String, Float> playerVolumes) {
        this.playerVolumes = playerVolumes;
    }

    public void setPlayerVolume(String playerName, float volume) {
        if (playerName == null || playerName.trim().isEmpty()) {
            return;
        }
        if (playerVolumes == null) {
            playerVolumes = new HashMap<>();
        }
        float clampedVolume = Math.max(MIN_VOLUME, Math.min(MAX_VOLUME, volume));
        if (clampedVolume == 1.0f) {
            playerVolumes.remove(playerName);
        } else {
            playerVolumes.put(playerName, clampedVolume);
        }
    }

    public float getPlayerVolume(String playerName) {
        if (playerVolumes == null) {
            return 1.0f;
        }
        return playerVolumes.getOrDefault(playerName, 1.0f);
    }

    public static ClientConfig load() {
        Path configPath = Paths.get("config", "voice-client.json");

        try {
            Files.createDirectories(configPath.getParent());
        } catch (IOException e) {
            System.err.println("Failed to create config directory: " + e.getMessage());
        }

        if (Files.exists(configPath)) {
            try (Reader reader = Files.newBufferedReader(configPath)) {
                ClientConfig config = GSON.fromJson(reader, ClientConfig.class);
                if (config != null) {
                    if (config.playerVolumes == null) {
                        config.playerVolumes = new HashMap<>();
                    }
                    return config;
                }
            } catch (IOException e) {
                System.err.println("Failed to load client config file: " + e.getMessage());
            }
        }

        ClientConfig defaultConfig = new ClientConfig();
        defaultConfig.save();
        return defaultConfig;
    }

    public void save() {
        Path configPath = Paths.get("config", "voice-client.json");

        try (Writer writer = Files.newBufferedWriter(configPath)) {
            GSON.toJson(this, writer);
        } catch (IOException e) {
            System.err.println("Failed to save client config file: " + e.getMessage());
        }
    }
}
