package com.sincity.infra.zookeper.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class App {
    private static final Logger logger = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) {
        Client client = new Client();
        client.watchAndWait();
    }
}
