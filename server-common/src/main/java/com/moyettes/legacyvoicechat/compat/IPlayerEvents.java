package com.moyettes.legacyvoicechat.compat;

public interface IPlayerEvents {
	void onPlayerJoin(VoicePlayer player);
	void onPlayerLeave(VoicePlayer player);
	void onPlayerDisconnect(VoicePlayer player, String reason);
}
