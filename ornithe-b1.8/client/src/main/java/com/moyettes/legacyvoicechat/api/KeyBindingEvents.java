package com.moyettes.legacyvoicechat.api;

import net.ornithemc.osl.core.api.events.Event;

import java.util.function.Consumer;

/**
 * Events related to Minecraft's keybinds.
 */
public class KeyBindingEvents {

	/**
	 * This event is invoked upon game start-up, giving mod
	 * developers the opportunity to register custom keybinds.
	 *
	 * <p>
	 * Callbacks to this event should be registered in your mod's entrypoint,
	 * and can be done as follows:
	 *
	 * <pre>
	 * {@code
	 * KeyBindingEvents.REGISTER_KEYBINDS.register(registry -> {
	 * 	KeyBinding cookieKey = registry.register("Cookie", Keyboard.KEY_NONE);
	 * 	...
	 * });
	 * }
	 * </pre>
	 */
	public static final Event<Consumer<KeyBindingRegistry>> REGISTER_KEYBINDS = Event.consumer();

}
