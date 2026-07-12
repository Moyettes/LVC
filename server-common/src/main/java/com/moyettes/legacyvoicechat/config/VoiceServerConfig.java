package com.moyettes.legacyvoicechat.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class VoiceServerConfig {

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    @SerializedName("port")
    private int port = 25566;

    @SerializedName("server_host")
    private String serverHost = "0.0.0.0";

    @SerializedName("audio")
    private AudioConfig audio = new AudioConfig();

    public VoiceServerConfig() {

    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public AudioConfig getAudio() {
        return audio;
    }

    public void setAudio(AudioConfig audio) {
        this.audio = audio;
    }

    public String getServerHost() {
        return serverHost;
    }

    public void setServerHost(String serverHost) {
        this.serverHost = serverHost;
    }

    public static VoiceServerConfig load() {
        Path configPath = Paths.get("config", "voice-server.json");

        try {
            Files.createDirectories(configPath.getParent());
        } catch (IOException e) {
            System.err.println("Failed to create config directory: " + e.getMessage());
        }

        if (Files.exists(configPath)) {
            try (Reader reader = Files.newBufferedReader(configPath)) {
                VoiceServerConfig config = GSON.fromJson(reader, VoiceServerConfig.class);
                if (config != null) {
                    return config;
                }
            } catch (IOException e) {
                System.err.println("Failed to load config file: " + e.getMessage());
            }
        }

        VoiceServerConfig defaultConfig = new VoiceServerConfig();
        defaultConfig.save();
        return defaultConfig;
    }

    public void save() {
        Path configPath = Paths.get("config", "voice-server.json");

        try (Writer writer = Files.newBufferedWriter(configPath)) {
            GSON.toJson(this, writer);
        } catch (IOException e) {
            System.err.println("Failed to save config file: " + e.getMessage());
        }
    }

    public static class AudioConfig {
        @SerializedName("max_distance")
        private double maxDistance = 48.0;

        public double getMaxDistance() {
            return maxDistance;
        }

        public void setMaxDistance(double maxDistance) {
            this.maxDistance = maxDistance;
        }

    }
}
