package com.moyettes.legacyvoicechat.udp;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class VoiceDataPacket extends UdpPacket {

    private Integer playerNetworkId;
    private byte[] audioData;
    private long sequenceNumber;

    public VoiceDataPacket() {
        super(PacketType.VOICE_DATA);
    }

    public VoiceDataPacket(Integer playerNetworkId, byte[] audioData, long sequenceNumber) {
        super(PacketType.VOICE_DATA);
        this.playerNetworkId = playerNetworkId;
        this.audioData = audioData;
        this.sequenceNumber = sequenceNumber;
    }

    @Override
    protected void writeData(DataOutputStream dos) throws IOException {
        dos.writeInt(playerNetworkId);
        dos.writeLong(sequenceNumber);
        dos.writeInt(audioData.length);
        dos.write(audioData);
    }

    public static VoiceDataPacket read(DataInputStream dis) throws IOException {
        VoiceDataPacket packet = new VoiceDataPacket();
        packet.playerNetworkId = dis.readInt();
        packet.sequenceNumber = dis.readLong();
        int dataLength = dis.readInt();

        if (dataLength < 0 || dataLength > 1024) {
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

    public Integer getPlayerNetworkId() {
        return playerNetworkId;
    }

    public byte[] getAudioData() {
        return audioData;
    }

    public long getSequenceNumber() {
        return sequenceNumber;
    }
}
