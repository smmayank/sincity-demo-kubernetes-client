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

    public static final String NAMESPACE = "/demo";
    public static final String NAMESPACE_STRIP_CONSTANT = NAMESPACE + '/';
    public static final String Z_NODE_PREFIX = NAMESPACE + "/c_";

}
