package com.moyettes.legacyvoicechat.client;

import com.moyettes.legacyvoicechat.LoaderId;
import com.moyettes.legacyvoicechat.api.client.ClientConnectionEvents;
import com.moyettes.legacyvoicechat.client.VoiceClient;
import com.moyettes.legacyvoicechat.client.VoiceClientBoot;
import com.moyettes.legacyvoicechat.client.VoiceClientContext;
import com.moyettes.legacyvoicechat.client.VoiceSession;
import com.moyettes.legacyvoicechat.client.bridge.GuiBridgeImpl;
import com.moyettes.legacyvoicechat.client.bridge.MinecraftClientAccessorImpl;
import com.moyettes.legacyvoicechat.client.bridge.RenderApiImpl;
import com.moyettes.legacyvoicechat.client.gui.screen.GroupMembersScreen;
import com.moyettes.legacyvoicechat.client.gui.screen.GroupsScreen;
import com.moyettes.legacyvoicechat.client.platform.IRenderApi;
import com.moyettes.legacyvoicechat.events.ClientEvents;
import com.moyettes.legacyvoicechat.events.client.KeyEvents;
import com.moyettes.legacyvoicechat.events.client.RenderEvents;
import com.moyettes.legacyvoicechat.mixin.client.ClientNetworkHandlerInterfaceMixin;
import com.moyettes.legacyvoicechat.mixin.client.ConnectionInterfaceMixin;
import com.moyettes.legacyvoicechat.packet.ClientPlayNetworking;
import com.moyettes.legacyvoicechat.packet.VoiceModSupportedPayload;
import com.moyettes.legacyvoicechat.packet.VoiceServerInfoPayload;
import com.moyettes.legacyvoicechat.utils.MinecraftAccessor;
import net.mine_diver.unsafeevents.listener.EventListener;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.ClientNetworkHandler;
import net.modificationstation.stationapi.api.event.mod.InitEvent;
import net.modificationstation.stationapi.api.mod.entrypoint.EntrypointManager;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.logging.Logger;

public class Voice {

	static {
		EntrypointManager.registerLookup(MethodHandles.lookup());
	}

	public static final Logger LOGGER = Logger.getLogger("LegacyVoiceChat");

	private static VoiceClient voiceClient;
	private static final String PROTOCOL_VERSION = "1.0.0";
	private static final boolean MIC_ENABLED = true;

	@EventListener
	private static void clientInit(InitEvent event) {
		LOGGER.info("Initializing voice mod (Babric+STAPI client)!");

		IRenderApi renderApi = new RenderApiImpl();
		VoiceClientContext ctx = VoiceClientContext.builder()
			.loader(LoaderId.BABRIC_STATIONAPI)
			.minecraftVersion("b1.7.3")
			.render(renderApi)
			.client(new MinecraftClientAccessorImpl())
			.gui(new GuiBridgeImpl(renderApi))
			.build();
		if (VoiceClientBoot.getContext() == null) {
			VoiceClientBoot.start(ctx);
		}

		registerNetworkListeners();
		new ClientEvents();
		new RenderEvents();
		ClientEvents.INSTANCE.registerKeyBindsEvent(KeyEvents::updateKeyBindings);
	}

	private static void registerNetworkListeners() {
		ClientPlayNetworking.registerListener("voice|server_info", VoiceServerInfoPayload::new,
			(minecraft, handler, payload) -> {
				LOGGER.info("Received voice server info - Port: " + payload.getVoiceServerPort() +
					", Protocol: " + payload.getProtocolVersion());

				connectToVoiceServer(payload, MinecraftAccessor.getMinecraft());
				return true;
			});

		ClientConnectionEvents.PLAY_READY.register((minecraft) -> {
			LOGGER.info("Connected to server, sending voice mod support");
			VoiceModSupportedPayload modSupported = new VoiceModSupportedPayload(PROTOCOL_VERSION);
			ClientPlayNetworking.send("voice|mod_supported", modSupported);
		});

		ClientConnectionEvents.DISCONNECT.register((minecraft) -> {
			if (voiceClient == null) return;
			voiceClient.disconnect();
		});
	}

	private static void connectToVoiceServer(VoiceServerInfoPayload serverInfo, Minecraft minecraft) {
		try {
			Integer playerNetworkId = getPlayerNetworkId(minecraft);
			voiceClient = new VoiceClient(playerNetworkId, MIC_ENABLED);
			VoiceSession.setActive(voiceClient);

			voiceClient.setHandshakeCallback(ack -> {
				if (!ack.isSuccess()) {
					LOGGER.warning("Failed to connect to voice server: " + ack.getMessage());
				} else {
					startListenerPositionUpdater(minecraft);
				}
			});

			String serverHost = serverInfo.getServerHost();

			if ("0.0.0.0".equals(serverHost)) {
				try {
					ClientNetworkHandler networkHandler = minecraft.getNetworkHandler();
					if (networkHandler != null) {
						ClientNetworkHandlerInterfaceMixin accessor = (ClientNetworkHandlerInterfaceMixin) networkHandler;
						net.minecraft.network.Connection connection = accessor.getConnection();
						if (connection != null) {
							ConnectionInterfaceMixin connectionAccessor = (ConnectionInterfaceMixin) connection;
							java.net.SocketAddress remoteAddress = connectionAccessor.getAddress();
							if (remoteAddress instanceof java.net.InetSocketAddress) {
								java.net.InetSocketAddress inetAddress = (java.net.InetSocketAddress) remoteAddress;
								java.net.InetAddress address = inetAddress.getAddress();

								if (address instanceof java.net.Inet4Address && !address.isLoopbackAddress()) {
									serverHost = address.getHostAddress();
								} else {
									serverHost = "127.0.0.1";
								}
							} else {
								serverHost = "127.0.0.1";
							}
						} else {
							serverHost = "127.0.0.1";
						}
					} else {
						serverHost = "127.0.0.1";
					}
				} catch (Exception e) {
					LOGGER.warning("Could not resolve 0.0.0.0, using localhost: " + e.getMessage());
					serverHost = "127.0.0.1";
				}
			}

			voiceClient.setGroupListCallback(groupList -> {
				GroupsScreen.updateCurrentGroupList(groupList.getGroups());
			});

			voiceClient.setGroupMemberCallback(memberNames -> {
				GroupMembersScreen.updateCurrentMemberList(memberNames);
			});

			voiceClient.connect(serverHost, serverInfo.getVoiceServerPort(), serverInfo.getAuthToken());

		} catch (IOException e) {
			LOGGER.severe("Failed to connect to voice server: " + e.getMessage());
		}
	}

	private static Integer getPlayerNetworkId(Minecraft minecraft) {
		try {
			if (minecraft != null && minecraft.player != null) {
				return minecraft.player.id;
			}
		} catch (Exception e) {
			LOGGER.severe("Failed to get player network ID: " + e.getMessage());
		}
		return 12345;
	}

	public static VoiceClient getVoiceClient() {
		return voiceClient;
	}

	public static void forceDisconnect() {
		if (voiceClient != null) {
			voiceClient.disconnect();
			voiceClient = null;
			VoiceSession.setActive(null);
		}
	}

	private static void startListenerPositionUpdater(Minecraft minecraft) {
		Thread positionUpdater = new Thread(() -> {
			while (voiceClient != null && voiceClient.isConnected()) {
				try {
					if (minecraft.player != null) {
						double x = minecraft.player.x;
						double y = minecraft.player.y;
						double z = minecraft.player.z;
						float yaw = minecraft.player.yaw;
						float pitch = minecraft.player.pitch;

						while (yaw < 0) yaw += 360;
						while (yaw >= 360) yaw -= 360;

						voiceClient.updateListenerPosition(x, y, z, yaw, pitch);
					}

					Thread.sleep(50);
				} catch (InterruptedException e) {
					break;
				} catch (Exception e) {
					LOGGER.severe("Error updating listener position: " + e.getMessage());
				}
			}
		}, "VoiceListenerPositionUpdater");

		positionUpdater.setDaemon(true);
		positionUpdater.start();
	}
}
