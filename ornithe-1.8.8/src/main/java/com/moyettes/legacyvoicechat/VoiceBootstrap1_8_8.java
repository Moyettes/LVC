package com.moyettes.legacyvoicechat;

import com.moyettes.legacyvoicechat.compat.V1_8_8Compat;
import com.moyettes.legacyvoicechat.compat.networking.VoiceNetworkingCompat1_8_8;
import net.ornithemc.osl.entrypoints.api.server.ServerModInitializer;

public class VoiceBootstrap1_8_8 implements ServerModInitializer {

	@Override
	public void initServer() {
		VoiceContext ctx = VoiceContext.builder()
			.loader(LoaderId.ORNITHE)
			.minecraftVersion("1.8.8")
			.minecraft(new V1_8_8Compat())
			.networking(new VoiceNetworkingCompat1_8_8())
			.build();

		new Voice().init(ctx);
	}
}
