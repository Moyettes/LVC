package com.moyettes.legacyvoicechat.mixin;

import com.moyettes.legacyvoicechat.packet.CustomPayloadPacket;
import net.minecraft.network.packet.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.Set;

@Mixin(Packet.class)
public class PacketMixin {

	@Shadow private static Map ID_TO_TYPE;
	@Shadow private static Map TYPE_TO_ID;
	@Shadow private static Set S2C;
	@Shadow private static Set C2S;

	@Inject(method = "<clinit>", at = @At("TAIL"))
	private static void legacyvoicechat$registerCustomPayload(CallbackInfo ci) {
		Integer id = Integer.valueOf(250);
		Class type = CustomPayloadPacket.class;
		ID_TO_TYPE.put(id, type);
		TYPE_TO_ID.put(type, id);
		S2C.add(id);
		C2S.add(id);
	}
}
