package com.moyettes.legacyvoicechat.api.client;

import com.moyettes.legacyvoicechat.api.events.Event;
import net.minecraft.client.Minecraft;

import java.util.function.Consumer;

public class ClientConnectionEvents {

	public static final Event<Consumer<Minecraft>> PLAY_READY = Event.consumer();

	public static final Event<Consumer<Minecraft>> DISCONNECT = Event.consumer();
}
