package com.moyettes.legacyvoicechat.mixin;

import com.moyettes.legacyvoicechat.Voice;
import com.moyettes.legacyvoicechat.api.KeyBindingEvents;
import com.moyettes.legacyvoicechat.api.KeyBindingRegistry;
import net.minecraft.client.options.GameOptions;
import net.minecraft.client.options.KeyBinding;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.LinkedHashSet;
import java.util.Set;


@Mixin(GameOptions.class)
public class GameOptionsMixin implements KeyBindingRegistry {

	@Shadow private KeyBinding[] keyBindings;
	private Set<KeyBinding> modKeyBindings;

	@Inject(
		method = "<init>(Lnet/minecraft/client/Minecraft;Ljava/io/File;)V",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/options/GameOptions;load()V"
		)
	)
	private void osl$keybinds$registerKeybinds(CallbackInfo ci) {
		Voice.LOGGER.info("Initializing keybinds");

		modKeyBindings = new LinkedHashSet<>();

		KeyBindingEvents.REGISTER_KEYBINDS.invoker().accept(this);

		KeyBinding[] mcKeybinds = keyBindings;
		keyBindings = new KeyBinding[mcKeybinds.length + modKeyBindings.size()];

		int i = 0;
		for (KeyBinding keybind : mcKeybinds) {
			keyBindings[i++] = keybind;
		}
		for (KeyBinding keybind : modKeyBindings) {
			keyBindings[i++] = keybind;
		}
	}

	@Override
	public KeyBinding register(String name, int defaultKeyCode) {
		return register(new KeyBinding(name, defaultKeyCode));
	}

	@Override
	public KeyBinding register(KeyBinding keybind) {
		modKeyBindings.add(keybind);
		return keybind;
	}
}
