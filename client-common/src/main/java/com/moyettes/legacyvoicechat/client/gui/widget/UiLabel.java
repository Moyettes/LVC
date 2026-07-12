package com.moyettes.legacyvoicechat.client.gui.widget;

import com.moyettes.legacyvoicechat.client.platform.IRenderApi;

public class UiLabel extends UiWidget {

	private final boolean centered;
	private int color;

	public UiLabel(int x, int y, String text, int color, boolean centered) {
		super(x, y, 0, 8, text);
		this.color = color;
		this.centered = centered;
	}

	public void setColor(int color) {
		this.color = color;
	}

	@Override
	public boolean isMouseOver(double mouseX, double mouseY) {
		return false;
	}

	@Override
	public void render(IRenderApi api, int mouseX, int mouseY, float partialTick) {
		if (!visible || message == null || message.isEmpty()) return;
		if (centered) {
			api.drawCenteredString(message, x, y, color);
		} else {
			api.drawString(message, x, y, color);
		}
	}
}
