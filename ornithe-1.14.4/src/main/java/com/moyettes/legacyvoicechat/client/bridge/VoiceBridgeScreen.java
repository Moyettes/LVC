package com.moyettes.legacyvoicechat.client.bridge;

import com.moyettes.legacyvoicechat.client.platform.IRenderApi;
import com.moyettes.legacyvoicechat.client.platform.IScreenLifecycle;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.LiteralText;

public final class VoiceBridgeScreen extends Screen {

	private final IScreenLifecycle lifecycle;
	private final IRenderApi api;

	public VoiceBridgeScreen(IScreenLifecycle lifecycle, IRenderApi api) {
		super(new LiteralText(""));
		this.lifecycle = lifecycle;
		this.api = api;
	}

	public IScreenLifecycle getLifecycle() {
		return lifecycle;
	}

	@Override
	public void init() {
		super.init();
		lifecycle.init(this.width, this.height);
	}

	@Override
	public void render(int mouseX, int mouseY, float tickDelta) {
		lifecycle.renderBackground(api, mouseX, mouseY, tickDelta);
		lifecycle.renderForeground(api, mouseX, mouseY, tickDelta);
		super.render(mouseX, mouseY, tickDelta);
	}

	@Override
	public void tick() {
		lifecycle.tick();
		super.tick();
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (lifecycle.mouseClicked(mouseX, mouseY, button)) return true;
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		boolean handled = lifecycle.mouseReleased(mouseX, mouseY, button);
		boolean parent = super.mouseReleased(mouseX, mouseY, button);
		return handled || parent;
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
		if (lifecycle.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) return true;
		return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollDelta) {
		if (lifecycle.mouseScrolled(mouseX, mouseY, scrollDelta)) return true;
		return super.mouseScrolled(mouseX, mouseY, scrollDelta);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (lifecycle.keyPressed(keyCode, scanCode, modifiers)) return true;
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public boolean charTyped(char character, int modifiers) {
		if (lifecycle.charTyped(character, modifiers)) return true;
		return super.charTyped(character, modifiers);
	}

	@Override
	public void removed() {
		lifecycle.close();
		super.removed();
	}
}
