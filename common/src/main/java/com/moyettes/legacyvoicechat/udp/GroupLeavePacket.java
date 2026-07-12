package com.moyettes.legacyvoicechat.udp;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class GroupLeavePacket extends UdpPacket {

    private String groupName; // Optional: leave specific group, null means leave current group

    public GroupLeavePacket() {
        super(PacketType.GROUP_LEAVE);
    }

    public GroupLeavePacket(String groupName) {
        super(PacketType.GROUP_LEAVE);
        this.groupName = groupName;
    }

    @Override
    protected void writeData(DataOutputStream dos) throws IOException {
        dos.writeBoolean(groupName != null);
        if (groupName != null) {
            dos.writeUTF(groupName);
        }
    }

    public static GroupLeavePacket read(DataInputStream dis) throws IOException {
        GroupLeavePacket packet = new GroupLeavePacket();
        boolean hasGroupName = dis.readBoolean();
        if (hasGroupName) {
            packet.groupName = dis.readUTF();
        } else {
            packet.groupName = null;
        }
        return packet;
    }

    public String getGroupName() {
        return groupName;
    }
}
