package com.moyettes.legacyvoicechat.mixin;

import com.moyettes.legacyvoicechat.extensions.KeyBindingExtension;
import com.moyettes.legacyvoicechat.utils.MinecraftAccessor;
import net.minecraft.client.options.KeyBinding;
import org.lwjgl.input.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(KeyBinding.class)
public abstract class KeyBindingMixin implements KeyBindingExtension {
	@Accessor
	public abstract int getKeyCode();

	private boolean wasPressed = false;

	@Override
	public boolean isPressed() {
		if (MinecraftAccessor.getMinecraft().screen != null)
			return false;

		if (this.getKeyCode() == Keyboard.KEY_NONE)
			return false;

		boolean isPressed = Keyboard.getEventKey() == this.getKeyCode();

		if (isPressed && !wasPressed) {
			wasPressed = true;
			return true;
		} else if (!isPressed && wasPressed) {
			wasPressed = false;
			return false;
		}

		return false;
	}
}
