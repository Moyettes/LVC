package com.moyettes.legacyvoicechat.events;

import com.moyettes.legacyvoicechat.client.VoiceClient;
import com.moyettes.legacyvoicechat.client.VoiceClientBoot;
import com.moyettes.legacyvoicechat.client.VoiceClientContext;
import com.moyettes.legacyvoicechat.client.gui.screen.VoiceSettingsScreen;
import com.moyettes.legacyvoicechat.client.platform.IGuiBridge;
import com.moyettes.legacyvoicechat.utils.MinecraftAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.options.KeyBinding;
import org.apache.logging.log4j.Logger;
import org.lwjgl.input.Keyboard;

// Risugami variant of KeyEvents. RawRetroMCP's copy relies on our
// KeyBindingMixin + KeyBindingEvents.REGISTER_KEYBINDS flow; Risugami has
// neither - ModLoader invokes BaseMod.KeyboardEvent(KeyBinding) directly on
// the leading edge of a key press, and mod_Voice delegates to this.onKey.
//
// mod_Voice lives in the default package so we can't import it from this
// package without reflection shenanigans. All interaction with mod_Voice goes
// through Class.forName to keep this class import-clean.
public class KeyEvents {

	public static final KeyBinding muteKeybind = new KeyBinding("Mute", Keyboard.KEY_M);
	public static final KeyBinding menuKeybind = new KeyBinding("Voice Settings", Keyboard.KEY_V);
	public static final KeyBinding ptt = new KeyBinding("Push to Talk", Keyboard.KEY_CAPITAL);
	public static final KeyBinding hideIconsKey = new KeyBinding("Hide Voice Chat Icons", Keyboard.KEY_H);

	private final Logger logger;
	private final Object modVoice;

	public KeyEvents(Object modVoice, Logger logger) {
		this.modVoice = modVoice;
		this.logger = logger;
		registerWithModLoader();
	}

	private void registerWithModLoader() {
		try {
			Class<?> modLoader = Class.forName("ModLoader");
			Class<?> baseMod = Class.forName("BaseMod");
			java.lang.reflect.Method register = modLoader.getMethod("RegisterKey", baseMod, KeyBinding.class, boolean.class);
			register.invoke(null, modVoice, muteKeybind, false);
			register.invoke(null, modVoice, menuKeybind, false);
			register.invoke(null, modVoice, ptt, false);
			register.invoke(null, modVoice, hideIconsKey, false);
		} catch (Exception e) {
			logger.warn("Failed to register Risugami keybinds: {}", e.getMessage());
		}
	}

	public void onKey(KeyBinding pressed) {
		if (pressed == muteKeybind) {
			VoiceClient vc = getVoiceClient();
			if (vc != null) {
				vc.toggleMute();
			} else {
				logger.info("Voice client not connected - cannot toggle mute");
			}
		} else if (pressed == menuKeybind) {
			Minecraft mc = MinecraftAccessor.getMinecraft();
			if (mc != null && mc.world != null && mc.screen == null) {
				VoiceClientContext ctx = VoiceClientBoot.getContext();
				IGuiBridge gui = ctx != null ? ctx.gui : null;
				if (gui != null) {
					gui.openScreen(new VoiceSettingsScreen());
				}
			}
		} else if (pressed == hideIconsKey) {
			toggleIconsReflective();
		}
	}

	// Push-to-talk is a hold, not an edge, so it has to be polled each tick -
	// but the VoiceClient API in :client:client-common doesn't yet expose the
	// setPttKeyPressed / isPushToTalk hooks the standalone uses. No-op today;
	// wire the poll here once those methods land on VoiceClient.
	public void onTick() {
	}

	private VoiceClient getVoiceClient() {
		try {
			return (VoiceClient) Class.forName("mod_Voice").getMethod("getVoiceClient").invoke(null);
		} catch (Exception e) {
			return null;
		}
	}

	private void toggleIconsReflective() {
		try {
			Class.forName("mod_Voice").getMethod("toggleIcons").invoke(null);
		} catch (Exception e) {
			// no-op
		}
	}
}
