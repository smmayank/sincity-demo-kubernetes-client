package com.sincity.infra.zookeper.client;

import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.sincity.infra.zookeper.client.Constants.CONNECTION_TIMEOUT;
import static com.sincity.infra.zookeper.client.Constants.ZOO_KEEPER_URL;

public class Client implements Watcher {

    private static final Logger logger = LoggerFactory.getLogger(App.class);

    // While connecting assume connected
    private final AtomicBoolean alive = new AtomicBoolean(true);

    public void watchAndWait() throws ClientException {
        logger.debug("zoo keeper url {}", ZOO_KEEPER_URL.get());
        ZooKeeper zooKeeper;
        try {
            zooKeeper = new ZooKeeper(ZOO_KEEPER_URL.get(), CONNECTION_TIMEOUT, this);
        } catch (IOException e) {
            throw new ClientException("Cannot create zookeeper connection", e);
        }
        while (alive.get()) {
            synchronized (alive) {
                try {
                    alive.wait(CONNECTION_TIMEOUT);
                } catch (InterruptedException e) {
                    throw new ClientException("Cannot wait further", e);
                }
            }
        }
        try {
            zooKeeper.close();
        } catch (InterruptedException e) {
            throw new ClientException("Cannot close zookeeper connection", e);
        }
    }

    @Override
    public void process(WatchedEvent event) {
        switch (event.getState()) {
            case Disconnected:
                logger.info("App is disconnected from zoo keeper cluster");
                connectionTerminated();
                break;
            case SyncConnected:
                logger.info("App is connected we are live and kicking");
                break;
            case AuthFailed:
                logger.info("App is Not connected we should be dying");
                connectionTerminated();
                break;
            case ConnectedReadOnly:
                logger.info("App is Not connected we should be dying");
                break;
            case SaslAuthenticated:
                logger.info("App is connected and authenticated dang it");
                break;
            case Expired:
                logger.info("Connection Expired let's lose the app");
                connectionTerminated();
                break;
        }
    }

    private void connectionTerminated() {
        alive.set(false);
        synchronized (alive) {
            alive.notify();
        }
    }

}
