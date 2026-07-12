package com.moyettes.legacyvoicechat;

import com.moyettes.legacyvoicechat.compat.V1_7_2Compat;
import com.moyettes.legacyvoicechat.compat.networking.VoiceNetworkingCompat1_7_2;
import net.ornithemc.osl.entrypoints.api.server.ServerModInitializer;

public class VoiceBootstrap1_7_2 implements ServerModInitializer {

	@Override
	public void initServer() {
		VoiceContext ctx = VoiceContext.builder()
			.loader(LoaderId.ORNITHE)
			.minecraftVersion("1.7.2")
			.minecraft(new V1_7_2Compat())
			.networking(new VoiceNetworkingCompat1_7_2())
			.build();

		new Voice().init(ctx);
	}
}
