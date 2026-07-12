package com.moyettes.legacyvoicechat.mixin;

import com.moyettes.legacyvoicechat.utils.MinecraftAccessor;
import net.minecraft.network.RemoteConnection;
import net.ornithemc.osl.networking.api.client.ClientConnectionEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RemoteConnection.class)
public class ConnectionMixin {


	@Inject(
		method = "disconnect(Ljava/lang/String;[Ljava/lang/Object;)V",
		at = @At("HEAD")
	)
	private void onDisconnect(CallbackInfo ci) {
		ClientConnectionEvents.DISCONNECT.invoker().accept(MinecraftAccessor.getMinecraft());
	}
}
