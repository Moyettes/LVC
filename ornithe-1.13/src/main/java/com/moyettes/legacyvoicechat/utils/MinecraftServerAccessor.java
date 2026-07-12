package com.moyettes.legacyvoicechat.utils;

import net.minecraft.server.MinecraftServer;

import java.util.concurrent.atomic.AtomicReference;

public interface MinecraftServerAccessor {
	AtomicReference<MinecraftServer> instance = new AtomicReference<>();

	static void setInstance(MinecraftServer minecraft) {
		instance.set(minecraft);
	}

	static MinecraftServer getMinecraftServer() {
		return instance.get();
	}

}
