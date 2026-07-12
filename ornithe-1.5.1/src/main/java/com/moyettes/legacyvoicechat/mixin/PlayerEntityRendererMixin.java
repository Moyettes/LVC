package com.moyettes.legacyvoicechat.mixin;

import com.moyettes.legacyvoicechat.client.Voice;
import com.moyettes.legacyvoicechat.client.VoiceClient;
import com.moyettes.legacyvoicechat.events.ClientEvents;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.entity.living.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntityRenderer.class)
public class PlayerEntityRendererMixin {

	@Inject(method = "render(Lnet/minecraft/entity/living/player/PlayerEntity;DDDFF)V", at = @At("TAIL"))
	public void renderIcons(PlayerEntity playerEntity, double d, double e, double f, float g, float h, CallbackInfo ci) {
		VoiceClient vc = Voice.getVoiceClient();
		if(vc == null){
			return;
		}

		ClientEvents.INSTANCE.onRenderPlayerIcons(playerEntity.networkId, d, e, f);
	}
}
