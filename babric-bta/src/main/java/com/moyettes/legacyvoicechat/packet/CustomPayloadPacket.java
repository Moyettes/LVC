package com.moyettes.legacyvoicechat.packet;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.net.handler.PacketHandlerClient;
import net.minecraft.core.net.handler.PacketHandler;
import net.minecraft.core.net.packet.Packet;
import net.minecraft.server.net.handler.PacketHandlerServer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class CustomPayloadPacket extends Packet  {
    public String channel;
    public int size;
    public byte[] data;

    public CustomPayloadPacket() {
    }

    public CustomPayloadPacket(String channel, byte[] data) {
        this.channel = channel;
        this.data = data;
        if (data != null) {
            this.size = data.length;
            if (this.size > Short.MAX_VALUE) {
                throw new IllegalArgumentException("Payload may not be larger than 32k");
            }
        }
    }

    @Override
    public void read(DataInputStream input) {
        try {
            this.channel = input.readUTF();
            this.size = input.readShort();
            if (this.size > 0 && this.size < Short.MAX_VALUE) {
                this.data = new byte[this.size];
                input.readFully(this.data);
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void write(DataOutputStream output) {
        try {
            output.writeUTF(this.channel);
            output.writeShort(this.size);
            if (this.data != null) {
                output.write(this.data);
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void handlePacket(PacketHandler arg) {
        EnvType env = FabricLoader.getInstance().getEnvironmentType();
        if (env == EnvType.CLIENT) {
            handleClient(arg);
        } else if (env == EnvType.SERVER) {
            ServerPlayNetworking.handle((PacketHandlerServer) arg, this);
        }
    }

    @Environment(EnvType.CLIENT)
    private void handleClient(PacketHandler arg) {
        ClientPlayNetworking.handle((PacketHandlerClient) arg, this);
    }

    @Override
    public int getEstimatedSize() {
        return 2 + this.channel.length() * 2 + 2 + this.size;
    }


}
