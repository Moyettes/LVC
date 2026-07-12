package com.moyettes.legacyvoicechat.mixin;

import com.moyettes.legacyvoicechat.utils.MinecraftServerAccessor;
import com.moyettes.legacyvoicechat.Voice;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.DedicatedServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DedicatedServer.class)
public class MinecraftServerMixin {

	@Inject(method = "init", at = @At("TAIL"))
	public void assignMinecraft(CallbackInfoReturnable<Boolean> cir) {
		MinecraftServerAccessor.setInstance((MinecraftServer) (Object) this);
		Voice.onServerStarted();
	}
}
