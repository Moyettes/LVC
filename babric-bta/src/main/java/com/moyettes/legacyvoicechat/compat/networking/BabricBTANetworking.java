package com.moyettes.legacyvoicechat.compat.networking;

import com.moyettes.legacyvoicechat.Voice;
import com.moyettes.legacyvoicechat.api.server.ServerConnectionEvents;
import com.moyettes.legacyvoicechat.packet.ServerPlayNetworking;
import com.moyettes.legacyvoicechat.packet.VoiceModSupportedPayload;
import com.moyettes.legacyvoicechat.packet.VoiceServerInfoPayload;

public class BabricBTANetworking implements VoiceNetworkingCompat {

	@Override
	public void registerNetworkListeners() {
		ServerPlayNetworking.registerListener("voice|mod_supported", VoiceModSupportedPayload::new,
			(server, handler, player, payload) -> {
				String name = player.username;
				int id = player.id;

				String token = Voice.generateAuthToken(name);
				Voice.getPlayerAuthTokens().put(id, token);

				VoiceServerInfoPayload info = new VoiceServerInfoPayload(
					Voice.getVoiceServer().getPort(),
					token,
					Voice.getVoiceServer().getProtocolVersion(),
					Voice.getVoiceServer().getConfig().getServerHost()
				);

				ServerPlayNetworking.send(player, "voice|server_info", info);
				return false;
			});

		ServerConnectionEvents.DISCONNECT.register((handler, player) -> {
			if (Voice.getVoiceServer() != null) {
				Voice.getVoiceServer().disconnectPlayer(player.id);
			}
			Voice.getPlayerAuthTokens().remove(player.id);
		});
	}
}
