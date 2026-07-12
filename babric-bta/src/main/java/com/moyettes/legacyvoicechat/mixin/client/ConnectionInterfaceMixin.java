package com.moyettes.legacyvoicechat.mixin.client;

import net.minecraft.core.net.NetworkManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.net.SocketAddress;

@Mixin(value = NetworkManager.class, remap = false)
public interface ConnectionInterfaceMixin {

	@Accessor("address")
	SocketAddress getAddress();
}
