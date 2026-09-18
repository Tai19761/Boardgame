package com.project.boardgame.domain.rules;

import com.project.boardgame.domain.models.BoardState;
import com.project.boardgame.domain.models.MoveStep;
import com.project.boardgame.domain.models.Piece;
import com.project.boardgame.domain.models.Player;
import com.project.boardgame.domain.models.Position;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class CaroRuleEngine implements GameRuleEngine {

    public static final int BOARD_SIZE = 15;

    @Override
    public boolean isValidMove(BoardState board, MoveStep move, Player player) {
        if (player != board.currentPlayer()) {
            return false;
        }
        
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
        return p.x() >= 0 && p.x() < BOARD_SIZE && p.y() >= 0 && p.y() < BOARD_SIZE;
    }

    @Override
    public BoardState executeMove(BoardState board, MoveStep move, Player player) {
        if (!isValidMove(board, move, player)) {
            throw new IllegalArgumentException("Invalid move: Cell is not empty");
        }
        Map<Position, Piece> newPieces = new HashMap<>(board.pieces());
        newPieces.put(move.to(), new Piece(player));
        Player nextPlayer = player == Player.PLAYER_1 ? Player.PLAYER_2 : Player.PLAYER_1;
        return new BoardState(newPieces, nextPlayer);
    }

    @Override
    public boolean isGameOver(BoardState board) {
        if (board.pieces().size() >= BOARD_SIZE * BOARD_SIZE) {
            return true; // Bàn cờ đầy (Hòa nếu không ai thắng)
        }
        for (Position pos : board.pieces().keySet()) {
            if (checkWinner(board, pos).isPresent()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Kiểm tra điều kiện thắng (có 5 quân liên tiếp trên cùng hàng ngang, dọc hoặc chéo)
     */
    @Override
    public Optional<Player> checkWinner(BoardState board, Position lastMove) {
        if (lastMove == null || board.pieces().get(lastMove) == null) {
            // Kết quả Optional.empty() chỉ có ý nghĩa xác định là 'Hòa' khi isGameOver(board) == true.
            // Nếu isGameOver(board) == false, ván đấu vẫn đang tiếp diễn (ONGOING).
            return Optional.empty();
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
                return Optional.of(player);
            }
        }
        // Kết quả Optional.empty() chỉ có ý nghĩa xác định là 'Hòa' khi isGameOver(board) == true.
        // Nếu isGameOver(board) == false, ván đấu vẫn đang tiếp diễn (ONGOING).
        return Optional.empty();
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
