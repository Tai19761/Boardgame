package com.project.boardgame.infrastructure.web.websocket.dto;

import com.project.boardgame.application.models.GameType;

/**
 * @param boardSize kích thước bàn Caro do client đề xuất (15/20/25). Server sẽ validate lại — KHÔNG tin client.
 */
public record CreateRoomRequest(GameType gameType, Integer boardSize) {

    /** Tương thích ngược: không chỉ định kích thước (server dùng mặc định). */
    public CreateRoomRequest(GameType gameType) {
        this(gameType, null);
    }
}
