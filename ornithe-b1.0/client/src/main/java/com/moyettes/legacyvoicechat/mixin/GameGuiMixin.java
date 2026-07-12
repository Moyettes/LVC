package com.moyettes.legacyvoicechat.mixin;


import com.moyettes.legacyvoicechat.events.ClientEvents;
import net.minecraft.client.gui.GameGui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameGui.class)
public class GameGuiMixin {

	@Inject(method = "render", at = @At("TAIL"))
	public void renderVoiceChatOverlays(float bl, boolean i, int j, int par4, CallbackInfo ci) {
		ClientEvents.INSTANCE.onRenderHUD(bl);
	}
}
