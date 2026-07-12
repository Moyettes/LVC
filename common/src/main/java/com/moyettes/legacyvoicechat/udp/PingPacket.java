package com.moyettes.legacyvoicechat.udp;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class PingPacket extends UdpPacket {

    public PingPacket() {
        super(PacketType.PING);
    }

    @Override
    protected void writeData(DataOutputStream dos) throws IOException {
        // Ping packet has no additional data
    }

    public static PingPacket read(DataInputStream dis) throws IOException {
        return new PingPacket();
    }
}
