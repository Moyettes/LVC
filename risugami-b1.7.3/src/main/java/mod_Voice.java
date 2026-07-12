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
import com.moyettes.legacyvoicechat.events.KeyEvents;
import com.moyettes.legacyvoicechat.events.RenderEvents;
import com.moyettes.legacyvoicechat.mixin.ClientNetworkHandlerMixin;
import com.moyettes.legacyvoicechat.mixin.ConnectionMixin;
import com.moyettes.legacyvoicechat.packet.ClientPlayNetworking;
import com.moyettes.legacyvoicechat.packet.HandshakePayload;
import com.moyettes.legacyvoicechat.packet.VoiceModSupportedPayload;
import com.moyettes.legacyvoicechat.packet.VoiceServerInfoPayload;
import com.moyettes.legacyvoicechat.utils.MinecraftAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.handler.ClientNetworkHandler;
import net.minecraft.network.Connection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

// Risugami ModLoader entry point. ModLoader scans the root of minecraft.jar for
// mod_*.class files and instantiates each one. The no-arg constructor runs the
// same setup MinecraftMixin does in the RawRetroMCP module (accessor + Voice
// event bus bring-up + network listener registration).
public class mod_Voice extends BaseMod {

	public static final Logger LOGGER = LogManager.getLogger("LegacyVoiceChat");

	private static final String PROTOCOL_VERSION = "1.0.0";
	private static final boolean MIC_ENABLED = true;

	private static VoiceClient voiceClient;
	private static volatile boolean hideIcons = false;
	public static KeyEvents keyEvents;

	public mod_Voice() {
		ModLoader.SetInGameHook(this, true, false);
		MinecraftAccessor.setInstance(ModLoader.getMinecraftInstance());
		LOGGER.info("Initializing voice mod");

		IRenderApi renderApi = new RenderApiImpl();
		VoiceClientContext ctx = VoiceClientContext.builder()
			.loader(LoaderId.RISUGAMI)
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
		keyEvents = new KeyEvents(this, LOGGER);
	}

	@Override
	public boolean OnTickInGame(Minecraft game) {
		// Risugami calls this every in-game tick. Dispatch to the shared render
		// bus (icons, player overlays) and poll PTT for hold-state detection.
		ClientEvents inst = ClientEvents.INSTANCE;
		if (inst != null) inst.onRenderHUD(0.0f);
		if (keyEvents != null) keyEvents.onTick();
		return true;
	}

	@Override
	public boolean OnTickInGame(Minecraft game, Screen gui) {
		return false;
	}

	@Override
	public void KeyboardEvent(net.minecraft.client.options.KeyBinding keybinding) {
		if (keyEvents != null) keyEvents.onKey(keybinding);
	}

	@Override
	public String Version() {
		return "0.1.0";
	}

	private static void registerNetworkListeners() {
		ClientPlayNetworking.registerListener(HandshakePayload.CHANNEL, HandshakePayload::new,
			(minecraft, handler, payload) -> {
				ClientPlayNetworking.send(HandshakePayload.CHANNEL, HandshakePayload.client());
				ClientConnectionEvents.PLAY_READY.invoker().accept(minecraft);
				return true;
			});

		ClientPlayNetworking.registerListener("voice|server_info", VoiceServerInfoPayload::new,
			(minecraft, handler, payload) -> {
				LOGGER.info("Received voice server info - port {}, protocol {}",
					payload.getVoiceServerPort(), payload.getProtocolVersion());
				connectToVoiceServer(payload, minecraft);
				return true;
			});

		ClientConnectionEvents.PLAY_READY.register(minecraft -> {
			LOGGER.info("Channel handshake complete - announcing voice mod support");
			ClientPlayNetworking.send("voice|mod_supported",
				new VoiceModSupportedPayload(PROTOCOL_VERSION));
		});

		ClientConnectionEvents.DISCONNECT.register(minecraft -> {
			LOGGER.info("Disconnected from server");
			forceDisconnect();
		});
	}

	private static void connectToVoiceServer(VoiceServerInfoPayload serverInfo, Minecraft minecraft) {
		try {
			Integer playerNetworkId = getPlayerNetworkId(minecraft);
			voiceClient = new VoiceClient(playerNetworkId, MIC_ENABLED);
			VoiceSession.setActive(voiceClient);

			voiceClient.setHandshakeCallback(ack -> {
				if (!ack.isSuccess()) {
					LOGGER.warn("Failed to connect to voice server: {}", ack.getMessage());
				} else {
					startListenerPositionUpdater(minecraft);
				}
			});

			voiceClient.setGroupListCallback(groupList -> {
				GroupsScreen.updateCurrentGroupList(groupList.getGroups());
			});

			voiceClient.setGroupMemberCallback(memberNames -> {
				GroupMembersScreen.updateCurrentMemberList(memberNames);
			});

			String serverHost = serverInfo.getServerHost();
			if ("0.0.0.0".equals(serverHost)) {
				try {
					InetAddress resolved = resolveServerHostFromConnection(minecraft);
					if (resolved instanceof Inet4Address && !resolved.isLoopbackAddress()) {
						serverHost = resolved.getHostAddress();
					} else {
						serverHost = "127.0.0.1";
					}
				} catch (Exception e) {
					LOGGER.warn("Could not resolve 0.0.0.0, falling back to localhost: {}", e.getMessage());
					serverHost = "127.0.0.1";
				}
			}

			voiceClient.connect(serverHost, serverInfo.getVoiceServerPort(), serverInfo.getAuthToken());
		} catch (IOException e) {
			LOGGER.error("Failed to connect to voice server: {}", e.getMessage());
		}
	}

	private static InetAddress resolveServerHostFromConnection(Minecraft minecraft) {
		ClientNetworkHandler networkHandler = minecraft.getNetworkHandler();
		if (networkHandler == null) throw new IllegalStateException("No network handler");
		Connection connection = ((ClientNetworkHandlerMixin) networkHandler).getConnection();
		if (connection == null) throw new IllegalStateException("No connection");
		SocketAddress remote = ((ConnectionMixin) connection).getAddress();
		if (!(remote instanceof InetSocketAddress)) throw new IllegalStateException("Not an InetSocketAddress");
		return ((InetSocketAddress) remote).getAddress();
	}

	private static Integer getPlayerNetworkId(Minecraft minecraft) {
		try {
			if (minecraft != null && minecraft.player != null) {
				return minecraft.player.networkId;
			}
		} catch (Exception e) {
			LOGGER.error("Failed to get player network ID: {}", e.getMessage());
		}
		return 12345;
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
					LOGGER.error("Error updating listener position: {}", e.getMessage());
				}
			}
		}, "VoiceListenerPositionUpdater");
		positionUpdater.setDaemon(true);
		positionUpdater.start();
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

	public static void toggleIcons() {
		hideIcons = !hideIcons;
	}

	public static boolean isIconsHidden() {
		return hideIcons;
	}
}
