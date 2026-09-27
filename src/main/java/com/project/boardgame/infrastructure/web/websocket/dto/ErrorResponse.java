package com.project.boardgame.infrastructure.web.websocket.dto;

public record ErrorResponse(String message, String errorType) {

    public ErrorResponse(String message) {
        this(message, "ERROR");
    }
}
