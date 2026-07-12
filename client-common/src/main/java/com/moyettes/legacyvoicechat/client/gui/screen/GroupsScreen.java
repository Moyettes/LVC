package com.moyettes.legacyvoicechat.client.gui.screen;

import com.moyettes.legacyvoicechat.client.VoiceSession;
import com.moyettes.legacyvoicechat.client.gui.widget.UiButton;
import com.moyettes.legacyvoicechat.client.gui.widget.UiWidget;
import com.moyettes.legacyvoicechat.client.platform.IRenderApi;
import com.moyettes.legacyvoicechat.client.platform.ResourceLike;
import com.moyettes.legacyvoicechat.udp.GroupListPacket;

import java.util.ArrayList;
import java.util.List;

public class GroupsScreen extends BaseVoiceScreen {

	private static final int ROW_HEIGHT = 24;
	private static final int NAME_WIDTH = 100;
	private static final int BUTTON_HEIGHT = 20;

	private final List<GroupEntry> entries = new ArrayList<GroupEntry>();
	private int scrollOffset;
	private List<GroupListPacket.GroupInfo> availableGroups = new ArrayList<GroupListPacket.GroupInfo>();
	private volatile List<GroupListPacket.GroupInfo> pendingAvailableGroups;

	private static GroupsScreen currentInstance;

	@Override
	protected void initContent() {
		currentInstance = this;
		VoiceSession.requestGroupList();
		buildGroupEntries();
	}

	@Override
	protected void renderContent(IRenderApi api, int mouseX, int mouseY, float partialTick) {
		drainPendingAvailableGroups();
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
		return "Join or Create Group";
	}

	private void buildGroupEntries() {
		entries.clear();

		List<UiWidget> preserved = new ArrayList<UiWidget>(tabWidgets);
		widgets.clear();
		widgets.addAll(preserved);

		int backgroundX = backgroundX();
		int backgroundY = backgroundY();
		int contentTop = backgroundY + 24;
		int buttonX = backgroundX + 10 + NAME_WIDTH + 4;
		int buttonWidth = backgroundWidth - (NAME_WIDTH + 20);

		for (GroupListPacket.GroupInfo group : availableGroups) {
			String buttonText = group.hasPassword() ? "Join (Private)" : "Join";
			final GroupListPacket.GroupInfo g = group;
			UiButton joinButton = new UiButton(buttonX, contentTop + entries.size() * ROW_HEIGHT, buttonWidth, 20,
				buttonText,
				() -> onJoinClicked(g));
			widgets.add(joinButton);
			entries.add(new GroupEntry(g, joinButton));
		}

		int buttonY = backgroundY + backgroundHeight - 28;
		UiButton createGroupButton = new UiButton(backgroundX + 8, buttonY, backgroundWidth - 16, 20, "Create Group",
			() -> openScreen(new CreateGroupScreen()));
		widgets.add(createGroupButton);
	}

	private void onJoinClicked(GroupListPacket.GroupInfo group) {
		if (group.hasPassword()) {
			openScreen(new JoinGroupScreen(group.getName()));
		} else {
			VoiceSession.joinGroup(group.getName(), null);
			openScreen(new GroupMembersScreen());
		}
	}

	private void layoutEntries(IRenderApi api) {
		int backgroundX = backgroundX();
		int backgroundY = backgroundY();
		int contentTop = backgroundY + 24;
		int contentBottom = backgroundY + backgroundHeight - 36;

		if (entries.isEmpty()) {
			String message = "No groups to join";
			int messageWidth = api.textWidth(message);
			int messageX = backgroundX + (backgroundWidth - messageWidth) / 2;
			int messageY = backgroundY + backgroundHeight / 2;
			api.drawString(message, messageX, messageY, 0xFFAAAAAA);
			return;
		}

		for (int i = 0; i < entries.size(); i++) {
			GroupEntry e = entries.get(i);
			int rowY = contentTop + i * ROW_HEIGHT - scrollOffset;
			boolean visible = rowY + BUTTON_HEIGHT > contentTop && rowY < contentBottom;
			e.joinButton.visible = visible;
			e.joinButton.x = backgroundX + 10 + NAME_WIDTH + 4;
			e.joinButton.y = rowY;

			if (visible) {
				String groupDisplay = e.group.getName() + " (" + e.group.getMemberCount() + ")"
					+ (e.group.hasPassword() ? " [Private]" : " [Public]");
				api.drawString(groupDisplay, backgroundX + 10, rowY + 6, 0xFFFFFFFF);
			}
		}
	}

	public void updateGroupList(List<GroupListPacket.GroupInfo> groups) {
		this.pendingAvailableGroups = new ArrayList<GroupListPacket.GroupInfo>(groups);
	}

	private void drainPendingAvailableGroups() {
		List<GroupListPacket.GroupInfo> pending = this.pendingAvailableGroups;
		if (pending == null) return;
		this.pendingAvailableGroups = null;
		this.availableGroups = pending;
		buildGroupEntries();
	}

	public static void updateCurrentGroupList(List<GroupListPacket.GroupInfo> groups) {
		GroupsScreen inst = currentInstance;
		if (inst != null) {
			inst.updateGroupList(groups);
		}
	}

	@Override
	public void close() {
		if (currentInstance == this) {
			currentInstance = null;
		}
	}

	private static final class GroupEntry {
		final GroupListPacket.GroupInfo group;
		final UiButton joinButton;

		GroupEntry(GroupListPacket.GroupInfo group, UiButton joinButton) {
			this.group = group;
			this.joinButton = joinButton;
		}
	}
}
