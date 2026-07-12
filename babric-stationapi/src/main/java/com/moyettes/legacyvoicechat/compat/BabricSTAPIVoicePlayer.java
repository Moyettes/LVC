package com.moyettes.legacyvoicechat.compat;

import net.minecraft.entity.player.ServerPlayerEntity;

public class BabricSTAPIVoicePlayer implements VoicePlayer {

	private final ServerPlayerEntity player;

	public BabricSTAPIVoicePlayer(ServerPlayerEntity player) {
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
		return player.name;
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
