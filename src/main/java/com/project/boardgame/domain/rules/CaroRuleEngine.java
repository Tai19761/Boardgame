package com.project.boardgame.domain.rules;

import com.project.boardgame.domain.models.BoardState;
import com.project.boardgame.domain.models.MoveStep;
import com.project.boardgame.domain.models.Piece;
import com.project.boardgame.domain.models.Player;
import com.project.boardgame.domain.models.Position;

import java.util.HashMap;
import java.util.Map;

public class CaroRuleEngine implements GameRuleEngine {

    @Override
    public boolean isValidMove(BoardState board, MoveStep move, Player player) {
        Position to = move.to();
        if (to == null) {
            return false;
        }
        
        if (!isWithinBounds(to)) {
            return false;
        }
        
        // Kiểm tra nước đi hợp lệ (chỉ được đặt quân vào ô trống)
        return !board.pieces().containsKey(to);
    }

    private boolean isWithinBounds(Position p) {
        return p.x() >= 0 && p.x() <= 14 && p.y() >= 0 && p.y() <= 14;
    }

    @Override
    public BoardState executeMove(BoardState board, MoveStep move, Player player) {
        if (!isValidMove(board, move, player)) {
            throw new IllegalArgumentException("Invalid move: Cell is not empty");
        }
        Map<Position, Piece> newPieces = new HashMap<>(board.pieces());
        newPieces.put(move.to(), new Piece(player));
        return new BoardState(newPieces);
    }

    /**
     * Kiểm tra điều kiện thắng (có 5 quân liên tiếp trên cùng hàng ngang, dọc hoặc chéo)
     */
    @Override
    public java.util.Optional<Player> checkWinner(BoardState board, Position lastMove) {
        if (lastMove == null || board.pieces().get(lastMove) == null) {
            return java.util.Optional.empty();
        }

        Player player = board.pieces().get(lastMove).player();

        int[][] directions = {
            {1, 0},  // Ngang
            {0, 1},  // Dọc
            {1, 1},  // Chéo chính
            {1, -1}  // Chéo phụ
        };

        for (int[] dir : directions) {
            int count = 1; // Tính cả quân vừa đánh
            count += countPiecesInDirection(board, lastMove, player, dir[0], dir[1]);
            count += countPiecesInDirection(board, lastMove, player, -dir[0], -dir[1]);

            if (count >= 5) {
                return java.util.Optional.of(player);
            }
        }
        return java.util.Optional.empty();
    }

    private int countPiecesInDirection(BoardState board, Position start, Player player, int dx, int dy) {
        int count = 0;
        int x = start.x() + dx;
        int y = start.y() + dy;

        while (true) {
            Position p = new Position(x, y);
            Piece piece = board.pieces().get(p);
            if (piece != null && piece.player() == player) {
                count++;
                x += dx;
                y += dy;
            } else {
                break;
            }
        }
        return count;
    }
}
