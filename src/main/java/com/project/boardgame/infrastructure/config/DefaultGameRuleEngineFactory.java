package com.project.boardgame.infrastructure.config;

import com.project.boardgame.application.models.GameType;
import com.project.boardgame.application.ports.GameRuleEngineFactory;
import com.project.boardgame.domain.rules.CaroRuleEngine;
import com.project.boardgame.domain.rules.GameRuleEngine;
import com.project.boardgame.domain.rules.GanhRuleEngine;
import com.project.boardgame.domain.rules.OAnQuanRuleEngine;

import org.springframework.stereotype.Component;

@Component
public class DefaultGameRuleEngineFactory implements GameRuleEngineFactory {

    @Override
    public GameRuleEngine create(GameType gameType, Integer boardSize) {
        return switch (gameType) {
            case CARO -> boardSize == null ? new CaroRuleEngine() : new CaroRuleEngine(boardSize);
            case GANH -> new GanhRuleEngine();
            case O_AN_QUAN -> new OAnQuanRuleEngine();
        };
    }
}
