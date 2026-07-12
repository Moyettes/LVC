package com.moyettes.legacyvoicechat.client.gui.widget;

import com.moyettes.legacyvoicechat.client.platform.IRenderApi;

public class UiTextField extends UiWidget {

	private static final int BORDER_COLOR = 0xFF000000;
	private static final int FACE_COLOR = 0xFF1A1A20;
	private static final int BORDER_FOCUSED = 0xFFFFFF80;
	private static final int TEXT_COLOR = 0xFFFFFFFF;
	private static final int PLACEHOLDER_COLOR = 0xFFAAAAAA;

	private String text = "";
	private String placeholder = "";
	private int maxLength = 32;
	private boolean focused;
	private boolean password;

	public UiTextField(int x, int y, int width, int height) {
		super(x, y, width, height, "");
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text == null ? "" : text;
		if (this.text.length() > maxLength) {
			this.text = this.text.substring(0, maxLength);
		}
	}

	public void setPlaceholder(String placeholder) {
		this.placeholder = placeholder == null ? "" : placeholder;
	}

	public void setMaxLength(int maxLength) {
		this.maxLength = maxLength;
	}

	public void setPassword(boolean password) {
		this.password = password;
	}

	public boolean isFocused() {
		return focused;
	}

	public void setFocused(boolean focused) {
		this.focused = focused;
	}

	@Override
	public void render(IRenderApi api, int mouseX, int mouseY, float partialTick) {
		if (!visible) return;
		api.setColor(1.0F, 1.0F, 1.0F, 1.0F);

		int border = focused ? BORDER_FOCUSED : BORDER_COLOR;
		api.fillRect(x, y, x + width, y + height, border);
		api.fillRect(x + 1, y + 1, x + width - 1, y + height - 1, FACE_COLOR);

		String render = text.isEmpty() ? placeholder : (password ? mask(text) : text);
		int color = text.isEmpty() ? PLACEHOLDER_COLOR : TEXT_COLOR;
		int textY = y + (height - api.textHeight()) / 2;
		api.drawString(render, x + 4, textY, color);

		if (focused && !text.isEmpty()) {
			int caretX = x + 4 + api.textWidth(password ? mask(text) : text);
			api.fillRect(caretX, textY - 1, caretX + 1, textY + api.textHeight(), TEXT_COLOR);
		}
	}

	private String mask(String s) {
		StringBuilder sb = new StringBuilder(s.length());
		for (int i = 0; i < s.length(); i++) {
			sb.append('*');
		}
		return sb.toString();
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (!visible || button != 0) return false;
		boolean over = isMouseOver(mouseX, mouseY);
		this.focused = over;
		return over;
	}

	public boolean charTyped(char c) {
		if (!focused) return false;
		if (c < 32 || c > 126) return false;
		if (text.length() >= maxLength) return true;
		text += c;
		return true;
	}

	public boolean backspace() {
		if (!focused || text.isEmpty()) return false;
		text = text.substring(0, text.length() - 1);
		return true;
	}
}
