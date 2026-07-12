package com.moyettes.legacyvoicechat.udp;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class GroupJoinPacket extends UdpPacket {

    private String groupName;
    private String password; // Can be null for public groups

    public GroupJoinPacket() {
        super(PacketType.GROUP_JOIN);
    }

    public GroupJoinPacket(String groupName, String password) {
        super(PacketType.GROUP_JOIN);
        this.groupName = groupName;
        this.password = password;
    }

    @Override
    protected void writeData(DataOutputStream dos) throws IOException {
        dos.writeUTF(groupName != null ? groupName : "");
        dos.writeBoolean(password != null);
        if (password != null) {
            dos.writeUTF(password);
        }
    }

    public static GroupJoinPacket read(DataInputStream dis) throws IOException {
        GroupJoinPacket packet = new GroupJoinPacket();
        packet.groupName = dis.readUTF();
        boolean hasPassword = dis.readBoolean();
        if (hasPassword) {
            packet.password = dis.readUTF();
        } else {
            packet.password = null;
        }
        return packet;
    }

    public String getGroupName() {
        return groupName;
    }

    public String getPassword() {
        return password;
    }

    public boolean isPublic() {
        return password == null || password.isEmpty();
    }
}
