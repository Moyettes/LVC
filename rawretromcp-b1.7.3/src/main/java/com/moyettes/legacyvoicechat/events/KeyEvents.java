package com.moyettes.legacyvoicechat.events;

import com.moyettes.legacyvoicechat.Voice;
import com.moyettes.legacyvoicechat.api.KeyBindingEvents;
import com.moyettes.legacyvoicechat.client.VoiceClient;
import com.moyettes.legacyvoicechat.client.VoiceClientBoot;
import com.moyettes.legacyvoicechat.client.VoiceClientContext;
import com.moyettes.legacyvoicechat.client.gui.screen.VoiceSettingsScreen;
import com.moyettes.legacyvoicechat.client.platform.IGuiBridge;
import com.moyettes.legacyvoicechat.extensions.KeyBindingExtension;
import com.moyettes.legacyvoicechat.utils.MinecraftAccessor;
import net.minecraft.client.options.KeyBinding;
import org.lwjgl.input.Keyboard;

public class KeyEvents {

	public static KeyBinding muteKeybind;
	public static KeyBinding menuKeybind;
	public static KeyBinding ptt;
	public static KeyBinding hideIcons;

	public KeyEvents() {
		ClientEvents.INSTANCE.onHandleKeyBinds(this::handleKeybinds);

		KeyBindingEvents.REGISTER_KEYBINDS.register(registry -> {
			muteKeybind = registry.register("Mute", Keyboard.KEY_M);
			menuKeybind = registry.register("Voice Settings", Keyboard.KEY_V);
			ptt = registry.register("Push to Talk", Keyboard.KEY_CAPITAL);
			hideIcons = registry.register("Hide Voice Chat Icons", Keyboard.KEY_H);
		});
	}

	private void handleKeybinds() {
		if (muteKeybind != null && ((KeyBindingExtension) muteKeybind).isPressed()) {
			VoiceClient voiceClient = Voice.getVoiceClient();
			if (voiceClient != null) {
				voiceClient.toggleMute();
			} else {
				System.out.println("Voice client not connected - cannot toggle mute");
			}
		}
		if (menuKeybind != null && ((KeyBindingExtension) menuKeybind).isPressed()) {
			VoiceClientContext ctx = VoiceClientBoot.getContext();
			IGuiBridge gui = ctx != null ? ctx.gui : null;
			if (gui != null) {
				gui.openScreen(new VoiceSettingsScreen());
			}
		}
		if (hideIcons != null && ((KeyBindingExtension) hideIcons).isPressed()) {
			Voice.toggleIcons();
		}
		if (ptt != null && Keyboard.isKeyDown(ptt.keyCode)) {
			// No-op until VoiceClient is wired.
		}
	}
}
