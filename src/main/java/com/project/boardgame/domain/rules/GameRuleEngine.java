package com.project.boardgame.domain.rules;

import com.project.boardgame.domain.models.BoardState;
import com.project.boardgame.domain.models.MoveStep;
import com.project.boardgame.domain.models.Player;
import com.project.boardgame.domain.models.Position;

public interface GameRuleEngine {
    boolean isValidMove(BoardState board, MoveStep move, Player player);
    BoardState executeMove(BoardState board, MoveStep move, Player player);
    java.util.Optional<Player> checkWinner(BoardState board, Position lastMove);
}
