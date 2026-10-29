package com.project.boardgame.domain.ai;

import java.util.Objects;

/**
 * Bảng băm Transposition Table (TT) lưu kết quả tìm kiếm Minimax dựa trên khóa Zobrist.
 * <p>
 * Kích thước cố định {@value #DEFAULT_SIZE} entries (khoảng ~2MB RAM), truy xuất bằng bitmask O(1).
 * Khởi tạo theo từng phiên tìm kiếm (per-search) để đảm bảo an toàn đa luồng giữa các phòng.
 */
public class TranspositionTable {

    public static final int DEFAULT_SIZE = 65536; // 2^16
    private final int mask;
    private final Entry[] table;

    public enum Flag {
        EXACT,       // Giá trị chính xác (alpha < score < beta)
        LOWER_BOUND, // Cắt tỉa Beta (fail-high, score >= beta)
        UPPER_BOUND  // Cắt tỉa Alpha (fail-low, score <= alpha)
    }

    public record Entry(long zobristKey, int depth, int score, Flag flag) {
        public Entry {
            Objects.requireNonNull(flag, "flag không được null");
        }
    }

    public TranspositionTable() {
        this(DEFAULT_SIZE);
    }

    public TranspositionTable(int size) {
        if (size <= 0 || (size & (size - 1)) != 0) {
            throw new IllegalArgumentException("Kích thước bảng TT phải là lũy thừa của 2 và > 0");
        }
        this.mask = size - 1;
        this.table = new Entry[size];
    }

    private int getIndex(long key) {
        return (int) (key & mask);
    }

    /**
     * Lấy entry từ bảng nếu khóa khớp.
     */
    public Entry get(long key) {
        int index = getIndex(key);
        Entry entry = table[index];
        if (entry != null && entry.zobristKey() == key) {
            return entry;
        }
        return null;
    }

    /**
     * Tra cứu xem entry có đủ điều kiện cắt nhánh với (remainingDepth, alpha, beta) hiện tại hay không.
     * Trả về điểm số nếu có thể cắt tỉa ngay, hoặc null nếu không dùng được.
     */
    public Integer lookup(long key, int remainingDepth, int alpha, int beta) {
        Entry entry = get(key);
        if (entry == null || entry.depth() < remainingDepth) {
            return null;
        }

        if (entry.flag() == Flag.EXACT) {
            return entry.score();
        }
        if (entry.flag() == Flag.LOWER_BOUND && entry.score() >= beta) {
            return entry.score();
        }
        if (entry.flag() == Flag.UPPER_BOUND && entry.score() <= alpha) {
            return entry.score();
        }
        return null;
    }

    /**
     * Lưu kết quả vào bảng theo chiến lược Depth-preferred replacement:
     * Ghi đè nếu ô trống, cùng khóa, hoặc depth của trạng thái mới lớn hơn/bằng depth cũ.
     */
    public void store(long key, int depth, int score, Flag flag) {
        int index = getIndex(key);
        Entry existing = table[index];
        if (existing == null || existing.zobristKey() == key || depth >= existing.depth()) {
            table[index] = new Entry(key, depth, score, flag);
        }
    }

    public void clear() {
        for (int i = 0; i < table.length; i++) {
            table[i] = null;
        }
    }

    public int capacity() {
        return table.length;
    }
}
