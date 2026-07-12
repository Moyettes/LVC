package com.moyettes.legacyvoicechat.mixin.server;

import com.moyettes.legacyvoicechat.api.server.ServerConnectionEvents;
import com.moyettes.legacyvoicechat.interfaces.mixin.IServerPlayNetworkHandler;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.LinkedHashSet;
import java.util.Set;

@Mixin(ServerPlayNetworkHandler.class)
public class ServerPlayNetworkHandlerMixin implements IServerPlayNetworkHandler {

    @Shadow
    private MinecraftServer server;
    @Shadow
    private ServerPlayerEntity player;

    /**
     * Channels that the client is listening to.
     */
    @Unique
    private Set<String> clientChannels;

    @Inject(
            method = "onDisconnect",
            at = @At(
                    value = "HEAD"
            )
    )
    private void osl$networking$handleDisconnect(CallbackInfo ci) {
        ServerConnectionEvents.DISCONNECT.invoker().accept(server, player);
        clientChannels = null;
    }

    @Override
    public ServerPlayerEntity osl$networking$getPlayer() {
        return player;
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
