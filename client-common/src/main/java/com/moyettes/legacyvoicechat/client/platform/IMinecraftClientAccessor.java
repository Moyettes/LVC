package com.moyettes.legacyvoicechat.client.platform;

import java.util.Collection;

public interface IMinecraftClientAccessor {

	double getPlayerX();
	double getPlayerY();
	double getPlayerZ();

	float getYaw();
	float getPitch();

	String getLocalPlayerName();
	Integer getLocalPlayerNetworkId();

	float getOptionsMasterVolume();

	Collection<PlayerRef> getOnlinePlayers();

	void openScreen(Object screenRef);
}
