package com.moyettes.legacyvoicechat.client.gui.widget;

import com.moyettes.legacyvoicechat.client.platform.IRenderApi;

public class UiButton extends UiWidget {

	private static final int BORDER_COLOR = 0xFF000000;
	private static final int FACE_NORMAL = 0xFF404050;
	private static final int FACE_HOVER = 0xFF606078;
	private static final int FACE_DISABLED = 0xFF2A2A30;
	private static final int HIGHLIGHT_NORMAL = 0xFF606070;
	private static final int HIGHLIGHT_HOVER = 0xFF8080A0;
	private static final int HIGHLIGHT_DISABLED = 0xFF404045;
	private static final int TEXT_NORMAL = 0xFFFFFFFF;
	private static final int TEXT_HOVER = 0xFFFFFFA0;
	private static final int TEXT_DISABLED = 0xFFA0A0A0;

	protected final Runnable onPress;

	public UiButton(int x, int y, int width, int height, String message, Runnable onPress) {
		super(x, y, width, height, message);
		this.onPress = onPress;
	}

	@Override
	public void render(IRenderApi api, int mouseX, int mouseY, float partialTick) {
		if (!visible) return;
		boolean hovered = active && isMouseOver(mouseX, mouseY);
		api.setColor(1.0F, 1.0F, 1.0F, 1.0F);

		int face;
		int highlight;
		int textColor;
		if (!active) {
			face = FACE_DISABLED;
			highlight = HIGHLIGHT_DISABLED;
			textColor = TEXT_DISABLED;
		} else if (hovered) {
			face = FACE_HOVER;
			highlight = HIGHLIGHT_HOVER;
			textColor = TEXT_HOVER;
		} else {
			face = FACE_NORMAL;
			highlight = HIGHLIGHT_NORMAL;
			textColor = TEXT_NORMAL;
		}

		api.fillRect(x, y, x + width, y + height, BORDER_COLOR);
		api.fillRect(x + 1, y + 1, x + width - 1, y + height - 1, face);
		api.fillRect(x + 1, y + 1, x + width - 1, y + 2, highlight);

		int textY = y + (height - api.textHeight()) / 2;
		api.drawCenteredString(message, x + width / 2, textY, textColor);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (active && visible && button == 0 && isMouseOver(mouseX, mouseY)) {
			onPress();
			return true;
		}
		return false;
	}

	protected void onPress() {
		if (onPress != null) {
			onPress.run();
		}
	}
}
