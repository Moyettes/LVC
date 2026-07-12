package com.moyettes.legacyvoicechat.events;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.moyettes.legacyvoicechat.client.Voice;
import com.moyettes.legacyvoicechat.client.VoiceClient;
import com.moyettes.legacyvoicechat.utils.MinecraftAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.render.Window;
import net.minecraft.client.resource.Identifier;
import net.minecraft.entity.living.player.PlayerEntity;
import org.lwjgl.opengl.GL11;

public class RenderEvents {

	private static final Identifier MICROPHONE_ICON = new Identifier("legacyvoicechat", "textures/icons/microphone.png");
	private static final Identifier MICROPHONE_MUTE_ICON = new Identifier("legacyvoicechat",  "textures/icons/microphone_off.png");
	private static final Identifier PLAYER_DISCONNECTED = new Identifier("legacyvoicechat", "textures/icons/player_disconnected.png");
	private static final Identifier PLAYER_MUTED = new Identifier("legacyvoicechat", "textures/icons/player_muted.png");
	private static final Identifier PLAYER_TALKING = new Identifier("legacyvoicechat", "textures/icons/player_talking.png");

	public RenderEvents() {
		ClientEvents.INSTANCE.registerRenderHUDEvent(this::onRenderHUD);
		ClientEvents.INSTANCE.registerRenderPlayerIconsEvent((networkId, d, e, f) -> {
			PlayerEntity entity = lookupPlayer(networkId);
			if (entity != null) onRenderPlayerIcons(entity, d, e, f);
		});
	}

	private PlayerEntity lookupPlayer(int networkId) {
		Minecraft mc = MinecraftAccessor.getMinecraft();
		if (mc == null || mc.world == null) return null;
		for (Object obj : mc.world.players) {
			if (obj instanceof PlayerEntity) {
				PlayerEntity player = (PlayerEntity) obj;
				if (player.getNetworkId() == networkId) return player;
			}
		}
		return null;

	}

	private void onRenderHUD() {
		Minecraft minecraft = MinecraftAccessor.getMinecraft();
		if (minecraft == null || Voice.isIconsHidden()) return;

		VoiceClient voiceClient = Voice.getVoiceClient();

		if (voiceClient == null) {
			renderHudIcon(minecraft, PLAYER_DISCONNECTED);
			return;
		}

		if(voiceClient.isDeafened()) {
			renderHudIcon(minecraft, PLAYER_MUTED);
		} else if (voiceClient.isMicMuted()) {
			renderHudIcon(minecraft, MICROPHONE_MUTE_ICON);
		} else if (voiceClient.isTalking()) {
			renderHudIcon(minecraft, MICROPHONE_ICON);
		}

	}

	private void onRenderPlayerIcons(PlayerEntity player, double d, double e, double f){
		VoiceClient voiceClient = Voice.getVoiceClient();
		if (voiceClient == null || Voice.isIconsHidden()) return;

		Integer id = player.getNetworkId();
		if (voiceClient.isPlayerVoiceSupported(id)) {
			if (voiceClient.isPlayerDeafened(id)) {
				renderPlayerIconWithTexture(player, d, e, f, PLAYER_MUTED);
			} else if (voiceClient.isPlayerTalking(id)) {
				renderPlayerIcon(player, d, e , f);
			}
		} else {
			renderPlayerIconWithTexture(player, d, e, f, PLAYER_DISCONNECTED);
		}
	}

	private void renderPlayerIconWithTexture(PlayerEntity player, double d, double e, double f, Identifier texturePath) {
		Minecraft mc = MinecraftAccessor.getMinecraft();
		if (mc == null) return;

		double baseNameTagY = player.isSneaking() ? 2.1 : 2.3;
		final double yOffset = baseNameTagY + 0.2;

		GL11.glPushMatrix();
		GL11.glTranslated(d, e + yOffset, f);
		GL11.glNormal3f(0.0F, 1.0F, 0.0F);
		GL11.glRotatef(-mc.player.yaw, 0.0F, 1.0F, 0.0F);
		GL11.glRotatef(mc.player.pitch, 1.0F, 0.0F, 0.0F);

		final float scale = 0.016666668F * 0.8F;
		GL11.glScalef(-scale, -scale, scale);

		GL11.glDisable(GL11.GL_LIGHTING);
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

		GL11.glEnable(GL11.GL_DEPTH_TEST);
		GL11.glDepthMask(true);
		GL11.glDisable(GL11.GL_CULL_FACE);

		mc.getTextureManager().bind(texturePath);

		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);

		int iconSize = 16;
		int half = iconSize / 2;

		BufferBuilder buffer = BufferBuilder.INSTANCE;
		buffer.start();
		buffer.vertex(-half, -half, 0, 0, 0);
		buffer.vertex(half, -half, 0, 1, 0);
		buffer.vertex(half, half, 0, 1, 1);
		buffer.vertex(-half, half, 0, 0, 1);
		buffer.end();

		GL11.glDisable(GL11.GL_BLEND);
		GL11.glEnable(GL11.GL_LIGHTING);
		GL11.glEnable(GL11.GL_CULL_FACE);

		GL11.glPopMatrix();
	}

	private void renderPlayerIcon(PlayerEntity player, double d, double e, double f) {
		Minecraft mc = MinecraftAccessor.getMinecraft();
		if (mc == null) return;
		double baseNameTagY = player.isSneaking() ? 2.1 : 2.3;
		final double yOffset = baseNameTagY + 0.2;

		GL11.glPushMatrix();
		GL11.glTranslated(d, e + yOffset, f);

		GL11.glNormal3f(0.0F, 1.0F, 0.0F);
		GL11.glRotatef(-mc.player.yaw, 0.0F, 1.0F, 0.0F);
		GL11.glRotatef(mc.player.pitch, 1.0F, 0.0F, 0.0F);

		final float scale = 0.016666668F * 0.8F;
		GL11.glScalef(-scale, -scale, scale);

		GL11.glDisable(GL11.GL_LIGHTING);
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

		GL11.glEnable(GL11.GL_DEPTH_TEST);
		GL11.glDepthMask(true);
		GL11.glDisable(GL11.GL_CULL_FACE);

		mc.getTextureManager().bind(PLAYER_TALKING);

		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);

		int iconSize = 16;
		int half = iconSize / 2;

		BufferBuilder buffer = BufferBuilder.INSTANCE;
		buffer.start();
		buffer.vertex(-half, -half, 0, 0, 0);
		buffer.vertex(half, -half, 0, 1, 0);
		buffer.vertex(half, half, 0, 1, 1);
		buffer.vertex(-half, half, 0, 0, 1);
		buffer.end();

		GL11.glDisable(GL11.GL_BLEND);
		GL11.glEnable(GL11.GL_LIGHTING);
		GL11.glEnable(GL11.GL_CULL_FACE);

		GL11.glPopMatrix();
	}



	private void renderHudIcon(Minecraft minecraft, Identifier texturePath) {
		Window window = new Window(minecraft, minecraft.width, minecraft.height);
		int scaledHeight = window.getHeight();

		int iconSize = 16;
		int padding = 8;
		int x = padding;
		int y = scaledHeight - iconSize - padding;

		// Bind
		minecraft.getTextureManager().bind(texturePath);
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

		// Draw
		BufferBuilder bufferBuilder = BufferBuilder.INSTANCE;
		bufferBuilder.start();
		bufferBuilder.vertex((double)x, (double)(y + iconSize), 0.0D, 0.0D, 1.0D);
		bufferBuilder.vertex((double)(x + iconSize), (double)(y + iconSize), 0.0D, 1.0D, 1.0D);
		bufferBuilder.vertex((double)(x + iconSize), (double)y, 0.0D, 1.0D, 0.0D);
		bufferBuilder.vertex((double)x, (double)y, 0.0D, 0.0D, 0.0D);
		bufferBuilder.end();

		GL11.glDisable(GL11.GL_BLEND);
	}

}
