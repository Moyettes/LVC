package com.moyettes.legacyvoicechat.mixin;

import net.minecraft.core.net.packet.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = Packet.class, remap = false)
public interface PacketAccessor {

	@Invoker("addMapping")
	public static void addMapping(int id, boolean clientBound, boolean serverBound, Class<? extends Packet> packetClass) {
		throw new AssertionError();
	}
}
