package com.moyettes.legacyvoicechat.compat.networking;

import com.moyettes.legacyvoicechat.Voice;
import com.moyettes.legacyvoicechat.packet.VoiceModSupportedPayload;
import com.moyettes.legacyvoicechat.packet.VoiceServerInfoPayload;
import net.ornithemc.osl.networking.api.server.ServerConnectionEvents;
import net.ornithemc.osl.networking.api.server.ServerPlayNetworking;

public class VoiceNetworkingCompatb1_0 implements VoiceNetworkingCompat {

	public static final class OslModSupported extends VoiceModSupportedPayload
		implements net.ornithemc.osl.networking.api.CustomPayload {
		public OslModSupported() {
			super();
		}
	}

	public static final class OslServerInfo extends VoiceServerInfoPayload
		implements net.ornithemc.osl.networking.api.CustomPayload {
		public OslServerInfo() {
			super();
		}

		public OslServerInfo(int port, String token, String protocolVersion, String host) {
			super(port, token, protocolVersion, host);
		}
	}

	@Override
	public void registerNetworkListeners() {
		ServerPlayNetworking.registerListener("voice|mod_supported", OslModSupported::new,
			(server, handler, player, payload) -> {
				String name = player.name;
				int id = player.networkId;

				String token = Voice.generateAuthToken(name);
				Voice.getPlayerAuthTokens().put(id, token);

				OslServerInfo info = new OslServerInfo(
					Voice.getVoiceServer().getPort(),
					token,
					Voice.getVoiceServer().getProtocolVersion(),
					Voice.getVoiceServer().getConfig().getServerHost()
				);

				ServerPlayNetworking.send(player, "voice|server_info", info);
				return false;
			});

		ServerConnectionEvents.DISCONNECT.register((handler, player) -> {
			Voice.getVoiceServer().disconnectPlayer(player.networkId);
			Voice.getPlayerAuthTokens().remove(player.networkId);
		});
	}
}
