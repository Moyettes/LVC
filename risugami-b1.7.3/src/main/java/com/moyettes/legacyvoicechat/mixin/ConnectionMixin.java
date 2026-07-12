package com.moyettes.legacyvoicechat.mixin;

import net.minecraft.network.Connection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.net.SocketAddress;

// Accessor-only mixin kept as an interface so mod_Voice can cast Connection to
// ConnectionMixin at compile time. Active injection (DISCONNECT event on
// network shutdown) lives in a separate ConnectionInjectMixin.
@Mixin(Connection.class)
public interface ConnectionMixin {

	@Accessor("address")
	SocketAddress getAddress();
}
