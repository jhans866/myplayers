package com.app.playerservicejava.config;

public class PlayerNotFoundException extends RuntimeException {
    public PlayerNotFoundException(String playerId) {
        super("Player not found with id: " + playerId);
    }
}
