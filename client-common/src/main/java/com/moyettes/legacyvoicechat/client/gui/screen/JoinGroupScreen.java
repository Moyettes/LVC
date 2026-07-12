package com.moyettes.legacyvoicechat.client.gui.screen;

import com.moyettes.legacyvoicechat.client.VoiceSession;
import com.moyettes.legacyvoicechat.client.gui.widget.UiButton;
import com.moyettes.legacyvoicechat.client.platform.IRenderApi;
import com.moyettes.legacyvoicechat.client.platform.ResourceLike;

public class JoinGroupScreen extends BaseVoiceScreen {

	private static final int MAX_FIELD_LEN = 32;
	private static final int KEY_ESCAPE = 256;
	private static final int KEY_ENTER = 257;
	private static final int KEY_BACKSPACE = 259;

	private String passwordText = "";
	private final String groupName;

	public JoinGroupScreen(String groupName) {
		this.groupName = groupName;
	}

	@Override
	protected void initContent() {
		int backgroundX = backgroundX();
		int backgroundY = backgroundY();

		int buttonY = backgroundY + backgroundHeight - 60;
		int buttonWidth = (backgroundWidth - 40) / 2;
		int buttonSpacing = 8;

		UiButton joinButton = new UiButton(backgroundX + 16, buttonY, buttonWidth, 20, "Join Group",
			this::submit);
		widgets.add(joinButton);

		UiButton backButton = new UiButton(backgroundX + 16 + buttonWidth + buttonSpacing, buttonY, buttonWidth, 20, "Back",
			() -> openScreen(new GroupsScreen()));
		widgets.add(backButton);
	}

	private void submit() {
		String pw = passwordText.trim();
		VoiceSession.joinGroup(groupName, pw.isEmpty() ? null : pw);
		openScreen(new GroupMembersScreen());
	}

	@Override
	protected void renderContent(IRenderApi api, int mouseX, int mouseY, float partialTick) {
		int backgroundX = backgroundX();
		int backgroundY = backgroundY();

		api.drawString("Join Group: " + groupName, backgroundX + 16, backgroundY + 30, 0xFFFFFFFF);
		api.drawString("Enter Password:", backgroundX + 16, backgroundY + 60, 0xFFFFFFFF);

		String displayPassword = passwordText.isEmpty() ? "Enter password..." : passwordText;
		api.drawString(displayPassword, backgroundX + 16, backgroundY + 75,
			passwordText.isEmpty() ? 0xFFAAAAAA : 0xFFFFFF00);
	}

	@Override
	protected ResourceLike backgroundTexture() {
		return BACKGROUND_GROUP;
	}

	@Override
	protected String title() {
		return "Join Group";
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (keyCode == KEY_ESCAPE) {
			openScreen(new GroupsScreen());
			return true;
		} else if (keyCode == KEY_ENTER) {
			submit();
			return true;
		} else if (keyCode == KEY_BACKSPACE) {
			if (!passwordText.isEmpty()) {
				passwordText = passwordText.substring(0, passwordText.length() - 1);
			}
			return true;
		}
		return false;
	}

	@Override
	public boolean charTyped(char character, int modifiers) {
		if (character == 8) {
			if (!passwordText.isEmpty()) {
				passwordText = passwordText.substring(0, passwordText.length() - 1);
			}
			return true;
		}
		if (character >= 32 && character <= 126) {
			if (passwordText.length() < MAX_FIELD_LEN) {
				passwordText += character;
			}
			return true;
		}
		return false;
	}
}
