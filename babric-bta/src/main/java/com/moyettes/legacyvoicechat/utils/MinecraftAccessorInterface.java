package com.moyettes.legacyvoicechat.utils;

import net.minecraft.client.net.handler.PacketHandlerClient;

public interface MinecraftAccessorInterface {
	PacketHandlerClient getNetworkHandler();
}
