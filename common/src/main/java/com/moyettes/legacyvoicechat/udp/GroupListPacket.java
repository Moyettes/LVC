package com.moyettes.legacyvoicechat.udp;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GroupListPacket extends UdpPacket {

    private List<GroupInfo> groups;

    public GroupListPacket() {
        super(PacketType.GROUP_LIST);
        this.groups = new ArrayList<>();
    }

    public GroupListPacket(List<GroupInfo> groups) {
        super(PacketType.GROUP_LIST);
        this.groups = groups != null ? groups : new ArrayList<>();
    }

    @Override
    protected void writeData(DataOutputStream dos) throws IOException {
        dos.writeInt(groups.size());
        for (GroupInfo group : groups) {
            dos.writeUTF(group.getName());
            dos.writeBoolean(group.hasPassword());
            dos.writeInt(group.getMemberCount());
        }
    }

    public static GroupListPacket read(DataInputStream dis) throws IOException {
        GroupListPacket packet = new GroupListPacket();
        int groupCount = dis.readInt();

        for (int i = 0; i < groupCount; i++) {
            String name = dis.readUTF();
            boolean hasPassword = dis.readBoolean();
            int memberCount = dis.readInt();
            packet.groups.add(new GroupInfo(name, hasPassword, memberCount));
        }

        return packet;
    }

    public List<GroupInfo> getGroups() {
        return groups;
    }

    public static class GroupInfo {
        private final String name;
        private final boolean hasPassword;
        private final int memberCount;

        public GroupInfo(String name, boolean hasPassword, int memberCount) {
            this.name = name;
            this.hasPassword = hasPassword;
            this.memberCount = memberCount;
        }

        public String getName() {
            return name;
        }

        public boolean hasPassword() {
            return hasPassword;
        }

        public int getMemberCount() {
            return memberCount;
        }
    }
}
