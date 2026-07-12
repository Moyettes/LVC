package com.moyettes.legacyvoicechat.client.gui.widget;

import com.moyettes.legacyvoicechat.client.platform.IRenderApi;

public abstract class UiWidget {

	public int x;
	public int y;
	public int width;
	public int height;
	public boolean visible = true;
	public boolean active = true;
	protected String message;

	protected UiWidget(int x, int y, int width, int height, String message) {
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
		this.message = message == null ? "" : message;
	}

	public final String getMessage() {
		return message;
	}

	public final void setMessage(String message) {
		this.message = message == null ? "" : message;
	}

	public boolean isMouseOver(double mouseX, double mouseY) {
		return visible && mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
	}

	public abstract void render(IRenderApi api, int mouseX, int mouseY, float partialTick);

	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		return false;
	}

	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		return false;
	}

	public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
		return false;
	}

	public void tick() {
	}
}
