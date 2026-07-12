package com.moyettes.legacyvoicechat.mixin.client;

import com.moyettes.legacyvoicechat.client.VoiceSession;
import com.moyettes.legacyvoicechat.client.VoiceClient;
import com.moyettes.legacyvoicechat.events.ClientEvents;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntityRenderer.class)
public class PlayerEntityRendererMixin {

	@Inject(method = "renderNameTag(Lnet/minecraft/entity/player/PlayerEntity;DDD)V", at = @At("TAIL"))
	public void renderIcons(PlayerEntity playerEntity, double d, double e, double f, CallbackInfo ci) {
		VoiceClient vc = VoiceSession.getActive();
		if (vc == null) return;
		if (ClientEvents.INSTANCE != null) {
			ClientEvents.INSTANCE.onRenderPlayerIcons(playerEntity.id, d, e, f);
		}
	}
}
