package com.moyettes.legacyvoicechat.client.gui.screen;

import com.moyettes.legacyvoicechat.client.VoiceSession;
import com.moyettes.legacyvoicechat.client.gui.widget.UiButton;
import com.moyettes.legacyvoicechat.client.platform.IRenderApi;
import com.moyettes.legacyvoicechat.client.platform.ResourceLike;

public class CreateGroupScreen extends BaseVoiceScreen {

	private static final int MAX_FIELD_LEN = 32;
	private static final int KEY_ESCAPE = 256;
	private static final int KEY_ENTER = 257;
	private static final int KEY_TAB = 258;
	private static final int KEY_BACKSPACE = 259;

	private String groupNameText = "";
	private String passwordText = "";
	private boolean isEditingPassword;

	@Override
	protected void initContent() {
		int backgroundX = backgroundX();
		int backgroundY = backgroundY();

		int buttonY = backgroundY + backgroundHeight - 60;
		int buttonWidth = (backgroundWidth - 40) / 2;
		int buttonSpacing = 8;

		UiButton createButton = new UiButton(backgroundX + 16, buttonY, buttonWidth, 20, "Create Group",
			this::submit);
		widgets.add(createButton);

		UiButton backButton = new UiButton(backgroundX + 16 + buttonWidth + buttonSpacing, buttonY, buttonWidth, 20, "Back",
			() -> openScreen(new GroupsScreen()));
		widgets.add(backButton);
	}

	private void submit() {
		String name = groupNameText.trim();
		String pw = passwordText.trim();
		if (!name.isEmpty()) {
			VoiceSession.createGroup(name, pw.isEmpty() ? null : pw);
			openScreen(new GroupMembersScreen());
		}
	}

	@Override
	protected void renderContent(IRenderApi api, int mouseX, int mouseY, float partialTick) {
		int backgroundX = backgroundX();
		int backgroundY = backgroundY();

		api.drawString("Group Name:", backgroundX + 16, backgroundY + 30, 0xFFFFFFFF);
		api.drawString("Password (Optional):", backgroundX + 16, backgroundY + 60, 0xFFFFFFFF);

		String displayGroupName = groupNameText.isEmpty() ? "Enter group name..." : groupNameText;
		String displayPassword = passwordText.isEmpty() ? "Enter password..." : passwordText;

		int groupNameColor = !isEditingPassword ? 0xFFFFFF00 : (groupNameText.isEmpty() ? 0xFFAAAAAA : 0xFFFFFFFF);
		int passwordColor = isEditingPassword ? 0xFFFFFF00 : (passwordText.isEmpty() ? 0xFFAAAAAA : 0xFFFFFFFF);

		api.drawString(displayGroupName, backgroundX + 16, backgroundY + 45, groupNameColor);
		api.drawString(displayPassword, backgroundX + 16, backgroundY + 75, passwordColor);
	}

	@Override
	protected ResourceLike backgroundTexture() {
		return BACKGROUND_GROUP;
	}

	@Override
	protected String title() {
		return "Create Group";
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (keyCode == KEY_ESCAPE) {
			openScreen(new GroupsScreen());
			return true;
		} else if (keyCode == KEY_ENTER) {
			submit();
			return true;
		} else if (keyCode == KEY_TAB) {
			isEditingPassword = !isEditingPassword;
			return true;
		} else if (keyCode == KEY_BACKSPACE) {
			if (isEditingPassword && !passwordText.isEmpty()) {
				passwordText = passwordText.substring(0, passwordText.length() - 1);
			} else if (!isEditingPassword && !groupNameText.isEmpty()) {
				groupNameText = groupNameText.substring(0, groupNameText.length() - 1);
			}
			return true;
		}
		return false;
	}

	@Override
	public boolean charTyped(char character, int modifiers) {
		if (character == 8) {
			if (isEditingPassword && !passwordText.isEmpty()) {
				passwordText = passwordText.substring(0, passwordText.length() - 1);
			} else if (!isEditingPassword && !groupNameText.isEmpty()) {
				groupNameText = groupNameText.substring(0, groupNameText.length() - 1);
			}
			return true;
		}
		if (character >= 32 && character <= 126) {
			if (isEditingPassword && passwordText.length() < MAX_FIELD_LEN) {
				passwordText += character;
			} else if (!isEditingPassword && groupNameText.length() < MAX_FIELD_LEN) {
				groupNameText += character;
			}
			return true;
		}
		return false;
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button == 0) {
			int backgroundX = backgroundX();
			int backgroundY = backgroundY();

			if (mouseX >= backgroundX + 16 && mouseX <= backgroundX + backgroundWidth - 16) {
				if (mouseY >= backgroundY + 45 && mouseY <= backgroundY + 60) {
					isEditingPassword = false;
					return true;
				}
				if (mouseY >= backgroundY + 75 && mouseY <= backgroundY + 90) {
					isEditingPassword = true;
					return true;
				}
			}
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}
}
