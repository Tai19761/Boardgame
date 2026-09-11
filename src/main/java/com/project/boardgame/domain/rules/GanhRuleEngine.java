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

    @Override
    public BoardState executeMove(BoardState board, MoveStep move, Player player) {
        if (!isValidMove(board, move, player)) {
            throw new IllegalArgumentException("Invalid move");
        }

        java.util.Map<Position, Piece> newPieces = new java.util.HashMap<>(board.pieces());
        
        // Move piece
        newPieces.remove(move.from());
        newPieces.put(move.to(), new Piece(player));

        Position to = move.to();
        Player opponent = (player == Player.PLAYER_1) ? Player.PLAYER_2 : Player.PLAYER_1;

        // Check horizontal Ganh
        checkAndApplyGanh(newPieces, player, opponent, 
            new Position(to.x() - 1, to.y()), 
            new Position(to.x() + 1, to.y()));

        // Check vertical Ganh
        checkAndApplyGanh(newPieces, player, opponent, 
            new Position(to.x(), to.y() - 1), 
            new Position(to.x(), to.y() + 1));

        // Check diagonal Ganh (only if target cell has diagonals)
        if ((to.x() + to.y()) % 2 == 0) {
            checkAndApplyGanh(newPieces, player, opponent, 
                new Position(to.x() - 1, to.y() - 1), 
                new Position(to.x() + 1, to.y() + 1));
            
            checkAndApplyGanh(newPieces, player, opponent, 
                new Position(to.x() - 1, to.y() + 1), 
                new Position(to.x() + 1, to.y() - 1));
        }

        return new BoardState(newPieces);
    }

    private void checkAndApplyGanh(java.util.Map<Position, Piece> pieces, Player player, Player opponent, Position p1, Position p2) {
        if (isWithinBounds(p1) && isWithinBounds(p2)) {
            Piece piece1 = pieces.get(p1);
            Piece piece2 = pieces.get(p2);
            
            if (piece1 != null && piece1.player() == opponent && 
                piece2 != null && piece2.player() == opponent) {
                // Apply Ganh: convert opponent pieces
                pieces.put(p1, new Piece(player));
                pieces.put(p2, new Piece(player));
            }
        }
    }

    private boolean isWithinBounds(Position p) {
        return p.x() >= 0 && p.x() <= 4 && p.y() >= 0 && p.y() <= 4;
    }
}
