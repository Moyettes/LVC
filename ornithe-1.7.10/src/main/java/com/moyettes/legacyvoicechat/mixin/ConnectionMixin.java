package com.moyettes.legacyvoicechat.mixin;

import com.moyettes.legacyvoicechat.utils.MinecraftAccessor;
import net.minecraft.network.Connection;
import net.ornithemc.osl.networking.api.client.ClientConnectionEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Connection.class)
public class ConnectionMixin {


	@Inject(
		method = "disconnect(Lnet/minecraft/text/Text;)V",
		at = @At("HEAD")
	)
	private void onDisconnect(CallbackInfo ci) {
		ClientConnectionEvents.DISCONNECT.invoker().accept(MinecraftAccessor.getMinecraft());
	}
}
