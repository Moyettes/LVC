package com.moyettes.legacyvoicechat.packet;

import com.moyettes.legacyvoicechat.packet.CustomPayload;
import com.moyettes.legacyvoicechat.api.DataStreams;
import com.moyettes.legacyvoicechat.api.IOConsumer;
import com.moyettes.legacyvoicechat.utils.MinecraftServerAccessor;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.packet.Packet;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.modificationstation.stationapi.api.network.packet.PacketHelper;
import net.modificationstation.stationapi.mixin.player.server.ServerPlayNetworkHandlerAccessor;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

public class ServerPlayNetworking {


    private static final MinecraftServer server = MinecraftServerAccessor.getMinecraftServer();

    public static <T extends CustomPayload> void registerListener(String channel, Supplier<T> initializer, PayloadListener<T> listener) {
        registerListenerImpl(channel, (server, handler, player, data) -> {
            T payload = initializer.get();
            payload.read(DataStreams.input(data));

            return listener.handle(server, handler, player, payload);
        });
    }

    public interface PayloadListener<T extends CustomPayload> {

        /**
         * Receive incoming data from the client.
         *
         * @return
         *  Whether the data is consumed. Should only return {@code false} if the
         *  data is completely ignored.
         */
        boolean handle(MinecraftServer server, ServerPlayNetworkHandler handler, ServerPlayerEntity player, T payload) throws IOException;

    }

    public static final Map<String, Listener> LISTENERS = new LinkedHashMap<>();

    private static void registerListenerImpl(String channel, Listener listener) {
        LISTENERS.compute(channel, (key, value) -> {
            if (value != null) {
                throw new IllegalStateException("there is already a listener on channel \'" + channel + "\'");
            }

            return listener;
        });
    }

    /**
     * Send a packet to the given player through the given channel. The payload
     * will only be written if the channel is open.
     */
    public static void send(ServerPlayerEntity player, String channel, CustomPayload payload) {
        PacketHelper.sendTo(player, makePacket(channel, payload));
    }

    private static Packet makePacket(String channel, CustomPayload payload) {
        return makePacket(channel, payload::write);
    }

    private static Packet makePacket(String channel, IOConsumer<DataOutputStream> writer) {
        try {
            return new CustomPayloadPacket(channel, DataStreams.output(writer).toByteArray());
        } catch (IOException e) {
            //LOGGER.warn("error writing custom payload to channel \'" + channel + "\'", e);
            return null;
        }
    }

    public static boolean handle(ServerPlayNetworkHandler handler, CustomPayloadPacket packet) {
        Listener listener = LISTENERS.get(packet.channel);

        if (listener != null) {
            ServerPlayerEntity player = ((ServerPlayNetworkHandlerAccessor)handler).getField_920();

            try {
                return listener.handle(server, handler, player, packet.data);
            } catch (IOException e) {
                //LOGGER.warn("error handling custom payload on channel \'" + packet.channel + "\'", e);
                return true;
            }
        }

        return false;
    }


    private interface Listener {

        boolean handle(MinecraftServer server, ServerPlayNetworkHandler handler, ServerPlayerEntity player, byte[] data) throws IOException;

    }
}
