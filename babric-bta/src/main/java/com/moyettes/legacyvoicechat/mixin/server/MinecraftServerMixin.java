package com.moyettes.legacyvoicechat.mixin.server;

import com.moyettes.legacyvoicechat.utils.MinecraftServerAccessor;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = MinecraftServer.class, remap = false)
public class MinecraftServerMixin {

	@Inject(method = "startServer", at = @At("TAIL"))
	public void assignMinecraft(CallbackInfoReturnable<Boolean> cir) {
		MinecraftServerAccessor.setInstance((MinecraftServer) (Object) this);
	}
}
