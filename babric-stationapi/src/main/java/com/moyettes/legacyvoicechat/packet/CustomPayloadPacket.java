package com.moyettes.legacyvoicechat.packet;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.network.ClientNetworkHandler;
import net.minecraft.network.NetworkHandler;
import net.minecraft.network.packet.Packet;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.modificationstation.stationapi.api.network.packet.ManagedPacket;
import net.modificationstation.stationapi.api.network.packet.PacketType;
import org.jetbrains.annotations.NotNull;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class CustomPayloadPacket extends Packet implements ManagedPacket<CustomPayloadPacket> {
    public static final PacketType<CustomPayloadPacket> TYPE = PacketType.builder(true, true, CustomPayloadPacket::new).build();

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
    public void apply(NetworkHandler arg) {
        switch (FabricLoader.getInstance().getEnvironmentType()) {
            case CLIENT -> handleClient(arg);
            case SERVER -> handleServer(arg);
        }
    }

    @Environment(EnvType.CLIENT)
    public void handleClient(NetworkHandler handler) {
        ClientPlayNetworking.handle((ClientNetworkHandler) handler, this);
    }

    @Environment(EnvType.SERVER)
    public void handleServer(NetworkHandler handler) {
        ServerPlayNetworking.handle((ServerPlayNetworkHandler) handler, this);
    }


    @Override
    public int size() {
        return 2 + this.channel.length() * 2 + 2 + this.size;
    }

    @Override
    public @NotNull PacketType<CustomPayloadPacket> getType() {
        return TYPE;
    }
}
