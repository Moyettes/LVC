package com.moyettes.legacyvoicechat.mixin.client;

import com.moyettes.legacyvoicechat.events.ClientEvents;
import com.moyettes.legacyvoicechat.utils.MinecraftAccessor;
import com.moyettes.legacyvoicechat.utils.MinecraftAccessorInterface;
import net.minecraft.client.Minecraft;
import net.minecraft.client.net.handler.PacketHandlerClient;
import net.minecraft.client.player.controller.PlayerController;
import net.minecraft.client.player.controller.PlayerControllerMP;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Minecraft.class, remap = false)
public class MinecraftMixin implements MinecraftAccessorInterface {

	@Shadow
	public PlayerController playerController;

	@Inject(method = "startGame", at = @At("TAIL"))
	public void assignMinecraft(CallbackInfo ci) {
		MinecraftAccessor.setInstance((Minecraft) (Object) this);
	}

	@Inject(method = "runTick", at = @At("TAIL"))
	public void postTick(CallbackInfo ci) {
		if (ClientEvents.INSTANCE != null) {
			ClientEvents.INSTANCE.onInput();
		}
	}

	@Override
	public PacketHandlerClient getNetworkHandler() {
		return this.playerController instanceof PlayerControllerMP
			? ((PlayerControllerMPMixin) this.playerController).getConnection()
			: null;
	}
}
