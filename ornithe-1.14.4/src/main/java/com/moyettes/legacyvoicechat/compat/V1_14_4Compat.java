package com.moyettes.legacyvoicechat.compat;

import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.entity.living.player.ServerPlayerEntity;
import com.moyettes.legacyvoicechat.utils.MinecraftServerAccessor;
import net.minecraft.server.world.ServerWorld;

import java.util.ArrayList;
import java.util.Collection;


public class V1_14_4Compat implements MinecraftCompat{

	@Override
	public VoicePlayer getPlayerById(int networkId) {
		MinecraftServer server = MinecraftServerAccessor.getMinecraftServer();
		if (server == null) return null;

		for (ServerWorld world : server.getWorlds()) {
			Entity entity = world.getEntity(networkId);
			if (entity instanceof ServerPlayerEntity) {
				return new VoicePlayer1_14_4((ServerPlayerEntity) entity);
			}
		}

		for (ServerPlayerEntity player : server.getPlayerManager().getAll()) {
			if (player.getNetworkId() == networkId) {
				return new VoicePlayer1_14_4(player);
			}
		}

		return null;
	}

	@Override
	public VoicePlayer getPlayerByName(String name) {
		MinecraftServer server = MinecraftServerAccessor.getMinecraftServer();
		if (server == null) return null;
		for (ServerPlayerEntity player : server.getPlayerManager().getAll()) {
			if (player.getName().getString().equals(name)) {
				return new VoicePlayer1_14_4(player);
			}
		}
		return null;
	}

	@Override
	public Collection<VoicePlayer> getAllConnectedPlayers() {
		MinecraftServer server = MinecraftServerAccessor.getMinecraftServer();
		Collection<VoicePlayer> result = new ArrayList<>();

		for (ServerPlayerEntity player : server.getPlayerManager().getAll()) {
			result.add(new VoicePlayer1_14_4(player));
		}

		return result;
	}

	@Override
	public double getDistanceSquared(VoicePlayer a, VoicePlayer b) {
		double dx = a.getX() - b.getX();
		double dy = a.getY() - b.getY();
		double dz = a.getZ() - b.getZ();
		return dx * dx + dy * dy + dz * dz;
	}
}
