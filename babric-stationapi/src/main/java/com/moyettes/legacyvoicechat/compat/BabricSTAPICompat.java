package com.moyettes.legacyvoicechat.compat;

import com.moyettes.legacyvoicechat.utils.MinecraftServerAccessor;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.Collection;

public class BabricSTAPICompat implements MinecraftCompat {

	@Override
	public VoicePlayer getPlayerById(int networkId) {
		MinecraftServer server = MinecraftServerAccessor.getMinecraftServer();
		if (server == null) return null;
		for (Object obj : server.playerManager.players) {
			if (obj instanceof ServerPlayerEntity) {
				ServerPlayerEntity player = (ServerPlayerEntity) obj;
				if (player.id == networkId) {
					return new BabricSTAPIVoicePlayer(player);
				}
			}
		}
		return null;
	}

	@Override
	public VoicePlayer getPlayerByName(String name) {
		MinecraftServer server = MinecraftServerAccessor.getMinecraftServer();
		if (server == null) return null;
		for (Object obj : server.playerManager.players) {
			if (obj instanceof ServerPlayerEntity) {
				ServerPlayerEntity player = (ServerPlayerEntity) obj;
				if (player.name.equals(name)) {
					return new BabricSTAPIVoicePlayer(player);
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
		for (Object obj : server.playerManager.players) {
			if (obj instanceof ServerPlayerEntity) {
				result.add(new BabricSTAPIVoicePlayer((ServerPlayerEntity) obj));
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
