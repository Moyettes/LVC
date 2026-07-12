package com.moyettes.legacyvoicechat.compat;

import net.minecraft.server.entity.living.player.ServerPlayerEntity;

public class VoicePlayer1_14_4 implements VoicePlayer {

	private final ServerPlayerEntity player;

	public VoicePlayer1_14_4(ServerPlayerEntity player) {
		this.player = player;
	}

	@Override
	public int getNetworkId() {
		return player.getNetworkId();
	}

	@Override
	public String getUniqueId() {
		return player.getUuid().toString();
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
		return player.getName().getString();
	}
}
