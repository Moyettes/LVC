package com.moyettes.legacyvoicechat.udp;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class PresenceUpdatePacket extends UdpPacket {

    private int playerNetworkId;
    private boolean voiceSupported;
    private boolean deafened;

    public PresenceUpdatePacket() {
        super(PacketType.PRESENCE_UPDATE);
    }

    public PresenceUpdatePacket(int playerNetworkId, boolean voiceSupported, boolean deafened) {
        super(PacketType.PRESENCE_UPDATE);
        this.playerNetworkId = playerNetworkId;
        this.voiceSupported = voiceSupported;
        this.deafened = deafened;
    }

    @Override
    protected void writeData(DataOutputStream dos) throws IOException {
        dos.writeInt(playerNetworkId);
        dos.writeBoolean(voiceSupported);
        dos.writeBoolean(deafened);
    }

    public static PresenceUpdatePacket read(DataInputStream dis) throws IOException {
        PresenceUpdatePacket packet = new PresenceUpdatePacket();
        packet.playerNetworkId = dis.readInt();
        packet.voiceSupported = dis.readBoolean();
        packet.deafened = dis.readBoolean();
        return packet;
    }

    public int getPlayerNetworkId() {
        return playerNetworkId;
    }

    public boolean isVoiceSupported() {
        return voiceSupported;
    }

    public boolean isDeafened() {
        return deafened;
    }
}

