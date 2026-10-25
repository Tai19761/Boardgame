package com.project.boardgame.domain.ai;

import com.project.boardgame.domain.models.BoardState;
import com.project.boardgame.domain.models.Piece;
import com.project.boardgame.domain.models.Player;
import com.project.boardgame.domain.models.Position;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CaroHeuristicEvaluatorTest {

    private final CaroHeuristicEvaluator evaluator = new CaroHeuristicEvaluator(15);

    private static BoardState row(Player player, int y, int fromX, int toX, Map<Position, Player> extra) {
        Map<Position, Piece> pieces = new HashMap<>();
        for (int x = fromX; x <= toX; x++) {
            pieces.put(new Position(x, y), new Piece(player));
        }
        extra.forEach((pos, pl) -> pieces.put(pos, new Piece(pl)));
        return new BoardState(pieces, Player.PLAYER_1);
    }

    @Test
    @DisplayName("Bàn trống: điểm đánh giá bằng 0")
    void emptyBoard_ScoresZero() {
        assertEquals(0, evaluator.evaluate(new BoardState(Map.of(), Player.PLAYER_1), Player.PLAYER_2));
    }

    @Test
    @DisplayName("Điểm đối xứng: thế mạnh của mình là dương, của đối thủ là âm")
    void score_IsPositiveForOwnerNegativeForOpponent() {
        BoardState state = row(Player.PLAYER_2, 7, 5, 7, Map.of());

        assertTrue(evaluator.evaluate(state, Player.PLAYER_2) > 0);
        assertTrue(evaluator.evaluate(state, Player.PLAYER_1) < 0);
    }

    @Test
    @DisplayName("Bốn mở > ba mở > hai mở")
    void longerOpenLines_ScoreHigher() {
        int open4 = evaluator.evaluate(row(Player.PLAYER_2, 7, 4, 7, Map.of()), Player.PLAYER_2);
        int open3 = evaluator.evaluate(row(Player.PLAYER_2, 7, 5, 7, Map.of()), Player.PLAYER_2);
        int open2 = evaluator.evaluate(row(Player.PLAYER_2, 7, 6, 7, Map.of()), Player.PLAYER_2);

        assertTrue(open4 > open3, "Bốn mở phải lớn hơn ba mở");
        assertTrue(open3 > open2, "Ba mở phải lớn hơn hai mở");
    }

    @Test
    @DisplayName("Chuỗi bị chặn một đầu có điểm thấp hơn chuỗi mở hai đầu")
    void halfBlockedLine_ScoresLowerThanOpenLine() {
        int open3 = evaluator.evaluate(row(Player.PLAYER_2, 7, 5, 7, Map.of()), Player.PLAYER_2);
        // Chặn đầu trái bằng quân đối thủ: chỉ so sánh phần điểm của PLAYER_2 nên dùng biên bàn cờ thay vì quân
        int blocked3 = evaluator.evaluate(row(Player.PLAYER_2, 7, 0, 2, Map.of()), Player.PLAYER_2);

        assertTrue(open3 > blocked3, "Ba bị chặn biên phải thấp hơn ba mở");
    }

    @Test
    @DisplayName("Năm quân liên tiếp: điểm thắng tuyệt đối")
    void fiveInRow_ScoresWin() {
        int five = evaluator.evaluate(row(Player.PLAYER_2, 7, 3, 7, Map.of()), Player.PLAYER_2);

        assertTrue(five >= CaroHeuristicEvaluator.FIVE_SCORE);
    }
}
