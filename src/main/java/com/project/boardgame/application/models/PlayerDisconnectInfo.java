package com.project.boardgame.application.models;

public record PlayerDisconnectInfo(
        String roomId,
        RoomStatus previousStatus,
        String disconnectedPlayerId,
        String opponentId
) {
}
