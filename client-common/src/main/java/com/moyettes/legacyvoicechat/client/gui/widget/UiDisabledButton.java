package com.moyettes.legacyvoicechat.client.gui.widget;

public class UiDisabledButton extends UiButton {

	public UiDisabledButton(int x, int y, int width, int height, String message) {
		super(x, y, width, height, message, null);
		this.active = false;
	}
}
