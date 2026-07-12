package com.moyettes.legacyvoicechat.udp;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class GroupCreatePacket extends UdpPacket {

    private String groupName;
    private String password; // Can be null for public groups

    public GroupCreatePacket() {
        super(PacketType.GROUP_CREATE);
    }

    public GroupCreatePacket(String groupName, String password) {
        super(PacketType.GROUP_CREATE);
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

    public static GroupCreatePacket read(DataInputStream dis) throws IOException {
        GroupCreatePacket packet = new GroupCreatePacket();
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
