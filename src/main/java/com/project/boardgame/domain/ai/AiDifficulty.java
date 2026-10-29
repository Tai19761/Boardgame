package com.project.boardgame.domain.ai;

/**
 * 4 cấp độ khó cho AI Cờ Caro:
 * - EASY: Phù hợp người mới, ưu tiên chặn thua/ăn thắng ngay, còn lại đi ngẫu nhiên trong top nước đi khả quan.
 * - NORMAL: Cấu hình chuẩn (Depth = 3), đánh chắc chắn và phản hồi nhanh.
 * - HARD: Cấu hình nâng cao (Depth = 4), tìm kiếm sâu hơn với Move Ordering.
 * - EXPERT: Cấu hình cực khó (Depth = 5), kích hoạt Transposition Table (Zobrist Hashing) và cắt nhánh tối ưu.
 */
public enum AiDifficulty {
    EASY,
    NORMAL,
    HARD,
    EXPERT
}
