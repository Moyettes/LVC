package com.moyettes.legacyvoicechat.events;

import com.moyettes.legacyvoicechat.utils.MinecraftAccessor;
import com.moyettes.legacyvoicechat.client.Voice;
import com.moyettes.legacyvoicechat.client.VoiceClient;
import com.moyettes.legacyvoicechat.utils.Tessellator;
import net.minecraft.client.Minecraft;
import net.minecraft.resource.Identifier;
import org.lwjgl.opengl.GL11;

public class RenderEvents {

	private static final Identifier MICROPHONE_ICON = new Identifier("legacyvoicechat", "textures/icons/microphone.png");
	private static final Identifier MICROPHONE_MUTE_ICON = new Identifier("legacyvoicechat",  "textures/icons/microphone_off.png");


	public RenderEvents() {
		ClientEvents.INSTANCE.registerRenderHUDEvent(this::onRenderHUD);
	}

	private void onRenderHUD(float tickDelta) {
		Minecraft minecraft = MinecraftAccessor.getMinecraft();
		if (minecraft == null) return;

		VoiceClient voiceClient = Voice.getVoiceClient();
		if (voiceClient == null) return;

		if (voiceClient.isMicMuted()) {
			renderIcon(minecraft, MICROPHONE_MUTE_ICON);
		} else if (voiceClient.isTalking()) {
			renderIcon(minecraft, MICROPHONE_ICON);
		}

	}

	private void renderIcon(Minecraft minecraft, Identifier texturePath) {
		int windowWidth = minecraft.window.getWidth();
		int windowHeight = minecraft.window.getHeight();

		int iconSize = 32;
		int padding = 8;
		int x = padding;
		int y = windowHeight - iconSize - padding;

		GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
		GL11.glDisable(GL11.GL_DEPTH_TEST);
		GL11.glDisable(GL11.GL_CULL_FACE);
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GL11.glMatrixMode(GL11.GL_PROJECTION);
		GL11.glPushMatrix();
		GL11.glLoadIdentity();
		GL11.glOrtho(0.0D, windowWidth, windowHeight, 0.0D, -1.0D, 1.0D);
		GL11.glMatrixMode(GL11.GL_MODELVIEW);
		GL11.glPushMatrix();
		GL11.glLoadIdentity();

		minecraft.getTextureManager().bind(texturePath);

		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);

		GL11.glBegin(GL11.GL_QUADS);
		GL11.glTexCoord2f(0.0F, 1.0F);
		GL11.glVertex2f(x, y + iconSize);
		GL11.glTexCoord2f(1.0F, 1.0F);
		GL11.glVertex2f(x + iconSize, y + iconSize);
		GL11.glTexCoord2f(1.0F, 0.0F);
		GL11.glVertex2f(x + iconSize, y);
		GL11.glTexCoord2f(0.0F, 0.0F);
		GL11.glVertex2f(x, y);
		GL11.glEnd();

		GL11.glPopMatrix();
		GL11.glMatrixMode(GL11.GL_PROJECTION);
		GL11.glPopMatrix();
		GL11.glMatrixMode(GL11.GL_MODELVIEW);
		GL11.glPopAttrib();
	}

}
