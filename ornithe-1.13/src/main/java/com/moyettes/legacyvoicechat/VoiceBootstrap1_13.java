package com.moyettes.legacyvoicechat;

import com.moyettes.legacyvoicechat.compat.V1_13Compat;
import com.moyettes.legacyvoicechat.compat.networking.VoiceNetworkingCompat1_13;
import net.ornithemc.osl.entrypoints.api.server.ServerModInitializer;

public class VoiceBootstrap1_13 implements ServerModInitializer {

	@Override
	public void initServer() {
		VoiceContext ctx = VoiceContext.builder()
			.loader(LoaderId.ORNITHE)
			.minecraftVersion("1.13")
			.minecraft(new V1_13Compat())
			.networking(new VoiceNetworkingCompat1_13())
			.build();

		new Voice().init(ctx);
	}
}
