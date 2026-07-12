package com.moyettes.legacyvoicechat.interfaces.mixin;

import net.minecraft.entity.player.ServerPlayerEntity;

import java.util.Set;

public interface IServerPlayNetworkHandler {

    ServerPlayerEntity osl$networking$getPlayer();

    boolean osl$networking$isPlayReady();

    void osl$networking$registerClientChannels(Set<String> channels);

    boolean osl$networking$isRegisteredClientChannel(String channel);

}
