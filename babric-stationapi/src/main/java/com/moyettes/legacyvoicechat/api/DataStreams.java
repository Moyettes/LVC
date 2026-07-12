package com.moyettes.legacyvoicechat.api;

import java.io.*;

public final class DataStreams {

    public static DataInputStream input(byte[] bytes) {
        return new DataInputStream(new ByteArrayInputStream(bytes == null ? new byte[0] : bytes));
    }

    public static ByteArrayOutputStream output(IOConsumer<DataOutputStream> writer) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream os = new DataOutputStream(bos);
        writer.accept(os);
        return bos;
    }
}