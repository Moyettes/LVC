package com.moyettes.legacyvoicechat.packet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class VoiceModSupportedPayload implements CustomPayload {

	private String protocolVersion;

	public VoiceModSupportedPayload() {
	}

	public VoiceModSupportedPayload(String protocolVersion) {
		this.protocolVersion = protocolVersion;
	}

	@Override
	public void read(DataInputStream input) throws IOException {
		this.protocolVersion = input.readUTF();
	}

	@Override
	public void write(DataOutputStream output) throws IOException {
		output.writeUTF(this.protocolVersion);
	}

	public String getProtocolVersion() {
		return protocolVersion;
	}
}
