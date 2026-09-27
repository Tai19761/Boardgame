package com.project.boardgame.infrastructure.web.websocket.dto;

import com.project.boardgame.application.models.GameType;

public record CreateRoomRequest(GameType gameType) {
}
