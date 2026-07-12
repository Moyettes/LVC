package com.moyettes.legacyvoicechat.packet;

import com.moyettes.legacyvoicechat.api.Channels;
import com.moyettes.legacyvoicechat.api.DataStreams;
import com.moyettes.legacyvoicechat.api.IOConsumer;
import com.moyettes.legacyvoicechat.utils.MinecraftAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.handler.ClientNetworkHandler;
import net.minecraft.network.packet.Packet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

public class ClientPlayNetworking {

	private static final Logger LOGGER = LogManager.getLogger("Client Play Networking");

	public static final Map<String, Listener> LISTENERS = new LinkedHashMap<>();

	public static <T extends CustomPayload> void registerListener(
			String channel,
			Supplier<T> initializer,
			com.moyettes.legacyvoicechat.api.client.ClientPlayNetworking.PayloadListener<T> listener) {
		registerListenerImpl(channel, (minecraft, handler, data) -> {
			T payload = initializer.get();
			payload.read(DataStreams.input(data));
			return listener.handle(minecraft, handler, payload);
		});
	}

	private static void registerListenerImpl(String channel, Listener listener) {
		LISTENERS.compute(channel, (key, value) -> {
			Channels.validate(channel);
			if (value != null) {
				throw new IllegalStateException("there is already a listener on channel '" + channel + "'");
			}
			return listener;
		});
	}

	public static boolean handle(ClientNetworkHandler handler, CustomPayloadPacket packet) {
		Listener listener = LISTENERS.get(packet.channel);
		if (listener != null) {
			try {
				return listener.handle(MinecraftAccessor.getMinecraft(), handler, packet.data);
			} catch (IOException e) {
				LOGGER.warn("error handling custom payload on channel '" + packet.channel + "'", e);
				return true;
			}
		}
		return false;
	}

	public static void send(String channel, CustomPayload payload) {
		Minecraft minecraft = MinecraftAccessor.getMinecraft();
		if (minecraft == null) return;
		Packet packet = makePacket(channel, payload);
		if (packet == null) return;
		ClientNetworkHandler networkHandler = minecraft.getNetworkHandler();
		if (networkHandler != null) {
			networkHandler.sendPacket(packet);
		}
	}

	private static Packet makePacket(String channel, CustomPayload payload) {
		return makePacket(channel, payload::write);
	}

	private static Packet makePacket(String channel, IOConsumer<DataOutputStream> writer) {
		try {
			return new CustomPayloadPacket(channel, DataStreams.output(writer).toByteArray());
		} catch (IOException e) {
			LOGGER.warn("error writing custom payload to channel '" + channel + "'", e);
			return null;
		}
	}

	private interface Listener {
		boolean handle(Minecraft minecraft, ClientNetworkHandler handler, byte[] data) throws IOException;
	}
}
