package com.moyettes.legacyvoicechat;

import com.moyettes.legacyvoicechat.compat.V1_5_1Compat;
import com.moyettes.legacyvoicechat.compat.networking.VoiceNetworkingCompat1_5_1;
import net.ornithemc.osl.entrypoints.api.server.ServerModInitializer;

public class VoiceBootstrap1_5_1 implements ServerModInitializer {

	@Override
	public void initServer() {
		VoiceContext ctx = VoiceContext.builder()
			.loader(LoaderId.ORNITHE)
			.minecraftVersion("1.5.1")
			.minecraft(new V1_5_1Compat())
			.networking(new VoiceNetworkingCompat1_5_1())
			.build();

		new Voice().init(ctx);
	}
}
