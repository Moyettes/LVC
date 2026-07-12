package com.moyettes.legacyvoicechat.client.gui.screen;

import com.moyettes.legacyvoicechat.client.VoiceClient;
import com.moyettes.legacyvoicechat.client.VoiceClientBoot;
import com.moyettes.legacyvoicechat.client.VoiceClientContext;
import com.moyettes.legacyvoicechat.client.VoiceSession;
import com.moyettes.legacyvoicechat.client.gui.widget.UiSlider;
import com.moyettes.legacyvoicechat.client.gui.widget.UiWidget;
import com.moyettes.legacyvoicechat.client.platform.IRenderApi;
import com.moyettes.legacyvoicechat.client.platform.PlayerRef;
import com.moyettes.legacyvoicechat.client.platform.ResourceLike;

import java.util.ArrayList;
import java.util.List;

public class PlayersScreen extends BaseVoiceScreen {

	private static final int ROW_HEIGHT = 24;
	private static final int NAME_WIDTH = 70;
	private static final int SLIDER_HEIGHT = 20;

	private final List<PlayerEntry> entries = new ArrayList<PlayerEntry>();
	private int scrollOffset;

	@Override
	protected void initContent() {
		buildPlayerEntries();
	}

	@Override
	protected void renderContent(IRenderApi api, int mouseX, int mouseY, float partialTick) {
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
		return BACKGROUND_PLAYERS;
	}

	@Override
	protected String title() {
		return "Players";
	}

	private void buildPlayerEntries() {
		entries.clear();

		List<UiWidget> preserved = new ArrayList<UiWidget>(tabWidgets);
		widgets.clear();
		widgets.addAll(preserved);

		VoiceClientContext ctx = VoiceClientBoot.getContext();
		if (ctx == null || ctx.client == null) return;

		int backgroundX = backgroundX();
		int backgroundY = backgroundY();
		int contentTop = backgroundY + 24;
		int sliderX = backgroundX + 10 + NAME_WIDTH + 4;
		int sliderWidth = backgroundWidth - (NAME_WIDTH + 20);

		VoiceClient vc = VoiceSession.getActive();
		String localName = ctx.client.getLocalPlayerName();

		for (PlayerRef player : ctx.client.getOnlinePlayers()) {
			String name = player.name();
			if (name == null) continue;
			if (localName != null && localName.equals(name)) continue;

			float initialVolume = vc != null ? vc.getPlayerVolume(name) : 1.0f;
			float sliderValue = initialVolume / 2.0f;
			UiSlider slider = new UiSlider(sliderX, contentTop + entries.size() * ROW_HEIGHT, sliderWidth, SLIDER_HEIGHT,
				name + ": " + Math.round(initialVolume * 100) + "%",
				sliderValue,
				s -> onPlayerVolumeChanged(s, name));
			widgets.add(slider);
			entries.add(new PlayerEntry(name, slider));
		}
	}

	private void onPlayerVolumeChanged(UiSlider slider, String playerName) {
		double vol = Math.max(0.0, Math.min(2.0, slider.value * 2.0));
		slider.setMessage(playerName + ": " + (int) Math.round(vol * 100) + "%");
		VoiceClient vc = VoiceSession.getActive();
		if (vc != null) vc.setPlayerVolume(playerName, (float) vol);
	}

	private void layoutEntries(IRenderApi api) {
		int backgroundX = backgroundX();
		int backgroundY = backgroundY();
		int contentTop = backgroundY + 24;
		int contentBottom = backgroundY + backgroundHeight - 36;
		int sliderX = backgroundX + 5 + NAME_WIDTH + 4;

		for (int i = 0; i < entries.size(); i++) {
			PlayerEntry e = entries.get(i);
			int rowY = contentTop + i * ROW_HEIGHT - scrollOffset;
			boolean visible = rowY + SLIDER_HEIGHT > contentTop && rowY < contentBottom;
			e.slider.visible = visible;
			e.slider.x = sliderX;
			e.slider.y = rowY;

			if (visible) {
				api.drawString(e.playerName, backgroundX + 10, rowY + 6, 0xFFFFFFFF);
			}
		}
	}

	private static final class PlayerEntry {
		final String playerName;
		final UiSlider slider;

		PlayerEntry(String playerName, UiSlider slider) {
			this.playerName = playerName;
			this.slider = slider;
		}
	}
}
