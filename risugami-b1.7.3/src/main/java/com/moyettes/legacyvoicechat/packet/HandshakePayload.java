package com.moyettes.legacyvoicechat.packet;

import com.moyettes.legacyvoicechat.api.Channels;
import net.minecraft.network.packet.Packet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;

public class HandshakePayload implements CustomPayload {

	public static final String CHANNEL = "OSL|Handshake";

	public Set<String> channels;

	public HandshakePayload() {
	}

	public HandshakePayload(Set<String> channels) {
		this.channels = channels;
	}

	public static HandshakePayload client() {
		return new HandshakePayload(ClientPlayNetworking.LISTENERS.keySet());
	}

	@Override
	public void read(DataInputStream input) throws IOException {
		channels = new LinkedHashSet<>();
		int channelCount = input.readInt();
		if (channelCount > 0) {
			for (int i = 0; i < channelCount; i++) {
				channels.add(Packet.readString(input, Channels.MAX_LENGTH));
			}
		}
	}

	@Override
	public void write(DataOutputStream output) throws IOException {
		output.writeInt(channels.size());
		for (String channel : channels) {
			Packet.writeString(channel, output);
		}
	}
}
