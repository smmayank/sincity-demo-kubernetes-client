package com.sincity.infra.zookeper.client;

import com.google.common.base.Strings;
import com.google.common.base.Suppliers;

import java.util.function.Supplier;

public final class Constants {
    public static final int CONNECTION_TIMEOUT = 1000;

    public static final Supplier<String> ZOO_KEEPER_URL = Suppliers.memoize(() -> {
        String zookeeperUrl = System.getenv("ZOO_KEEPER_URL");
        if (Strings.isNullOrEmpty(zookeeperUrl)) {
            return "localhost:2181";
        }
        return zookeeperUrl;
    });
}
