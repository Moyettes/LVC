package com.moyettes.legacyvoicechat.mixin;

import com.moyettes.legacyvoicechat.client.Voice;
import com.moyettes.legacyvoicechat.client.VoiceClient;
import com.moyettes.legacyvoicechat.events.ClientEvents;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.living.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public class PlayerEntityRendererMixin {

	@Inject(method = "renderNameTag(Lnet/minecraft/entity/Entity;DDDLjava/lang/String;D)V", at = @At("TAIL"))
	public void renderIcons(Entity entity, double x, double y, double z, String name, double range, CallbackInfo ci) {
		if(entity instanceof PlayerEntity){
			VoiceClient vc = Voice.getVoiceClient();
			if(vc == null){
				return;
			}

			ClientEvents.INSTANCE.onRenderPlayerIcons(((PlayerEntity) entity).getNetworkId(), x, y, z);
		}
	}
}
