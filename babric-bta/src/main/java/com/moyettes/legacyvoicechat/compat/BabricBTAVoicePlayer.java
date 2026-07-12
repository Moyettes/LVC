package com.moyettes.legacyvoicechat.compat;

import net.minecraft.server.entity.player.PlayerServer;

public class BabricBTAVoicePlayer implements VoicePlayer {

	private final PlayerServer player;

	public BabricBTAVoicePlayer(PlayerServer player) {
		this.player = player;
	}

	@Override
	public int getNetworkId() {
		return player.id;
	}

	@Override
	public String getUniqueId() {
		return Integer.toString(player.id);
	}

	@Override
	public String getName() {
		return player.username;
	}

	@Override
	public double getX() {
		return player.x;
	}

	@Override
	public double getY() {
		return player.y;
	}

	@Override
	public double getZ() {
		return player.z;
	}

	@Override
	public Object getWorld() {
		return player.world;
	}
}
