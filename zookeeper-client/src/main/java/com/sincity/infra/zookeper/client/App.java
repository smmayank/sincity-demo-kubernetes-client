package com.sincity.infra.zookeper.client;

import com.google.common.base.Strings;
import com.google.common.base.Suppliers;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;


public class App implements Watcher {

    private static final Logger logger = LoggerFactory.getLogger(App.class);

    private static final int CONNECTION_TIMEOUT = 1000;

    private static Supplier<String> ZOO_KEEPER_URL = Suppliers.memoize(() -> {
        String zookeeperUrl = System.getenv("ZOO_KEEPER_URL");
        if (Strings.isNullOrEmpty(zookeeperUrl)) {
            return "localhost:2181";
        }
        return zookeeperUrl;
    });

    // While connecting assume connected
    private final AtomicBoolean alive = new AtomicBoolean(true);

    public static void main(String[] args) throws IOException, InterruptedException {
        App app = new App();
        app.init();
    }

    private void init() throws IOException, InterruptedException {
        logger.debug("zoo keeper url {}", ZOO_KEEPER_URL.get());
        ZooKeeper zooKeeper = new ZooKeeper(ZOO_KEEPER_URL.get(), CONNECTION_TIMEOUT, this);
        while (alive.get()) {
            synchronized (alive) {
                alive.wait(CONNECTION_TIMEOUT);
            }
        }
        logger.debug("Closing zoo keeper connection");
        zooKeeper.close();
    }


    @Override
    public void process(WatchedEvent event) {
        switch (event.getState()) {
            case Disconnected:
                logger.info("App is disconnected from zoo keeper cluster");
                terminateApp();
                break;
            case SyncConnected:
                logger.info("App is connected we are live and kicking");
                break;
            case AuthFailed:
                logger.info("App is Not connected we should be dying");
                terminateApp();
                break;
            case ConnectedReadOnly:
                logger.info("App is Not connected we should be dying");
                break;
            case SaslAuthenticated:
                logger.info("App is connected and authenticated dang it");
                break;
            case Expired:
                logger.info("Connection Expired let's lose the app");
                terminateApp();
                break;
        }
    }

    private void terminateApp() {
        alive.set(false);
        synchronized (alive) {
            alive.notify();
        }
    }
}
