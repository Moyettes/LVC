package com.moyettes.legacyvoicechat.events;

import com.moyettes.legacyvoicechat.client.Voice;
import com.moyettes.legacyvoicechat.client.VoiceClient;
import com.moyettes.legacyvoicechat.client.VoiceClientBoot;
import com.moyettes.legacyvoicechat.client.VoiceClientContext;
import com.moyettes.legacyvoicechat.client.gui.screen.VoiceSettingsScreen;
import com.moyettes.legacyvoicechat.client.platform.IGuiBridge;
import net.minecraft.client.options.KeyBinding;
import net.ornithemc.osl.keybinds.api.KeyBindingEvents;
import org.lwjgl.glfw.GLFW;

public class KeyEvents {

    public static KeyBinding muteKeybind;
    public static KeyBinding menuKeybind;
	public static KeyBinding ptt;
	public KeyEvents() {
		ClientEvents.INSTANCE.onHandleKeyBinds(this::handleKeybinds);

		KeyBindingEvents.REGISTER_KEYBINDS.register(registry -> {
			muteKeybind = registry.register(new KeyBinding("Mute", GLFW.GLFW_KEY_M, "voice" ));
			menuKeybind =registry.register(new KeyBinding("Voice Settings", GLFW.GLFW_KEY_V, "voice" ));
			ptt =registry.register(new KeyBinding("Push to Talk", GLFW.GLFW_KEY_CAPS_LOCK, "voice" ));
		});
	}

	private void handleKeybinds() {
		if(muteKeybind.isPressed()){
			VoiceClient voiceClient = Voice.getVoiceClient();
			if (voiceClient != null) {
				voiceClient.toggleMute();
			} else {
				Voice.LOGGER.info("Voice client not connected - cannot toggle mute");
			}
		}
		if (menuKeybind.isPressed()) {
			VoiceClientContext ctx = VoiceClientBoot.getContext();
			IGuiBridge gui = ctx != null ? ctx.gui : null;
			if (gui != null) {
				gui.openScreen(new VoiceSettingsScreen());
			}
		}

		if (ptt.isPressed()) {
			VoiceClient voiceClient = Voice.getVoiceClient();
			if (voiceClient != null && voiceClient.isPushToTalk()) {
				voiceClient.setPttKeyPressed(true);
			}
		}

	}


}
