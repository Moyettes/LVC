package com.moyettes.legacyvoicechat.utils;

import net.minecraft.client.Minecraft;

import java.util.concurrent.atomic.AtomicReference;

// Regular class, NOT an interface. A static method on an interface is a Java 8
// feature that emits an InterfaceMethodref in the constant pool, and invokestatic
// on an InterfaceMethodref requires class file version >= 52. Minecraft b1.7.3
// classes are compiled at version 49 (Java 5), so baking the mixin invocation
// site into Minecraft.class with an invokestatic to an interface would trip the
// JVM verifier with "Illegal type in constant pool". Keeping this as a final
// class emits a plain Methodref which invokestatic accepts at any class version.
public final class MinecraftAccessor {

	private static final AtomicReference<Minecraft> instance = new AtomicReference<>();

	private MinecraftAccessor() {}

	public static void setInstance(Minecraft minecraft) {
		instance.set(minecraft);
	}

	public static Minecraft getMinecraft() {
		return instance.get();
	}
}
