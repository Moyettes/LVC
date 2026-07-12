package com.moyettes.legacyvoicechat.client.gui.widget;

import com.moyettes.legacyvoicechat.client.platform.IRenderApi;

public class UiSlider extends UiWidget {

	private static final int BORDER_COLOR = 0xFF000000;
	private static final int TRACK_COLOR = 0xFF2A2A30;
	private static final int THUMB_COLOR = 0xFF808090;
	private static final int THUMB_HOVER = 0xFFA0A0B8;
	private static final int TEXT_COLOR = 0xFFFFFFFF;
	private static final int THUMB_WIDTH = 8;

	public float value;
	public boolean dragging;
	private final ValueListener listener;

	public UiSlider(int x, int y, int width, int height, String message, float initialValue, ValueListener listener) {
		super(x, y, width, height, message);
		this.value = clamp(initialValue);
		this.listener = listener;
	}

	private static float clamp(float v) {
		if (v < 0.0F) return 0.0F;
		if (v > 1.0F) return 1.0F;
		return v;
	}

	@Override
	public void render(IRenderApi api, int mouseX, int mouseY, float partialTick) {
		if (!visible) return;
		api.setColor(1.0F, 1.0F, 1.0F, 1.0F);

		api.fillRect(x, y, x + width, y + height, BORDER_COLOR);
		api.fillRect(x + 1, y + 1, x + width - 1, y + height - 1, TRACK_COLOR);

		int thumbX = x + (int) (value * (width - THUMB_WIDTH));
		boolean hovered = active && isMouseOver(mouseX, mouseY);
		int thumbColor = (dragging || hovered) ? THUMB_HOVER : THUMB_COLOR;
		api.fillRect(thumbX, y, thumbX + THUMB_WIDTH, y + height, BORDER_COLOR);
		api.fillRect(thumbX + 1, y + 1, thumbX + THUMB_WIDTH - 1, y + height - 1, thumbColor);

		int textY = y + (height - api.textHeight()) / 2;
		api.drawCenteredString(message, x + width / 2, textY, TEXT_COLOR);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (active && visible && button == 0 && isMouseOver(mouseX, mouseY)) {
			updateValueFromMouse(mouseX);
			this.dragging = true;
			return true;
		}
		return false;
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
		if (this.dragging && button == 0) {
			updateValueFromMouse(mouseX);
			return true;
		}
		return false;
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		if (button == 0 && this.dragging) {
			this.dragging = false;
			return true;
		}
		return false;
	}

	private void updateValueFromMouse(double mouseX) {
		float newValue = (float) (mouseX - (x + THUMB_WIDTH / 2.0)) / (float) (width - THUMB_WIDTH);
		float clamped = clamp(newValue);
		if (clamped != this.value) {
			this.value = clamped;
			if (listener != null) {
				listener.onValueChanged(this);
			}
		} else {
			this.value = clamped;
		}
	}

	@FunctionalInterface
	public interface ValueListener {
		void onValueChanged(UiSlider slider);
	}
}
