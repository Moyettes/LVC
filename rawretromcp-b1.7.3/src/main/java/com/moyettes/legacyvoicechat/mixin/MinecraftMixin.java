package com.moyettes.legacyvoicechat.mixin;


import com.moyettes.legacyvoicechat.Voice;
import com.moyettes.legacyvoicechat.events.ClientEvents;
import com.moyettes.legacyvoicechat.utils.MinecraftAccessor;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftMixin {

	// Matches the standalone's pattern: the patched Minecraft.startGame() in
	// All Versions/RawRetroMCP-b1.7.3 calls MinecraftAccessor.setInstance(this)
	// and Voice.init() as the very first two lines. In Feather b1.7.3 that
	// method is `init` - we inject at HEAD so we run BEFORE GameOptions is
	// constructed (which happens later in the same method body). Getting this
	// order right is what lets GameOptionsMixin's keybind event find our
	// KeyEvents listener already registered when its invoker fires.
	@Inject(method = "init", at = @At("HEAD"))
	public void assignMinecraft(CallbackInfo ci) {
		MinecraftAccessor.setInstance((Minecraft) (Object) this);
		Voice.init();
	}

	@Inject(method = "tick", at = @At("TAIL"))
	public void postTick(CallbackInfo ci) {
		ClientEvents.INSTANCE.onInput();
	}
}
