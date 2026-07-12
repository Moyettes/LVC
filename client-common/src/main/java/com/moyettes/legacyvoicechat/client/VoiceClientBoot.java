package com.moyettes.legacyvoicechat.client;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class VoiceClientBoot {

	public static final Logger LOGGER = LogManager.getLogger("LegacyVoiceChat");

	private static VoiceClientContext context;

	private VoiceClientBoot() {}

	public static void start(VoiceClientContext ctx) {
		if (context != null) {
			throw new IllegalStateException("VoiceClientBoot has already been started");
		}
		context = ctx;
		LOGGER.info("LegacyVoiceChat client booting on {} ({})", ctx.loader, ctx.minecraftVersion);
	}

	public static VoiceClientContext getContext() {
		return context;
	}
}
