package com.moyettes.legacyvoicechat.packet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class VoiceServerInfoPayload implements CustomPayload {

	private int voiceServerPort;
	private String authToken;
	private String protocolVersion;
	private String serverHost;

	public VoiceServerInfoPayload() {
	}

	public VoiceServerInfoPayload(int voiceServerPort, String authToken, String protocolVersion, String serverHost) {
		this.voiceServerPort = voiceServerPort;
		this.authToken = authToken;
		this.protocolVersion = protocolVersion;
		this.serverHost = serverHost;
	}

	@Override
	public void read(DataInputStream input) throws IOException {
		this.voiceServerPort = input.readInt();
		this.authToken = input.readUTF();
		this.protocolVersion = input.readUTF();
		this.serverHost = input.readUTF();
	}

	@Override
	public void write(DataOutputStream output) throws IOException {
		output.writeInt(this.voiceServerPort);
		output.writeUTF(this.authToken);
		output.writeUTF(this.protocolVersion);
		output.writeUTF(this.serverHost);
	}

	public int getVoiceServerPort() {
		return voiceServerPort;
	}

	public String getAuthToken() {
		return authToken;
	}

	public String getProtocolVersion() {
		return protocolVersion;
	}

	public String getServerHost() {
		return serverHost;
	}
}
