package com.moyettes.legacyvoicechat.client;

import com.moyettes.legacyvoicechat.client.platform.IGuiBridge;
import com.moyettes.legacyvoicechat.client.platform.IHudRenderer;
import com.moyettes.legacyvoicechat.client.platform.IKeyBindingRegistry;
import com.moyettes.legacyvoicechat.client.platform.IMinecraftClientAccessor;
import com.moyettes.legacyvoicechat.client.platform.INetworkBridge;
import com.moyettes.legacyvoicechat.client.platform.IRenderApi;
import com.moyettes.legacyvoicechat.LoaderId;

public final class VoiceClientContext {

	public final LoaderId loader;
	public final String minecraftVersion;
	public final IMinecraftClientAccessor client;
	public final IRenderApi render;
	public final IKeyBindingRegistry keys;
	public final INetworkBridge net;
	public final IGuiBridge gui;
	public final IHudRenderer hud;

	private VoiceClientContext(Builder b) {
		this.loader = b.loader;
		this.minecraftVersion = b.minecraftVersion;
		this.client = b.client;
		this.render = b.render;
		this.keys = b.keys;
		this.net = b.net;
		this.gui = b.gui;
		this.hud = b.hud;
	}

	public static Builder builder() {
		return new Builder();
	}

	public static final class Builder {
		private LoaderId loader;
		private String minecraftVersion;
		private IMinecraftClientAccessor client;
		private IRenderApi render;
		private IKeyBindingRegistry keys;
		private INetworkBridge net;
		private IGuiBridge gui;
		private IHudRenderer hud;

		public Builder loader(LoaderId loader) {
			this.loader = loader;
			return this;
		}

		public Builder minecraftVersion(String minecraftVersion) {
			this.minecraftVersion = minecraftVersion;
			return this;
		}

		public Builder client(IMinecraftClientAccessor client) {
			this.client = client;
			return this;
		}

		public Builder render(IRenderApi render) {
			this.render = render;
			return this;
		}

		public Builder keys(IKeyBindingRegistry keys) {
			this.keys = keys;
			return this;
		}

		public Builder networking(INetworkBridge net) {
			this.net = net;
			return this;
		}

		public Builder gui(IGuiBridge gui) {
			this.gui = gui;
			return this;
		}

		public Builder hud(IHudRenderer hud) {
			this.hud = hud;
			return this;
		}

		public VoiceClientContext build() {
			if (loader == null) {
				throw new IllegalStateException("VoiceClientContext requires a loader");
			}
			if (minecraftVersion == null) {
				throw new IllegalStateException("VoiceClientContext requires a minecraftVersion");
			}
			return new VoiceClientContext(this);
		}
	}
}
