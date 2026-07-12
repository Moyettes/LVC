package com.moyettes.legacyvoicechat.client.platform;

public final class PlayerRef {

	private final int networkId;
	private final String name;

	public PlayerRef(int networkId, String name) {
		this.networkId = networkId;
		this.name = name;
	}

	public int networkId() {
		return networkId;
	}

	public String name() {
		return name;
	}
}
