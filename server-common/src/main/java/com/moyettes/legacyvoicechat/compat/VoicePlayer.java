package com.moyettes.legacyvoicechat.compat;

public interface VoicePlayer {
	int getNetworkId();
	String getUniqueId();
	String getName();
	double getX();
	double getY();
	double getZ();
	Object getWorld();
}
