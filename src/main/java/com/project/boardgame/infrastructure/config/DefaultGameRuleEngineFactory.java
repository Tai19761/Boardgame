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
    public GameRuleEngine create(GameType gameType) {
        return switch (gameType) {
            case CARO -> new CaroRuleEngine();
            case GANH -> new GanhRuleEngine();
            case O_AN_QUAN -> new OAnQuanRuleEngine();
        };
    }
}
