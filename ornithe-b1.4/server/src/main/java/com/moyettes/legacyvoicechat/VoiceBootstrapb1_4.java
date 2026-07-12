package com.moyettes.legacyvoicechat;

import com.moyettes.legacyvoicechat.LoaderId;
import com.moyettes.legacyvoicechat.compat.b1_4Compat;
import com.moyettes.legacyvoicechat.compat.networking.VoiceNetworkingCompatb1_4;
import net.ornithemc.osl.entrypoints.api.ModInitializer;

public class VoiceBootstrapb1_4 implements ModInitializer {

	@Override
	public void init() {
		VoiceContext ctx = VoiceContext.builder()
			.loader(LoaderId.ORNITHE)
			.minecraftVersion("b1.4_01")
			.minecraft(new b1_4Compat())
			.networking(new VoiceNetworkingCompatb1_4())
			.build();

		new Voice().init(ctx);
	}
}
