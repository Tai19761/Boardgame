package com.project.boardgame.application.ports;

import com.project.boardgame.application.models.GameType;
import com.project.boardgame.domain.rules.GameRuleEngine;

public interface GameRuleEngineFactory {
    GameRuleEngine create(GameType gameType);
}
