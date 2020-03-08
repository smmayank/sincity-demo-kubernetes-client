package com.sincity.infra.zookeper.client;

public class ClientException extends Exception {

    public ClientException(String message, Throwable cause) {
        super(message, cause);
    }

    public ClientException(String message) {
        this(message, null);
    }
}
