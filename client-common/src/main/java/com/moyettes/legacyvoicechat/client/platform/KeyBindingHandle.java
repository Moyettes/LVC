package com.moyettes.legacyvoicechat.client.platform;

public interface KeyBindingHandle {
	boolean isPressed();
	boolean isHeld();
	int getKeyCode();
	void setKeyCode(int keyCode);
}
