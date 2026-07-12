package com.moyettes.legacyvoicechat.compat;

import com.moyettes.legacyvoicechat.utils.MinecraftServerAccessor;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.entity.player.PlayerServer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class BabricBTACompat implements MinecraftCompat {

	@Override
	public VoicePlayer getPlayerById(int networkId) {
		MinecraftServer server = MinecraftServerAccessor.getMinecraftServer();
		if (server == null) return null;
		List<?> players = server.playerList.playerEntities;
		for (Object obj : players) {
			if (obj instanceof PlayerServer) {
				PlayerServer player = (PlayerServer) obj;
				if (player.id == networkId) {
					return new BabricBTAVoicePlayer(player);
				}
			}
		}
		return null;
	}

	@Override
	public VoicePlayer getPlayerByName(String name) {
		MinecraftServer server = MinecraftServerAccessor.getMinecraftServer();
		if (server == null) return null;
		List<?> players = server.playerList.playerEntities;
		for (Object obj : players) {
			if (obj instanceof PlayerServer) {
				PlayerServer player = (PlayerServer) obj;
				if (player.username.equals(name)) {
					return new BabricBTAVoicePlayer(player);
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
		List<?> players = server.playerList.playerEntities;
		for (Object obj : players) {
			if (obj instanceof PlayerServer) {
				result.add(new BabricBTAVoicePlayer((PlayerServer) obj));
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
