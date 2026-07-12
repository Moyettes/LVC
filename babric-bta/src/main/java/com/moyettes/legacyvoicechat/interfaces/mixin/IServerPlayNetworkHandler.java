package com.moyettes.legacyvoicechat.interfaces.mixin;

import net.minecraft.server.entity.player.PlayerServer;

import java.util.Set;

public interface IServerPlayNetworkHandler {

	PlayerServer osl$networking$getPlayer();

    boolean osl$networking$isPlayReady();

    void osl$networking$registerClientChannels(Set<String> channels);

    boolean osl$networking$isRegisteredClientChannel(String channel);

}
