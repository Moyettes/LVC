package com.moyettes.legacyvoicechat;

import com.moyettes.legacyvoicechat.LoaderId;
import com.moyettes.legacyvoicechat.compat.V1_0_0Compat;
import com.moyettes.legacyvoicechat.compat.networking.VoiceNetworkingCompat1_0_0;
import net.ornithemc.osl.entrypoints.api.ModInitializer;

public class VoiceBootstrap1_0_0 implements ModInitializer {

	@Override
	public void init() {
		VoiceContext ctx = VoiceContext.builder()
			.loader(LoaderId.ORNITHE)
			.minecraftVersion("1.0.0")
			.minecraft(new V1_0_0Compat())
			.networking(new VoiceNetworkingCompat1_0_0())
			.build();

		new Voice().init(ctx);
	}
}
