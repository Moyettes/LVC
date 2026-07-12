package com.moyettes.legacyvoicechat.mixin.client;

import com.moyettes.legacyvoicechat.events.ClientEvents;
import net.minecraft.client.gui.hud.HudIngame;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = HudIngame.class, remap = false)
public class GameGuiMixin {

	@Inject(method = "renderGameOverlay", at = @At("TAIL"))
	public void renderVoiceChatOverlays(float bl, boolean i, int j, int par4, CallbackInfo ci) {
		if (ClientEvents.INSTANCE != null) {
			ClientEvents.INSTANCE.onRenderHUD(bl);
		}
	}
}
