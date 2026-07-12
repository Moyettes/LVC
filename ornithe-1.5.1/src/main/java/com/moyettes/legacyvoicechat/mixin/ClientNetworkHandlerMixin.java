package com.moyettes.legacyvoicechat.mixin;

import net.minecraft.client.network.handler.ClientNetworkHandler;
import net.minecraft.network.Connection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClientNetworkHandler.class)
public interface ClientNetworkHandlerMixin {
    
    @Accessor("connection")
    Connection getConnection();
}
