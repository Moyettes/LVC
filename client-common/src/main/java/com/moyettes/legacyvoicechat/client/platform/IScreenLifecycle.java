package com.moyettes.legacyvoicechat.client.platform;

public interface IScreenLifecycle {

	void init(int width, int height);

	void renderBackground(IRenderApi api, int mouseX, int mouseY, float partialTick);

	void renderForeground(IRenderApi api, int mouseX, int mouseY, float partialTick);

	void tick();

	boolean mouseClicked(double mouseX, double mouseY, int button);

	boolean mouseReleased(double mouseX, double mouseY, int button);

	boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY);

	boolean mouseScrolled(double mouseX, double mouseY, double scrollDelta);

	boolean keyPressed(int keyCode, int scanCode, int modifiers);

	boolean charTyped(char character, int modifiers);

	void close();
}
