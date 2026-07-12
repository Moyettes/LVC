package com.moyettes.legacyvoicechat.udp;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
public class VoiceHandshakePacket extends UdpPacket {

    private Integer playerNetworkId;
    private String authToken;
    private boolean micEnabled;

    public VoiceHandshakePacket() {
        super(PacketType.VOICE_HANDSHAKE);
    }

    public VoiceHandshakePacket(Integer playerNetworkId, String authToken, boolean micEnabled) {
        super(PacketType.VOICE_HANDSHAKE);
        this.playerNetworkId = playerNetworkId;
        this.authToken = authToken;
        this.micEnabled = micEnabled;
    }

    @Override
    protected void writeData(DataOutputStream dos) throws IOException {
        dos.writeInt(playerNetworkId);
        dos.writeUTF(authToken);
        dos.writeBoolean(micEnabled);
    }

    public static VoiceHandshakePacket read(DataInputStream dis) throws IOException {
        VoiceHandshakePacket packet = new VoiceHandshakePacket();
        packet.playerNetworkId = dis.readInt();
        packet.authToken = dis.readUTF();
        packet.micEnabled = dis.readBoolean();
        return packet;
    }

    public Integer getPlayerNetworkId() {
        return playerNetworkId;
    }

    public String getAuthToken() {
        return authToken;
    }

    public boolean isMicEnabled() {
        return micEnabled;
    }
}
