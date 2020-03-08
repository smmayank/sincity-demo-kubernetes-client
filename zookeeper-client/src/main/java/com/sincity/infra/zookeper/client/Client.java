package com.sincity.infra.zookeper.client;

import org.apache.zookeeper.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.sincity.infra.zookeper.client.Constants.*;

public class Client implements Watcher {

    private static final Logger logger = LoggerFactory.getLogger(Client.class);

    // While connecting assume connected
    private final AtomicBoolean alive = new AtomicBoolean(true);

    // While connecting assume connected
    private final AtomicBoolean connected = new AtomicBoolean(false);

    private ZooKeeper zooKeeper;

    private String currentNode;

    public void watch() throws ClientException {
        logger.debug("zoo keeper url {}", ZOO_KEEPER_URL.get());
        try {
            zooKeeper = new ZooKeeper(ZOO_KEEPER_URL.get(), CONNECTION_TIMEOUT, this);
        } catch (IOException e) {
            throw new ClientException("Cannot create zookeeper connection", e);
        }
    }

    private void isInitialized() throws ClientException {
        if (Objects.isNull(zooKeeper)) {
            throw new ClientException("Client is not initialized call watch first");
        }
    }

    public void electionResult() throws ClientException {
        isInitialized();
        List<String> children;
        try {
            children = zooKeeper.getChildren(NAMESPACE, false);
        } catch (KeeperException | InterruptedException e) {
            throw new ClientException("Unable to fetch children", e);
        }
        children.sort(String::compareTo);
        String leaderNode = children.stream().findFirst().orElseThrow(() -> new ClientException("No children found"));
        if (Objects.equals(leaderNode, currentNode)) {
            logger.info("I'm the leader");
        } else {
            logger.info("I'm not the leader, but the leader is {} is", leaderNode);
        }
    }

    public void applyCandidacy() throws ClientException {
        isInitialized();
        while (!connected.get()) {
            synchronized (connected) {
                try {
                    connected.wait(CONNECTION_TIMEOUT);
                } catch (InterruptedException e) {
                    throw new ClientException("Cannot wait further for connection to start", e);
                }
            }
        }
        try {
            currentNode = zooKeeper.create(Z_NODE_PREFIX, new byte[]{}, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
            currentNode = currentNode.replace(NAMESPACE_STRIP_CONSTANT, "");
            logger.info("I'm connected as node {}", currentNode);
        } catch (KeeperException | InterruptedException e) {
            throw new ClientException("Unable to apply for leadership", e);
        }
    }

    public void waitForTermination() throws ClientException {
        isInitialized();
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
                connected.set(true);
                synchronized (connected) {
                    connected.notify();
                }
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
