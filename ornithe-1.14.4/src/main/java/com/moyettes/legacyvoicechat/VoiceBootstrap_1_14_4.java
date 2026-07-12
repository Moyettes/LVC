package com.moyettes.legacyvoicechat;

import com.moyettes.legacyvoicechat.LoaderId;
import com.moyettes.legacyvoicechat.compat.V1_14_4Compat;
import com.moyettes.legacyvoicechat.compat.networking.VoiceNetworkingCompatV1_14_4;
import net.ornithemc.osl.entrypoints.api.server.ServerModInitializer;

public class VoiceBootstrap_1_14_4 implements ServerModInitializer {

	@Override
	public void initServer() {
		VoiceContext ctx = VoiceContext.builder()
			.loader(LoaderId.ORNITHE)
			.minecraftVersion("1.14.4")
			.minecraft(new V1_14_4Compat())
			.networking(new VoiceNetworkingCompatV1_14_4())
			.build();

		new Voice().init(ctx);
	}
}
