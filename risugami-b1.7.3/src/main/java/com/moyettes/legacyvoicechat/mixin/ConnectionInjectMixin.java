package com.moyettes.legacyvoicechat.mixin;

import com.moyettes.legacyvoicechat.api.client.ClientConnectionEvents;
import com.moyettes.legacyvoicechat.utils.MinecraftAccessor;
import net.minecraft.network.Connection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Mirrors the standalone's NetworkManager.networkShutdown patch: fire
// DISCONNECT at the head of the shutdown method. Feather renames this to
// Connection.close(String, Object...).
@Mixin(Connection.class)
public class ConnectionInjectMixin {

	@Inject(method = "close(Ljava/lang/String;[Ljava/lang/Object;)V", at = @At("HEAD"))
	public void legacyvoicechat$fireDisconnect(String reason, Object[] args, CallbackInfo ci) {
		ClientConnectionEvents.DISCONNECT.invoker().accept(MinecraftAccessor.getMinecraft());
	}
}
