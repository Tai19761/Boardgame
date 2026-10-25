package com.project.boardgame.domain.ai;

import com.project.boardgame.domain.models.BoardState;
import com.project.boardgame.domain.models.Piece;
import com.project.boardgame.domain.models.Player;
import com.project.boardgame.domain.models.Position;

import java.util.Map;

/**
 * Hàm đánh giá heuristic cho Cờ Caro (Java thuần, không phụ thuộc framework).
 * <p>
 * Duyệt mọi chuỗi quân liên tiếp theo 4 hướng (ngang, dọc, 2 chéo), chấm điểm theo độ dài
 * và số đầu mở. Kết quả = tổng điểm của {@code me} - tổng điểm của đối thủ.
 */
public class CaroHeuristicEvaluator {

    public static final int FIVE_SCORE = 1_000_000;
    static final int OPEN_FOUR = 100_000;
    static final int HALF_FOUR = 10_000;
    static final int OPEN_THREE = 5_000;
    static final int HALF_THREE = 500;
    static final int OPEN_TWO = 200;
    static final int HALF_TWO = 20;
    static final int OPEN_ONE = 10;
    static final int HALF_ONE = 1;

    /** Trọng số cho việc sắp xếp nước đi (theo số quân liên tiếp sẽ tạo/chặn được). */
    private static final int[] MOVE_WEIGHTS = {0, 1, 10, 100, 1_000, 100_000};

    private static final int[][] DIRECTIONS = {
            {1, 0},  // Ngang
            {0, 1},  // Dọc
            {1, 1},  // Chéo chính
            {1, -1}  // Chéo phụ
    };

    private final int boardSize;

    public CaroHeuristicEvaluator(int boardSize) {
        if (boardSize <= 0) {
            throw new IllegalArgumentException("boardSize phải > 0");
        }
        this.boardSize = boardSize;
    }

    /**
     * Đánh giá thế cờ từ góc nhìn của {@code me}: dương là có lợi cho {@code me}, âm là có lợi cho đối thủ.
     */
    public int evaluate(BoardState board, Player me) {
        Map<Position, Piece> pieces = board.pieces();
        long mine = 0;
        long theirs = 0;

        for (Map.Entry<Position, Piece> entry : pieces.entrySet()) {
            Position start = entry.getKey();
            Player owner = entry.getValue().player();

            for (int[] dir : DIRECTIONS) {
                int dx = dir[0];
                int dy = dir[1];

                // Chỉ chấm điểm từ quân ĐẦU chuỗi để không đếm trùng
                Position before = new Position(start.x() - dx, start.y() - dy);
                if (ownerAt(pieces, before) == owner) {
                    continue;
                }

                int length = 1;
                while (ownerAt(pieces, new Position(start.x() + length * dx, start.y() + length * dy)) == owner) {
                    length++;
                }
                Position after = new Position(start.x() + length * dx, start.y() + length * dy);

                int openEnds = (isEmptyCell(pieces, before) ? 1 : 0) + (isEmptyCell(pieces, after) ? 1 : 0);
                int score = scoreLine(length, openEnds);

                if (owner == me) {
                    mine += score;
                } else {
                    theirs += score;
                }
            }
        }

        long diff = mine - theirs;
        return (int) Math.max(Integer.MIN_VALUE / 2, Math.min(Integer.MAX_VALUE / 2, diff));
    }

    /**
     * Chấm điểm nhanh một ô trống để sắp xếp thứ tự duyệt (move ordering) cho Alpha-Beta:
     * ô vừa giúp {@code mover} nối dài chuỗi (tấn công) vừa chặn chuỗi đối thủ (phòng thủ) được ưu tiên.
     */
    public int quickMoveScore(BoardState board, Position cell, Player mover) {
        Map<Position, Piece> pieces = board.pieces();
        Player opponent = mover == Player.PLAYER_1 ? Player.PLAYER_2 : Player.PLAYER_1;
        int score = 0;
        for (int[] dir : DIRECTIONS) {
            int attack = countRun(pieces, cell, mover, dir[0], dir[1]) + countRun(pieces, cell, mover, -dir[0], -dir[1]);
            int defense = countRun(pieces, cell, opponent, dir[0], dir[1]) + countRun(pieces, cell, opponent, -dir[0], -dir[1]);
            // Tấn công ưu tiên hơn phòng thủ cùng cấp (ví dụ: tự tạo 5 quân > chặn 4 quân)
            score += MOVE_WEIGHTS[Math.min(attack + 1, 5)] * 2;
            score += MOVE_WEIGHTS[Math.min(defense + 1, 5)];
        }
        return score;
    }

    private int countRun(Map<Position, Piece> pieces, Position from, Player player, int dx, int dy) {
        int count = 0;
        int x = from.x() + dx;
        int y = from.y() + dy;
        while (ownerAt(pieces, new Position(x, y)) == player) {
            count++;
            x += dx;
            y += dy;
        }
        return count;
    }

    static int scoreLine(int length, int openEnds) {
        if (length >= 5) {
            return FIVE_SCORE;
        }
        if (openEnds == 0) {
            return 0; // Chuỗi bị chặn 2 đầu không còn khả năng thành 5
        }
        boolean open = openEnds == 2;
        return switch (length) {
            case 4 -> open ? OPEN_FOUR : HALF_FOUR;
            case 3 -> open ? OPEN_THREE : HALF_THREE;
            case 2 -> open ? OPEN_TWO : HALF_TWO;
            default -> open ? OPEN_ONE : HALF_ONE;
        };
    }

    private Player ownerAt(Map<Position, Piece> pieces, Position p) {
        Piece piece = pieces.get(p);
        return piece == null ? null : piece.player();
    }

    private boolean isEmptyCell(Map<Position, Piece> pieces, Position p) {
        return isWithinBounds(p) && !pieces.containsKey(p);
    }

    private boolean isWithinBounds(Position p) {
        return p.x() >= 0 && p.x() < boardSize && p.y() >= 0 && p.y() < boardSize;
    }
}
