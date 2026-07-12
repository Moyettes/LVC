package com.moyettes.legacyvoicechat.udp;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public abstract class UdpPacket {

    public enum PacketType {
        VOICE_HANDSHAKE(0x01),
        VOICE_HANDSHAKE_ACK(0x02),
        VOICE_DATA(0x03),
        VOICE_DATA_RELAY(0x04),
        GROUP_CREATE(0x05),
        GROUP_JOIN(0x06),
        GROUP_LEAVE(0x07),
        GROUP_LIST(0x08),
        GROUP_DATA(0x09),
        GROUP_MEMBER_UPDATE(0x0A),
        PING(0x0B),
        PRESENCE_UPDATE(0x0C),
        PRESENCE_BULK(0x0D);

        private final int id;

        PacketType(int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }

        public static PacketType fromId(int id) {
            for (PacketType type : values()) {
                if (type.id == id) {
                    return type;
                }
            }
            throw new IllegalArgumentException("Unknown packet type: " + id);
        }
    }

    private final PacketType type;

    protected UdpPacket(PacketType type) {
        this.type = type;
    }

    public PacketType getType() {
        return type;
    }

    public byte[] toBytes() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        dos.writeByte(type.getId());
        writeData(dos);

        return baos.toByteArray();
    }

    public static UdpPacket fromBytes(byte[] data) throws IOException {
        if (data == null || data.length == 0) {
            throw new IOException("Cannot parse null or empty packet data");
        }

        if (data.length < 1) {
            throw new IOException("Packet too short to contain type byte");
        }

        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        DataInputStream dis = new DataInputStream(bais);

        int typeId = dis.readByte();
        PacketType type = PacketType.fromId(typeId);

        if (type == null) {
            throw new IOException("Unknown packet type ID: " + typeId);
        }

        switch (type) {
            case VOICE_HANDSHAKE:
                return VoiceHandshakePacket.read(dis);
            case VOICE_HANDSHAKE_ACK:
                return VoiceHandshakeAckPacket.read(dis);
            case VOICE_DATA:
                return VoiceDataPacket.read(dis);
            case VOICE_DATA_RELAY:
                return VoiceDataRelayPacket.read(dis);
            case GROUP_CREATE:
                return GroupCreatePacket.read(dis);
            case GROUP_JOIN:
                return GroupJoinPacket.read(dis);
            case GROUP_LEAVE:
                return GroupLeavePacket.read(dis);
            case GROUP_LIST:
                return GroupListPacket.read(dis);
            case GROUP_DATA:
                return GroupDataPacket.read(dis);
            case GROUP_MEMBER_UPDATE:
                return GroupMemberUpdatePacket.read(dis);
            case PING:
                return PingPacket.read(dis);
            case PRESENCE_UPDATE:
                return PresenceUpdatePacket.read(dis);
            case PRESENCE_BULK:
                return PresenceBulkPacket.read(dis);
            default:
                throw new IllegalArgumentException("Unknown packet type: " + type);
        }
    }

    protected abstract void writeData(DataOutputStream dos) throws IOException;
}
