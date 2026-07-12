package com.moyettes.legacyvoicechat.api.server;

import com.moyettes.legacyvoicechat.api.events.Event;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;

import java.util.function.BiConsumer;

public class ServerConnectionEvents {

    /**
     * This event is fired when a client disconnects from the server.
     *
     * <p>
     * Callbacks to this event should be registered in your mod's entrypoint,
     * and can be done as follows:
     *
     * <pre>
     * {@code
     * ServerConnectionEvents.DISCONNECT.register((server, player) -> {
     * 	...
     * });
     * }
     * </pre>
     */
    public static final Event<BiConsumer<MinecraftServer, ServerPlayerEntity>> DISCONNECT = Event.biConsumer();

}
