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
import com.moyettes.legacyvoicechat.mixin.PacketAccessor;
import com.moyettes.legacyvoicechat.mixin.client.ClientNetworkHandlerInterfaceMixin;
import com.moyettes.legacyvoicechat.mixin.client.ConnectionInterfaceMixin;
import com.moyettes.legacyvoicechat.packet.ClientPlayNetworking;
import com.moyettes.legacyvoicechat.packet.CustomPayloadPacket;
import com.moyettes.legacyvoicechat.packet.VoiceModSupportedPayload;
import com.moyettes.legacyvoicechat.packet.VoiceServerInfoPayload;
import com.moyettes.legacyvoicechat.utils.MinecraftAccessor;
import com.moyettes.legacyvoicechat.utils.MinecraftAccessorInterface;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.net.handler.PacketHandlerClient;
import net.minecraft.core.net.NetworkManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class Voice implements ClientModInitializer {

	public static final String MOD_ID = "legacyvoicechat";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static VoiceClient voiceClient;
	private static final String PROTOCOL_VERSION = "1.0.0";
	private static final boolean MIC_ENABLED = true;
	public static KeyEvents keyEvents;

	@Override
	public void onInitializeClient() {
		LOGGER.info("Registering packet ID 55 for CustomPayloadPacket");
		PacketAccessor.addMapping(55, true, true, CustomPayloadPacket.class);

		IRenderApi renderApi = new RenderApiImpl();
		VoiceClientContext ctx = VoiceClientContext.builder()
			.loader(LoaderId.BABRIC_BTA)
			.minecraftVersion("bta-" + "7.3_04")
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
		keyEvents = new KeyEvents();
		ClientEvents.INSTANCE.registerKeyBindsEvent(keyEvents::handleKeybinds);
		KeyEvents.register();
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
					LOGGER.warn("Failed to connect to voice server: " + ack.getMessage());
				} else {
					startListenerPositionUpdater(minecraft);
				}
			});

			String serverHost = serverInfo.getServerHost();

			if ("0.0.0.0".equals(serverHost)) {
				try {
					PacketHandlerClient networkHandler = ((MinecraftAccessorInterface) minecraft).getNetworkHandler();
					if (networkHandler != null) {
						ClientNetworkHandlerInterfaceMixin accessor = (ClientNetworkHandlerInterfaceMixin) networkHandler;
						NetworkManager connection = accessor.getConnection();
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
					LOGGER.warn("Could not resolve 0.0.0.0, using localhost: " + e.getMessage());
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
			LOGGER.error("Failed to connect to voice server: " + e.getMessage());
		}
	}

	private static Integer getPlayerNetworkId(Minecraft minecraft) {
		try {
			if (minecraft != null && minecraft.thePlayer != null) {
				return minecraft.thePlayer.id;
			}
		} catch (Exception e) {
			LOGGER.error("Failed to get player network ID: " + e.getMessage());
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
					if (minecraft.thePlayer != null) {
						double x = minecraft.thePlayer.x;
						double y = minecraft.thePlayer.y;
						double z = minecraft.thePlayer.z;
						float yaw = minecraft.thePlayer.yRot;
						float pitch = minecraft.thePlayer.xRot;

						while (yaw < 0) yaw += 360;
						while (yaw >= 360) yaw -= 360;

						voiceClient.updateListenerPosition(x, y, z, yaw, pitch);
					}

					Thread.sleep(50);
				} catch (InterruptedException e) {
					break;
				} catch (Exception e) {
					LOGGER.error("Error updating listener position: " + e.getMessage());
				}
			}
		}, "VoiceListenerPositionUpdater");

		positionUpdater.setDaemon(true);
		positionUpdater.start();
	}
}
