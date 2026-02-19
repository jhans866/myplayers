package com.app.playerservicejava.config;

public class PlayerAlreadyExistsException extends RuntimeException {
    public PlayerAlreadyExistsException(String playerId) {
        super("Player already exists with id: " + playerId);
    }
}