package com.moyettes.legacyvoicechat.mixin.client;

import com.moyettes.legacyvoicechat.api.client.ClientConnectionEvents;
import com.moyettes.legacyvoicechat.utils.MinecraftAccessor;
import net.minecraft.client.network.ClientNetworkHandler;
import net.minecraft.network.packet.login.LoginHelloPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientNetworkHandler.class)
public class ClientNetworkHandlerMixin {

	@Inject(method = "onHello", at = @At("TAIL"))
	private void onLoginSuccess(LoginHelloPacket packet, CallbackInfo ci) {
		ClientConnectionEvents.PLAY_READY.invoker().accept(MinecraftAccessor.getMinecraft());
	}
}
