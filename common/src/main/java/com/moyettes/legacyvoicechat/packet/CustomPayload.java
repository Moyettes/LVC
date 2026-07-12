package com.moyettes.legacyvoicechat.packet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public interface CustomPayload {
	void read(DataInputStream input) throws IOException;
	void write(DataOutputStream output) throws IOException;
}
