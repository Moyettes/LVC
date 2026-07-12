package com.moyettes.legacyvoicechat;

import com.moyettes.legacyvoicechat.compat.V1_3_1Compat;
import com.moyettes.legacyvoicechat.compat.networking.VoiceNetworkingCompat1_3_1;
import net.ornithemc.osl.entrypoints.api.server.ServerModInitializer;

public class VoiceBootstrap1_3_1 implements ServerModInitializer {

	@Override
	public void initServer() {
		VoiceContext ctx = VoiceContext.builder()
			.loader(LoaderId.ORNITHE)
			.minecraftVersion("1.3.1")
			.minecraft(new V1_3_1Compat())
			.networking(new VoiceNetworkingCompat1_3_1())
			.build();

		new Voice().init(ctx);
	}
}
