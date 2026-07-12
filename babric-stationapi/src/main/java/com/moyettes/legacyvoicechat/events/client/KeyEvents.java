package com.moyettes.legacyvoicechat.events.client;

import com.moyettes.legacyvoicechat.client.Voice;
import com.moyettes.legacyvoicechat.client.VoiceClient;
import com.moyettes.legacyvoicechat.client.VoiceClientBoot;
import com.moyettes.legacyvoicechat.client.VoiceClientContext;
import com.moyettes.legacyvoicechat.client.gui.screen.VoiceSettingsScreen;
import com.moyettes.legacyvoicechat.client.platform.IGuiBridge;
import net.mine_diver.unsafeevents.listener.EventListener;
import net.minecraft.client.option.KeyBinding;
import net.modificationstation.stationapi.api.client.event.option.KeyBindingRegisterEvent;
import org.lwjgl.input.Keyboard;

public class KeyEvents {

	public static KeyBinding muteKeybind;
	public static KeyBinding menuKeybind;
	public static KeyBinding ptt;

	private static boolean muteKeyPressed;
	private static boolean menuKeyPressed;
	private static boolean pttKeyPressed;

	@EventListener
	public static void registerKeyBindings(KeyBindingRegisterEvent event) {
		event.keyBindings.add(muteKeybind = new KeyBinding("Mute", Keyboard.KEY_M));
		event.keyBindings.add(menuKeybind = new KeyBinding("Voice Settings", Keyboard.KEY_V));
		event.keyBindings.add(ptt = new KeyBinding("Push to Talk", Keyboard.KEY_CAPITAL));
	}

	public static void updateKeyBindings() {
		boolean currentMutePressed = Keyboard.isKeyDown(muteKeybind.code);
		if (currentMutePressed && !muteKeyPressed) {
			VoiceClient voiceClient = Voice.getVoiceClient();
			if (voiceClient != null) {
				voiceClient.toggleMute();
			}
		}
		muteKeyPressed = currentMutePressed;

		boolean currentMenuPressed = Keyboard.isKeyDown(menuKeybind.code);
		if (currentMenuPressed && !menuKeyPressed) {
			VoiceClientContext ctx = VoiceClientBoot.getContext();
			IGuiBridge gui = ctx != null ? ctx.gui : null;
			if (gui != null) {
				gui.openScreen(new VoiceSettingsScreen());
			}
		}
		menuKeyPressed = currentMenuPressed;

		boolean currentPressed = Keyboard.isKeyDown(ptt.code);
		if (currentPressed != pttKeyPressed) {
			pttKeyPressed = currentPressed;
			VoiceClient voiceClient = Voice.getVoiceClient();
			if (voiceClient != null && voiceClient.isPushToTalk()) {
				voiceClient.setPttKeyPressed(pttKeyPressed);
			}
		}
	}
}
