package com.moyettes.legacyvoicechat.mixin;

import com.moyettes.legacyvoicechat.api.client.ClientConnectionEvents;
import com.moyettes.legacyvoicechat.packet.ClientPlayNetworking;
import com.moyettes.legacyvoicechat.packet.HandshakePayload;
import com.moyettes.legacyvoicechat.utils.MinecraftAccessor;
import net.minecraft.client.network.handler.ClientNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Risugami doesn't wait on a server-initiated OSL handshake - the client has
// to speak first. Mirrors the standalone's NetClientHandler.handleLogin patch
// which sends the handshake payload + fires PLAY_READY at the tail of the
// login handler. Injecting into <init> instead would no-op silently because
// Minecraft only assigns mc.networkHandler after the constructor returns -
// any sendPacket() call from inside <init> can't find a handler to use.
@Mixin(ClientNetworkHandler.class)
public class ClientNetworkHandlerInjectMixin {

	@Inject(method = "handleLogin", at = @At("TAIL"))
	public void legacyvoicechat$sendHandshakeOnConnect(CallbackInfo ci) {
		ClientPlayNetworking.send(HandshakePayload.CHANNEL, HandshakePayload.client());
		ClientConnectionEvents.PLAY_READY.invoker().accept(MinecraftAccessor.getMinecraft());
	}
}
