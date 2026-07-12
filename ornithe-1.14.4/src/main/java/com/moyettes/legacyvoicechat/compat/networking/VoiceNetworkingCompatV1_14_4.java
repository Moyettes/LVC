package com.moyettes.legacyvoicechat.compat.networking;

import com.moyettes.legacyvoicechat.Voice;
import com.moyettes.legacyvoicechat.packet.VoiceModSupportedPayload;
import com.moyettes.legacyvoicechat.packet.VoiceServerInfoPayload;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.resource.Identifier;
import net.ornithemc.osl.networking.api.server.ServerConnectionEvents;
import net.ornithemc.osl.networking.api.server.ServerPlayNetworking;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class VoiceNetworkingCompatV1_14_4 implements VoiceNetworkingCompat {

	public static final class OslModSupported extends VoiceModSupportedPayload
		implements net.ornithemc.osl.networking.api.CustomPayload {
		public OslModSupported() { super(); }

		@Override
		public void read(PacketByteBuf buf) throws IOException {
			byte[] bytes = new byte[buf.readableBytes()];
			buf.readBytes(bytes);
			super.read(new DataInputStream(new ByteArrayInputStream(bytes)));
		}

		@Override
		public void write(PacketByteBuf buf) throws IOException {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			super.write(new DataOutputStream(baos));
			buf.writeBytes(baos.toByteArray());
		}
	}

	public static final class OslServerInfo extends VoiceServerInfoPayload
		implements net.ornithemc.osl.networking.api.CustomPayload {
		public OslServerInfo() { super(); }

		public OslServerInfo(int port, String token, String protocolVersion, String host) {
			super(port, token, protocolVersion, host);
		}

		@Override
		public void read(PacketByteBuf buf) throws IOException {
			byte[] bytes = new byte[buf.readableBytes()];
			buf.readBytes(bytes);
			super.read(new DataInputStream(new ByteArrayInputStream(bytes)));
		}

		@Override
		public void write(PacketByteBuf buf) throws IOException {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			super.write(new DataOutputStream(baos));
			buf.writeBytes(baos.toByteArray());
		}
	}

	@Override
	public void registerNetworkListeners() {
		final Identifier MOD_SUPPORTED = new Identifier("voice", "mod_supported");
		final Identifier SERVER_INFO = new Identifier("voice", "server_info");

		ServerPlayNetworking.registerListener(
			MOD_SUPPORTED,
			OslModSupported::new,
			(server, handler, player, payload) -> {
				String playerName = player.getName().getString();
				int networkId = player.getNetworkId();

				Voice.LOGGER.info("Player " + playerName + " supports voice mod with protocol " + payload.getProtocolVersion());

				String authToken = Voice.generateAuthToken(playerName);
				Voice.getPlayerAuthTokens().put(networkId, authToken);

				OslServerInfo serverInfo = new OslServerInfo(
					Voice.getVoiceServer().getPort(),
					authToken,
					Voice.getVoiceServer().getProtocolVersion(),
					Voice.getVoiceServer().getConfig().getServerHost()
				);

				ServerPlayNetworking.send(player, SERVER_INFO, serverInfo);
				Voice.LOGGER.info("Sent voice server info to " + playerName);
				return true;
			}
		);

		ServerConnectionEvents.DISCONNECT.register((handler, p) -> {
			if (Voice.getVoiceServer() != null) {
				Voice.getVoiceServer().disconnectPlayer(p.getNetworkId());
			}
			Voice.getPlayerAuthTokens().remove(p.getNetworkId());
		});
	}
}
