package com.moyettes.legacyvoicechat.mixin.client;

import net.minecraft.client.net.handler.PacketHandlerClient;
import net.minecraft.core.net.NetworkManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = PacketHandlerClient.class, remap = false)
public interface ClientNetworkHandlerInterfaceMixin {

	@Accessor("netManager")
	NetworkManager getConnection();
}
