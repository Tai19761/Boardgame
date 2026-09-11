package com.project.boardgame.domain.rules;

import com.project.boardgame.domain.models.BoardState;
import com.project.boardgame.domain.models.MoveStep;
import com.project.boardgame.domain.models.Player;

public interface GameRuleEngine {
    boolean isValidMove(BoardState board, MoveStep move, Player player);
}
