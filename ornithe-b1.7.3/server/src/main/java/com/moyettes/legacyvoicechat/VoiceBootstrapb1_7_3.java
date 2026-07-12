package com.moyettes.legacyvoicechat;

import com.moyettes.legacyvoicechat.LoaderId;
import com.moyettes.legacyvoicechat.compat.b1_7_3Compat;
import com.moyettes.legacyvoicechat.compat.networking.VoiceNetworkingCompatb1_7_3;
import net.ornithemc.osl.entrypoints.api.ModInitializer;

public class VoiceBootstrapb1_7_3 implements ModInitializer {

	@Override
	public void init() {
		VoiceContext ctx = VoiceContext.builder()
			.loader(LoaderId.ORNITHE)
			.minecraftVersion("b1.7.3")
			.minecraft(new b1_7_3Compat())
			.networking(new VoiceNetworkingCompatb1_7_3())
			.build();

		new Voice().init(ctx);
	}
}
