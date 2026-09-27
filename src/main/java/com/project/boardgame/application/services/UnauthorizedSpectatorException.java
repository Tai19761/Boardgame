package com.project.boardgame.application.services;

public class UnauthorizedSpectatorException extends RuntimeException {
    public UnauthorizedSpectatorException(String message) {
        super(message);
    }
}
