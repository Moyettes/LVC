package com.moyettes.legacyvoicechat.client;

public final class VoiceSession {

	private static volatile VoiceClient active;

	private VoiceSession() {
	}

	public static void setActive(VoiceClient client) {
		active = client;
	}

	public static VoiceClient getActive() {
		return active;
	}

	public static boolean isInGroup() {
		VoiceClient c = active;
		return c != null && c.isInGroup();
	}

	public static void createGroup(String name, String password) {
		VoiceClient c = active;
		if (c != null) {
			c.createGroup(name, password);
		}
	}

	public static void joinGroup(String name, String password) {
		VoiceClient c = active;
		if (c != null) {
			c.joinGroup(name, password);
		}
	}

	public static void leaveGroup() {
		VoiceClient c = active;
		if (c != null) {
			c.leaveGroup();
		}
	}

	public static void requestGroupList() {
		VoiceClient c = active;
		if (c != null) {
			c.requestGroupList();
		}
	}
}
