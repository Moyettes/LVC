package com.moyettes.legacyvoicechat.client.gui.screen;

import com.moyettes.legacyvoicechat.client.VoiceClient;
import com.moyettes.legacyvoicechat.client.VoiceSession;
import com.moyettes.legacyvoicechat.client.gui.widget.UiButton;
import com.moyettes.legacyvoicechat.client.gui.widget.UiWidget;
import com.moyettes.legacyvoicechat.client.platform.IRenderApi;
import com.moyettes.legacyvoicechat.client.platform.ResourceLike;

import java.util.ArrayList;
import java.util.List;

public class GroupMembersScreen extends BaseVoiceScreen {

	private static final int ROW_HEIGHT = 24;
	private static final int BUTTON_HEIGHT = 20;

	private final List<MemberEntry> entries = new ArrayList<MemberEntry>();
	private int scrollOffset;
	private List<String> groupMembers = new ArrayList<String>();
	private volatile List<String> pendingGroupMembers;
	private String currentGroupName = "";

	private static GroupMembersScreen currentInstance;

	@Override
	protected void initContent() {
		currentInstance = this;

		VoiceClient vc = VoiceSession.getActive();
		if (vc != null) {
			currentGroupName = vc.getCurrentGroupName();
			groupMembers = new ArrayList<String>();
			for (Integer memberId : vc.getGroupMembers()) {
				String memberName = vc.getPlayerNameFromId(memberId);
				if (memberName != null) {
					groupMembers.add(memberName);
				}
			}
		}

		buildMemberEntries();
	}

	@Override
	protected void renderContent(IRenderApi api, int mouseX, int mouseY, float partialTick) {
		drainPendingGroupMembers();
		layoutEntries(api);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollDelta) {
		int contentTop = backgroundY() + 24;
		int contentBottom = backgroundY() + backgroundHeight - 36;
		int contentHeight = contentBottom - contentTop;
		int maxScroll = Math.max(0, entries.size() * ROW_HEIGHT - contentHeight);
		scrollOffset -= (int) Math.signum(scrollDelta) * 16;
		if (scrollOffset < 0) scrollOffset = 0;
		if (scrollOffset > maxScroll) scrollOffset = maxScroll;
		return true;
	}

	@Override
	protected ResourceLike backgroundTexture() {
		return BACKGROUND_GROUP;
	}

	@Override
	protected String title() {
		return "Group: " + (currentGroupName == null || currentGroupName.isEmpty() ? "Unknown" : currentGroupName);
	}

	private void buildMemberEntries() {
		entries.clear();

		List<UiWidget> preserved = new ArrayList<UiWidget>(tabWidgets);
		widgets.clear();
		widgets.addAll(preserved);

		for (String memberName : groupMembers) {
			entries.add(new MemberEntry(memberName));
		}

		int backgroundX = backgroundX();
		int backgroundY = backgroundY();
		int buttonY = backgroundY + backgroundHeight - 28;
		UiButton leaveButton = new UiButton(backgroundX + 8, buttonY, backgroundWidth - 16, 20, "Leave Group",
			() -> {
				VoiceSession.leaveGroup();
				openScreen(new GroupsScreen());
			});
		widgets.add(leaveButton);
	}

	private void layoutEntries(IRenderApi api) {
		int backgroundX = backgroundX();
		int backgroundY = backgroundY();
		int contentTop = backgroundY + 24;
		int contentBottom = backgroundY + backgroundHeight - 36;

		if (entries.isEmpty()) {
			String message = "No members in group";
			int messageWidth = api.textWidth(message);
			int messageX = backgroundX + (backgroundWidth - messageWidth) / 2;
			int messageY = backgroundY + backgroundHeight / 2;
			api.drawString(message, messageX, messageY, 0xFFAAAAAA);
			return;
		}

		for (int i = 0; i < entries.size(); i++) {
			MemberEntry e = entries.get(i);
			int rowY = contentTop + i * ROW_HEIGHT - scrollOffset;
			boolean visible = rowY + BUTTON_HEIGHT > contentTop && rowY < contentBottom;

			if (visible) {
				api.drawString(e.memberName, backgroundX + 10, rowY + 6, 0xFFFFFFFF);
			}
		}
	}

	public void updateMemberList(List<String> members) {
		this.pendingGroupMembers = new ArrayList<String>(members);
	}

	private void drainPendingGroupMembers() {
		List<String> pending = this.pendingGroupMembers;
		if (pending == null) return;
		this.pendingGroupMembers = null;
		this.groupMembers = pending;
		buildMemberEntries();
	}

	public static void updateCurrentMemberList(List<String> members) {
		GroupMembersScreen inst = currentInstance;
		if (inst != null) {
			inst.updateMemberList(members);
		}
	}

	@Override
	public void close() {
		if (currentInstance == this) {
			currentInstance = null;
		}
	}

	private static final class MemberEntry {
		final String memberName;

		MemberEntry(String memberName) {
			this.memberName = memberName;
		}
	}
}
