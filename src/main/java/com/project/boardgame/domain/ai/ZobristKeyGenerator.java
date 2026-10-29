package com.project.boardgame.domain.ai;

import com.project.boardgame.domain.models.BoardState;
import com.project.boardgame.domain.models.Piece;
import com.project.boardgame.domain.models.Player;
import com.project.boardgame.domain.models.Position;

import java.util.Map;
import java.util.Objects;
import java.util.Random;

/**
 * Trình sinh khóa băm Zobrist cho Cờ Caro (hỗ trợ kích thước bàn tối đa 25x25).
 * <p>
 * Khởi tạo tĩnh bảng số ngẫu nhiên 64-bit với seed cố định để đảm bảo tính xác định (deterministic)
 * qua các lần chạy và môi trường kiểm thử.
 */
public final class ZobristKeyGenerator {

    public static final int MAX_BOARD_SIZE = 25;
    private static final long SEED = 0xCAFE_BABE_1234_5678L;

    private static final long[][][] PIECE_KEYS = new long[MAX_BOARD_SIZE][MAX_BOARD_SIZE][2];
    private static final long SIDE_TO_MOVE_KEY;

    static {
        Random random = new Random(SEED);
        for (int x = 0; x < MAX_BOARD_SIZE; x++) {
            for (int y = 0; y < MAX_BOARD_SIZE; y++) {
                PIECE_KEYS[x][y][0] = random.nextLong(); // PLAYER_1
                PIECE_KEYS[x][y][1] = random.nextLong(); // PLAYER_2
            }
        }
        SIDE_TO_MOVE_KEY = random.nextLong();
    }

    private ZobristKeyGenerator() {
        // Utility class
    }

    public static long getPieceKey(int x, int y, Player player) {
        if (x < 0 || x >= MAX_BOARD_SIZE || y < 0 || y >= MAX_BOARD_SIZE) {
            throw new IllegalArgumentException("Tọa độ ngoài phạm vi Zobrist (0..24): (" + x + ", " + y + ")");
        }
        Objects.requireNonNull(player, "player không được null");
        int playerIdx = player == Player.PLAYER_1 ? 0 : 1;
        return PIECE_KEYS[x][y][playerIdx];
    }

    public static long getPieceKey(Position pos, Player player) {
        Objects.requireNonNull(pos, "pos không được null");
        return getPieceKey(pos.x(), pos.y(), player);
    }

    public static long getSideToMoveKey() {
        return SIDE_TO_MOVE_KEY;
    }

    /**
     * Tính toán mã băm ban đầu từ trạng thái bàn cờ hiện tại.
     * Độ phức tạp O(K) với K là số quân cờ hiện có trên bàn.
     */
    public static long computeHash(BoardState board) {
        Objects.requireNonNull(board, "board không được null");
        long hash = 0L;
        for (Map.Entry<Position, Piece> entry : board.pieces().entrySet()) {
            Position pos = entry.getKey();
            Piece piece = entry.getValue();
            if (pos.x() >= 0 && pos.x() < MAX_BOARD_SIZE && pos.y() >= 0 && pos.y() < MAX_BOARD_SIZE) {
                hash ^= getPieceKey(pos.x(), pos.y(), piece.player());
            }
        }
        if (board.currentPlayer() == Player.PLAYER_2) {
            hash ^= SIDE_TO_MOVE_KEY;
        }
        return hash;
    }

    /**
     * Cập nhật gia tăng O(1) khi đặt quân vào ô (x, y).
     */
    public static long updateMove(long currentHash, Position pos, Player player) {
        return currentHash ^ getPieceKey(pos, player) ^ SIDE_TO_MOVE_KEY;
    }
}
