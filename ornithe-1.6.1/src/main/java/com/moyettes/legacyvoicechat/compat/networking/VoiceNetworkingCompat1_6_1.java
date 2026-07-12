package com.moyettes.legacyvoicechat.compat.networking;

import com.moyettes.legacyvoicechat.Voice;
import com.moyettes.legacyvoicechat.packet.VoiceModSupportedPayload;
import com.moyettes.legacyvoicechat.packet.VoiceServerInfoPayload;
import net.ornithemc.osl.networking.api.server.ServerConnectionEvents;
import net.ornithemc.osl.networking.api.server.ServerPlayNetworking;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;

public class VoiceNetworkingCompat1_6_1 implements VoiceNetworkingCompat {

	private static void bridgeRead(VoiceModSupportedPayload p, DataInput input) throws IOException {
		if (input instanceof DataInputStream) {
			p.read((DataInputStream) input);
		} else {
			throw new IOException("Expected DataInputStream from OSL");
		}
	}

	private static void bridgeRead(VoiceServerInfoPayload p, DataInput input) throws IOException {
		if (input instanceof DataInputStream) {
			p.read((DataInputStream) input);
		} else {
			throw new IOException("Expected DataInputStream from OSL");
		}
	}

	private static void bridgeWrite(VoiceModSupportedPayload p, DataOutput output) throws IOException {
		if (output instanceof DataOutputStream) {
			p.write((DataOutputStream) output);
		} else {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			p.write(new DataOutputStream(baos));
			output.write(baos.toByteArray());
		}
	}

	private static void bridgeWrite(VoiceServerInfoPayload p, DataOutput output) throws IOException {
		if (output instanceof DataOutputStream) {
			p.write((DataOutputStream) output);
		} else {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			p.write(new DataOutputStream(baos));
			output.write(baos.toByteArray());
		}
	}

	public static final class OslModSupported extends VoiceModSupportedPayload
		implements net.ornithemc.osl.networking.api.CustomPayload {
		public OslModSupported() { super(); }

		@Override
		public void read(DataInput input) throws IOException {
			bridgeRead(this, input);
		}

		@Override
		public void write(DataOutput output) throws IOException {
			bridgeWrite(this, output);
		}
	}

	public static final class OslServerInfo extends VoiceServerInfoPayload
		implements net.ornithemc.osl.networking.api.CustomPayload {
		public OslServerInfo() { super(); }
		public OslServerInfo(int port, String token, String protocolVersion, String host) {
			super(port, token, protocolVersion, host);
		}

		@Override
		public void read(DataInput input) throws IOException {
			bridgeRead(this, input);
		}

		@Override
		public void write(DataOutput output) throws IOException {
			bridgeWrite(this, output);
		}
	}

	@Override
	public void registerNetworkListeners() {
		ServerPlayNetworking.registerListener("voice|mod_supported", OslModSupported::new,
			(server, handler, player, payload) -> {
				String name = player.getName();
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
