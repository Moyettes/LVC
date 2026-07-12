// Compile-only stub of Risugami ModLoader's BaseMod. Default package matches
// the runtime layout in a Risugami-patched minecraft.jar (after Risugami's
// reobfuscator strips net.minecraft.src, BaseMod.class ends up at jar root).
// shadowJar excludes this file so we don't overwrite the user's real
// BaseMod.class when the drop-in is merged into their minecraft.jar.
public abstract class BaseMod {

	public boolean OnTickInGame(net.minecraft.client.Minecraft game) {
		return false;
	}

	public boolean OnTickInGame(net.minecraft.client.Minecraft game, net.minecraft.client.gui.screen.Screen gui) {
		return false;
	}

	public void KeyboardEvent(net.minecraft.client.options.KeyBinding event) {
	}

	public void ModsLoaded() {
	}

	public abstract String Version();
}
