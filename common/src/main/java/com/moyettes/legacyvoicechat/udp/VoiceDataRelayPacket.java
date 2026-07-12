package com.moyettes.legacyvoicechat.udp;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class VoiceDataRelayPacket extends UdpPacket {

    private Integer fromPlayerNetworkId;
    private byte[] audioData;
    private long sequenceNumber;
    private double distance;
    private float volume;
    private double playerX;
    private double playerY;
    private double playerZ;

    public VoiceDataRelayPacket() {
        super(PacketType.VOICE_DATA_RELAY);
    }

    public VoiceDataRelayPacket(Integer fromPlayerNetworkId, byte[] audioData, long sequenceNumber, double distance, float volume, double playerX, double playerY, double playerZ) {
        super(PacketType.VOICE_DATA_RELAY);
        this.fromPlayerNetworkId = fromPlayerNetworkId;
        this.audioData = audioData;
        this.sequenceNumber = sequenceNumber;
        this.distance = distance;
        this.volume = volume;
        this.playerX = playerX;
        this.playerY = playerY;
        this.playerZ = playerZ;
    }

    @Override
    protected void writeData(DataOutputStream dos) throws IOException {
        dos.writeInt(fromPlayerNetworkId);
        dos.writeLong(sequenceNumber);
        dos.writeDouble(distance);
        dos.writeFloat(volume);
        dos.writeDouble(playerX);
        dos.writeDouble(playerY);
        dos.writeDouble(playerZ);
        dos.writeInt(audioData.length);
        dos.write(audioData);
    }

    public static VoiceDataRelayPacket read(DataInputStream dis) throws IOException {
        VoiceDataRelayPacket packet = new VoiceDataRelayPacket();
        packet.fromPlayerNetworkId = dis.readInt();
        packet.sequenceNumber = dis.readLong();
        packet.distance = dis.readDouble();
        packet.volume = dis.readFloat();
        packet.playerX = dis.readDouble();
        packet.playerY = dis.readDouble();
        packet.playerZ = dis.readDouble();
        int dataLength = dis.readInt();

        // Validate data length to prevent excessive memory allocation
        if (dataLength < 0 || dataLength > 1024) { // Max 1KB audio data (Opus limit)
            throw new IOException("Invalid audio data length: " + dataLength);
        }

        // Note: Don't check dis.available() as it's unreliable for UDP packets
        // Just try to read and let readFully handle insufficient data

        packet.audioData = new byte[dataLength];
        try {
            dis.readFully(packet.audioData);
        } catch (java.io.EOFException e) {
            throw new IOException("Insufficient data in packet: expected " + dataLength + " bytes", e);
        }
        return packet;
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

    public double getDistance() {
        return distance;
    }

    public float getVolume() {
        return volume;
    }

    public double getPlayerX() {
        return playerX;
    }

    public double getPlayerY() {
        return playerY;
    }

    public double getPlayerZ() {
        return playerZ;
    }
}
