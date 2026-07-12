package com.moyettes.legacyvoicechat.events.client;

import com.moyettes.legacyvoicechat.client.Voice;
import com.moyettes.legacyvoicechat.client.VoiceClient;
import com.moyettes.legacyvoicechat.client.VoiceClientBoot;
import com.moyettes.legacyvoicechat.client.VoiceClientContext;
import com.moyettes.legacyvoicechat.client.gui.screen.VoiceSettingsScreen;
import com.moyettes.legacyvoicechat.client.platform.IGuiBridge;
import net.minecraft.client.input.InputDevice;
import net.minecraft.client.option.GameSettings;
import net.minecraft.client.option.KeyBinding;
import org.lwjgl.input.Keyboard;

public class KeyEvents {

	public static KeyBinding muteKeybind;
	public static KeyBinding menuKeybind;
	public static KeyBinding ptt;

	public KeyEvents() {
		muteKeybind = new KeyBinding("Mute").setDefault(InputDevice.keyboard, Keyboard.KEY_M);
		menuKeybind = new KeyBinding("Voice Settings").setDefault(InputDevice.keyboard, Keyboard.KEY_V);
		ptt = new KeyBinding("Push to Talk").setDefault(InputDevice.keyboard, Keyboard.KEY_CAPITAL);
	}

	public static void register() {
		if (muteKeybind != null) GameSettings.keys.add(muteKeybind);
		if (menuKeybind != null) GameSettings.keys.add(menuKeybind);
		if (ptt != null) GameSettings.keys.add(ptt);
	}

	public void handleKeybinds() {
		if (muteKeybind != null && muteKeybind.isPressed()) {
			VoiceClient voiceClient = Voice.getVoiceClient();
			if (voiceClient != null) {
				voiceClient.toggleMute();
			}
		}

		if (menuKeybind != null && menuKeybind.isPressed()) {
			VoiceClientContext ctx = VoiceClientBoot.getContext();
			IGuiBridge gui = ctx != null ? ctx.gui : null;
			if (gui != null) {
				gui.openScreen(new VoiceSettingsScreen());
			}
		}

		if (ptt != null) {
			VoiceClient voiceClient = Voice.getVoiceClient();
			if (voiceClient != null && voiceClient.isPushToTalk()) {
				boolean pttPressed = Keyboard.isKeyDown(ptt.getKeyCode());
				voiceClient.setPttKeyPressed(pttPressed);
			}
		}
	}
}
