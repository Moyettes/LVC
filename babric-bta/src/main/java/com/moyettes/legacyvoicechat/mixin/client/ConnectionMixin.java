package com.moyettes.legacyvoicechat.mixin.client;

import com.moyettes.legacyvoicechat.api.client.ClientConnectionEvents;
import com.moyettes.legacyvoicechat.utils.MinecraftAccessor;
import net.minecraft.core.net.NetworkManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = NetworkManager.class, remap = false)
public class ConnectionMixin {

	@Inject(method = "networkShutdown(Ljava/lang/String;[Ljava/lang/Object;)V", at = @At("HEAD"))
	private void onDisconnect(CallbackInfo ci) {
		ClientConnectionEvents.DISCONNECT.invoker().accept(MinecraftAccessor.getMinecraft());
	}
}
