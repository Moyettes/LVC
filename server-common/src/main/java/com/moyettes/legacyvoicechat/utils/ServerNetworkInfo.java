package com.moyettes.legacyvoicechat.utils;

import java.util.concurrent.atomic.AtomicReference;

public class ServerNetworkInfo {
    private static final AtomicReference<String> serverIP = new AtomicReference<>();
    private static final AtomicReference<Integer> serverPort = new AtomicReference<>();

    public static void setServerIP(String ip) {
        serverIP.set(ip);
    }

    public static String getServerIP() {
        return serverIP.get();
    }

    public static void setServerPort(int port) {
        serverPort.set(port);
    }

    public static Integer getServerPort() {
        return serverPort.get();
    }

    public static boolean hasServerInfo() {
        return serverIP.get() != null && serverPort.get() != null;
    }
}
