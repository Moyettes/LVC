package com.moyettes.legacyvoicechat.compat;

import net.minecraft.server.entity.living.player.ServerPlayerEntity;

public class VoicePlayer1_7_10 implements VoicePlayer {

	private final ServerPlayerEntity player;

	public VoicePlayer1_7_10(ServerPlayerEntity player) {
		this.player = player;
	}

	@Override
	public int getNetworkId() {
		return player.getNetworkId();
	}

	@Override
	public String getUniqueId() {
		return Integer.toString(player.getNetworkId());
	}

	@Override
	public String getName() {
		return player.getName();
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
