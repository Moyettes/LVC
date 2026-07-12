package com.moyettes.legacyvoicechat.mixin;

import net.minecraft.client.network.handler.ClientNetworkHandler;
import net.minecraft.network.Connection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

// Accessor-only mixin kept as an interface so mod_Voice can cast
// ClientNetworkHandler to this interface at compile time. Active injection
// (proactive handshake + PLAY_READY on connect) lives in a separate
// ClientNetworkHandlerInjectMixin since Mixin only permits @Inject on class
// mixins, not interface mixins.
@Mixin(ClientNetworkHandler.class)
public interface ClientNetworkHandlerMixin {

	@Accessor("connection")
	Connection getConnection();
}
