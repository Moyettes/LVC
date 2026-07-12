package com.moyettes.legacyvoicechat.mixin;

import net.minecraft.network.RemoteConnection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.net.SocketAddress;

@Mixin(RemoteConnection.class)
public interface ConnectionInterfaceMixin {

    @Accessor("address")
    SocketAddress getAddress();
}
