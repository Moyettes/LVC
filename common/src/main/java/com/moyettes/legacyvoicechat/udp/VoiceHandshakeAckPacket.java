package com.moyettes.legacyvoicechat.udp;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class VoiceHandshakeAckPacket extends UdpPacket {

    private boolean success;
    private String message;

    public VoiceHandshakeAckPacket() {
        super(PacketType.VOICE_HANDSHAKE_ACK);
    }

    public VoiceHandshakeAckPacket(boolean success, String message) {
        super(PacketType.VOICE_HANDSHAKE_ACK);
        this.success = success;
        this.message = message;
    }

    @Override
    protected void writeData(DataOutputStream dos) throws IOException {
        dos.writeBoolean(success);
        dos.writeUTF(message);
    }

    public static VoiceHandshakeAckPacket read(DataInputStream dis) throws IOException {
        VoiceHandshakeAckPacket packet = new VoiceHandshakeAckPacket();
        packet.success = dis.readBoolean();
        packet.message = dis.readUTF();
        return packet;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }
}
