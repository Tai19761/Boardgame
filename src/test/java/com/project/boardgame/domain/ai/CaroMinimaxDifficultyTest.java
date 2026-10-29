package com.project.boardgame.domain.ai;

import com.project.boardgame.domain.models.BoardState;
import com.project.boardgame.domain.models.Piece;
import com.project.boardgame.domain.models.Player;
import com.project.boardgame.domain.models.Position;
import com.project.boardgame.domain.rules.CaroRuleEngine;
import com.project.boardgame.domain.rules.GameRuleEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CaroMinimax 4 Difficulty Levels Tests")
class CaroMinimaxDifficultyTest {

    private GameRuleEngine ruleEngine;

    @BeforeEach
    void setUp() {
        ruleEngine = new CaroRuleEngine();
    }

    private static Position p(int x, int y) {
        return new Position(x, y);
    }

    private static BoardState board(Player current, Map<Position, Player> placed) {
        Map<Position, Piece> pieces = new HashMap<>();
        placed.forEach((pos, player) -> pieces.put(pos, new Piece(player)));
        return new BoardState(pieces, current);
    }

    @Test
    @DisplayName("Cả 4 cấp độ: Bàn trống luôn đánh vào ô giữa (7, 7)")
    void allDifficulties_EmptyBoard_PlaysCenter() {
        for (AiDifficulty diff : AiDifficulty.values()) {
            CaroMinimax minimax = new CaroMinimax(ruleEngine, 15, diff);
            assertEquals(diff, minimax.getDifficulty());
            BoardState empty = new BoardState(Map.of(), Player.PLAYER_2);
            assertEquals(p(7, 7), minimax.findBestMove(empty, Player.PLAYER_2), "Độ khó " + diff + " phải đánh ô giữa khi bàn trống");
        }
    }

    @Test
    @DisplayName("Cấp độ EASY: Có nước thắng ngay (4 quân đã có) -> BẮT BUỘC đánh thắng ngay (không đi ngẫu nhiên)")
    void easy_HasWinningMove_PlaysImmediately() {
        CaroMinimax easy = new CaroMinimax(ruleEngine, 15, AiDifficulty.EASY);

        // Bot (PLAYER_2) có 4 quân: (3, 5), (4, 5), (5, 5), (6, 5)
        Map<Position, Player> placed = new HashMap<>();
        placed.put(p(3, 5), Player.PLAYER_2);
        placed.put(p(4, 5), Player.PLAYER_2);
        placed.put(p(5, 5), Player.PLAYER_2);
        placed.put(p(6, 5), Player.PLAYER_2);
        placed.put(p(0, 0), Player.PLAYER_1); // Đối thủ ở xa

        BoardState b = board(Player.PLAYER_2, placed);
        Position move = easy.findBestMove(b, Player.PLAYER_2);

        assertTrue(move.equals(p(2, 5)) || move.equals(p(7, 5)), "EASY phải đánh nước thắng ngay tại (2,5) hoặc (7,5)");
    }

    @Test
    @DisplayName("Cấp độ EASY: Đối thủ có 4 quân sắp thắng ở lượt sau -> BẮT BUỘC chặn (không đi ngớ ngẩn)")
    void easy_OpponentHasWinningThreat_BlocksImmediately() {
        CaroMinimax easy = new CaroMinimax(ruleEngine, 15, AiDifficulty.EASY);

        // Đối thủ (PLAYER_1) có 4 quân dọc: (7, 3), (7, 4), (7, 5), (7, 6)
        Map<Position, Player> placed = new HashMap<>();
        placed.put(p(7, 3), Player.PLAYER_1);
        placed.put(p(7, 4), Player.PLAYER_1);
        placed.put(p(7, 5), Player.PLAYER_1);
        placed.put(p(7, 6), Player.PLAYER_1);
        placed.put(p(0, 0), Player.PLAYER_2); // Bot ở xa

        BoardState b = board(Player.PLAYER_2, placed);
        Position move = easy.findBestMove(b, Player.PLAYER_2);

        assertTrue(move.equals(p(7, 2)) || move.equals(p(7, 7)), "EASY phải chặn đầu tại (7,2) hoặc (7,7)");
    }

    @Test
    @DisplayName("Cấp độ HARD: Depth = 4, tính toán sâu và trả về nước đi hợp lệ")
    void hard_FindsMoveFast() {
        CaroMinimax hard = new CaroMinimax(ruleEngine, 15, AiDifficulty.HARD);
        assertEquals(4, hard.getDepth());

        Map<Position, Player> placed = new HashMap<>();
        placed.put(p(7, 7), Player.PLAYER_1);
        BoardState b = board(Player.PLAYER_2, placed);

        Position move = hard.findBestMove(b, Player.PLAYER_2);
        assertNotNull(move);
        assertTrue(ruleEngine.isValidMove(b, new com.project.boardgame.domain.models.MoveStep(null, move), Player.PLAYER_2));
    }

    @Test
    @DisplayName("Cấp độ EXPERT: Depth = 5 kích hoạt Transposition Table, phản hồi nhanh dưới 2 giây")
    void expert_WithTranspositionTable_RunsWithinTimeLimit() {
        CaroMinimax expert = new CaroMinimax(ruleEngine, 15, AiDifficulty.EXPERT);
        assertEquals(5, expert.getDepth());

        Map<Position, Player> placed = new HashMap<>();
        placed.put(p(7, 7), Player.PLAYER_1);
        placed.put(p(7, 8), Player.PLAYER_2);
        placed.put(p(6, 7), Player.PLAYER_1);
        BoardState b = board(Player.PLAYER_2, placed);

        long start = System.currentTimeMillis();
        Position move = expert.findBestMove(b, Player.PLAYER_2);
        long elapsed = System.currentTimeMillis() - start;

        assertNotNull(move);
        assertTrue(ruleEngine.isValidMove(b, new com.project.boardgame.domain.models.MoveStep(null, move), Player.PLAYER_2));
        assertTrue(elapsed < 2000, "EXPERT phải hoàn thành trong vòng dưới 2000ms (thực tế: " + elapsed + "ms)");
    }
}
