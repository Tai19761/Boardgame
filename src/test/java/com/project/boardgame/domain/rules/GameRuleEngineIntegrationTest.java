package com.project.boardgame.domain.rules;

import com.project.boardgame.domain.models.BoardState;
import com.project.boardgame.domain.models.Piece;
import com.project.boardgame.domain.models.Player;
import com.project.boardgame.domain.models.Position;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test mô phỏng đúng luồng điều phối của MatchEngineService:
 * 1. Gọi isGameOver(board) trước.
 * 2. Nếu isGameOver == false -> Trạng thái ván đấu là ONGOING (không được coi Optional.empty là Hòa).
 * 3. Nếu isGameOver == true -> Gọi checkWinner(board, lastMove):
 *    - Optional.of(Player) -> WIN
 *    - Optional.empty()    -> DRAW (Hòa)
 */
class GameRuleEngineIntegrationTest {

    enum MatchStatus { ONGOING, WIN_P1, WIN_P2, DRAW }

    private MatchStatus evaluateMatchStatus(GameRuleEngine engine, BoardState board, Position lastMove) {
        if (!engine.isGameOver(board)) {
            return MatchStatus.ONGOING;
        }
        Optional<Player> winner = engine.checkWinner(board, lastMove);
        if (winner.isPresent()) {
            return winner.get() == Player.PLAYER_1 ? MatchStatus.WIN_P1 : MatchStatus.WIN_P2;
        }
        return MatchStatus.DRAW;
    }

    @Test
    void testOAnQuan_OrchestrationFlow_Ongoing_Win_Draw() {
        GameRuleEngine engine = new OAnQuanRuleEngine();

        // 1. Trạng thái ONGOING: Bàn cờ còn Quan
        BoardState ongoingBoard = OAnQuanRuleEngine.createInitialBoard();
        assertEquals(MatchStatus.ONGOING, evaluateMatchStatus(engine, ongoingBoard, null));
        // Kiểm chứng quy ước: nếu tầng trên gọi nhầm checkWinner mà không check isGameOver thì nhận empty
        assertEquals(Optional.empty(), engine.checkWinner(ongoingBoard, null),
                "checkWinner trả về empty khi chưa gameOver");
        assertFalse(engine.isGameOver(ongoingBoard));

        // 2. Trạng thái WIN (Game Over + P1 nhiều điểm hơn): Cả 2 quan hết quân, P1 > P2
        Map<Position, Integer> winCounts = new HashMap<>();
        for (int i = 0; i < 12; i++) {
            winCounts.put(OAnQuanRuleEngine.POSITIONS[i], 0);
        }
        // 2 Quan = 0
        winCounts.put(OAnQuanRuleEngine.POSITIONS[0], 0);
        winCounts.put(OAnQuanRuleEngine.POSITIONS[6], 0);
        BoardState winBoard = new BoardState(Map.of(), winCounts, 35, 25, Player.PLAYER_1);

        assertTrue(engine.isGameOver(winBoard));
        assertEquals(MatchStatus.WIN_P1, evaluateMatchStatus(engine, winBoard, null));
        assertEquals(Optional.of(Player.PLAYER_1), engine.checkWinner(winBoard, null));

        // 3. Trạng thái DRAW (Game Over + 2 bên bằng điểm): Cả 2 quan hết quân, P1 == P2
        Map<Position, Integer> drawCounts = new HashMap<>();
        for (int i = 0; i < 12; i++) {
            drawCounts.put(OAnQuanRuleEngine.POSITIONS[i], 0);
        }
        drawCounts.put(OAnQuanRuleEngine.POSITIONS[0], 0);
        drawCounts.put(OAnQuanRuleEngine.POSITIONS[6], 0);
        BoardState drawBoard = new BoardState(Map.of(), drawCounts, 30, 30, Player.PLAYER_1);

        assertTrue(engine.isGameOver(drawBoard));
        assertEquals(MatchStatus.DRAW, evaluateMatchStatus(engine, drawBoard, null));
        assertEquals(Optional.empty(), engine.checkWinner(drawBoard, null));
    }

    @Test
    void testCaro_OrchestrationFlow_Ongoing_Win_Draw() {
        GameRuleEngine engine = new CaroRuleEngine();

        // 1. ONGOING
        BoardState emptyBoard = new BoardState(Map.of(), Player.PLAYER_1);
        assertFalse(engine.isGameOver(emptyBoard));
        assertEquals(MatchStatus.ONGOING, evaluateMatchStatus(engine, emptyBoard, null));

        // 2. WIN
        Map<Position, Piece> winPieces = new HashMap<>();
        for (int x = 0; x < 5; x++) {
            winPieces.put(new Position(x, 0), new Piece(Player.PLAYER_1));
        }
        BoardState winBoard = new BoardState(winPieces, Player.PLAYER_1);
        Position lastMove = new Position(4, 0);

        assertTrue(engine.isGameOver(winBoard));
        assertEquals(MatchStatus.WIN_P1, evaluateMatchStatus(engine, winBoard, lastMove));

        // 3. DRAW (Bàn cờ 15x15 đầy quân, không có chuỗi 5 liên tiếp nào)
        Map<Position, Piece> drawPieces = new HashMap<>();
        for (int x = 0; x < 15; x++) {
            for (int y = 0; y < 15; y++) {
                Player p = ((x / 2 + y) % 2 == 0) ? Player.PLAYER_1 : Player.PLAYER_2;
                drawPieces.put(new Position(x, y), new Piece(p));
            }
        }
        BoardState fullBoard = new BoardState(drawPieces, Player.PLAYER_1);
        assertTrue(engine.isGameOver(fullBoard));
        assertEquals(MatchStatus.DRAW, evaluateMatchStatus(engine, fullBoard, new Position(14, 14)));
    }

    @Test
    void testGanh_OrchestrationFlow_Ongoing_Win() {
        GameRuleEngine engine = new GanhRuleEngine();

        // 1. ONGOING
        Map<Position, Piece> pieces = new HashMap<>();
        pieces.put(new Position(0, 0), new Piece(Player.PLAYER_1));
        pieces.put(new Position(4, 4), new Piece(Player.PLAYER_2));
        BoardState ongoingBoard = new BoardState(pieces, Player.PLAYER_1);

        assertFalse(engine.isGameOver(ongoingBoard));
        assertEquals(MatchStatus.ONGOING, evaluateMatchStatus(engine, ongoingBoard, null));

        // 2. WIN (Player 2 hết quân)
        Map<Position, Piece> winPieces = new HashMap<>();
        winPieces.put(new Position(0, 0), new Piece(Player.PLAYER_1));
        BoardState winBoard = new BoardState(winPieces, Player.PLAYER_1);

        assertTrue(engine.isGameOver(winBoard));
        assertEquals(MatchStatus.WIN_P1, evaluateMatchStatus(engine, winBoard, null));
    }
}
