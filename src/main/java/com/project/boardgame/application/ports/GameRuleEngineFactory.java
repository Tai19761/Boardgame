package com.project.boardgame.application.ports;

import com.project.boardgame.application.models.GameType;
import com.project.boardgame.domain.rules.GameRuleEngine;

public interface GameRuleEngineFactory {

    /**
     * Tạo rule engine cho loại game.
     *
     * @param boardSize kích thước bàn (chỉ áp dụng cho CARO; null = mặc định). Các game khác bỏ qua tham số này.
     */
    GameRuleEngine create(GameType gameType, Integer boardSize);

    /** Tương thích ngược: tạo rule engine với cấu hình mặc định. */
    default GameRuleEngine create(GameType gameType) {
        return create(gameType, null);
    }
}
