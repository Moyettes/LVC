package com.moyettes.legacyvoicechat;

import com.moyettes.legacyvoicechat.compat.V1_7_10Compat;
import com.moyettes.legacyvoicechat.compat.networking.VoiceNetworkingCompat1_7_10;
import net.ornithemc.osl.entrypoints.api.server.ServerModInitializer;

public class VoiceBootstrap1_7_10 implements ServerModInitializer {

	@Override
	public void initServer() {
		VoiceContext ctx = VoiceContext.builder()
			.loader(LoaderId.ORNITHE)
			.minecraftVersion("1.7.10")
			.minecraft(new V1_7_10Compat())
			.networking(new VoiceNetworkingCompat1_7_10())
			.build();

		new Voice().init(ctx);
	}
}
