package com.moyettes.legacyvoicechat.mixin;

import net.minecraft.client.network.handler.ClientPlayNetworkHandler;
import net.minecraft.network.Connection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClientPlayNetworkHandler.class)
public interface ClientNetworkHandlerMixin {

    @Accessor("connection")
    Connection getConnection();
}
