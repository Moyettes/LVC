package com.moyettes.legacyvoicechat.udp;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GroupMemberUpdatePacket extends UdpPacket {

    public enum UpdateType {
        MEMBER_JOINED(0),
        MEMBER_LEFT(1),
        GROUP_CREATED(2),
        GROUP_DELETED(3);

        private final int id;

        UpdateType(int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }

        public static UpdateType fromId(int id) {
            for (UpdateType type : values()) {
                if (type.id == id) {
                    return type;
                }
            }
            throw new IllegalArgumentException("Unknown update type: " + id);
        }
    }

    private UpdateType updateType;
    private String groupName;
    private Integer playerNetworkId;
    private String playerName;
    private List<String> groupMembers;

    public GroupMemberUpdatePacket() {
        super(PacketType.GROUP_MEMBER_UPDATE);
        this.groupMembers = new ArrayList<>();
    }

    public GroupMemberUpdatePacket(UpdateType updateType, String groupName, Integer playerNetworkId, String playerName, List<String> groupMembers) {
        super(PacketType.GROUP_MEMBER_UPDATE);
        this.updateType = updateType;
        this.groupName = groupName;
        this.playerNetworkId = playerNetworkId;
        this.playerName = playerName;
        this.groupMembers = groupMembers != null ? groupMembers : new ArrayList<>();
    }

    @Override
    protected void writeData(DataOutputStream dos) throws IOException {
        dos.writeByte(updateType.getId());
        dos.writeUTF(groupName != null ? groupName : "");
        dos.writeInt(playerNetworkId != null ? playerNetworkId : -1);
        dos.writeUTF(playerName != null ? playerName : "");
        dos.writeInt(groupMembers.size());
        for (String member : groupMembers) {
            dos.writeUTF(member);
        }
    }

    public static GroupMemberUpdatePacket read(DataInputStream dis) throws IOException {
        GroupMemberUpdatePacket packet = new GroupMemberUpdatePacket();
        packet.updateType = UpdateType.fromId(dis.readByte());
        packet.groupName = dis.readUTF();
        int playerId = dis.readInt();
        packet.playerNetworkId = playerId == -1 ? null : playerId;
        packet.playerName = dis.readUTF();

        int memberCount = dis.readInt();
        for (int i = 0; i < memberCount; i++) {
            packet.groupMembers.add(dis.readUTF());
        }

        return packet;
    }

    public UpdateType getUpdateType() {
        return updateType;
    }

    public String getGroupName() {
        return groupName;
    }

    public Integer getPlayerNetworkId() {
        return playerNetworkId;
    }

    public String getPlayerName() {
        return playerName;
    }

    public List<String> getGroupMembers() {
        return groupMembers;
    }
}
