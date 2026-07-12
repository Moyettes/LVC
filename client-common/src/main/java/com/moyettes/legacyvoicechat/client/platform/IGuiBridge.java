package com.moyettes.legacyvoicechat.client.platform;

public interface IGuiBridge {

	void openScreen(IScreenLifecycle lifecycle);

	void closeScreen();

	boolean isScreenOpen();
}
