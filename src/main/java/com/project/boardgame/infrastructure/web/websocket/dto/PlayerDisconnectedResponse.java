package com.project.boardgame.infrastructure.web.websocket.dto;

public record PlayerDisconnectedResponse(
        String type,
        String roomId,
        String disconnectedPlayerId,
        String message
) {

    public PlayerDisconnectedResponse(String roomId, String disconnectedPlayerId) {
        this("PLAYER_DISCONNECTED", roomId, disconnectedPlayerId, "Đối thủ đã ngắt kết nối");
    }
}
