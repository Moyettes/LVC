package com.moyettes.legacyvoicechat.compat;

import com.moyettes.legacyvoicechat.utils.MinecraftServerAccessor;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.entity.living.player.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.Collection;

public class b1_7_3Compat implements MinecraftCompat {

	@Override
	public VoicePlayer getPlayerById(int networkId) {
		MinecraftServer server = MinecraftServerAccessor.getMinecraftServer();
		if (server == null) {
			return null;
		}

		for (int i = 0; i < server.playerManager.players.size(); i++) {
			ServerPlayerEntity player =
				(ServerPlayerEntity) server.playerManager.players.get(i);
			if (player.networkId == networkId) {
				return new VoicePlayerb1_7_3(player);
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
					return new VoicePlayerb1_7_3(player);
				}
			}
		}
		return null;
	}

	@Override
	public Collection<VoicePlayer> getAllConnectedPlayers() {
		MinecraftServer server = MinecraftServerAccessor.getMinecraftServer();
		Collection<VoicePlayer> result = new ArrayList<>();

		for (Object obj : server.playerManager.players) {
			if (obj instanceof ServerPlayerEntity) {
				ServerPlayerEntity player = (ServerPlayerEntity) obj;
				result.add(new VoicePlayerb1_7_3(player));
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
