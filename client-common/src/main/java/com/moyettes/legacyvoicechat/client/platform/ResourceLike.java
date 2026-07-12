package com.moyettes.legacyvoicechat.client.platform;

public final class ResourceLike {

	private final String namespace;
	private final String path;

	public ResourceLike(String namespace, String path) {
		this.namespace = namespace;
		this.path = path;
	}

	public static ResourceLike of(String path) {
		return new ResourceLike("legacyvoicechat", path);
	}

	public String namespace() {
		return namespace;
	}

	public String path() {
		return path;
	}

	@Override
	public String toString() {
		return namespace + ":" + path;
	}
}
