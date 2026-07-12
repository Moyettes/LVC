package com.moyettes.legacyvoicechat.api.client;

import com.moyettes.legacyvoicechat.packet.CustomPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.net.handler.PacketHandlerClient;

import java.io.IOException;

public class ClientPlayNetworking {

	public interface PayloadListener<T extends CustomPayload> {
		boolean handle(Minecraft minecraft, PacketHandlerClient handler, T payload) throws IOException;
	}
}
