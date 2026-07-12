package com.moyettes.legacyvoicechat;

import com.moyettes.legacyvoicechat.metrics.Metrics;
import com.moyettes.legacyvoicechat.server.VoiceServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class Voice {

	public static final Logger LOGGER = LogManager.getLogger("LegacyVoiceChat");

	private static VoiceContext context;
	private static VoiceServer voiceServer;
	private static Metrics metrics;
	private static final Map<Integer, String> playerAuthTokens = new HashMap<>();

	public void init(VoiceContext ctx) {
		if (context != null) {
			throw new IllegalStateException("Voice has already been initialized");
		}
		context = ctx;

		LOGGER.info("Initializing LegacyVoiceChat on {} ({})", ctx.loader, ctx.minecraftVersion);

		try {
			voiceServer = new VoiceServer();
			voiceServer.start();
			LOGGER.info("Voice server listening on UDP {}", voiceServer.getPort());
		} catch (IOException e) {
			LOGGER.error("Failed to start voice server: {}", e.getMessage());
		}

		ctx.net.registerNetworkListeners();
	}

	public static VoiceContext getContext() {
		return context;
	}

	public static String generateAuthToken(String playerName) {
		return "token_" + playerName + "_" + System.currentTimeMillis();
	}

	public static VoiceServer getVoiceServer() {
		return voiceServer;
	}

	public static String getPlayerAuthToken(Integer playerNetworkId) {
		return playerAuthTokens.get(playerNetworkId);
	}

	public static Map<Integer, String> getPlayerAuthTokens() {
		return playerAuthTokens;
	}

	public static void onServerStarted() {
		if (metrics != null) return;
		try {
			metrics = new Metrics(10168, new File("config"));
		} catch (Throwable t) {
			LOGGER.warn("Failed to start metrics: {}", t.getMessage());
		}
	}
}
