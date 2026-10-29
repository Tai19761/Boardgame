package com.project.boardgame.domain.ai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TranspositionTable Unit Tests")
class TranspositionTableTest {

    private TranspositionTable tt;

    @BeforeEach
    void setUp() {
        tt = new TranspositionTable(1024); // Sử dụng bảng nhỏ 1024 cho unit test
    }

    @Test
    @DisplayName("Lưu và tra cứu EXACT: trả về điểm số chính xác nếu depth đủ lớn")
    void testStoreAndLookup_Exact() {
        long key = 123456789L;
        tt.store(key, 3, 500, TranspositionTable.Flag.EXACT);

        // Tra cứu với depth = 3 -> khớp
        Integer score = tt.lookup(key, 3, -1000, 1000);
        assertNotNull(score);
        assertEquals(500, score);

        // Tra cứu với depth = 2 (nông hơn depth đã lưu) -> vẫn dùng được
        Integer scoreShallower = tt.lookup(key, 2, -1000, 1000);
        assertEquals(500, scoreShallower);

        // Tra cứu với depth = 4 (sâu hơn depth đã lưu) -> không dùng được (trả về null)
        Integer scoreDeeper = tt.lookup(key, 4, -1000, 1000);
        assertNull(scoreDeeper);
    }

    @Test
    @DisplayName("Lưu và tra cứu LOWER_BOUND (Beta Cutoff): chỉ trả về điểm khi score >= beta")
    void testLookup_LowerBound_BetaCutoff() {
        long key = 987654321L;
        tt.store(key, 4, 300, TranspositionTable.Flag.LOWER_BOUND);

        // beta = 200 (score >= beta) -> thỏa điều kiện cắt tỉa Beta
        Integer cutoffScore = tt.lookup(key, 4, -500, 200);
        assertEquals(300, cutoffScore);

        // beta = 400 (score < beta) -> không đủ điều kiện cắt tỉa
        Integer noCutoff = tt.lookup(key, 4, -500, 400);
        assertNull(noCutoff);
    }

    @Test
    @DisplayName("Lưu và tra cứu UPPER_BOUND (Alpha Cutoff): chỉ trả về điểm khi score <= alpha")
    void testLookup_UpperBound_AlphaCutoff() {
        long key = 555666777L;
        tt.store(key, 4, -200, TranspositionTable.Flag.UPPER_BOUND);

        // alpha = -100 (score <= alpha) -> thỏa điều kiện cắt tỉa Alpha
        Integer cutoffScore = tt.lookup(key, 4, -100, 500);
        assertEquals(-200, cutoffScore);

        // alpha = -300 (score > alpha) -> không đủ điều kiện cắt tỉa
        Integer noCutoff = tt.lookup(key, 4, -300, 500);
        assertNull(noCutoff);
    }

    @Test
    @DisplayName("Chiến lược ghi đè Depth-preferred: giữ lại entry có depth lớn hơn khi đụng độ index")
    void testReplacementStrategy_PrefersDeeperDepth() {
        int size = tt.capacity();
        long key1 = 100L;
        long key2 = 100L + size; // key2 cùng index với key1 (cùng bitmask)

        // Lưu key1 với depth 4
        tt.store(key1, 4, 100, TranspositionTable.Flag.EXACT);
        assertEquals(100, tt.get(key1).score());

        // Cố tình ghi đè bằng key2 nhưng depth chỉ là 2 (nông hơn) -> bị từ chối
        tt.store(key2, 2, 200, TranspositionTable.Flag.EXACT);
        assertNotNull(tt.get(key1), "Entry sâu hơn (depth 4) phải được giữ lại");
        assertNull(tt.get(key2));

        // Ghi đè bằng key2 với depth = 5 (sâu hơn) -> thành công
        tt.store(key2, 5, 300, TranspositionTable.Flag.EXACT);
        assertNull(tt.get(key1));
        assertNotNull(tt.get(key2));
        assertEquals(300, tt.get(key2).score());
    }

    @Test
    @DisplayName("Clear xóa toàn bộ entries trong bảng")
    void testClear_RemovesAllEntries() {
        long key = 111L;
        tt.store(key, 2, 50, TranspositionTable.Flag.EXACT);
        assertNotNull(tt.get(key));

        tt.clear();
        assertNull(tt.get(key));
    }

    @Test
    @DisplayName("Khởi tạo kích thước không phải lũy thừa của 2 ném IllegalArgumentException")
    void testConstructor_InvalidSize_Throws() {
        assertThrows(IllegalArgumentException.class, () -> new TranspositionTable(1000));
        assertThrows(IllegalArgumentException.class, () -> new TranspositionTable(0));
        assertThrows(IllegalArgumentException.class, () -> new TranspositionTable(-16));
        assertDoesNotThrow(() -> new TranspositionTable(65536));
    }
}
