package com.project.boardgame.domain.ai;

import com.project.boardgame.domain.models.BoardState;
import com.project.boardgame.domain.models.Piece;
import com.project.boardgame.domain.models.Player;
import com.project.boardgame.domain.models.Position;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ZobristKeyGenerator Unit Tests")
class ZobristKeyGeneratorTest {

    @Test
    @DisplayName("Khóa ngẫu nhiên cố định: cùng tọa độ và người chơi luôn cho ra cùng một khóa")
    void testDeterminism_SameCoordinatesAndPlayer_ReturnSameKey() {
        long key1 = ZobristKeyGenerator.getPieceKey(7, 7, Player.PLAYER_1);
        long key2 = ZobristKeyGenerator.getPieceKey(7, 7, Player.PLAYER_1);
        assertEquals(key1, key2);

        long keySide1 = ZobristKeyGenerator.getSideToMoveKey();
        long keySide2 = ZobristKeyGenerator.getSideToMoveKey();
        assertEquals(keySide1, keySide2);
    }

    @Test
    @DisplayName("Khóa phân biệt: khác tọa độ hoặc khác người chơi cho ra khóa khác nhau")
    void testDifferentCoordinatesAndPlayers_HaveDifferentKeys() {
        long keyP1 = ZobristKeyGenerator.getPieceKey(7, 7, Player.PLAYER_1);
        long keyP2 = ZobristKeyGenerator.getPieceKey(7, 7, Player.PLAYER_2);
        assertNotEquals(keyP1, keyP2, "Hai người chơi tại cùng vị trí phải có khóa Zobrist khác nhau");

        long keyPos1 = ZobristKeyGenerator.getPieceKey(0, 0, Player.PLAYER_1);
        long keyPos2 = ZobristKeyGenerator.getPieceKey(0, 1, Player.PLAYER_1);
        assertNotEquals(keyPos1, keyPos2, "Hai vị trí khác nhau phải có khóa Zobrist khác nhau");
    }

    @Test
    @DisplayName("XOR tính chất đảo ngược: đặt quân rồi rút quân về lại mã băm ban đầu")
    void testInvertibleXor_ApplyingAndRevertingMove_RestoresOriginalHash() {
        BoardState emptyBoard = new BoardState(Map.of(), Player.PLAYER_1);
        long initialHash = ZobristKeyGenerator.computeHash(emptyBoard);

        Position pos = new Position(5, 5);
        long hashAfterMove = ZobristKeyGenerator.updateMove(initialHash, pos, Player.PLAYER_1);
        assertNotEquals(initialHash, hashAfterMove);

        // Đảo ngược nước đi bằng cách XOR lại chính quân cờ đó và đổi lại lượt
        long revertedHash = ZobristKeyGenerator.updateMove(hashAfterMove, pos, Player.PLAYER_1);
        assertEquals(initialHash, revertedHash, "XOR hai lần cùng giá trị phải phục hồi mã băm ban đầu");
    }

    @Test
    @DisplayName("Mã băm gia tăng (Incremental) khớp 100% với tính toán từ đầu (Full computation)")
    void testIncrementalMatchesFullComputation() {
        BoardState board1 = new BoardState(Map.of(), Player.PLAYER_1);
        long hash1 = ZobristKeyGenerator.computeHash(board1);

        Position p1 = new Position(7, 7);
        long hash2Incremental = ZobristKeyGenerator.updateMove(hash1, p1, Player.PLAYER_1);

        BoardState board2 = new BoardState(Map.of(p1, new Piece(Player.PLAYER_1)), Player.PLAYER_2);
        long hash2Full = ZobristKeyGenerator.computeHash(board2);

        assertEquals(hash2Full, hash2Incremental, "Mã băm cập nhật O(1) phải trùng khớp với mã băm tính từ đầu");
    }

    @Test
    @DisplayName("Kiểm tra biên: ném IllegalArgumentException khi tọa độ vượt ngoài 0..24")
    void testBoundsCheck_ThrowsWhenOutOfBounds() {
        assertThrows(IllegalArgumentException.class, () -> ZobristKeyGenerator.getPieceKey(-1, 0, Player.PLAYER_1));
        assertThrows(IllegalArgumentException.class, () -> ZobristKeyGenerator.getPieceKey(0, 25, Player.PLAYER_1));
        assertThrows(NullPointerException.class, () -> ZobristKeyGenerator.getPieceKey(0, 0, null));
    }
}
