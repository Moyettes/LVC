package com.moyettes.legacyvoicechat.client.platform;

public interface IKeyBindingRegistry {
	KeyBindingHandle register(String translationKey, int defaultKeyCode, String category);
}
