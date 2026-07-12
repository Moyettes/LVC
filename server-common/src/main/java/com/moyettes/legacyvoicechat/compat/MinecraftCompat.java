package com.moyettes.legacyvoicechat.compat;

import java.util.Collection;

public interface MinecraftCompat {
	VoicePlayer getPlayerById(int networkId);
	VoicePlayer getPlayerByName(String name);
	Collection<VoicePlayer> getAllConnectedPlayers();
	double getDistanceSquared(VoicePlayer a, VoicePlayer b);

	default int getOnlinePlayerCount() {
		return getAllConnectedPlayers().size();
	}

	default boolean isOnlineMode() {
		return false;
	}
}
