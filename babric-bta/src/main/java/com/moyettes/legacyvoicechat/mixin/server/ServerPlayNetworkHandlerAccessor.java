package com.moyettes.legacyvoicechat.mixin.server;

import net.minecraft.server.entity.player.PlayerServer;
import net.minecraft.server.net.handler.PacketHandlerServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;


@Mixin(value = PacketHandlerServer.class, remap = false)
public interface ServerPlayNetworkHandlerAccessor {
	@Accessor
	PlayerServer getPlayerEntity();
}
