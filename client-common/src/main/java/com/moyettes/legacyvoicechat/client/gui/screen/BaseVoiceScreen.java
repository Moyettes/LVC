package com.moyettes.legacyvoicechat.client.gui.screen;

import com.moyettes.legacyvoicechat.client.VoiceClientBoot;
import com.moyettes.legacyvoicechat.client.VoiceSession;
import com.moyettes.legacyvoicechat.client.gui.widget.UiButton;
import com.moyettes.legacyvoicechat.client.gui.widget.UiDisabledButton;
import com.moyettes.legacyvoicechat.client.gui.widget.UiWidget;
import com.moyettes.legacyvoicechat.client.platform.IGuiBridge;
import com.moyettes.legacyvoicechat.client.platform.IRenderApi;
import com.moyettes.legacyvoicechat.client.platform.IScreenLifecycle;
import com.moyettes.legacyvoicechat.client.platform.ResourceLike;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseVoiceScreen implements IScreenLifecycle {

	protected static final ResourceLike BACKGROUND_MAIN = ResourceLike.of("textures/gui/background.png");
	protected static final ResourceLike BACKGROUND_PLAYERS = ResourceLike.of("textures/gui/players.png");
	protected static final ResourceLike BACKGROUND_GROUP = ResourceLike.of("textures/gui/group.png");

	protected int backgroundWidth = 176;
	protected int backgroundHeight = 222;

	protected int width;
	protected int height;

	protected final List<UiWidget> widgets = new ArrayList<UiWidget>();
	protected final List<UiWidget> tabWidgets = new ArrayList<UiWidget>();

	@Override
	public final void init(int width, int height) {
		this.width = width;
		this.height = height;
		this.widgets.clear();
		this.tabWidgets.clear();
		initTabs();
		initContent();
	}

	protected void initTabs() {
		int backgroundX = backgroundX();
		int backgroundY = backgroundY();

		int tabY = backgroundY - 20;
		int tabWidth = 56;
		int tabHeight = 20;
		int tabStartX = backgroundX + 2;

		UiWidget settings;
		UiWidget groups;
		UiWidget players;

		if (this instanceof VoiceSettingsScreen) {
			settings = new UiDisabledButton(tabStartX, tabY, tabWidth, tabHeight, "Settings");
			groups = new UiButton(tabStartX + tabWidth, tabY, tabWidth, tabHeight, "Groups", this::navigateToGroups);
			players = new UiButton(tabStartX + tabWidth * 2, tabY, tabWidth, tabHeight, "Players", this::navigateToPlayers);
		} else if (isGroupsScreen()) {
			settings = new UiButton(tabStartX, tabY, tabWidth, tabHeight, "Settings", this::navigateToSettings);
			groups = new UiDisabledButton(tabStartX + tabWidth, tabY, tabWidth, tabHeight, "Groups");
			players = new UiButton(tabStartX + tabWidth * 2, tabY, tabWidth, tabHeight, "Players", this::navigateToPlayers);
		} else if (this instanceof PlayersScreen) {
			settings = new UiButton(tabStartX, tabY, tabWidth, tabHeight, "Settings", this::navigateToSettings);
			groups = new UiButton(tabStartX + tabWidth, tabY, tabWidth, tabHeight, "Groups", this::navigateToGroups);
			players = new UiDisabledButton(tabStartX + tabWidth * 2, tabY, tabWidth, tabHeight, "Players");
		} else {
			settings = new UiButton(tabStartX, tabY, tabWidth, tabHeight, "Settings", this::navigateToSettings);
			groups = new UiButton(tabStartX + tabWidth, tabY, tabWidth, tabHeight, "Groups", this::navigateToGroups);
			players = new UiButton(tabStartX + tabWidth * 2, tabY, tabWidth, tabHeight, "Players", this::navigateToPlayers);
		}

		tabWidgets.add(settings);
		tabWidgets.add(groups);
		tabWidgets.add(players);
		widgets.add(settings);
		widgets.add(groups);
		widgets.add(players);
	}

	private boolean isGroupsScreen() {
		return this instanceof GroupsScreen
			|| this instanceof GroupMembersScreen
			|| this instanceof CreateGroupScreen
			|| this instanceof JoinGroupScreen;
	}

	private void navigateToSettings() {
		openScreen(new VoiceSettingsScreen());
	}

	private void navigateToGroups() {
		if (VoiceSession.isInGroup()) {
			openScreen(new GroupMembersScreen());
		} else {
			openScreen(new GroupsScreen());
		}
	}

	private void navigateToPlayers() {
		openScreen(new PlayersScreen());
	}

	protected final void openScreen(BaseVoiceScreen screen) {
		IGuiBridge bridge = gui();
		if (bridge != null) {
			bridge.openScreen(screen);
		}
	}

	protected final void closeScreen() {
		IGuiBridge bridge = gui();
		if (bridge != null) {
			bridge.closeScreen();
		}
	}

	protected static IGuiBridge gui() {
		return VoiceClientBoot.getContext() != null ? VoiceClientBoot.getContext().gui : null;
	}

	protected final int backgroundX() {
		return (width - backgroundWidth) / 2;
	}

	protected final int backgroundY() {
		return (height - backgroundHeight) / 2;
	}

	@Override
	public void renderBackground(IRenderApi api, int mouseX, int mouseY, float partialTick) {
		int backgroundX = backgroundX();
		int backgroundY = backgroundY();

		api.fillRect(0, 0, width, height, 0xC0101018);

		api.setColor(1.0F, 1.0F, 1.0F, 1.0F);
		api.drawTexturedRect(backgroundTexture(), backgroundX, backgroundY, backgroundWidth, backgroundHeight, 0, 0, 256, 256);
	}

	@Override
	public void renderForeground(IRenderApi api, int mouseX, int mouseY, float partialTick) {
		renderContent(api, mouseX, mouseY, partialTick);
		for (UiWidget w : widgets) {
			w.render(api, mouseX, mouseY, partialTick);
		}
		drawForeground(api);
	}

	protected void drawForeground(IRenderApi api) {
		int backgroundX = backgroundX();
		int backgroundY = backgroundY();
		String title = title();
		if (title != null && !title.isEmpty()) {
			int titleWidth = api.textWidth(title);
			int titleX = backgroundX + (backgroundWidth - titleWidth) / 2;
			api.drawString(title, titleX, backgroundY + 6, 0x404040);
		}
	}

	@Override
	public void tick() {
		for (UiWidget w : widgets) {
			w.tick();
		}
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		for (int i = widgets.size() - 1; i >= 0; i--) {
			UiWidget w = widgets.get(i);
			if (w.mouseClicked(mouseX, mouseY, button)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		boolean handled = false;
		for (UiWidget w : widgets) {
			if (w.mouseReleased(mouseX, mouseY, button)) {
				handled = true;
			}
		}
		return handled;
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
		for (UiWidget w : widgets) {
			if (w.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollDelta) {
		return false;
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		return false;
	}

	@Override
	public boolean charTyped(char character, int modifiers) {
		return false;
	}

	@Override
	public void close() {
	}

	protected abstract void initContent();

	protected abstract void renderContent(IRenderApi api, int mouseX, int mouseY, float partialTick);

	protected abstract ResourceLike backgroundTexture();

	protected abstract String title();
}
