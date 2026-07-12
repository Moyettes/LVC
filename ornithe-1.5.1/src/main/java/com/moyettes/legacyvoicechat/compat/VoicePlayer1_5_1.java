package com.moyettes.legacyvoicechat.compat;

import net.minecraft.server.entity.living.player.ServerPlayerEntity;

public class VoicePlayer1_5_1 implements VoicePlayer {

	private final ServerPlayerEntity player;

	public VoicePlayer1_5_1(ServerPlayerEntity player) {
		this.player = player;
	}

	@Override
	public int getNetworkId() {
		return player.networkId;
	}

	@Override
	public String getUniqueId() {
		return Integer.toString(player.networkId);
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

	@Override
	public String getName() {
		return player.name;
	}
}
