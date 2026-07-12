package com.moyettes.legacyvoicechat.mixin.client;

import com.moyettes.legacyvoicechat.api.client.ClientConnectionEvents;
import com.moyettes.legacyvoicechat.utils.MinecraftAccessor;
import net.minecraft.client.net.handler.PacketHandlerClient;
import net.minecraft.core.net.packet.PacketLogin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = PacketHandlerClient.class, remap = false)
public class ClientNetworkHandlerMixin {

	@Inject(method = "handleLogin", at = @At("TAIL"))
	private void onLoginSuccess(PacketLogin packet, CallbackInfo ci) {
		ClientConnectionEvents.PLAY_READY.invoker().accept(MinecraftAccessor.getMinecraft());
	}
}
