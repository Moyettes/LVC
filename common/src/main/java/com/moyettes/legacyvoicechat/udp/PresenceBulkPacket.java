package com.moyettes.legacyvoicechat.udp;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class PresenceBulkPacket extends UdpPacket {

    public static class PlayerState {
        public boolean voiceSupported;
        public boolean deafened;

        public PlayerState(boolean voiceSupported, boolean deafened) {
            this.voiceSupported = voiceSupported;
            this.deafened = deafened;
        }
    }

    private Map<Integer, PlayerState> playerStates = new HashMap<>();

    public PresenceBulkPacket() {
        super(PacketType.PRESENCE_BULK);
    }

    public PresenceBulkPacket(Map<Integer, PlayerState> playerStates) {
        super(PacketType.PRESENCE_BULK);
        this.playerStates = playerStates;
    }

    @Override
    protected void writeData(DataOutputStream dos) throws IOException {
        dos.writeInt(playerStates.size());
        for (Map.Entry<Integer, PlayerState> entry : playerStates.entrySet()) {
            dos.writeInt(entry.getKey());
            dos.writeBoolean(entry.getValue().voiceSupported);
            dos.writeBoolean(entry.getValue().deafened);
        }
    }

    public static PresenceBulkPacket read(DataInputStream dis) throws IOException {
        PresenceBulkPacket packet = new PresenceBulkPacket();
        int count = dis.readInt();
        for (int i = 0; i < count; i++) {
            int playerId = dis.readInt();
            boolean voiceSupported = dis.readBoolean();
            boolean deafened = dis.readBoolean();
            packet.playerStates.put(playerId, new PlayerState(voiceSupported, deafened));
        }
        return packet;
    }

    public Map<Integer, PlayerState> getPlayerStates() {
        return playerStates;
    }
}

