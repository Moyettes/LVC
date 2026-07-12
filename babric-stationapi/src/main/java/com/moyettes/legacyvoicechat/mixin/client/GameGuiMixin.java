package com.moyettes.legacyvoicechat.mixin.client;

import com.moyettes.legacyvoicechat.events.ClientEvents;
import net.minecraft.client.gui.hud.InGameHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class GameGuiMixin {

	@Inject(method = "render", at = @At("TAIL"))
	public void renderVoiceChatOverlays(float bl, boolean i, int j, int par4, CallbackInfo ci) {
		if (ClientEvents.INSTANCE != null) {
			ClientEvents.INSTANCE.onRenderHUD(bl);
		}
	}
}
