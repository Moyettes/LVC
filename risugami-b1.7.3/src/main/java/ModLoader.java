// Compile-only stub of Risugami ModLoader's ModLoader class. Static helpers only -
// the runtime class sits in the user's Risugami-patched minecraft.jar at root.
// shadowJar excludes this so it doesn't overwrite.
public class ModLoader {

	public static net.minecraft.client.Minecraft getMinecraftInstance() {
		return null;
	}

	public static void SetInGameHook(BaseMod mod, boolean autoTick, boolean useClock) {
	}

	public static void RegisterKey(BaseMod mod, net.minecraft.client.options.KeyBinding key, boolean allowRepeat) {
	}
}
