package com.moyettes.legacyvoicechat;

import com.moyettes.legacyvoicechat.compat.BabricSTAPICompat;
import com.moyettes.legacyvoicechat.compat.networking.BabricSTAPINetworking;
import net.mine_diver.unsafeevents.listener.EventListener;
import net.modificationstation.stationapi.api.event.mod.InitEvent;
import net.modificationstation.stationapi.api.event.network.packet.PacketRegisterEvent;
import net.modificationstation.stationapi.api.mod.entrypoint.EntrypointManager;
import net.modificationstation.stationapi.api.registry.PacketTypeRegistry;
import net.modificationstation.stationapi.api.registry.Registry;
import net.modificationstation.stationapi.api.util.Identifier;
import net.modificationstation.stationapi.api.util.Namespace;

import java.lang.invoke.MethodHandles;

public class VoiceBootstrapBabricStationAPI {

	static {
		EntrypointManager.registerLookup(MethodHandles.lookup());
	}

	@SuppressWarnings("UnstableApiUsage")
	public static final Namespace NAMESPACE = Namespace.resolve();

	@EventListener
	public static void serverInit(InitEvent event) {
		VoiceContext ctx = VoiceContext.builder()
			.loader(LoaderId.BABRIC_STATIONAPI)
			.minecraftVersion("b1.7.3")
			.minecraft(new BabricSTAPICompat())
			.networking(new BabricSTAPINetworking())
			.build();

		new Voice().init(ctx);
	}

	@EventListener
	public void registerPackets(PacketRegisterEvent event) {
		Registry.register(PacketTypeRegistry.INSTANCE, id("custom_payload"),
			com.moyettes.legacyvoicechat.packet.CustomPayloadPacket.TYPE);
	}

	public static Identifier id(String name) {
		return NAMESPACE.id(name);
	}
}
