package com.moyettes.legacyvoicechat.audio.filter;

public interface AudioFilter {
    String getName();
    short[] process(short[] samples);
    boolean isEnabled();
    int getSupportedChannels();
}
