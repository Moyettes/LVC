package com.moyettes.legacyvoicechat.events.client;

import com.moyettes.legacyvoicechat.client.Voice;
import com.moyettes.legacyvoicechat.client.VoiceClient;
import com.moyettes.legacyvoicechat.events.ClientEvents;
import com.moyettes.legacyvoicechat.utils.MinecraftAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.util.ScreenScaler;
import org.lwjgl.opengl.GL11;

public class RenderEvents {

	private static final String MICROPHONE_ICON = "/assets/legacyvoicechat/textures/icons/microphone.png";
	private static final String MICROPHONE_MUTE_ICON = "/assets/legacyvoicechat/textures/icons/microphone_off.png";
	private static final String PLAYER_DISCONNECTED = "/assets/legacyvoicechat/textures/icons/player_disconnected.png";
	private static final String PLAYER_MUTED = "/assets/legacyvoicechat/textures/icons/player_muted.png";
	private static final String PLAYER_TALKING = "/assets/legacyvoicechat/textures/icons/player_talking.png";

	public RenderEvents() {
		ClientEvents.INSTANCE.registerRenderHUDEvent((Float tickDelta) -> onRenderHUD(tickDelta));
		ClientEvents.INSTANCE.registerRenderPlayerIconsEvent(this::onRenderPlayerIcons);
	}

	private void onRenderHUD(float tickDelta) {
		Minecraft minecraft = MinecraftAccessor.getMinecraft();
		if (minecraft == null) return;

		VoiceClient voiceClient = Voice.getVoiceClient();
		if (voiceClient == null) {
			renderHudIcon(minecraft, PLAYER_DISCONNECTED);
			return;
		}

		if (voiceClient.isDeafened()) {
			renderHudIcon(minecraft, PLAYER_MUTED);
		} else if (voiceClient.isMicMuted()) {
			renderHudIcon(minecraft, MICROPHONE_MUTE_ICON);
		} else if (voiceClient.isTalking()) {
			renderHudIcon(minecraft, MICROPHONE_ICON);
		}
	}

	private void onRenderPlayerIcons(int playerNetworkId, double d, double e, double f) {
		VoiceClient voiceClient = Voice.getVoiceClient();
		if (voiceClient == null) return;

		String texture;
		if (!voiceClient.isPlayerVoiceSupported(playerNetworkId)) {
			texture = PLAYER_DISCONNECTED;
		} else if (voiceClient.isPlayerDeafened(playerNetworkId)) {
			texture = PLAYER_MUTED;
		} else if (voiceClient.isPlayerTalking(playerNetworkId)) {
			texture = PLAYER_TALKING;
		} else {
			return;
		}

		Minecraft mc = MinecraftAccessor.getMinecraft();
		if (mc == null || mc.player == null) return;

		double baseNameTagY = 2.3;
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

		GL11.glBindTexture(GL11.GL_TEXTURE_2D, mc.textureManager.getTextureId(texture));
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);

		int iconSize = 16;
		int half = iconSize / 2;

		GL11.glBegin(GL11.GL_QUADS);
		GL11.glTexCoord2f(0.0F, 0.0F); GL11.glVertex3f(-half, -half, 0);
		GL11.glTexCoord2f(1.0F, 0.0F); GL11.glVertex3f(half, -half, 0);
		GL11.glTexCoord2f(1.0F, 1.0F); GL11.glVertex3f(half, half, 0);
		GL11.glTexCoord2f(0.0F, 1.0F); GL11.glVertex3f(-half, half, 0);
		GL11.glEnd();

		GL11.glDisable(GL11.GL_BLEND);
		GL11.glEnable(GL11.GL_LIGHTING);
		GL11.glEnable(GL11.GL_CULL_FACE);
		GL11.glPopMatrix();
	}

	private void renderHudIcon(Minecraft minecraft, String texturePath) {
		ScreenScaler window = new ScreenScaler(minecraft.options, minecraft.displayWidth, minecraft.displayHeight);
		int scaledHeight = window.getScaledHeight();

		int iconSize = 16;
		int padding = 8;
		int x = padding;
		int y = scaledHeight - iconSize - padding;

		GL11.glBindTexture(GL11.GL_TEXTURE_2D, minecraft.textureManager.getTextureId(texturePath));
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

		GL11.glBegin(GL11.GL_QUADS);
		GL11.glTexCoord2f(0.0F, 1.0F); GL11.glVertex2f(x, y + iconSize);
		GL11.glTexCoord2f(1.0F, 1.0F); GL11.glVertex2f(x + iconSize, y + iconSize);
		GL11.glTexCoord2f(1.0F, 0.0F); GL11.glVertex2f(x + iconSize, y);
		GL11.glTexCoord2f(0.0F, 0.0F); GL11.glVertex2f(x, y);
		GL11.glEnd();

		GL11.glDisable(GL11.GL_BLEND);
	}
}
