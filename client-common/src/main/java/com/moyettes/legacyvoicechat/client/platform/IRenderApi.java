package com.moyettes.legacyvoicechat.client.platform;

public interface IRenderApi {

	void drawString(String text, int x, int y, int color);

	void drawCenteredString(String text, int centerX, int y, int color);

	int textWidth(String text);

	int textHeight();

	void fillRect(int x1, int y1, int x2, int y2, int color);

	void bindTexture(ResourceLike texture);

	void setColor(float r, float g, float b, float a);

	void enableBlend();

	void disableBlend();

	void drawTexturedRect(ResourceLike texture, int x, int y, int width, int height, float u, float v);

	void drawTexturedRect(ResourceLike texture, int x, int y, int width, int height, int u, int v, int textureWidth, int textureHeight);

	void drawTextureScaled(ResourceLike texture, int x, int y, int width, int height);

	void pushMatrix();

	void popMatrix();

	void translate(float x, float y, float z);
}
