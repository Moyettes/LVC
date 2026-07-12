package com.moyettes.legacyvoicechat.events;

import com.moyettes.legacyvoicechat.client.VoiceClient;
import com.moyettes.legacyvoicechat.client.VoiceSession;
import com.moyettes.legacyvoicechat.utils.MinecraftAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.render.Window;
import net.minecraft.client.render.vertex.Tesselator;
import net.minecraft.entity.mob.player.PlayerEntity;
import org.lwjgl.opengl.GL11;

public class RenderEvents {

	private static final String MICROPHONE_ICON = "/assets/legacyvoicechat/textures/icons/microphone.png";
	private static final String MICROPHONE_MUTE_ICON = "/assets/legacyvoicechat/textures/icons/microphone_off.png";
	private static final String PLAYER_DISCONNECTED = "/assets/legacyvoicechat/textures/icons/player_disconnected.png";
	private static final String PLAYER_MUTED = "/assets/legacyvoicechat/textures/icons/player_muted.png";
	private static final String PLAYER_TALKING = "/assets/legacyvoicechat/textures/icons/player_talking.png";

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
				if (player.networkId == networkId) return player;
			}
		}
		return null;
	}

	private void onRenderHUD() {
		Minecraft minecraft = MinecraftAccessor.getMinecraft();
		if (minecraft == null || iconsHidden()) return;

		VoiceClient voiceClient = VoiceSession.getActive();

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

	private void onRenderPlayerIcons(PlayerEntity player, double d, double e, double f) {
		VoiceClient voiceClient = VoiceSession.getActive();
		if (voiceClient == null || iconsHidden()) return;

		Integer id = player.networkId;
		if (voiceClient.isPlayerVoiceSupported(id)) {
			if (voiceClient.isPlayerDeafened(id)) {
				renderPlayerIconWithTexture(player, d, e, f, PLAYER_MUTED);
			} else if (voiceClient.isPlayerTalking(id)) {
				renderPlayerIcon(player, d, e, f);
			}
		} else {
			renderPlayerIconWithTexture(player, d, e, f, PLAYER_DISCONNECTED);
		}
	}

	private void renderPlayerIconWithTexture(PlayerEntity player, double d, double e, double f, String texturePath) {
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

		int textureId = mc.textureManager.load(texturePath);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);

		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);

		int iconSize = 16;
		int half = iconSize / 2;

		Tesselator buffer = Tesselator.INSTANCE;
		buffer.begin();
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

		int textureId = mc.textureManager.load(PLAYER_TALKING);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);

		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);

		int iconSize = 16;
		int half = iconSize / 2;

		Tesselator buffer = Tesselator.INSTANCE;
		buffer.begin();
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

	private void renderHudIcon(Minecraft minecraft, String texturePath) {
		Window window = new Window(minecraft.options, minecraft.width, minecraft.height);
		int scaledHeight = window.getHeight();

		int iconSize = 16;
		int padding = 8;
		int x = padding;
		int y = scaledHeight - iconSize - padding;

		GL11.glBindTexture(GL11.GL_TEXTURE_2D, minecraft.textureManager.load(texturePath));
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

		Tesselator tesselator = Tesselator.INSTANCE;
		tesselator.begin();
		tesselator.vertex((double) x, (double) (y + iconSize), 0.0D, 0.0D, 1.0D);
		tesselator.vertex((double) (x + iconSize), (double) (y + iconSize), 0.0D, 1.0D, 1.0D);
		tesselator.vertex((double) (x + iconSize), (double) y, 0.0D, 1.0D, 0.0D);
		tesselator.vertex((double) x, (double) y, 0.0D, 0.0D, 0.0D);
		tesselator.end();

		GL11.glDisable(GL11.GL_BLEND);
	}

	private static boolean iconsHidden() {
		try {
			Boolean b = (Boolean) Class.forName("mod_Voice").getMethod("isIconsHidden").invoke(null);
			return b != null && b;
		} catch (Exception e) {
			return false;
		}
	}
}
