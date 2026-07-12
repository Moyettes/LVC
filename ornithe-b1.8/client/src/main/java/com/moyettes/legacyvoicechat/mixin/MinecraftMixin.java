package com.moyettes.legacyvoicechat.mixin;

import com.moyettes.legacyvoicechat.events.ClientEvents;
import com.moyettes.legacyvoicechat.utils.MinecraftAccessor;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftMixin {

	@Inject(method = "init", at = @At("TAIL"))
	public void assignMinecraft(CallbackInfo ci) {
		MinecraftAccessor.setInstance((Minecraft) (Object) this);
	}

	@Inject(method = "tick", at = @At("TAIL"))
	public void postTick(CallbackInfo ci) {
		ClientEvents.INSTANCE.onInput();
	}
}
