package com.project.boardgame.infrastructure.web.websocket.dto;

import com.project.boardgame.application.models.GameType;
import com.project.boardgame.domain.ai.AiDifficulty;

/**
 * @param boardSize kích thước bàn Caro do client đề xuất (15/20/25). Server sẽ validate lại — KHÔNG tin client.
 * @param difficulty cấp độ khó khi tạo phòng Đấu với Máy (EASY, NORMAL, HARD, EXPERT).
 */
public record CreateRoomRequest(GameType gameType, Integer boardSize, AiDifficulty difficulty) {

    /** Tương thích ngược: không chỉ định kích thước và độ khó (server dùng mặc định). */
    public CreateRoomRequest(GameType gameType) {
        this(gameType, null, null);
    }

    /** Tương thích ngược: chỉ định kích thước, độ khó mặc định. */
    public CreateRoomRequest(GameType gameType, Integer boardSize) {
        this(gameType, boardSize, null);
    }
}
