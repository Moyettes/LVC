package com.moyettes.legacyvoicechat;

import com.moyettes.legacyvoicechat.compat.IPlayerEvents;
import com.moyettes.legacyvoicechat.LoaderId;
import com.moyettes.legacyvoicechat.compat.MinecraftCompat;
import com.moyettes.legacyvoicechat.compat.networking.VoiceNetworkingCompat;

public final class VoiceContext {

	public final LoaderId loader;
	public final String minecraftVersion;
	public final MinecraftCompat mc;
	public final VoiceNetworkingCompat net;
	public final IPlayerEvents events;

	private VoiceContext(Builder b) {
		this.loader = b.loader;
		this.minecraftVersion = b.minecraftVersion;
		this.mc = b.mc;
		this.net = b.net;
		this.events = b.events;
	}

	public static Builder builder() {
		return new Builder();
	}

	public static final class Builder {
		private LoaderId loader;
		private String minecraftVersion;
		private MinecraftCompat mc;
		private VoiceNetworkingCompat net;
		private IPlayerEvents events;

		public Builder loader(LoaderId loader) {
			this.loader = loader;
			return this;
		}

		public Builder minecraftVersion(String minecraftVersion) {
			this.minecraftVersion = minecraftVersion;
			return this;
		}

		public Builder minecraft(MinecraftCompat mc) {
			this.mc = mc;
			return this;
		}

		public Builder networking(VoiceNetworkingCompat net) {
			this.net = net;
			return this;
		}

		public Builder events(IPlayerEvents events) {
			this.events = events;
			return this;
		}

		public VoiceContext build() {
			if (loader == null) {
				throw new IllegalStateException("VoiceContext requires a loader");
			}
			if (minecraftVersion == null) {
				throw new IllegalStateException("VoiceContext requires a minecraftVersion");
			}
			if (mc == null) {
				throw new IllegalStateException("VoiceContext requires a MinecraftCompat");
			}
			if (net == null) {
				throw new IllegalStateException("VoiceContext requires a VoiceNetworkingCompat");
			}
			return new VoiceContext(this);
		}
	}
}
