package com.moyettes.legacyvoicechat.api;

import java.io.IOException;

public interface IOConsumer<T> {

    void accept(T t) throws IOException;

}
