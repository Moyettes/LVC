package com.moyettes.legacyvoicechat;

import com.moyettes.legacyvoicechat.LoaderId;
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
import com.moyettes.legacyvoicechat.events.KeyEvents;
import com.moyettes.legacyvoicechat.events.RenderEvents;
import com.moyettes.legacyvoicechat.mixin.ClientNetworkHandlerMixin;
import com.moyettes.legacyvoicechat.mixin.ConnectionInterfaceMixin;
import com.moyettes.legacyvoicechat.mixin.ConnectionMixin;
import com.moyettes.legacyvoicechat.packet.VoiceModSupportedPayload;
import com.moyettes.legacyvoicechat.packet.VoiceServerInfoPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.handler.ClientNetworkHandler;
import net.minecraft.network.Connection;
import net.ornithemc.osl.entrypoints.api.ModInitializer;
import net.ornithemc.osl.networking.api.client.ClientConnectionEvents;
import net.ornithemc.osl.networking.api.client.ClientPlayNetworking;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

public class Voice implements ModInitializer {

	public static final Logger LOGGER = LogManager.getLogger("LegacyVoiceChat");

	public static final class OslModSupported extends VoiceModSupportedPayload
		implements net.ornithemc.osl.networking.api.CustomPayload {
		public OslModSupported() { super(); }
		public OslModSupported(String protocolVersion) { super(protocolVersion); }
	}

	public static final class OslServerInfo extends VoiceServerInfoPayload
		implements net.ornithemc.osl.networking.api.CustomPayload {
		public OslServerInfo() { super(); }
	}

	private static VoiceClient voiceClient;
	private static final String PROTOCOL_VERSION = "1.0.0";
	private static final boolean MIC_ENABLED = true;
	private static volatile boolean hideIcons = false;
	public static KeyEvents keyEvents;

	@Override
	public void init() {
		LOGGER.info("Initializing voice mod!");

		IRenderApi renderApi = new RenderApiImpl();
		VoiceClientContext ctx = VoiceClientContext.builder()
			.loader(LoaderId.ORNITHE)
			.minecraftVersion("1.0.0")
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
	}

	private void registerNetworkListeners() {
		ClientPlayNetworking.registerListener("voice|server_info", OslServerInfo::new,
			(minecraft, handler, payload) -> {
				LOGGER.info("Received voice server info - Port: " + payload.getVoiceServerPort() +
					", Protocol: " + payload.getProtocolVersion());

				connectToVoiceServer(payload, minecraft);
				return true;
			});

		ClientConnectionEvents.PLAY_READY.register((minecraft) -> {
			LOGGER.info("Connected to server, sending voice mod support");
			ClientPlayNetworking.send("voice|mod_supported", new OslModSupported(PROTOCOL_VERSION));
		});

		ClientConnectionEvents.DISCONNECT.register((minecraft) -> {
			if(voiceClient == null) return;
			voiceClient.disconnect();
		});
	}

	private void connectToVoiceServer(VoiceServerInfoPayload serverInfo, Minecraft minecraft) {
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
					InetAddress resolvedAddress = resolveServerHostFromConnection(minecraft);
					if (resolvedAddress instanceof Inet4Address && !resolvedAddress.isLoopbackAddress()) {
						serverHost = resolvedAddress.getHostAddress();
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

	private InetAddress resolveServerHostFromConnection(Minecraft minecraft) {
		ClientNetworkHandler networkHandler = minecraft.getNetworkHandler();
		if (networkHandler == null) throw new IllegalStateException("No network handler");

		ClientNetworkHandlerMixin accessor = (ClientNetworkHandlerMixin) networkHandler;
		Connection connection = accessor.getConnection();
		if (connection == null) throw new IllegalStateException("No connection");

		ConnectionInterfaceMixin connectionAccessor = (ConnectionInterfaceMixin) connection;
		SocketAddress remoteAddress = connectionAccessor.getAddress();
		if (!(remoteAddress instanceof InetSocketAddress)) throw new IllegalStateException("Not an InetSocketAddress");

		return ((InetSocketAddress) remoteAddress).getAddress();
	}

	private Integer getPlayerNetworkId(net.minecraft.client.Minecraft minecraft) {
		try {
			if (minecraft != null && minecraft.player != null) {
				return minecraft.player.networkId;
			}
		} catch (Exception e) {
			LOGGER.error("Failed to get player network ID: " + e.getMessage());
		}
		return 12345;
	}


	public static VoiceClient getVoiceClient() {
		return voiceClient;
	}

	public static void createGroup(String groupName, String password) {
		if (voiceClient != null) {
			voiceClient.createGroup(groupName, password);
		} else {
			LOGGER.warn("Cannot create group: voice client not connected");
		}
	}

	public static void joinGroup(String groupName, String password) {
		if (voiceClient != null) {
			voiceClient.joinGroup(groupName, password);
		} else {
			LOGGER.warn("Cannot join group: voice client not connected");
		}
	}

	public static void leaveGroup() {
		if (voiceClient != null) {
			voiceClient.leaveGroup();
		} else {
			LOGGER.warn("Cannot leave group: voice client not connected");
		}
	}

	public static void requestGroupList() {
		if (voiceClient != null) {
			voiceClient.requestGroupList();
		} else {
			LOGGER.warn("Cannot request group list: voice client not connected");
		}
	}

	public static boolean isInGroup() {
		return voiceClient != null && voiceClient.isInGroup();
	}

	public static String getCurrentGroupName() {
		return voiceClient != null ? voiceClient.getCurrentGroupName() : null;
	}

	public static void forceDisconnect() {
		if (voiceClient != null) {
			voiceClient.disconnect();
			voiceClient = null;
			VoiceSession.setActive(null);
		}
	}

	public static void toggleIcons() {
		hideIcons = !hideIcons;
	}

	public static boolean isIconsHidden(){
		return hideIcons;
	}

	private void startListenerPositionUpdater(net.minecraft.client.Minecraft minecraft) {
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
					LOGGER.error("Error updating listener position: " + e.getMessage());
				}
			}
		}, "VoiceListenerPositionUpdater");

		positionUpdater.setDaemon(true);
		positionUpdater.start();
	}
}
