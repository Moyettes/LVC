package com.moyettes.legacyvoicechat;

import com.moyettes.legacyvoicechat.LoaderId;
import com.moyettes.legacyvoicechat.compat.b1_0Compat;
import com.moyettes.legacyvoicechat.compat.networking.VoiceNetworkingCompatb1_0;
import net.ornithemc.osl.entrypoints.api.ModInitializer;

public class VoiceBootstrap_b1_0 implements ModInitializer {

	@Override
	public void init() {
		VoiceContext ctx = VoiceContext.builder()
			.loader(LoaderId.ORNITHE)
			.minecraftVersion("b1.0")
			.minecraft(new b1_0Compat())
			.networking(new VoiceNetworkingCompatb1_0())
			.build();

		new Voice().init(ctx);
	}
}
