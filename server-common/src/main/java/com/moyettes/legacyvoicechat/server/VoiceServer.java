package com.moyettes.legacyvoicechat.server;

import com.moyettes.legacyvoicechat.Voice;
import com.moyettes.legacyvoicechat.compat.MinecraftCompat;
import com.moyettes.legacyvoicechat.compat.VoicePlayer;
import com.moyettes.legacyvoicechat.config.VoiceServerConfig;
import com.moyettes.legacyvoicechat.udp.*;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class VoiceServer {

	private static final String PROTOCOL_VERSION = "1.0.0";
	private static final long CACHE_UPDATE_INTERVAL = 1000;

	private DatagramSocket socket;
	private ExecutorService executor;
	private boolean running = false;
	private int port;
	private VoiceServerConfig config;

	private final Map<SocketAddress, Integer> connectedPlayers = new HashMap<>();

	private final Map<String, Group> groups = new ConcurrentHashMap<>();
	private final Map<Integer, String> playerToGroup = new ConcurrentHashMap<>();

	private final Map<Integer, VoicePlayer> playerCache = new ConcurrentHashMap<>();
	private final Map<Integer, SocketAddress> playerAddressCache = new ConcurrentHashMap<>();
	private long lastCacheUpdate = 0;

	private final Map<SocketAddress, Long> lastKeepAlive = new ConcurrentHashMap<>();
	private final Map<SocketAddress, Long> lastSentKeepAlive = new ConcurrentHashMap<>();

	private final Map<Integer, Boolean> playerVoiceSupported = new ConcurrentHashMap<>();
	private final Map<Integer, Boolean> playerDeafenState = new ConcurrentHashMap<>();

	public VoiceServer() {
		this.config = VoiceServerConfig.load();
		this.port = config.getPort();
		this.executor = Executors.newCachedThreadPool();
	}

	public void start() throws IOException {
		if (running) return;

		socket = new DatagramSocket(port);

		try {
			socket.setReceiveBufferSize(262144);
			socket.setSendBufferSize(262144);
			socket.setReuseAddress(true);
			socket.setTrafficClass(0x04);
		} catch (Exception e) {
			System.err.println("Failed to set socket buffer sizes: " + e.getMessage());
		}

		running = true;
		executor.submit(this::listenForPackets);
	}

	public void stop() {
		running = false;
		connectedPlayers.clear();
		if (socket != null && !socket.isClosed()) socket.close();
		if (executor != null) executor.shutdown();
	}

	private void listenForPackets() {
		byte[] buffer = new byte[2048];
		DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

		while (running && !socket.isClosed()) {
			try {
				socket.receive(packet);

				final int length = packet.getLength();
				if (length <= 0) continue;
				final byte[] dataCopy = new byte[length];
				System.arraycopy(packet.getData(), packet.getOffset(), dataCopy, 0, length);
				final SocketAddress sender = packet.getSocketAddress();

				executor.submit(() -> {
					DatagramPacket safePacket = new DatagramPacket(dataCopy, dataCopy.length, sender);
					handlePacket(safePacket);
				});
			} catch (IOException e) {
				if (running) {
					String errorMsg = e.getMessage();
					if (errorMsg != null && (
						errorMsg.contains("Connection reset") ||
						errorMsg.contains("Connection refused") ||
						errorMsg.contains("Network is unreachable") ||
						errorMsg.contains("Socket closed")
					)) {
					} else {
						System.err.println("Error receiving packet: " + e.getMessage());
					}
				}
			}
		}
	}

	private void handlePacket(DatagramPacket packet) {
		try {
			int packetLength = packet.getLength();
			if (packetLength <= 0) return;

			byte[] data = new byte[packetLength];
			System.arraycopy(packet.getData(), packet.getOffset(), data, 0, packetLength);

			UdpPacket udpPacket;
			try {
				udpPacket = UdpPacket.fromBytes(data);
			} catch (IOException e) {
				System.err.println("Failed to parse packet from " + packet.getSocketAddress() +
					" (" + data.length + " bytes): " + e.getMessage());
				return;
			}

			SocketAddress clientAddress = packet.getSocketAddress();

			switch (udpPacket.getType()) {
				case VOICE_HANDSHAKE:
					handleHandshake((VoiceHandshakePacket) udpPacket, clientAddress);
					break;
				case VOICE_DATA:
					handleVoiceData((VoiceDataPacket) udpPacket, clientAddress);
					break;
				case GROUP_CREATE:
					handleGroupCreate((GroupCreatePacket) udpPacket, clientAddress);
					break;
				case GROUP_JOIN:
					handleGroupJoin((GroupJoinPacket) udpPacket, clientAddress);
					break;
				case GROUP_LEAVE:
					handleGroupLeave((GroupLeavePacket) udpPacket, clientAddress);
					break;
				case GROUP_LIST:
					handleGroupList((GroupListPacket) udpPacket, clientAddress);
					break;
				case GROUP_DATA:
					handleGroupData((GroupDataPacket) udpPacket, clientAddress);
					break;
				case PING:
					handlePing((PingPacket) udpPacket, clientAddress);
					break;
				case PRESENCE_UPDATE:
					handlePresenceUpdate((PresenceUpdatePacket) udpPacket, clientAddress);
					break;
				default:
					System.out.println("Unknown packet type received from " + clientAddress);
			}
		} catch (Exception e) {
			System.err.println("Unexpected error handling packet from " + packet.getSocketAddress() + ": " + e.getMessage());
			e.printStackTrace();
		}
	}

	private void handleHandshake(VoiceHandshakePacket handshake, SocketAddress clientAddress) {
		Integer playerNetworkId = handshake.getPlayerNetworkId();
		String authToken = handshake.getAuthToken();

		boolean success = validateAuthToken(playerNetworkId, authToken);
		String message = success ? "Connection established" : "Invalid auth token";

		if (success) {
			connectedPlayers.put(clientAddress, playerNetworkId);
			playerAddressCache.put(playerNetworkId, clientAddress);
			playerVoiceSupported.put(playerNetworkId, true);
			playerDeafenState.put(playerNetworkId, false);

			lastKeepAlive.put(clientAddress, System.currentTimeMillis());
			lastSentKeepAlive.put(clientAddress, System.currentTimeMillis());
			updatePlayerCache();

			sendPresenceBulkToPlayer(clientAddress);
			broadcastPresenceUpdate(playerNetworkId, true, false);
		}

		sendPacket(new VoiceHandshakeAckPacket(success, message), clientAddress);
	}

	private void handleVoiceData(VoiceDataPacket voiceData, SocketAddress clientAddress) {
		Integer playerNetworkId = voiceData.getPlayerNetworkId();
		byte[] audioData = voiceData.getAudioData();
		long sequenceNumber = voiceData.getSequenceNumber();

		if (!connectedPlayers.containsValue(playerNetworkId)) return;
		relayAudioData(playerNetworkId, audioData, sequenceNumber);
	}

	private void handlePing(PingPacket pingPacket, SocketAddress clientAddress) {
		lastKeepAlive.put(clientAddress, System.currentTimeMillis());
		sendPacket(new PingPacket(), clientAddress);
	}

	private void handlePresenceUpdate(PresenceUpdatePacket packet, SocketAddress clientAddress) {
		Integer playerNetworkId = packet.getPlayerNetworkId();
		boolean voiceSupported = packet.isVoiceSupported();
		boolean deafened = packet.isDeafened();

		if (!connectedPlayers.containsValue(playerNetworkId)) return;

		playerVoiceSupported.put(playerNetworkId, voiceSupported);
		playerDeafenState.put(playerNetworkId, deafened);

		broadcastPresenceUpdate(playerNetworkId, voiceSupported, deafened);
	}

	private void sendPresenceBulkToPlayer(SocketAddress clientAddress) {
		try {
			Map<Integer, PresenceBulkPacket.PlayerState> playerStates = new HashMap<>();
			for (Map.Entry<Integer, Boolean> entry : playerVoiceSupported.entrySet()) {
				Integer playerId = entry.getKey();
				boolean voiceSupported = entry.getValue();
				boolean deafened = playerDeafenState.getOrDefault(playerId, false);
				playerStates.put(playerId, new PresenceBulkPacket.PlayerState(voiceSupported, deafened));
			}

			if (!playerStates.isEmpty()) {
				sendPacket(new PresenceBulkPacket(playerStates), clientAddress);
			}
		} catch (Exception e) {
			System.err.println("Failed to send presence bulk: " + e.getMessage());
		}
	}

	private void broadcastPresenceUpdate(int playerNetworkId, boolean voiceSupported, boolean deafened) {
		try {
			PresenceUpdatePacket packet = new PresenceUpdatePacket(playerNetworkId, voiceSupported, deafened);
			for (SocketAddress address : connectedPlayers.keySet()) {
				sendPacket(packet, address);
			}
		} catch (Exception e) {
			System.err.println("Failed to broadcast presence update: " + e.getMessage());
		}
	}

	private void relayAudioData(Integer fromPlayerId, byte[] audioData, long sequenceNumber) {
		String senderGroupName = playerToGroup.get(fromPlayerId);
		if (senderGroupName != null) {
			relayGroupAudio(fromPlayerId, audioData, sequenceNumber, senderGroupName);
			return;
		}
		relayProximityAudio(fromPlayerId, audioData, sequenceNumber);
	}

	private void relayGroupAudio(Integer fromPlayerId, byte[] audioData, long sequenceNumber, String groupName) {
		Group group = groups.get(groupName);
		if (group == null || !group.getMembers().contains(fromPlayerId)) return;

		for (Integer memberId : group.getMembers()) {
			if (!memberId.equals(fromPlayerId)) {
				SocketAddress memberAddress = playerAddressCache.get(memberId);
				if (memberAddress != null) {
					try {
						VoiceDataRelayPacket relayPacket = new VoiceDataRelayPacket(
							fromPlayerId, audioData, sequenceNumber, 0.0, 1.0f, 0.0, 0.0, 0.0);
						sendPacket(relayPacket, memberAddress);
					} catch (Exception e) {

					}
				}
			}
		}
	}

	private void relayProximityAudio(Integer fromPlayerId, byte[] audioData, long sequenceNumber) {
		updatePlayerCacheIfNeeded();

		VoicePlayer senderPlayer = playerCache.get(fromPlayerId);
		if (senderPlayer == null) return;

		double maxDistance = config.getAudio().getMaxDistance();
		double maxDistanceSquared = maxDistance * maxDistance * 1.25F;

		MinecraftCompat compat = Voice.getContext().mc;

		List<DatagramPacket> packetsToSend = new ArrayList<>();

		for (Map.Entry<SocketAddress, Integer> entry : connectedPlayers.entrySet()) {
			Integer recipientPlayerId = entry.getValue();
			if (recipientPlayerId.equals(fromPlayerId)) continue;
			if (playerToGroup.containsKey(recipientPlayerId)) continue;

			VoicePlayer recipientPlayer = playerCache.get(recipientPlayerId);
			if (recipientPlayer == null) continue;

			if (maxDistanceSquared > 0) {
				Object senderWorld = senderPlayer.getWorld();
				Object recipientWorld = recipientPlayer.getWorld();
				if (senderWorld == null || !senderWorld.equals(recipientWorld)) continue;

				try {
					double distanceSquared = compat.getDistanceSquared(senderPlayer, recipientPlayer);
					if (distanceSquared > maxDistanceSquared) continue;

					double distance = Math.sqrt(distanceSquared);
					float volume = calculateVolumeAttenuation(distance, maxDistance);

					VoiceDataRelayPacket relayPacket = new VoiceDataRelayPacket(
						fromPlayerId, audioData, sequenceNumber, distance, volume,
						senderPlayer.getX(), senderPlayer.getY(), senderPlayer.getZ());

					byte[] packetData = relayPacket.toBytes();
					packetsToSend.add(new DatagramPacket(packetData, packetData.length, entry.getKey()));
				} catch (Exception e) {

				}
			}
		}

		for (DatagramPacket packet : packetsToSend) {
			try {
				socket.send(packet);
			} catch (IOException e) {
				String errorMsg = e.getMessage();
				if (errorMsg != null && !errorMsg.contains("Connection reset") &&
					!errorMsg.contains("Connection refused") &&
					!errorMsg.contains("Network is unreachable") &&
					!errorMsg.contains("Socket closed") &&
					!errorMsg.contains("Broken pipe")) {
					System.err.println("Failed to relay audio: " + e.getMessage());
				}
			}
		}
	}

	private boolean validateAuthToken(Integer playerNetworkId, String authToken) {
		String validToken = Voice.getPlayerAuthToken(playerNetworkId);
		return validToken != null && validToken.equals(authToken);
	}

	private void sendPacket(UdpPacket packet, SocketAddress address) {
		try {
			byte[] data = packet.toBytes();
			socket.send(new DatagramPacket(data, data.length, address));
		} catch (IOException e) {
			String errorMsg = e.getMessage();
			if (errorMsg != null && (
				errorMsg.contains("Connection reset") ||
				errorMsg.contains("Connection refused") ||
				errorMsg.contains("Network is unreachable") ||
				errorMsg.contains("Socket closed") ||
				errorMsg.contains("Broken pipe")
			)) {

			} else {
				System.err.println("Error sending packet to " + address + ": " + e.getMessage());
			}
		}
	}

	public int getPort() {
		return port;
	}

	public String getProtocolVersion() {
		return PROTOCOL_VERSION;
	}

	public boolean isRunning() {
		return running;
	}

	public void disconnectPlayer(Integer playerNetworkId) {
		SocketAddress playerAddress = null;
		for (Map.Entry<SocketAddress, Integer> entry : connectedPlayers.entrySet()) {
			if (entry.getValue().equals(playerNetworkId)) {
				playerAddress = entry.getKey();
				break;
			}
		}

		if (playerAddress != null) {
			connectedPlayers.remove(playerAddress);
		}

		playerCache.remove(playerNetworkId);
		playerAddressCache.remove(playerNetworkId);
		playerVoiceSupported.remove(playerNetworkId);
		playerDeafenState.remove(playerNetworkId);

		broadcastPresenceUpdate(playerNetworkId, false, false);
		leaveGroup(playerNetworkId);
	}

	public Map<SocketAddress, Integer> getConnectedPlayers() {
		return new HashMap<>(connectedPlayers);
	}

	public VoiceServerConfig getConfig() {
		return config;
	}

	private void updatePlayerCacheIfNeeded() {
		long now = System.currentTimeMillis();
		if (now - lastCacheUpdate > CACHE_UPDATE_INTERVAL) {
			updatePlayerCache();
			lastCacheUpdate = now;
		}
	}

	private void updatePlayerCache() {
		playerCache.clear();

		MinecraftCompat compat = Voice.getContext().mc;
		if (compat == null) return;

		for (Map.Entry<SocketAddress, Integer> entry : connectedPlayers.entrySet()) {
			Integer playerId = entry.getValue();
			SocketAddress address = entry.getKey();
			playerAddressCache.put(playerId, address);

			VoicePlayer player = compat.getPlayerById(playerId);
			if (player != null) {
				playerCache.put(playerId, player);
			}
		}
	}

	private VoicePlayer getPlayerEntity(Integer networkId) {
		updatePlayerCacheIfNeeded();
		return playerCache.get(networkId);
	}

	private float calculateVolumeAttenuation(double distance, double maxDistance) {
		if (distance <= 0) return 1.0f;

		double fadeDistance = maxDistance / 8;
		if (distance <= fadeDistance) return 1.0f;

		float rolloffFactor = 0.95f;
		float attenuation = (float) (fadeDistance / (fadeDistance + rolloffFactor * (distance - fadeDistance)));
		return Math.max(0.0f, Math.min(1.0f, attenuation));
	}

	private void handleGroupCreate(GroupCreatePacket packet, SocketAddress clientAddress) {
		Integer playerId = connectedPlayers.get(clientAddress);
		if (playerId == null) return;

		String groupName = packet.getGroupName();
		String password = packet.getPassword();

		if (groupName == null || groupName.trim().isEmpty()) return;
		groupName = groupName.trim();

		if (groups.containsKey(groupName)) return;

		if (playerToGroup.containsKey(playerId)) {
			leaveGroup(playerId);
		}

		Group group = new Group(groupName, password, playerId);
		groups.put(groupName, group);
		playerToGroup.put(playerId, groupName);

		sendGroupMemberUpdate(GroupMemberUpdatePacket.UpdateType.GROUP_CREATED, groupName, playerId,
			getPlayerName(playerId), new ArrayList<>(group.getMembers()));
	}

	private void handleGroupJoin(GroupJoinPacket packet, SocketAddress clientAddress) {
		Integer playerId = connectedPlayers.get(clientAddress);
		if (playerId == null) return;

		String groupName = packet.getGroupName();
		String password = packet.getPassword();

		if (groupName == null || groupName.trim().isEmpty()) return;
		groupName = groupName.trim();

		Group group = groups.get(groupName);
		if (group == null) return;

		if (playerToGroup.containsKey(playerId)) {
			leaveGroup(playerId);
		}

		if (group.hasPassword() && !group.getPassword().equals(password)) return;

		group.addMember(playerId);
		playerToGroup.put(playerId, groupName);

		sendGroupMemberUpdate(GroupMemberUpdatePacket.UpdateType.MEMBER_JOINED, groupName, playerId,
			getPlayerName(playerId), new ArrayList<>(group.getMembers()));
	}

	private void handleGroupLeave(GroupLeavePacket packet, SocketAddress clientAddress) {
		Integer playerId = connectedPlayers.get(clientAddress);
		if (playerId == null) return;
		leaveGroup(playerId);
	}

	private void handleGroupList(GroupListPacket packet, SocketAddress clientAddress) {
		Integer playerId = connectedPlayers.get(clientAddress);
		if (playerId == null) return;

		List<GroupListPacket.GroupInfo> groupList = new ArrayList<>();
		for (Group group : groups.values()) {
			groupList.add(new GroupListPacket.GroupInfo(
				group.getName(),
				group.hasPassword(),
				group.getMembers().size()
			));
		}

		sendPacket(new GroupListPacket(groupList), clientAddress);
	}

	private void handleGroupData(GroupDataPacket packet, SocketAddress clientAddress) {
		Integer playerId = connectedPlayers.get(clientAddress);
		if (playerId == null) return;

		String groupName = packet.getGroupName();
		byte[] audioData = packet.getAudioData();
		long sequenceNumber = packet.getSequenceNumber();

		if (!playerToGroup.containsKey(playerId) || !playerToGroup.get(playerId).equals(groupName)) return;

		Group group = groups.get(groupName);
		if (group == null || !group.getMembers().contains(playerId)) return;

		for (Integer memberId : group.getMembers()) {
			if (!memberId.equals(playerId)) {
				SocketAddress memberAddress = playerAddressCache.get(memberId);
				if (memberAddress != null) {
					VoiceDataRelayPacket relayPacket = new VoiceDataRelayPacket(
						playerId, audioData, sequenceNumber, 0.0, 1.0f, 0.0, 0.0, 0.0);
					sendPacket(relayPacket, memberAddress);
				}
			}
		}
	}

	private void leaveGroup(Integer playerId) {
		String groupName = playerToGroup.get(playerId);
		if (groupName == null) return;

		Group group = groups.get(groupName);
		if (group == null) return;

		group.removeMember(playerId);
		playerToGroup.remove(playerId);

		sendGroupMemberUpdate(GroupMemberUpdatePacket.UpdateType.MEMBER_LEFT, groupName, playerId,
			getPlayerName(playerId), new ArrayList<>(group.getMembers()));

		if (group.getMembers().isEmpty()) {
			groups.remove(groupName);
			sendGroupMemberUpdate(GroupMemberUpdatePacket.UpdateType.GROUP_DELETED, groupName, null,
				null, Collections.emptyList());
		}
	}

	private void sendGroupMemberUpdate(GroupMemberUpdatePacket.UpdateType updateType, String groupName,
	                                   Integer playerId, String playerName, List<Integer> groupMembers) {
		for (SocketAddress address : connectedPlayers.keySet()) {
			List<String> memberNames = new ArrayList<>();
			for (Integer memberId : groupMembers) {
				memberNames.add(getPlayerName(memberId));
			}
			GroupMemberUpdatePacket updatePacket = new GroupMemberUpdatePacket(
				updateType, groupName, playerId, playerName, memberNames);
			sendPacket(updatePacket, address);
		}
	}

	private String getPlayerName(Integer playerId) {
		VoicePlayer player = getPlayerEntity(playerId);
		return player != null ? player.getName() : "Unknown";
	}

	private static class Group {
		private final String name;
		private final String password;
		private final Set<Integer> members;
		private final Integer creatorId;

		public Group(String name, String password, Integer creatorId) {
			this.name = name;
			this.password = password;
			this.creatorId = creatorId;
			this.members = new HashSet<>();
			this.members.add(creatorId);
		}

		public String getName() {
			return name;
		}

		public String getPassword() {
			return password;
		}

		public boolean hasPassword() {
			return password != null && !password.isEmpty();
		}

		public Set<Integer> getMembers() {
			return new HashSet<>(members);
		}

		public void addMember(Integer playerId) {
			members.add(playerId);
		}

		public void removeMember(Integer playerId) {
			members.remove(playerId);
		}
	}
}
