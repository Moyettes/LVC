package com.moyettes.legacyvoicechat.mixin.client;

import net.minecraft.client.net.handler.PacketHandlerClient;
import net.minecraft.client.player.controller.PlayerControllerMP;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = PlayerControllerMP.class, remap = false)
public interface PlayerControllerMPMixin {

	@Accessor("netHandler")
	PacketHandlerClient getConnection();
}
