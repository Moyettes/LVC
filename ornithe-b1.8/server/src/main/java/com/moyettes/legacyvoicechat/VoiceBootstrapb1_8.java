package com.moyettes.legacyvoicechat;

import com.moyettes.legacyvoicechat.LoaderId;
import com.moyettes.legacyvoicechat.compat.b1_8Compat;
import com.moyettes.legacyvoicechat.compat.networking.VoiceNetworkingCompatb1_8;
import net.ornithemc.osl.entrypoints.api.ModInitializer;

public class VoiceBootstrapb1_8 implements ModInitializer {

	@Override
	public void init() {
		VoiceContext ctx = VoiceContext.builder()
			.loader(LoaderId.ORNITHE)
			.minecraftVersion("b1.8")
			.minecraft(new b1_8Compat())
			.networking(new VoiceNetworkingCompatb1_8())
			.build();

		new Voice().init(ctx);
	}
}
