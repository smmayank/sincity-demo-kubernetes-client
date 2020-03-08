package com.sincity.infra.zookeper.client;

public class ClientException extends RuntimeException {
    public ClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
