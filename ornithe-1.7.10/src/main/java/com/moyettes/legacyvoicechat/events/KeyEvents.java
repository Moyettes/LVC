package com.moyettes.legacyvoicechat.events;

import com.moyettes.legacyvoicechat.client.Voice;
import com.moyettes.legacyvoicechat.client.VoiceClient;
import com.moyettes.legacyvoicechat.client.VoiceClientBoot;
import com.moyettes.legacyvoicechat.client.VoiceClientContext;
import com.moyettes.legacyvoicechat.client.gui.screen.VoiceSettingsScreen;
import com.moyettes.legacyvoicechat.client.platform.IGuiBridge;
import com.moyettes.legacyvoicechat.utils.MinecraftAccessor;
import com.moyettes.legacyvoicechat.api.KeyBindingEvents;
import com.moyettes.legacyvoicechat.extensions.KeyBindingExtension;
import net.minecraft.client.options.KeyBinding;
import org.lwjgl.input.Keyboard;

public class KeyEvents {

    public static KeyBinding muteKeybind;
    public static KeyBinding menuKeybind;
	public static KeyBinding ptt;
	public static KeyBinding hideIcons;

	public KeyEvents() {
		ClientEvents.INSTANCE.registerKeyBindsEvent(this::handleKeybinds);

		KeyBindingEvents.REGISTER_KEYBINDS.register(registry -> {
			muteKeybind = registry.register("Mute", Keyboard.KEY_M);
            menuKeybind = registry.register("Voice Settings", Keyboard.KEY_V);
			ptt = registry.register("Push to Talk", Keyboard.KEY_CAPITAL);
			hideIcons = registry.register("Hide Voice Chat Icons", Keyboard.KEY_H);
		});
	}

	private void handleKeybinds() {
        if(((KeyBindingExtension) muteKeybind).isPressed()){
			VoiceClient voiceClient = Voice.getVoiceClient();
			if (voiceClient != null) {
				voiceClient.toggleMute();
			} else {
				Voice.LOGGER.info("Voice client not connected - cannot toggle mute");
			}
		}
        if (((KeyBindingExtension) menuKeybind).isPressed()) {
            net.minecraft.client.Minecraft mc = MinecraftAccessor.getMinecraft();
            if (mc != null && mc.world != null && mc.screen == null) {
                VoiceClientContext ctx = VoiceClientBoot.getContext();
                IGuiBridge gui = ctx != null ? ctx.gui : null;
                if (gui != null) {
                    gui.openScreen(new VoiceSettingsScreen());
                }
            }
        }
		if(((KeyBindingExtension) hideIcons).isPressed()){
			Voice.toggleIcons();
		}

		VoiceClient voiceClient = Voice.getVoiceClient();
		if (voiceClient != null && voiceClient.isPushToTalk()) {
			boolean pttPressed = Keyboard.isKeyDown(ptt.getKeyCode());
			voiceClient.setPttKeyPressed(pttPressed);
		}
	}


}
