package com.moyettes.legacyvoicechat.mixin.server;

import com.moyettes.legacyvoicechat.api.server.ServerConnectionEvents;
import com.moyettes.legacyvoicechat.interfaces.mixin.IServerPlayNetworkHandler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.entity.player.PlayerServer;
import net.minecraft.server.net.handler.PacketHandlerServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.LinkedHashSet;
import java.util.Set;

@Mixin(value = PacketHandlerServer.class, remap = false)
public class ServerPlayNetworkHandlerMixin implements IServerPlayNetworkHandler {

    @Shadow
    private MinecraftServer mcServer;
    @Shadow
    private PlayerServer playerEntity;

    /**
     * Channels that the client is listening to.
     */
    @Unique
    private Set<String> clientChannels;

    @Inject(
            method = "kickPlayer",
            at = @At(
                    value = "HEAD"
            )
    )
    private void osl$networking$handleDisconnect(CallbackInfo ci) {
        ServerConnectionEvents.DISCONNECT.invoker().accept(mcServer, playerEntity);
        clientChannels = null;
    }

    @Override
    public PlayerServer osl$networking$getPlayer() {
        return playerEntity;
    }

    @Override
    public boolean osl$networking$isPlayReady() {
        return clientChannels != null;
    }

    @Override
    public void osl$networking$registerClientChannels(Set<String> channels) {
        clientChannels = new LinkedHashSet<>(channels);
    }

    @Override
    public boolean osl$networking$isRegisteredClientChannel(String channel) {
        return clientChannels != null && clientChannels.contains(channel);
    }
}
