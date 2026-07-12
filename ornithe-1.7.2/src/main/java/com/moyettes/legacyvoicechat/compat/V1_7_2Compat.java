package com.moyettes.legacyvoicechat.compat;

import com.moyettes.legacyvoicechat.utils.MinecraftServerAccessor;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.entity.living.player.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.ArrayList;
import java.util.Collection;

public class V1_7_2Compat implements MinecraftCompat {

	@Override
	public VoicePlayer getPlayerById(int networkId) {
		MinecraftServer server = MinecraftServerAccessor.getMinecraftServer();
		if (server == null) return null;

		for (ServerWorld world : server.worlds) {
			Entity entity = world.getEntity(networkId);
			if (entity instanceof ServerPlayerEntity) {
				return new VoicePlayer1_7_2((ServerPlayerEntity) entity);
			}
		}

		for (Object obj : server.getPlayerManager().players) {
			if (obj instanceof ServerPlayerEntity) {
				ServerPlayerEntity player = (ServerPlayerEntity) obj;
				if (player.getNetworkId() == networkId) {
					return new VoicePlayer1_7_2(player);
				}
			}
		}

		return null;
	}

	@Override
	public VoicePlayer getPlayerByName(String name) {
		MinecraftServer server = MinecraftServerAccessor.getMinecraftServer();
		if (server == null) return null;
		for (Object obj : server.getPlayerManager().players) {
			if (obj instanceof ServerPlayerEntity) {
				ServerPlayerEntity player = (ServerPlayerEntity) obj;
				if (player.getName().equals(name)) {
					return new VoicePlayer1_7_2(player);
				}
			}
		}
		return null;
	}

	@Override
	public Collection<VoicePlayer> getAllConnectedPlayers() {
		MinecraftServer server = MinecraftServerAccessor.getMinecraftServer();
		Collection<VoicePlayer> result = new ArrayList<>();
		if (server == null) return result;

		for (Object obj : server.getPlayerManager().players) {
			if (obj instanceof ServerPlayerEntity) {
				ServerPlayerEntity player = (ServerPlayerEntity) obj;
				result.add(new VoicePlayer1_7_2(player));
			}
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
