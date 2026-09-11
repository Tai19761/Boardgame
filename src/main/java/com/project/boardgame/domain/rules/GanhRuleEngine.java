package com.project.boardgame.domain.rules;

import com.project.boardgame.domain.models.BoardState;
import com.project.boardgame.domain.models.MoveStep;
import com.project.boardgame.domain.models.Piece;
import com.project.boardgame.domain.models.Player;
import com.project.boardgame.domain.models.Position;

public class GanhRuleEngine implements GameRuleEngine {

    @Override
    public boolean isValidMove(BoardState board, MoveStep move, Player player) {
        Position from = move.from();
        Position to = move.to();

        // Check bounds
        if (!isWithinBounds(from) || !isWithinBounds(to)) {
            return false;
        }

        // Check from position belongs to player
        Piece pieceAtFrom = board.pieces().get(from);
        if (pieceAtFrom == null || pieceAtFrom.player() != player) {
            return false;
        }

        // Check to position is empty
        if (board.pieces().containsKey(to)) {
            return false;
        }

        // Check if move is adjacent
        int dx = Math.abs(to.x() - from.x());
        int dy = Math.abs(to.y() - from.y());

        if (dx > 1 || dy > 1 || (dx == 0 && dy == 0)) {
            return false; // Moving more than 1 step or not moving
        }

        if (dx == 1 && dy == 1) {
            // Diagonal move is only valid if (x + y) is even
            if ((from.x() + from.y()) % 2 != 0) {
                return false;
            }
        }

        return true;
    }

    private boolean isWithinBounds(Position p) {
        return p.x() >= 0 && p.x() <= 4 && p.y() >= 0 && p.y() <= 4;
    }
}
