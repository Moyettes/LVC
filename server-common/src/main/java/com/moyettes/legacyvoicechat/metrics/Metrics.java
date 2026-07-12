package com.moyettes.legacyvoicechat.metrics;

import com.moyettes.legacyvoicechat.Voice;
import com.moyettes.legacyvoicechat.VoiceContext;
import com.moyettes.legacyvoicechat.compat.MinecraftCompat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

public class Metrics {

	private static final String SUBMIT_URL = "https://moystats-production.up.railway.app/api/v1/metrics";
	private static final long REPORT_INTERVAL_MS = 1000 * 60 * 30;

	private final String serviceId;
	private final String serverUUID;
	private final boolean enabled;
	private final Timer timer;

	public Metrics(int serviceId, File configFolder) {
		this.serviceId = String.valueOf(serviceId);

		File moyStatsFolder = new File(configFolder, "moyStats");
		moyStatsFolder.mkdirs();
		File configFile = new File(moyStatsFolder, "config.yml");

		String uuid = null;
		boolean isEnabled = true;

		if (!configFile.exists()) {
			uuid = UUID.randomUUID().toString();
			writeConfig(configFile, uuid, true);
		} else {
			try (BufferedReader br = new BufferedReader(new FileReader(configFile))) {
				String line;
				while ((line = br.readLine()) != null) {
					if (line.trim().startsWith("serverUuid:")) {
						uuid = line.substring(line.indexOf(":") + 1).trim();
					} else if (line.trim().startsWith("enabled:")) {
						isEnabled = Boolean.parseBoolean(line.substring(line.indexOf(":") + 1).trim());
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}

			if (uuid == null) {
				uuid = UUID.randomUUID().toString();
				writeConfig(configFile, uuid, isEnabled);
			}
		}

		this.serverUUID = uuid;
		this.enabled = isEnabled;
		this.timer = new Timer(true);

		if (enabled) {
			startSubmitting();
		}
	}

	private void writeConfig(File file, String uuid, boolean enabled) {
		try (PrintWriter writer = new PrintWriter(file)) {
			writer.println("# bStats (Custom Backend) Config");
			writer.println("enabled: " + enabled);
			writer.println("serverUuid: " + uuid);
			writer.println("");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void startSubmitting() {
		timer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				submitData();
			}
		}, 0, REPORT_INTERVAL_MS);
	}

	private void submitData() {
		try {
			VoiceContext ctx = Voice.getContext();
			if (ctx == null) return;
			MinecraftCompat compat = ctx.mc;
			if (compat == null) return;

			int playerAmount = compat.getOnlinePlayerCount();
			String osName = System.getProperty("os.name");
			String osArch = System.getProperty("os.arch");
			String javaVersion = System.getProperty("java.version");
			int coreCount = Runtime.getRuntime().availableProcessors();
			String pluginVersion = "0.1.0";

			String jsonPayload = String.format(
				"{" +
					"\"serverUUID\": \"%s\"," +
					"\"serviceId\": %s," +
					"\"metrics\": {" +
					"\"playerAmount\": %d," +
					"\"osName\": \"%s\"," +
					"\"osArch\": \"%s\"," +
					"\"javaVersion\": \"%s\"," +
					"\"coreCount\": %d," +
					"\"pluginVersion\": \"%s\"," +
					"\"onlineMode\": %b," +
					"\"minecraftVersion\": \"%s\"," +
					"\"serverSoftware\": \"%s\"" +
					"}" +
					"}",
				serverUUID,
				serviceId,
				playerAmount,
				escapeJson(osName),
				escapeJson(osArch),
				escapeJson(javaVersion),
				coreCount,
				escapeJson(pluginVersion),
				compat.isOnlineMode(),
				escapeJson(ctx.minecraftVersion),
				escapeJson(ctx.loader != null ? ctx.loader.name() : "Unknown")
			);

			sendPostRequest(jsonPayload);
		} catch (Exception e) {
			System.err.println("[Metrics] Failed to submit metrics: " + e.getMessage());
		}
	}

	private void sendPostRequest(String json) throws IOException {
		URL url = new URL(SUBMIT_URL);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setRequestMethod("POST");
		conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
		conn.setRequestProperty("Accept", "application/json");
		conn.setDoOutput(true);

		try (OutputStream os = conn.getOutputStream()) {
			byte[] input = json.getBytes(StandardCharsets.UTF_8);
			os.write(input, 0, input.length);
		}

		int code = conn.getResponseCode();
		if (code != 200) {
			System.err.println("[Metrics] Error response: " + code);
		}

		try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
			StringBuilder response = new StringBuilder();
			String responseLine;
			while ((responseLine = br.readLine()) != null) {
				response.append(responseLine.trim());
			}
		}
	}

	private String escapeJson(String s) {
		if (s == null) return "";
		return s.replace("\\", "\\\\").replace("\"", "\\\"");
	}
}
