package com.moyettes.legacyvoicechat.audio.codec;

public enum OpusMode {
    VOIP(2048),
    AUDIO(2049),
    RESTRICTED_LOWDELAY(2051);

    private final int application;

    OpusMode(int application) {
        this.application = application;
    }

    public int getApplication() {
        return application;
    }
}
