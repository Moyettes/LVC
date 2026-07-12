package com.moyettes.legacyvoicechat.mixin;

import net.minecraft.network.Connection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.net.SocketAddress;

@Mixin(Connection.class)
public interface ConnectionMixin {

    @Accessor("address")
    SocketAddress getAddress();
}
