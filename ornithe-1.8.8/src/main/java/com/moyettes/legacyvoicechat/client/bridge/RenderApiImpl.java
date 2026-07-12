package com.moyettes.legacyvoicechat.client.bridge;

import com.moyettes.legacyvoicechat.client.platform.IRenderApi;
import com.moyettes.legacyvoicechat.client.platform.ResourceLike;
import com.moyettes.legacyvoicechat.utils.MinecraftAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.render.TextRenderer;
import net.minecraft.resource.Identifier;
import org.lwjgl.opengl.GL11;

public final class RenderApiImpl implements IRenderApi {

	@Override
	public void drawString(String text, int x, int y, int color) {
		TextRenderer font = font();
		if (font != null) {
			font.draw(text, x, y, color);
		}
	}

	@Override
	public void drawCenteredString(String text, int centerX, int y, int color) {
		TextRenderer font = font();
		if (font != null) {
			int w = font.getWidth(text);
			font.draw(text, centerX - w / 2, y, color);
		}
	}

	@Override
	public int textWidth(String text) {
		TextRenderer font = font();
		return font != null ? font.getWidth(text) : text.length() * 6;
	}

	@Override
	public int textHeight() {
		return 8;
	}

	@Override
	public void fillRect(int x1, int y1, int x2, int y2, int color) {
		float a = ((color >> 24) & 0xFF) / 255.0F;
		float r = ((color >> 16) & 0xFF) / 255.0F;
		float g = ((color >> 8) & 0xFF) / 255.0F;
		float b = (color & 0xFF) / 255.0F;
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GL11.glColor4f(r, g, b, a);
		GL11.glBegin(GL11.GL_QUADS);
		GL11.glVertex2f(x1, y2);
		GL11.glVertex2f(x2, y2);
		GL11.glVertex2f(x2, y1);
		GL11.glVertex2f(x1, y1);
		GL11.glEnd();
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
	}

	@Override
	public void bindTexture(ResourceLike texture) {
		Minecraft mc = MinecraftAccessor.getMinecraft();
		if (mc == null) return;
		mc.getTextureManager().bind(new Identifier(texture.namespace(), texture.path()));
	}

	@Override
	public void setColor(float r, float g, float b, float a) {
		GL11.glColor4f(r, g, b, a);
	}

	@Override
	public void enableBlend() {
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
	}

	@Override
	public void disableBlend() {
		GL11.glDisable(GL11.GL_BLEND);
	}

	@Override
	public void drawTexturedRect(ResourceLike texture, int x, int y, int width, int height, float u, float v) {
		drawTexturedRect(texture, x, y, width, height, (int) u, (int) v, 256, 256);
	}

	@Override
	public void drawTexturedRect(ResourceLike texture, int x, int y, int width, int height, int u, int v, int textureWidth, int textureHeight) {
		bindTexture(texture);
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		float uMin = (float) u / textureWidth;
		float vMin = (float) v / textureHeight;
		float uMax = (float) (u + width) / textureWidth;
		float vMax = (float) (v + height) / textureHeight;
		GL11.glBegin(GL11.GL_QUADS);
		GL11.glTexCoord2f(uMin, vMax);
		GL11.glVertex2f(x, y + height);
		GL11.glTexCoord2f(uMax, vMax);
		GL11.glVertex2f(x + width, y + height);
		GL11.glTexCoord2f(uMax, vMin);
		GL11.glVertex2f(x + width, y);
		GL11.glTexCoord2f(uMin, vMin);
		GL11.glVertex2f(x, y);
		GL11.glEnd();
	}

	@Override
	public void drawTextureScaled(ResourceLike texture, int x, int y, int width, int height) {
		bindTexture(texture);
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		GL11.glBegin(GL11.GL_QUADS);
		GL11.glTexCoord2f(0.0F, 1.0F);
		GL11.glVertex2f(x, y + height);
		GL11.glTexCoord2f(1.0F, 1.0F);
		GL11.glVertex2f(x + width, y + height);
		GL11.glTexCoord2f(1.0F, 0.0F);
		GL11.glVertex2f(x + width, y);
		GL11.glTexCoord2f(0.0F, 0.0F);
		GL11.glVertex2f(x, y);
		GL11.glEnd();
	}

	@Override
	public void pushMatrix() {
		GL11.glPushMatrix();
	}

	@Override
	public void popMatrix() {
		GL11.glPopMatrix();
	}

	@Override
	public void translate(float x, float y, float z) {
		GL11.glTranslatef(x, y, z);
	}

	private static TextRenderer font() {
		Minecraft mc = MinecraftAccessor.getMinecraft();
		return mc != null ? mc.textRenderer : null;
	}
}
