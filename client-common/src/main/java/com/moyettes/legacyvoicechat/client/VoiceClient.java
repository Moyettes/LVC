package com.moyettes.legacyvoicechat.client;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.moyettes.legacyvoicechat.audio.*;
import com.moyettes.legacyvoicechat.udp.UdpPacket;
import com.moyettes.legacyvoicechat.udp.VoiceHandshakePacket;
import com.moyettes.legacyvoicechat.udp.VoiceHandshakeAckPacket;
import com.moyettes.legacyvoicechat.udp.VoiceDataPacket;
import com.moyettes.legacyvoicechat.udp.VoiceDataRelayPacket;
import com.moyettes.legacyvoicechat.udp.GroupCreatePacket;
import com.moyettes.legacyvoicechat.udp.GroupJoinPacket;
import com.moyettes.legacyvoicechat.udp.GroupLeavePacket;
import com.moyettes.legacyvoicechat.udp.GroupListPacket;
import com.moyettes.legacyvoicechat.udp.GroupDataPacket;
import com.moyettes.legacyvoicechat.udp.GroupMemberUpdatePacket;
import com.moyettes.legacyvoicechat.udp.PingPacket;
import com.moyettes.legacyvoicechat.sound.Simple3DAudio;
import com.moyettes.legacyvoicechat.config.ClientConfig;
import com.moyettes.legacyvoicechat.audio.utils.AudioUtils;
import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.zip.CRC32;

public class VoiceClient {

	private static final Logger LOGGER = LogManager.getLogger(VoiceClient.class);

    private DatagramSocket socket;
    private ExecutorService executor;
    private boolean connected = false;
    private String serverHost;
    private int serverPort;
    private Integer playerNetworkId;
    private boolean micEnabled;
    private volatile boolean micMuted = false;
	private volatile boolean userDeafened = false;
    private volatile boolean pushToTalk = false;
    private volatile boolean pttKeyPressed = false;
    private volatile double silenceThreshold = 0.0;
    private volatile float microphoneGain = 1.0f;
    private volatile float masterVolume = 1.0f;
    private volatile String inputDeviceName = null;
    private volatile String outputDeviceName = null;
    private volatile boolean isTalking = false;
    private volatile long lastDeviceChangeTime = 0;
    private long lastTalkingTime = 0;

    private Consumer<VoiceHandshakeAckPacket> handshakeCallback;
    private Consumer<GroupMemberUpdatePacket> groupCallback;
    private Consumer<GroupListPacket> groupListCallback;

    private Consumer<List<String>> groupMemberCallback;
    private MicrophoneCapture microphoneCapture;
    private AudioProcessor audioProcessor;
    private StereoAudioPlayback stereoAudioPlayback;

    private enum PlaybackState { OPENAL, STEREO_FALLBACK, FAILED }

    private final Map<Integer, OpenALAudioPlayback> openALPlaybackBySender = new ConcurrentHashMap<>();
    private final Map<Integer, StereoAudioPlayback> stereoPlaybackBySender = new ConcurrentHashMap<>();
    private final Map<Integer, PlaybackState> playbackStateMap = new ConcurrentHashMap<>();
    private final Map<Integer, AudioProcessor> decoderBySender = new ConcurrentHashMap<>();
    private long sequenceNumber = 0;
    private long lastAudioReceived = 0;

    private final Map<Integer, Long> lastSeqBySender = new ConcurrentHashMap<>();
    private final Map<Integer, TreeMap<Long, BufferedPacket>> reorderBuffers = new ConcurrentHashMap<>();
    private final Map<Integer, Integer> warmupRemainingBySender = new ConcurrentHashMap<>();
    private final Map<Integer, Long> lastTalkingByRemote = new ConcurrentHashMap<>();
    private final Set<Integer> activatedPlayers = ConcurrentHashMap.newKeySet();
    private final Set<Integer> presenceSupported = Collections.newSetFromMap(new ConcurrentHashMap<Integer, Boolean>());
    private final Set<Integer> presenceDeafened = Collections.newSetFromMap(new ConcurrentHashMap<Integer, Boolean>());
    private static final int REORDER_BUFFER_THRESHOLD = 10;
    private static final int WARMUP_PACKETS = 3;

    private final Map<Integer, Thread> senderThreads = new ConcurrentHashMap<>();
    private final Map<Integer, BlockingQueue<AdaptiveJitterBuffer.BufferedPacket>> senderQueues = new ConcurrentHashMap<>();
    private final Map<Integer, Boolean> senderThreadRunning = new ConcurrentHashMap<>();
    private final Map<Integer, Set<Long>> enqueuedSeqBySender = new ConcurrentHashMap<>();
    private static final int SMALL_GAP_PLC_MAX = 4;
    private enum SourceType { DIRECT, RELAY, GROUP }
    private final Map<Integer, SourceType> activeSourceBySender = new ConcurrentHashMap<>();

    private boolean shouldProcessSource(Integer senderId, SourceType sourceType) {
        SourceType prev = activeSourceBySender.putIfAbsent(senderId, sourceType);
        return prev == null || prev == sourceType;
    }

    private void clearSourceState(Integer senderId) {
        senderThreadRunning.put(senderId, false);
        Thread thread = senderThreads.remove(senderId);
        if (thread != null) {
            thread.interrupt();
        }
        senderQueues.remove(senderId);
        enqueuedSeqBySender.remove(senderId);

        reorderBuffers.remove(senderId);
        activeSourceBySender.remove(senderId);
        OpenALAudioPlayback playback = openALPlaybackBySender.remove(senderId);
        if (playback != null) {
            playback.close();
        }

        StereoAudioPlayback stereoPlayback = stereoPlaybackBySender.remove(senderId);
        if (stereoPlayback != null) {
            stereoPlayback.close();
        }

        playbackStateMap.remove(senderId);

        AudioProcessor decoder = decoderBySender.remove(senderId);
        if (decoder != null) {
            decoder.close();
        }
    }

    private final Map<Integer, ArrayDeque<Integer>> recentOpusCrcBySender = new ConcurrentHashMap<>();
    private static final int OPUS_CRC_WINDOW = 8;

    private boolean isDuplicateOpusPacket(Integer senderId, byte[] opusData) {
        if (senderId == null || opusData == null || opusData.length == 0) return false;
        CRC32 crc = new CRC32();
        crc.update(opusData, 0, opusData.length);
        int hash = (int) crc.getValue();
        ArrayDeque<Integer> window = recentOpusCrcBySender.computeIfAbsent(senderId, k -> new ArrayDeque<>(OPUS_CRC_WINDOW));
        if (window.contains(hash)) {
            return true;
        }
        if (window.size() >= OPUS_CRC_WINDOW) {
            window.pollFirst();
        }
        window.offerLast(hash);
        return false;
    }

    private long lastKeepAlive = 0;
    private long lastSentKeepAlive = 0;
    private boolean handshakeComplete = false;

    private Simple3DAudio simple3D;

    private MicrophoneCapture testMicrophoneCapture;
    private volatile boolean isMicrophoneTesting = false;

    private final Map<String, Float> playerVolumes = new ConcurrentHashMap<>();

    private volatile String currentGroupName = null;
    private final Set<Integer> groupMembers = ConcurrentHashMap.newKeySet();

    private ClientConfig config;

    private final byte[] audioGainBuffer = new byte[AudioProcessor.getFrameSize() * 2];

    private final Set<Long> sentSequenceNumbers = Collections.newSetFromMap(new ConcurrentHashMap<Long, Boolean>());

    private static final boolean DEBUG_LOG = false;
    private void dbg(String msg) { if (DEBUG_LOG) LOGGER.info("[VoiceClient] " + msg); }

    public VoiceClient(Integer playerNetworkId, boolean micEnabled) {
        this.playerNetworkId = playerNetworkId;
        this.micEnabled = micEnabled;
        this.executor = Executors.newCachedThreadPool();

        this.config = ClientConfig.load();
        loadConfigSettings();

        simple3D = new Simple3DAudio();
        if (!simple3D.initialize()) {
            LOGGER.warn("Warning: Simple 3D audio not initialized. 3D audio may not work.");
        }

        this.audioProcessor = new AudioProcessor();

        if (audioProcessor.isInitialized()) {
            audioProcessor.setMicrophoneGain(this.microphoneGain);
        }

        boolean useSpecificDevice = (this.outputDeviceName != null && !this.outputDeviceName.equals("Default"));

        this.stereoAudioPlayback = new StereoAudioPlayback(this.outputDeviceName);
        if (!stereoAudioPlayback.initialize()) {
            LOGGER.warn("Warning: Stereo audio playback not initialized. Microphone testing may not work.");
        }

        if (useSpecificDevice) {
            LOGGER.info("Using StereoAudioPlayback for output device: " + this.outputDeviceName);
        } else {
            LOGGER.info("OpenAL will be used for audio playback (sources created per sender), StereoAudioPlayback for testing");
            dbg("Playback backend: OpenAL (per-sender sources)");
        }
    }

    private void loadConfigSettings() {
        if (config == null) return;

        this.microphoneGain = config.getMicrophoneGain();
        this.masterVolume = config.getMasterVolume();
        this.silenceThreshold = config.getActivationThreshold();
        this.pushToTalk = config.isPushToTalk();
        this.inputDeviceName = config.getInputDevice();
        this.outputDeviceName = config.getOutputDevice();

        if (config.getPlayerVolumes() != null) {
            playerVolumes.clear();
            playerVolumes.putAll(config.getPlayerVolumes());
        }
    }

    public void connect(String serverHost, int serverPort, String authToken) throws IOException {
        if (connected) {
            return;
        }

        if (socket != null && !socket.isClosed()) {
            socket.close();
        }

        this.serverHost = serverHost;
        this.serverPort = serverPort;

        socket = new DatagramSocket();

        try {
            socket.setReceiveBufferSize(131072);
            socket.setSendBufferSize(131072);
            socket.setReuseAddress(true);
            socket.setTrafficClass(0x04);
        } catch (Exception e) {
            LOGGER.warn("Failed to set socket optimizations: " + e.getMessage());
        }

        try {
            socket.connect(InetAddress.getByName(serverHost), serverPort);
        } catch (Exception ignored) { }

        VoiceHandshakePacket handshake = new VoiceHandshakePacket(playerNetworkId, authToken, micEnabled);
        sendPacket(handshake, serverHost, serverPort);

        connected = true;
        handshakeComplete = false;

        executor.submit(this::listenForPackets);
    }

    public void disconnect() {
        if (!connected) {
            return;
        }

        connected = false;

        stopMicrophoneCapture();

        for (Integer senderId : senderThreadRunning.keySet()) {
            senderThreadRunning.put(senderId, false);
        }
        for (Thread thread : senderThreads.values()) {
            try {
                thread.interrupt();
                thread.join(500);
            } catch (InterruptedException ignored) {}
        }
        senderThreads.clear();
        senderQueues.clear();
        senderThreadRunning.clear();

        for (com.moyettes.legacyvoicechat.audio.OpenALAudioPlayback playback : openALPlaybackBySender.values()) {
            if (playback != null) {
                playback.close();
            }
        }
        openALPlaybackBySender.clear();

        for (AudioProcessor decoder : decoderBySender.values()) {
            if (decoder != null) {
                decoder.close();
            }
        }
        decoderBySender.clear();

        if (stereoAudioPlayback != null) {
            stereoAudioPlayback.clearQueue();
            stereoAudioPlayback.flushAudioBuffer();
            stereoAudioPlayback.close();
        }

        if (audioProcessor != null) {
            audioProcessor.close();
        }

        if (simple3D != null) {
            simple3D.cleanup();
            simple3D = null;
        }

        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
        if (executor != null) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(2, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    private void listenForPackets() {
        byte[] buffer = new byte[2048];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

        while (connected && !socket.isClosed()) {
            try {
                socket.receive(packet);
                handlePacket(packet);

                checkForStaleAudio();
                checkKeepAlive();
            } catch (IOException e) {
                if (!socket.isClosed()) {
                    String errorMsg = e.getMessage();
                    if (errorMsg != null && !errorMsg.contains("Connection reset") &&
                        !errorMsg.contains("Connection refused") &&
                        !errorMsg.contains("Network is unreachable") &&
                        !errorMsg.contains("Receive timed out")) {
                        LOGGER.warn("Error receiving packet: " + errorMsg);
                    }
                }
            }
        }
    }

    private void handlePacket(DatagramPacket packet) {
        try {
            byte[] data = new byte[packet.getLength()];
            System.arraycopy(packet.getData(), packet.getOffset(), data, 0, packet.getLength());

            UdpPacket udpPacket = UdpPacket.fromBytes(data);

            switch (udpPacket.getType()) {
                case VOICE_HANDSHAKE_ACK:
                    handleHandshakeAck((VoiceHandshakeAckPacket) udpPacket);
                    break;
                case VOICE_DATA:
                    handleVoiceData((VoiceDataPacket) udpPacket);
                    break;
                case VOICE_DATA_RELAY:
                    handleVoiceDataRelay((VoiceDataRelayPacket) udpPacket);
                    break;
                case GROUP_DATA:
                    handleGroupData((GroupDataPacket) udpPacket);
                    break;
                case GROUP_MEMBER_UPDATE:
                    handleGroupMemberUpdate((GroupMemberUpdatePacket) udpPacket);
                    break;
                case GROUP_LIST:
                    handleGroupList((GroupListPacket) udpPacket);
                    break;
                case PING:
                    handlePing();
                    break;
                case PRESENCE_UPDATE:
                    handlePresenceUpdate((com.moyettes.legacyvoicechat.udp.PresenceUpdatePacket) udpPacket);
                    break;
                case PRESENCE_BULK:
                    handlePresenceBulk((com.moyettes.legacyvoicechat.udp.PresenceBulkPacket) udpPacket);
                    break;
                default:
                    LOGGER.info("Received unhandled packet type: " + udpPacket.getType());
                    break;
            }

        } catch (IOException e) {
            if (connected) {
                String errorMsg = e.getMessage();
                if (errorMsg != null && !errorMsg.contains("Connection reset") &&
                    !errorMsg.contains("Connection refused") &&
                    !errorMsg.contains("Network is unreachable")) {
                    LOGGER.warn("Error handling packet: " + errorMsg);
                }
            }
        }
    }

    private void handleHandshakeAck(VoiceHandshakeAckPacket ack) {
        if (ack.isSuccess()) {
            handshakeComplete = true;

            lastKeepAlive = System.currentTimeMillis();
            lastSentKeepAlive = System.currentTimeMillis();

            if (micEnabled) {
                startMicrophoneCapture();
            }

            sendPresenceUpdate();
        } else {
            LOGGER.warn("Failed to connect to voice server: " + ack.getMessage());
            disconnect();
        }

        if (handshakeCallback != null) {
            handshakeCallback.accept(ack);
        }
    }

    private void handleVoiceData(VoiceDataPacket voiceData) {
        if (voiceData.getPlayerNetworkId() != null && !shouldProcessSource(voiceData.getPlayerNetworkId(), SourceType.DIRECT)) {
            dbg("Drop DIRECT from sender=" + voiceData.getPlayerNetworkId() + " due to activeSource=" + activeSourceBySender.get(voiceData.getPlayerNetworkId()));
            return;
        }
        if (voiceData.getAudioData().length == 0) {
            if (voiceData.getPlayerNetworkId() != null) {
                lastSeqBySender.remove(voiceData.getPlayerNetworkId());
                activatedPlayers.remove(voiceData.getPlayerNetworkId());
                clearSourceState(voiceData.getPlayerNetworkId());
                dbg("DIRECT end: cleared state for sender=" + voiceData.getPlayerNetworkId());
            }
            return;
        }

        if (voiceData.getPlayerNetworkId() != null && isDuplicateOpusPacket(voiceData.getPlayerNetworkId(), voiceData.getAudioData())) {
            dbg("CRC dedupe: drop DIRECT seq=" + voiceData.getSequenceNumber() + " sender=" + voiceData.getPlayerNetworkId());
            return;
        }
        processIncomingAudio(voiceData.getAudioData(), voiceData.getPlayerNetworkId(), voiceData.getSequenceNumber());
    }

    private void handleVoiceDataRelay(VoiceDataRelayPacket voiceDataRelay) {
        if (voiceDataRelay.getFromPlayerNetworkId() != null && !shouldProcessSource(voiceDataRelay.getFromPlayerNetworkId(), SourceType.RELAY)) {
            dbg("Drop RELAY from sender=" + voiceDataRelay.getFromPlayerNetworkId() + " due to activeSource=" + activeSourceBySender.get(voiceDataRelay.getFromPlayerNetworkId()));
            return;
        }
        if (voiceDataRelay.getAudioData().length == 0) {
            if (voiceDataRelay.getFromPlayerNetworkId() != null) {
                lastSeqBySender.remove(voiceDataRelay.getFromPlayerNetworkId());
                activatedPlayers.remove(voiceDataRelay.getFromPlayerNetworkId());
                clearSourceState(voiceDataRelay.getFromPlayerNetworkId());
                dbg("RELAY end: cleared state for sender=" + voiceDataRelay.getFromPlayerNetworkId());
            }
            return;
        }

        if (voiceDataRelay.getFromPlayerNetworkId() != null && isDuplicateOpusPacket(voiceDataRelay.getFromPlayerNetworkId(), voiceDataRelay.getAudioData())) {
            dbg("CRC dedupe: drop RELAY seq=" + voiceDataRelay.getSequenceNumber() + " sender=" + voiceDataRelay.getFromPlayerNetworkId());
            return;
        }
        processIncomingAudio(voiceDataRelay.getAudioData(), voiceDataRelay.getFromPlayerNetworkId(),
                           voiceDataRelay.getSequenceNumber(), voiceDataRelay.getDistance(), voiceDataRelay.getVolume(),
                           voiceDataRelay.getPlayerX(), voiceDataRelay.getPlayerY(), voiceDataRelay.getPlayerZ());
    }

    private void handleGroupData(GroupDataPacket groupData) {
        if (groupData.getFromPlayerNetworkId() != null && !shouldProcessSource(groupData.getFromPlayerNetworkId(), SourceType.GROUP)) {
            dbg("Drop GROUP from sender=" + groupData.getFromPlayerNetworkId() + " due to activeSource=" + activeSourceBySender.get(groupData.getFromPlayerNetworkId()));
            return;
        }
        if (groupData.getAudioData().length == 0) {
            if (groupData.getFromPlayerNetworkId() != null) {
                activatedPlayers.remove(groupData.getFromPlayerNetworkId());
                clearSourceState(groupData.getFromPlayerNetworkId());
                dbg("GROUP end: cleared state for sender=" + groupData.getFromPlayerNetworkId());
            }
            return;
        }

        if (isInGroup() && currentGroupName.equals(groupData.getGroupName())) {
            if (groupData.getFromPlayerNetworkId() != null && isDuplicateOpusPacket(groupData.getFromPlayerNetworkId(), groupData.getAudioData())) {
                dbg("CRC dedupe: drop GROUP seq=" + groupData.getSequenceNumber() + " sender=" + groupData.getFromPlayerNetworkId());
                return;
            }
            processIncomingAudio(groupData.getAudioData(), groupData.getFromPlayerNetworkId(),
                               groupData.getSequenceNumber(), 0.0, 1.0f, 0.0, 0.0, 0.0);
        }
    }

    private void handleGroupMemberUpdate(GroupMemberUpdatePacket memberUpdate) {
        switch (memberUpdate.getUpdateType()) {
            case GROUP_CREATED:
                if (memberUpdate.getPlayerNetworkId() != null && memberUpdate.getPlayerNetworkId().equals(playerNetworkId)) {
                    currentGroupName = memberUpdate.getGroupName();
                    groupMembers.clear();
                    groupMembers.add(playerNetworkId);
                }
                break;
            case MEMBER_JOINED:
                if (memberUpdate.getPlayerNetworkId() != null && memberUpdate.getPlayerNetworkId().equals(playerNetworkId)) {
                    currentGroupName = memberUpdate.getGroupName();
                    groupMembers.clear();

                    for (String memberName : memberUpdate.getGroupMembers()) {
                        Integer memberId = getPlayerIdFromName(memberName);
                        if (memberId != null) {
                            groupMembers.add(memberId);
                        }
                    }
                } else if (isInGroup() && currentGroupName.equals(memberUpdate.getGroupName()) && memberUpdate.getPlayerNetworkId() != null) {
                    groupMembers.add(memberUpdate.getPlayerNetworkId());
                }
                break;
            case MEMBER_LEFT:
                if (memberUpdate.getPlayerNetworkId() != null && memberUpdate.getPlayerNetworkId().equals(playerNetworkId)) {
                    currentGroupName = null;
                    groupMembers.clear();
                } else if (isInGroup() && currentGroupName.equals(memberUpdate.getGroupName()) && memberUpdate.getPlayerNetworkId() != null) {
                    groupMembers.remove(memberUpdate.getPlayerNetworkId());
                }
                break;
            case GROUP_DELETED:
                if (isInGroup() && currentGroupName.equals(memberUpdate.getGroupName())) {
                    currentGroupName = null;
                    groupMembers.clear();
                }
                break;
        }

        if (groupCallback != null) {
            groupCallback.accept(memberUpdate);
        }

        if (groupMemberCallback != null && isInGroup()) {
            List<String> memberNames = new ArrayList<>();
            for (Integer memberId : groupMembers) {
                String memberName = getPlayerNameFromId(memberId);
                if (memberName != null) {
                    memberNames.add(memberName);
                }
            }
            groupMemberCallback.accept(memberNames);
        }
    }

    private void handleGroupList(GroupListPacket groupList) {
        for (GroupListPacket.GroupInfo group : groupList.getGroups()) {
            LOGGER.info("  - " + group.getName() + " (" + group.getMemberCount() + " members)" +
                             (group.hasPassword() ? " [Private]" : " [Public]"));
        }

        if (groupListCallback != null) {
            groupListCallback.accept(groupList);
        }
    }

    private void handlePresenceUpdate(com.moyettes.legacyvoicechat.udp.PresenceUpdatePacket packet) {
        updatePresence(packet.getPlayerNetworkId(), packet.isVoiceSupported(), packet.isDeafened());
    }

    private void handlePresenceBulk(com.moyettes.legacyvoicechat.udp.PresenceBulkPacket packet) {
        for (java.util.Map.Entry<Integer, com.moyettes.legacyvoicechat.udp.PresenceBulkPacket.PlayerState> entry : packet.getPlayerStates().entrySet()) {
            updatePresence(entry.getKey(), entry.getValue().voiceSupported, entry.getValue().deafened);
        }
    }

    private void handlePing() throws IOException {
        lastKeepAlive = System.currentTimeMillis();

		sendPacket(new PingPacket(), serverHost, serverPort);
    }

    private void processIncomingAudio(byte[] audioData, Integer fromPlayerId, long sequenceNumber) {
        processIncomingAudio(audioData, fromPlayerId, sequenceNumber, 0.0, 1.0f, 0.0, 0.0, 0.0);
    }


    private void processIncomingAudio(byte[] audioData, Integer fromPlayerId, long sequenceNumber, double distance, float volume,
                                    double playerX, double playerY, double playerZ) {
        lastAudioReceived = System.currentTimeMillis();
        lastKeepAlive = System.currentTimeMillis();

        if (userDeafened) {
            return;
        }

        if (fromPlayerId.equals(playerNetworkId)) {
            return;
        }

        Long lastSeqForSender = lastSeqBySender.get(fromPlayerId);
        if (lastSeqForSender == null) {
            TreeMap<Long, BufferedPacket> buffer = reorderBuffers.computeIfAbsent(fromPlayerId, k -> new TreeMap<>());
            buffer.put(sequenceNumber, new BufferedPacket(audioData, sequenceNumber, distance, volume, playerX, playerY, playerZ));
            int remaining = warmupRemainingBySender.getOrDefault(fromPlayerId, WARMUP_PACKETS);
            if (remaining > 1) {
                warmupRemainingBySender.put(fromPlayerId, remaining - 1);
                return;
            }
            warmupRemainingBySender.remove(fromPlayerId);
            Long first = buffer.firstKey();
            if (first != null) {
                lastSeqForSender = first - 1;
                lastSeqBySender.put(fromPlayerId, lastSeqForSender);
            }
            buffer.remove(sequenceNumber);
        }
        if (lastSeqForSender != null) {
            if (sequenceNumber <= lastSeqForSender) {
                return;
            }
            long expectedNext = lastSeqForSender + 1;
            if (sequenceNumber > expectedNext) {
                long missing = sequenceNumber - expectedNext;
                if (missing <= REORDER_BUFFER_THRESHOLD) {
                    TreeMap<Long, BufferedPacket> buffer = reorderBuffers.computeIfAbsent(fromPlayerId, k -> new TreeMap<>());
                    buffer.put(sequenceNumber, new BufferedPacket(audioData, sequenceNumber, distance, volume, playerX, playerY, playerZ));
                    return;
                }
            }
        }

        String playerName = getPlayerNameFromId(fromPlayerId);

        AudioProcessor senderDecoder = decoderBySender.computeIfAbsent(fromPlayerId, k -> {
            AudioProcessor decoder = new AudioProcessor();
            if (decoder.isInitialized()) {
                return decoder;
            } else {
                decoder.close();
                return null;
            }
        });

        if (senderDecoder != null && senderDecoder.isInitialized()) {
            Long lastSeqSeen = lastSeqBySender.get(fromPlayerId);
            if (lastSeqSeen != null && sequenceNumber > lastSeqSeen + 1) {
                long expectedNext = lastSeqSeen + 1;
                long gap = sequenceNumber - expectedNext;
                if (gap > 0) {
                    if (gap <= SMALL_GAP_PLC_MAX) {
                        Set<Long> enqueuedSeqsPlc = enqueuedSeqBySender.computeIfAbsent(fromPlayerId, k -> ConcurrentHashMap.newKeySet());
                        for (long missingSeq = expectedNext; missingSeq < sequenceNumber; missingSeq++) {
                            if (!enqueuedSeqsPlc.contains(missingSeq)) {
                                byte[] plc = senderDecoder.decodeSilence();
                                if (plc != null && plc.length > 0) {
                                    AdaptiveJitterBuffer.BufferedPacket plcPacket =
                                        new AdaptiveJitterBuffer.BufferedPacket(
                                            plc, missingSeq, distance, 1.0f, playerX, playerY, playerZ,
                                            System.currentTimeMillis(), 0);
                                    BlockingQueue<AdaptiveJitterBuffer.BufferedPacket> queue =
                                        senderQueues.get(fromPlayerId);
                                    if (queue != null) {
                                        queue.offer(plcPacket);
                                        enqueuedSeqsPlc.add(missingSeq);
                                    }
                                }
                            }
                        }
                    } else {
                        for (long missingSeq = expectedNext; missingSeq < sequenceNumber; missingSeq++) {
                            senderDecoder.decodeSilence();
                        }
                    }
                }
            }

            byte[] decodedAudio = senderDecoder.decodeAudio(audioData);
            if (decodedAudio != null && decodedAudio.length > 0) {
                lastTalkingByRemote.put(fromPlayerId, System.currentTimeMillis());

                float playerVolume = playerName != null ? getPlayerVolume(playerName) : 1.0f;
                float finalVolume = volume * masterVolume * playerVolume;

                Set<Long> enqueuedSeqs = enqueuedSeqBySender.computeIfAbsent(fromPlayerId, k -> ConcurrentHashMap.newKeySet());
                if (enqueuedSeqs.contains(sequenceNumber)) {
                    return;
                }

                AdaptiveJitterBuffer.BufferedPacket packet =
                    new AdaptiveJitterBuffer.BufferedPacket(
                        decodedAudio, sequenceNumber, distance, finalVolume, playerX, playerY, playerZ,
                        System.currentTimeMillis(), 0);

                ensureSenderThread(fromPlayerId);

                BlockingQueue<AdaptiveJitterBuffer.BufferedPacket> queue =
                    senderQueues.get(fromPlayerId);
                if (queue != null) {
                    queue.offer(packet);
                    enqueuedSeqs.add(sequenceNumber);
                    if (enqueuedSeqs.size() > 128) {
                        enqueuedSeqs.clear();
                        enqueuedSeqs.add(sequenceNumber);
                    }
                }

                lastSeqBySender.put(fromPlayerId, sequenceNumber);

                TreeMap<Long, BufferedPacket> buffer = reorderBuffers.get(fromPlayerId);
                if (buffer != null) {
                    while (true) {
                        Long nextKey = buffer.higherKey(lastSeqBySender.get(fromPlayerId) - 1);
                        long expectedNext = lastSeqBySender.get(fromPlayerId) + 1;
                        if (nextKey != null && nextKey == expectedNext) {
                            BufferedPacket next = buffer.remove(nextKey);
                            if (next == null) break;
                            byte[] nextDecoded = senderDecoder.decodeAudio(next.audioData);
                            if (nextDecoded != null && nextDecoded.length > 0) {
                                Set<Long> enqueuedSeqs2 = enqueuedSeqBySender.computeIfAbsent(fromPlayerId, k -> ConcurrentHashMap.newKeySet());
                                if (!enqueuedSeqs2.contains(next.sequenceNumber)) {
                                    AdaptiveJitterBuffer.BufferedPacket bufferedPacket =
                                        new AdaptiveJitterBuffer.BufferedPacket(
                                            nextDecoded, next.sequenceNumber, next.distance, next.volume,
                                            next.playerX, next.playerY, next.playerZ, System.currentTimeMillis(), 0);
                                    ensureSenderThread(fromPlayerId);
                                    BlockingQueue<AdaptiveJitterBuffer.BufferedPacket> queue2 =
                                        senderQueues.get(fromPlayerId);
                                    if (queue2 != null) {
                                        queue2.offer(bufferedPacket);
                                        enqueuedSeqs2.add(next.sequenceNumber);
                                    }
                                    dbg("Offered buffered next sender=" + fromPlayerId + " seq=" + next.sequenceNumber);
                                }
                                lastSeqBySender.put(fromPlayerId, next.sequenceNumber);
                            }
                        } else {
                            break;
                        }
                    }
                    if (buffer.isEmpty()) {
                        reorderBuffers.remove(fromPlayerId);
                    }
                }
            }
        } else {
            LOGGER.warn("Audio processor not available for decoding audio from player " + fromPlayerId);
        }
    }

    // Network-id-based so client-common has no compile-time dependency on any
    // specific MC PlayerEntity path. Callers pass player.networkId directly
    // (or whatever the equivalent is on their mapping).
    public boolean isPlayerTalking(Integer id) {
        if (id == null) return false;
        Long last = lastTalkingByRemote.get(id);
        if (last == null) return false;
        long now = System.currentTimeMillis();
        long delta = now - last;
        if (delta < 500) {
            return true;
        }
        lastTalkingByRemote.remove(id);
        activatedPlayers.remove(id);
        return false;
    }

    public void updatePresence(int playerNetworkId, boolean supported, boolean deafened) {
        if (supported) {
            presenceSupported.add(playerNetworkId);
        } else {
            presenceSupported.remove(playerNetworkId);
        }

        if (deafened) {
            presenceDeafened.add(playerNetworkId);
        } else {
            presenceDeafened.remove(playerNetworkId);
        }
    }

    public boolean isPlayerVoiceSupported(Integer id) {
        if (id == null) return false;
        return presenceSupported.contains(id);
    }

    public boolean isPlayerDeafened(Integer id) {
        if (id == null) return false;
        return presenceDeafened.contains(id);
    }


    private void queueToOutput(Integer senderId, byte[] monoDecoded, double playerX, double playerY, double playerZ, final float finalVolume) {
        if (monoDecoded == null || monoDecoded.length == 0) {
            LOGGER.warn("Attempted to queue null or empty audio");
            return;
        }

        if (monoDecoded.length % 2 != 0) {
            LOGGER.warn("Invalid mono audio size: " + monoDecoded.length + " bytes (must be even)");
            return;
        }

        PlaybackState state = playbackStateMap.get(senderId);

        if (state == PlaybackState.OPENAL) {
            OpenALAudioPlayback senderPlayback = openALPlaybackBySender.get(senderId);
            if (senderPlayback != null && senderPlayback.isInitialized()) {
                byte[] stereoAudio = calculateStereoPositioning(monoDecoded, playerX, playerY, playerZ, finalVolume);
                if (stereoAudio != null && stereoAudio.length > 0) {
                    dbg("Write to OpenAL sender=" + senderId + " len=" + stereoAudio.length);
                    senderPlayback.writeAudioDirect(stereoAudio);
                }
            }
        } else if (state == PlaybackState.STEREO_FALLBACK) {
            StereoAudioPlayback fallbackPlayback = stereoPlaybackBySender.get(senderId);
            if (fallbackPlayback != null && fallbackPlayback.isInitialized()) {
                byte[] positionedAudio = calculateStereoPositioning(monoDecoded, playerX, playerY, playerZ, finalVolume);
                if (positionedAudio != null && positionedAudio.length > 0) {
                    dbg("Enqueue to fallback StereoAudioPlayback sender=" + senderId + " len=" + positionedAudio.length);
                    fallbackPlayback.queueAudio(positionedAudio);
                }
            } else if (stereoAudioPlayback != null && stereoAudioPlayback.isInitialized()) {
                byte[] positionedAudio = calculateStereoPositioning(monoDecoded, playerX, playerY, playerZ, finalVolume);
                if (positionedAudio != null && positionedAudio.length > 0) {
                    dbg("Enqueue to global StereoAudioPlayback len=" + positionedAudio.length);
                    stereoAudioPlayback.queueAudio(positionedAudio);
                }
            }
        }
    }

    private static class BufferedPacket {
        final byte[] audioData;
        final long sequenceNumber;
        final double distance;
        final float volume;
        final double playerX;
        final double playerY;
        final double playerZ;

        BufferedPacket(byte[] audioData, long sequenceNumber, double distance, float volume, double playerX, double playerY, double playerZ) {
            this.audioData = audioData;
            this.sequenceNumber = sequenceNumber;
            this.distance = distance;
            this.volume = volume;
            this.playerX = playerX;
            this.playerY = playerY;
            this.playerZ = playerZ;
        }
    }

    private void ensureSenderThread(Integer senderId) {
        if (senderThreads.containsKey(senderId)) {
            return;
        }

        BlockingQueue<AdaptiveJitterBuffer.BufferedPacket> queue =
            new LinkedBlockingQueue<>();
        senderQueues.put(senderId, queue);
        senderThreadRunning.put(senderId, true);

        Thread thread = new Thread(() -> {
            LOGGER.info("[VoiceClient] Started playback thread for sender=" + senderId);

            boolean useSpecificDevice = (outputDeviceName != null && !outputDeviceName.equals("Default"));

            if (!useSpecificDevice) {
                OpenALAudioPlayback playback = new OpenALAudioPlayback();
                if (playback.initialize()) {
                    openALPlaybackBySender.put(senderId, playback);
                    playbackStateMap.put(senderId, PlaybackState.OPENAL);
                    LOGGER.info("[VoiceClient] OpenAL source created for sender=" + senderId);
                } else {
                    playback.close();
                    LOGGER.warn("[VoiceClient] OpenAL initialization failed for sender=" + senderId + ", falling back to StereoAudioPlayback");

                    StereoAudioPlayback stereoPlayback = new StereoAudioPlayback(outputDeviceName);
                    if (stereoPlayback.initialize()) {
                        stereoPlaybackBySender.put(senderId, stereoPlayback);
                        playbackStateMap.put(senderId, PlaybackState.STEREO_FALLBACK);
                        LOGGER.info("[VoiceClient] StereoAudioPlayback fallback initialized for sender=" + senderId);
                    } else {
                        stereoPlayback.close();
                        playbackStateMap.put(senderId, PlaybackState.FAILED);
                        LOGGER.error("[VoiceClient] Both OpenAL and StereoAudioPlayback failed for sender=" + senderId);
                        return;
                    }
                }
            } else {
                playbackStateMap.put(senderId, PlaybackState.STEREO_FALLBACK);
                LOGGER.info("[VoiceClient] Using global StereoAudioPlayback for sender=" + senderId);
            }

            try {
                while (senderThreadRunning.getOrDefault(senderId, false) && connected) {
                    AdaptiveJitterBuffer.BufferedPacket pkt = queue.poll(10, TimeUnit.MILLISECONDS);
                    if (pkt == null) {
                        continue;
                    }

                    queueToOutput(senderId, pkt.audioData, pkt.playerX, pkt.playerY, pkt.playerZ, (float) pkt.volume);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                LOGGER.warn("[VoiceClient] Error in sender thread " + senderId + ": " + e.getMessage());
            } finally {
                LOGGER.info("[VoiceClient] Stopped playback thread for sender=" + senderId);
            }
        }, "Voice-Sender-" + senderId);

        thread.setDaemon(true);
        thread.start();
        senderThreads.put(senderId, thread);
    }


    private void sendPacket(UdpPacket packet, String host, int port) throws IOException {
        byte[] data = packet.toBytes();
        InetAddress address = InetAddress.getByName(host);
        DatagramPacket udpPacket = new DatagramPacket(data, data.length, address, port);
        socket.send(udpPacket);
    }

    public void setHandshakeCallback(Consumer<VoiceHandshakeAckPacket> callback) {
        this.handshakeCallback = callback;
    }

    public void setGroupCallback(Consumer<GroupMemberUpdatePacket> callback) {
        this.groupCallback = callback;
    }

    public void setGroupListCallback(Consumer<GroupListPacket> callback) {
        this.groupListCallback = callback;
    }

    public void setGroupMemberCallback(Consumer<List<String>> callback) {
        this.groupMemberCallback = callback;
    }

    public boolean isConnected() {
        return connected;
    }

    public Integer getPlayerNetworkId() {
        return playerNetworkId;
    }

    public boolean isMicEnabled() {
        return micEnabled;
    }

    private void startMicrophoneCapture() {
        if (microphoneCapture != null) {
            return;
        }

        microphoneCapture = new MicrophoneCapture(this::onAudioData, this.inputDeviceName);
        if (!microphoneCapture.startCapture()) {
            LOGGER.warn("Failed to start microphone capture");
            microphoneCapture = null;
        } else {
            LOGGER.info("Microphone capture started");
        }
    }

    private void stopMicrophoneCapture() {
        if (microphoneCapture != null) {
            microphoneCapture.stopCapture();
            microphoneCapture.close();
            microphoneCapture = null;
            LOGGER.info("Microphone capture stopped");
        }

        if (audioProcessor != null) {
            audioProcessor.resetEncoder();
        }
    }

    public void toggleMute() {
        if (userDeafened && micMuted) {
            toggleDeafened();
            return;
        }

        micMuted = !micMuted;
    }

    public boolean isMicMuted() {
        return micMuted;
    }

	public void toggleDeafened() {
		userDeafened = !userDeafened;
		micMuted = userDeafened;
		sendPresenceUpdate();
	}

	public boolean isDeafened() {
		return userDeafened;
	}

	private void sendPresenceUpdate() {
		if (!connected || playerNetworkId == null || serverHost == null) {
			return;
		}

		try {
			com.moyettes.legacyvoicechat.udp.PresenceUpdatePacket packet =
				new com.moyettes.legacyvoicechat.udp.PresenceUpdatePacket(playerNetworkId, true, userDeafened);
			sendPacket(packet, serverHost, serverPort);
		} catch (Exception e) {
			LOGGER.warn("Failed to send presence update: " + e.getMessage());
		}
	}


    public boolean isPushToTalk() {
        return pushToTalk;
    }

    public void setPushToTalk(boolean pushToTalk) {
        this.pushToTalk = pushToTalk;
        this.pttKeyPressed = false;
        if (config != null) {
            config.setPushToTalk(pushToTalk);
            config.save();
        }
    }

    public void setPttKeyPressed(boolean pressed) {
        this.pttKeyPressed = pressed;
    }

    public boolean isPttKeyPressed() {
        return pttKeyPressed;
    }

    public double getSilenceThreshold() {
        return silenceThreshold;
    }

    public void setSilenceThreshold(double silenceThreshold) {
        if (silenceThreshold < -127.0) silenceThreshold = -127.0;
        if (silenceThreshold > 0.0) silenceThreshold = 0.0;
        this.silenceThreshold = silenceThreshold;
        if (config != null) {
            config.setActivationThreshold(silenceThreshold);
            config.save();
        }
    }

    public float getMicrophoneGain() {
        return microphoneGain;
    }

    public void setMicrophoneGain(float microphoneGain) {
        if (microphoneGain < 0.0f) microphoneGain = 0.0f;
        if (microphoneGain > 2.0f) microphoneGain = 2.0f;
        this.microphoneGain = microphoneGain;

        if (audioProcessor != null) {
            audioProcessor.setMicrophoneGain(microphoneGain);
        }

        if (config != null) {
            config.setMicrophoneGain(microphoneGain);
            config.save();
        }
    }

    public float getMasterVolume() {
        return masterVolume;
    }

    public void setMasterVolume(float masterVolume) {
        if (masterVolume < 0.0f) masterVolume = 0.0f;
        if (masterVolume > 2.0f) masterVolume = 2.0f;
        this.masterVolume = masterVolume;
        if (config != null) {
            config.setMasterVolume(masterVolume);
            config.save();
        }
    }

    public float getPlayerVolume(String playerName) {
        return playerVolumes.getOrDefault(playerName, 1.0f);
    }

    public void setPlayerVolume(String playerName, float volume) {
        if (volume < 0.0f) volume = 0.0f;
        if (volume > 2.0f) volume = 2.0f;
        if (volume == 1.0f) {
            playerVolumes.remove(playerName);
        } else {
            playerVolumes.put(playerName, volume);
        }
        if (config != null) {
            config.setPlayerVolume(playerName, volume);
            config.save();
        }
    }

    public String getCurrentGroupName() {
        return currentGroupName;
    }

    public boolean isInGroup() {
        return currentGroupName != null && !currentGroupName.isEmpty();
    }

    public Set<Integer> getGroupMembers() {
        return new HashSet<>(groupMembers);
    }

    public void createGroup(String groupName, String password) {
        if (!connected) {
            LOGGER.warn("Cannot create group: not connected to voice server");
            return;
        }

        try {
            GroupCreatePacket packet = new GroupCreatePacket(groupName, password);
            sendPacket(packet, serverHost, serverPort);
            LOGGER.info("Sent group creation request for: " + groupName);
        } catch (IOException e) {
            LOGGER.warn("Failed to send group creation packet: " + e.getMessage());
        }
    }

    public void joinGroup(String groupName, String password) {
        if (!connected) {
            LOGGER.warn("Cannot join group: not connected to voice server");
            return;
        }

        try {
            GroupJoinPacket packet = new GroupJoinPacket(groupName, password);
            sendPacket(packet, serverHost, serverPort);
            LOGGER.info("Sent group join request for: " + groupName);
        } catch (IOException e) {
            LOGGER.warn("Failed to send group join packet: " + e.getMessage());
        }
    }

    public void leaveGroup() {
        if (!connected) {
            LOGGER.warn("Cannot leave group: not connected to voice server");
            return;
        }

        if (!isInGroup()) {
            LOGGER.warn("Cannot leave group: not currently in a group");
            return;
        }

        try {
            GroupLeavePacket packet = new GroupLeavePacket(currentGroupName);
            sendPacket(packet, serverHost, serverPort);
            LOGGER.info("Sent group leave request for: " + currentGroupName);
        } catch (IOException e) {
            LOGGER.warn("Failed to send group leave packet: " + e.getMessage());
        }
    }

    public void requestGroupList() {
        if (!connected) {
            LOGGER.warn("Cannot request group list: not connected to voice server");
            return;
        }

        try {
            GroupListPacket packet = new GroupListPacket();
            sendPacket(packet, serverHost, serverPort);
        } catch (IOException e) {
            LOGGER.warn("Failed to send group list request: " + e.getMessage());
        }
    }

    public String getInputDeviceName() {
        return inputDeviceName;
    }

    public void setInputDeviceName(String deviceName) {
        this.inputDeviceName = deviceName;
        if (config != null) {
            config.setInputDevice(deviceName);
            config.save();
        }

        boolean wasTestingMic = isMicrophoneTesting;
        if (wasTestingMic) {
            stopMicrophoneTesting();
        }

        if (connected) {
            stopMicrophoneCapture();
            microphoneCapture = new MicrophoneCapture(this::onAudioData, this.inputDeviceName);
            if (!microphoneCapture.startCapture()) {
                LOGGER.warn("Failed to start microphone capture for device: " + this.inputDeviceName);
                microphoneCapture = null;
            }
        }

        if (wasTestingMic) {
            startMicrophoneTesting();
        }
    }

    public String getOutputDeviceName() {
        return outputDeviceName;
    }

    public void setOutputDeviceName(String deviceName) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastDeviceChangeTime < 100) {
            return;
        }
        lastDeviceChangeTime = currentTime;

        this.outputDeviceName = deviceName;
        if (config != null) {
            config.setOutputDevice(deviceName);
            config.save();
        }

        boolean shouldUseStereo = (deviceName != null && !deviceName.equals("Default"));

        if (shouldUseStereo && !openALPlaybackBySender.isEmpty()) {
            LOGGER.info("Switching to StereoAudioPlayback for device selection support");
            for (OpenALAudioPlayback playback : openALPlaybackBySender.values()) {
                if (playback != null) playback.close();
            }
            openALPlaybackBySender.clear();
        } else if (!shouldUseStereo) {
            LOGGER.info("Switched back to OpenAL (per-sender sources will be created on demand)");
        }

        if (stereoAudioPlayback != null) {
            try {
                stereoAudioPlayback.close();
            } catch (Exception e) {
                LOGGER.warn("Error closing previous audio playback: " + e.getMessage());
            }
        }

        try {
            stereoAudioPlayback = new StereoAudioPlayback(this.outputDeviceName);
            if (!stereoAudioPlayback.initialize()) {
                LOGGER.warn("Warning: Stereo audio playback not initialized with new device. Voice output may not work.");
                stereoAudioPlayback = null;
            }
        } catch (Exception e) {
            LOGGER.warn("Error initializing audio playback with new device: " + e.getMessage());
            stereoAudioPlayback = null;
        }
    }

    public boolean isTalking() {
        long timeSinceLastTalking = System.currentTimeMillis() - lastTalkingTime;
        boolean currentlyTalking = isTalking && timeSinceLastTalking < 500;

        if (!currentlyTalking && isTalking) {
            isTalking = false;
        }

        return currentlyTalking;
    }

    private static long lastDebugLog = 0;

    private boolean isAudioSilent(byte[] audioData) {
        if (audioData == null || audioData.length == 0) {
            return true;
        }
        int offset = AudioUtils.getActivationOffset(audioData, this.silenceThreshold);

        return offset < 0;
    }

    private void checkForStaleAudio() {
        long timeSinceLastAudio = System.currentTimeMillis() - lastAudioReceived;
        if (timeSinceLastAudio > 10000 && stereoAudioPlayback != null) {
            stereoAudioPlayback.flushAudioBuffer();
        }
    }

    private void checkKeepAlive() {
        if (!handshakeComplete) {
            return;
        }

        long now = System.currentTimeMillis();

        if (now - lastSentKeepAlive > 10000) {
            try {
                sendPacket(new PingPacket(), serverHost, serverPort);
                lastSentKeepAlive = now;
            } catch (IOException ignored) { }
        }

        long timeSinceLastActivity = now - lastKeepAlive;
        if (timeSinceLastActivity > 120000) {
            LOGGER.warn("Voice server connection timed out (no activity for " + (timeSinceLastActivity / 1000) + " seconds)");
            disconnect();
        }
    }

    private void onAudioData(byte[] audioData) {
        if (!connected || audioData == null || audioData.length == 0 || micMuted) {
            return;
        }

        boolean isSilence;
        if (pushToTalk) {
            isSilence = !pttKeyPressed || audioData.length == 0;
        } else {
            isSilence = isAudioSilent(audioData);
        }

        if (isSilence) {
            return;
        }

        if (audioProcessor != null && audioProcessor.isInitialized()) {
            processAudioFrame(audioData);
        }
    }

    private void processAudioFrame(byte[] audioData) {
        try {
            byte[] encodedAudio = null;
            if (audioProcessor != null && audioProcessor.isInitialized()) {
                encodedAudio = audioProcessor.encodeAudio(audioData);
            }

            if (encodedAudio != null && encodedAudio.length > 0) {
                if (encodedAudio.length > 1024) {
                    LOGGER.warn("Encoded audio too large: " + encodedAudio.length + " bytes, skipping packet");
                    return;
                }

                long currentSeq = sequenceNumber++;

                if (sentSequenceNumbers.contains(currentSeq)) {
                    return;
                }
                sentSequenceNumbers.add(currentSeq);

                if (sentSequenceNumbers.size() > 1000) {
                    sentSequenceNumbers.clear();
                }

                try {
                    if (isInGroup()) {
                        GroupDataPacket groupPacket = new GroupDataPacket(
                            currentGroupName,
                            playerNetworkId,
                            encodedAudio,
                            currentSeq,
                            new ArrayList<>(groupMembers)
                        );
                        sendPacket(groupPacket, serverHost, serverPort);
                    } else {
                        VoiceDataPacket voicePacket = new VoiceDataPacket(playerNetworkId, encodedAudio, currentSeq);
                        sendPacket(voicePacket, serverHost, serverPort);
                    }
                } catch (IOException e) {
                    if (connected) {
                        String errorMsg = e.getMessage();
                        if (errorMsg != null && !errorMsg.contains("Connection reset") &&
                            !errorMsg.contains("Connection refused") &&
                            !errorMsg.contains("Network is unreachable")) {
                            LOGGER.warn("Failed to send voice data: " + errorMsg);
                        }
                    }
                }

                isTalking = true;
                lastTalkingTime = System.currentTimeMillis();
            }
        } catch (Exception e) {
            if (connected) {
                String errorMsg = e.getMessage();
                if (errorMsg != null && !errorMsg.contains("Connection reset") &&
                    !errorMsg.contains("Connection refused") &&
                    !errorMsg.contains("Network is unreachable")) {
                    LOGGER.warn("Failed to send voice data: " + errorMsg);
                }
            }
        }
    }




    private byte[] applyMicrophoneGain(byte[] audioData, float gain) {
        if (gain == 1.0f) return audioData;

        if (audioGainBuffer.length < audioData.length) {
            byte[] out = new byte[audioData.length];
            applyGainToBuffer(audioData, out, gain);
            return out;
        }

        applyGainToBuffer(audioData, audioGainBuffer, gain);

        byte[] result = new byte[audioData.length];
        System.arraycopy(audioGainBuffer, 0, result, 0, audioData.length);
        return result;
    }

    private void applyGainToBuffer(byte[] input, byte[] output, float gain) {
        for (int i = 0; i + 1 < input.length; i += 2) {
            int sample = (input[i] & 0xFF) | ((input[i + 1] & 0xFF) << 8);
            if (sample > 32767) sample -= 65536;
            int scaled = (int)Math.round(sample * gain);
            if (scaled > 32767) scaled = 32767;
            if (scaled < -32768) scaled = -32768;
            int le = scaled;
            output[i] = (byte)(le & 0xFF);
            output[i + 1] = (byte)((le >>> 8) & 0xFF);
        }
        if ((input.length & 1) == 1) {
            output[input.length - 1] = input[input.length - 1];
        }
    }

    private byte[] calculateStereoPositioning(byte[] audioData, double playerX, double playerY, double playerZ, float volume) {
        if (audioData == null || audioData.length == 0) {
            return null;
        }

		if (isInGroup()) {
			return duplicateMonoToStereo(audioData, volume);
		}

		double relX = playerX - simple3D.listenerX;
        double relZ = playerZ - simple3D.listenerZ;
        double distance = Math.sqrt(relX * relX + relZ * relZ);
        if (distance < 1e-6) {
            return duplicateMonoToStereo(audioData, volume);
        }
        double unitX = relX / distance;
        double unitZ = relZ / distance;

        double yawRad = Math.toRadians(simple3D.listenerYaw);
        double rx = Math.cos(yawRad);
        double rz = Math.sin(yawRad);
        float pan = (float) -(unitX * rx + unitZ * rz);

        if (volume < 1.0f) {
            float distanceAttenuation = (float) Math.max(0.1, Math.min(1.0, 10.0 / Math.max(1.0, distance)));
            volume *= distanceAttenuation;
        }

        pan = Math.max(-1.0f, Math.min(1.0f, pan));

        return applyStereoPanning(audioData, pan, volume);
    }


    private byte[] applyStereoPanning(byte[] audioData, float pan, float volume) {
        if (Math.abs(pan) < 0.01f) {
            return duplicateMonoToStereo(audioData, volume);
        }

        byte[] stereoAudio = new byte[audioData.length * 2];

        float leftGain  = (float) Math.sqrt(0.5 * (1.0 - pan));
        float rightGain = (float) Math.sqrt(0.5 * (1.0 + pan));

        volume = Math.min(volume, 1.0f);

        for (int i = 0; i < audioData.length; i += 2) {
            if (i + 1 < audioData.length) {
                short sample = (short) ((audioData[i + 1] << 8) | (audioData[i] & 0xFF));

                int scaledSample = (int) (sample * volume);
                scaledSample = Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, scaledSample));
                short volumeSample = (short) scaledSample;

                int leftScaled = (int) (volumeSample * leftGain);
                int rightScaled = (int) (volumeSample * rightGain);
                leftScaled = Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, leftScaled));
                rightScaled = Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, rightScaled));

                short leftSample = (short) leftScaled;
                short rightSample = (short) rightScaled;

                int stereoIndex = (i / 2) * 4;

                stereoAudio[stereoIndex] = (byte) (leftSample & 0xFF);
                stereoAudio[stereoIndex + 1] = (byte) ((leftSample >> 8) & 0xFF);

                stereoAudio[stereoIndex + 2] = (byte) (rightSample & 0xFF);
                stereoAudio[stereoIndex + 3] = (byte) ((rightSample >> 8) & 0xFF);
            }
        }

        return stereoAudio;
    }

    private byte[] duplicateMonoToStereo(byte[] monoAudio, float volume) {
        byte[] stereoAudio = new byte[monoAudio.length * 2];

        volume = Math.min(volume, 1.0f);

        for (int i = 0; i < monoAudio.length; i += 2) {
            if (i + 1 < monoAudio.length) {
                short sample = (short) ((monoAudio[i + 1] << 8) | (monoAudio[i] & 0xFF));

                int scaledSample = (int) (sample * volume);
                scaledSample = Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, scaledSample));
                short finalSample = (short) scaledSample;

                int stereoIndex = (i / 2) * 4;

                stereoAudio[stereoIndex] = (byte) (finalSample & 0xFF);
                stereoAudio[stereoIndex + 1] = (byte) ((finalSample >> 8) & 0xFF);

                stereoAudio[stereoIndex + 2] = (byte) (finalSample & 0xFF);
                stereoAudio[stereoIndex + 3] = (byte) ((finalSample >> 8) & 0xFF);
            }
        }

        return stereoAudio;
    }

    public void updateListenerPosition(double x, double y, double z, float yaw, float pitch) {
        if (simple3D != null && simple3D.isInitialized()) {
            simple3D.updateListener(x, y, z, yaw, pitch);
        }
    }

    public boolean startMicrophoneTesting() {
        if (isMicrophoneTesting) {
            return true;
        }

        if (testMicrophoneCapture != null) {
            stopMicrophoneTesting();
        }

        boolean wasCapturing = microphoneCapture != null;
        if (wasCapturing) {
            stopMicrophoneCapture();
        }

        testMicrophoneCapture = new MicrophoneCapture(this::onTestMicrophoneData, this.inputDeviceName);
        if (!testMicrophoneCapture.startCapture()) {
            LOGGER.warn("Failed to start microphone testing");
            testMicrophoneCapture = null;

            if (wasCapturing && connected) {
                startMicrophoneCapture();
            }
            return false;
        }

        isMicrophoneTesting = true;
        LOGGER.info("Microphone testing started");
        return true;
    }

    public void stopMicrophoneTesting() {
        if (!isMicrophoneTesting) {
            return;
        }

        isMicrophoneTesting = false;

        if (testMicrophoneCapture != null) {
            testMicrophoneCapture.stopCapture();
            testMicrophoneCapture.close();
            testMicrophoneCapture = null;
        }

        if (connected && microphoneCapture == null) {
            startMicrophoneCapture();
        }

        LOGGER.info("Microphone testing stopped");
    }

    public boolean isMicrophoneTesting() {
        return isMicrophoneTesting;
    }

    private void onTestMicrophoneData(byte[] audioData) {
        if (!isMicrophoneTesting || audioData == null || audioData.length == 0) {
            return;
        }

        try {
            byte[] gainedAudio = applyMicrophoneGain(audioData, this.microphoneGain);

            byte[] stereoAudio = duplicateMonoToStereo(gainedAudio, 1.0f);

            if (stereoAudioPlayback != null && stereoAudioPlayback.isInitialized()) {
                if (!stereoAudioPlayback.isRunning()) {
                    if (!stereoAudioPlayback.startPlayback()) {
                        LOGGER.warn("Failed to start stereo audio playback for mic testing");
                        return;
                    }
                }
                stereoAudioPlayback.queueAudio(stereoAudio);
            }
        } catch (Exception e) {
            LOGGER.warn("Error during microphone testing: " + e.getMessage());
        }
    }

    public interface PlayerResolver {
        String nameFromNetworkId(int networkId);
        Integer networkIdFromName(String name);
    }

    private volatile PlayerResolver playerResolver;

    public void setPlayerResolver(PlayerResolver resolver) {
        this.playerResolver = resolver;
    }

    public String getPlayerNameFromId(Integer playerId) {
        if (playerId == null) return null;
        PlayerResolver r = this.playerResolver;
        if (r == null) return null;
        try {
            return r.nameFromNetworkId(playerId);
        } catch (Exception ignored) {
            return null;
        }
    }

    private Integer getPlayerIdFromName(String playerName) {
        if (playerName == null) return null;
        PlayerResolver r = this.playerResolver;
        if (r == null) return null;
        try {
            return r.networkIdFromName(playerName);
        } catch (Exception ignored) {
            return null;
        }
    }
}
