package com.moyettes.legacyvoicechat.udp;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GroupDataPacket extends UdpPacket {

    private String groupName;
    private Integer fromPlayerNetworkId;
    private byte[] audioData;
    private long sequenceNumber;
    private List<Integer> targetPlayerIds; // List of player IDs in the group

    public GroupDataPacket() {
        super(PacketType.GROUP_DATA);
        this.targetPlayerIds = new ArrayList<>();
    }

    public GroupDataPacket(String groupName, Integer fromPlayerNetworkId, byte[] audioData, long sequenceNumber, List<Integer> targetPlayerIds) {
        super(PacketType.GROUP_DATA);
        this.groupName = groupName;
        this.fromPlayerNetworkId = fromPlayerNetworkId;
        this.audioData = audioData;
        this.sequenceNumber = sequenceNumber;
        this.targetPlayerIds = targetPlayerIds != null ? targetPlayerIds : new ArrayList<>();
    }

    @Override
    protected void writeData(DataOutputStream dos) throws IOException {
        dos.writeUTF(groupName != null ? groupName : "");
        dos.writeInt(fromPlayerNetworkId);
        dos.writeLong(sequenceNumber);
        dos.writeInt(targetPlayerIds.size());
        for (Integer playerId : targetPlayerIds) {
            dos.writeInt(playerId);
        }
        dos.writeInt(audioData.length);
        dos.write(audioData);
    }

    public static GroupDataPacket read(DataInputStream dis) throws IOException {
        GroupDataPacket packet = new GroupDataPacket();
        packet.groupName = dis.readUTF();
        packet.fromPlayerNetworkId = dis.readInt();
        packet.sequenceNumber = dis.readLong();

        int targetCount = dis.readInt();
        for (int i = 0; i < targetCount; i++) {
            packet.targetPlayerIds.add(dis.readInt());
        }

        int dataLength = dis.readInt();

        // Validate data length to prevent excessive memory allocation
        if (dataLength < 0 || dataLength > 1024) { // Max 1KB audio data (Opus limit)
            throw new IOException("Invalid audio data length: " + dataLength);
        }

        packet.audioData = new byte[dataLength];
        try {
            dis.readFully(packet.audioData);
        } catch (java.io.EOFException e) {
            throw new IOException("Insufficient data in packet: expected " + dataLength + " bytes", e);
        }

        return packet;
    }

    public String getGroupName() {
        return groupName;
    }

    public Integer getFromPlayerNetworkId() {
        return fromPlayerNetworkId;
    }

    public byte[] getAudioData() {
        return audioData;
    }

    public long getSequenceNumber() {
        return sequenceNumber;
    }

    public List<Integer> getTargetPlayerIds() {
        return targetPlayerIds;
    }
}
