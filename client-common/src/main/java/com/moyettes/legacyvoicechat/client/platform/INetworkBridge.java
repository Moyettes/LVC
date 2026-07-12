package com.moyettes.legacyvoicechat.client.platform;

import java.util.function.BiConsumer;

public interface INetworkBridge {

	void sendServerPayload(String channelId, byte[] data);

	void registerClientReceiver(String channelId, BiConsumer<PlayerRef, byte[]> handler);
}
