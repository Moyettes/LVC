package com.moyettes.legacyvoicechat;

import com.moyettes.legacyvoicechat.compat.BabricBTACompat;
import com.moyettes.legacyvoicechat.compat.networking.BabricBTANetworking;
import com.moyettes.legacyvoicechat.mixin.PacketAccessor;
import com.moyettes.legacyvoicechat.packet.CustomPayloadPacket;
import net.fabricmc.api.DedicatedServerModInitializer;

public class VoiceBootstrapBabricBTA implements DedicatedServerModInitializer {

	@Override
	public void onInitializeServer() {
		PacketAccessor.addMapping(55, true, true, CustomPayloadPacket.class);

		VoiceContext ctx = VoiceContext.builder()
			.loader(LoaderId.BABRIC_BTA)
			.minecraftVersion("bta-" + "7.3_04")
			.minecraft(new BabricBTACompat())
			.networking(new BabricBTANetworking())
			.build();

		new Voice().init(ctx);
	}
}
