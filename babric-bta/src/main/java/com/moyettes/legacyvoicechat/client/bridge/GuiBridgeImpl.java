package com.moyettes.legacyvoicechat.client.bridge;

import com.moyettes.legacyvoicechat.client.platform.IGuiBridge;
import com.moyettes.legacyvoicechat.client.platform.IRenderApi;
import com.moyettes.legacyvoicechat.client.platform.IScreenLifecycle;
import com.moyettes.legacyvoicechat.utils.MinecraftAccessor;
import net.minecraft.client.Minecraft;

public final class GuiBridgeImpl implements IGuiBridge {

	private final IRenderApi api;

	public GuiBridgeImpl(IRenderApi api) {
		this.api = api;
	}

	@Override
	public void openScreen(IScreenLifecycle lifecycle) {
		Minecraft mc = MinecraftAccessor.getMinecraft();
		if (mc == null) return;
		mc.displayScreen(new VoiceBridgeScreen(lifecycle, api));
	}

	@Override
	public void closeScreen() {
		Minecraft mc = MinecraftAccessor.getMinecraft();
		if (mc == null) return;
		mc.displayScreen(null);
	}

	@Override
	public boolean isScreenOpen() {
		Minecraft mc = MinecraftAccessor.getMinecraft();
		return mc != null && mc.currentScreen instanceof VoiceBridgeScreen;
	}
}
