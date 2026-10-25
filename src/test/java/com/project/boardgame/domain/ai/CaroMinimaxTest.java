package com.project.boardgame.domain.ai;

import com.project.boardgame.domain.models.BoardState;
import com.project.boardgame.domain.models.MoveStep;
import com.project.boardgame.domain.models.Piece;
import com.project.boardgame.domain.models.Player;
import com.project.boardgame.domain.models.Position;
import com.project.boardgame.domain.rules.CaroRuleEngine;
import com.project.boardgame.domain.rules.GameRuleEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class CaroMinimaxTest {

    private static final int BOARD_SIZE = 15;

    private GameRuleEngine ruleEngine;
    private CaroMinimax minimax;

    @BeforeEach
    void setUp() {
        // Engine được truyền vào qua interface GameRuleEngine (production lấy từ factory)
        ruleEngine = new CaroRuleEngine();
        minimax = new CaroMinimax(ruleEngine, new CaroHeuristicEvaluator(BOARD_SIZE), BOARD_SIZE, CaroMinimax.DEFAULT_DEPTH);
    }

    private static BoardState board(Player current, Map<Position, Player> placed) {
        Map<Position, Piece> pieces = new HashMap<>();
        placed.forEach((pos, player) -> pieces.put(pos, new Piece(player)));
        return new BoardState(pieces, current);
    }

    private static Position p(int x, int y) {
        return new Position(x, y);
    }

    @Test
    @DisplayName("Bàn trống: Bot đánh mặc định ô giữa (7,7)")
    void emptyBoard_PlaysCenter() {
        BoardState empty = new BoardState(Map.of(), Player.PLAYER_2);

        assertEquals(p(7, 7), minimax.findBestMove(empty, Player.PLAYER_2));
    }

    @Test
    @DisplayName("Case 1 - Đi nước thắng: Bot có 4 quân liên tiếp (1 đầu bị chặn) → đặt quân thứ 5 vào đầu còn lại")
    void hasFourInRow_PlaysWinningMove() {
        Map<Position, Player> placed = new HashMap<>();
        // Bot (PLAYER_2): 4 quân ngang hàng y=5 từ x=3..6
        placed.put(p(3, 5), Player.PLAYER_2);
        placed.put(p(4, 5), Player.PLAYER_2);
        placed.put(p(5, 5), Player.PLAYER_2);
        placed.put(p(6, 5), Player.PLAYER_2);
        // PLAYER_1: chặn đầu trái (2,5), các quân khác rải rác không tạo đe doạ
        placed.put(p(2, 5), Player.PLAYER_1);
        placed.put(p(10, 10), Player.PLAYER_1);
        placed.put(p(12, 10), Player.PLAYER_1);
        placed.put(p(10, 12), Player.PLAYER_1);
        placed.put(p(12, 12), Player.PLAYER_1);

        Position move = minimax.findBestMove(board(Player.PLAYER_2, placed), Player.PLAYER_2);

        assertEquals(p(7, 5), move, "Bot phải đánh vào ô (7,5) để tạo 5 quân liên tiếp");
    }

    @Test
    @DisplayName("Case 1b - Ưu tiên thắng hơn chặn: cả hai bên đều có 4 quân → Bot chọn nước thắng ngay")
    void bothHaveFour_PrefersWinningOverBlocking() {
        Map<Position, Player> placed = new HashMap<>();
        // Bot (PLAYER_2): hàng y=3, x=3..6, đầu trái bị chặn bởi (2,3)
        placed.put(p(3, 3), Player.PLAYER_2);
        placed.put(p(4, 3), Player.PLAYER_2);
        placed.put(p(5, 3), Player.PLAYER_2);
        placed.put(p(6, 3), Player.PLAYER_2);
        // PLAYER_1: hàng y=9, x=3..6, đầu trái bị chặn bởi (2,9) + quân chặn (2,3)
        placed.put(p(3, 9), Player.PLAYER_1);
        placed.put(p(4, 9), Player.PLAYER_1);
        placed.put(p(5, 9), Player.PLAYER_1);
        placed.put(p(6, 9), Player.PLAYER_1);
        placed.put(p(2, 3), Player.PLAYER_1);
        placed.put(p(2, 9), Player.PLAYER_2);

        Position move = minimax.findBestMove(board(Player.PLAYER_2, placed), Player.PLAYER_2);

        assertEquals(p(7, 3), move, "Bot phải thắng ngay tại (7,3) thay vì chặn (7,9)");
    }

    @Test
    @DisplayName("Case 2 - Chặn đối thủ: đối thủ có 4 quân liên tiếp (1 đầu bị chặn) → Bot chặn đầu còn lại")
    void opponentHasFour_BlocksIt() {
        Map<Position, Player> placed = new HashMap<>();
        // PLAYER_1: hàng y=7, x=3..6
        placed.put(p(3, 7), Player.PLAYER_1);
        placed.put(p(4, 7), Player.PLAYER_1);
        placed.put(p(5, 7), Player.PLAYER_1);
        placed.put(p(6, 7), Player.PLAYER_1);
        // Bot (PLAYER_2): đã chặn đầu trái (2,7), 2 quân rải rác khác
        placed.put(p(2, 7), Player.PLAYER_2);
        placed.put(p(10, 2), Player.PLAYER_2);
        placed.put(p(12, 3), Player.PLAYER_2);

        Position move = minimax.findBestMove(board(Player.PLAYER_2, placed), Player.PLAYER_2);

        assertEquals(p(7, 7), move, "Bot phải chặn ô (7,7) để đối thủ không đạt 5 quân");
    }

    @Test
    @DisplayName("Case 2b - Chặn đối thủ theo đường chéo: đối thủ có 4 quân chéo (1 đầu bị chặn)")
    void opponentHasDiagonalFour_BlocksIt() {
        Map<Position, Player> placed = new HashMap<>();
        // PLAYER_1: chéo chính (4,4) → (7,7)
        placed.put(p(4, 4), Player.PLAYER_1);
        placed.put(p(5, 5), Player.PLAYER_1);
        placed.put(p(6, 6), Player.PLAYER_1);
        placed.put(p(7, 7), Player.PLAYER_1);
        // Bot: chặn (3,3) và 2 quân khác
        placed.put(p(3, 3), Player.PLAYER_2);
        placed.put(p(12, 1), Player.PLAYER_2);
        placed.put(p(1, 12), Player.PLAYER_2);

        Position move = minimax.findBestMove(board(Player.PLAYER_2, placed), Player.PLAYER_2);

        assertEquals(p(8, 8), move, "Bot phải chặn đầu chéo còn lại (8,8)");
    }

    @Test
    @DisplayName("Case 3 - Nước hợp lệ: nước đi nằm trong bàn, ô trống, hợp luật và trong bán kính 2 quanh quân đã có")
    void midGame_ReturnsLegalMoveWithinRadius() {
        Map<Position, Player> placed = new HashMap<>();
        placed.put(p(7, 7), Player.PLAYER_1);
        placed.put(p(8, 8), Player.PLAYER_2);
        placed.put(p(7, 8), Player.PLAYER_1);
        placed.put(p(6, 6), Player.PLAYER_2);
        placed.put(p(9, 7), Player.PLAYER_1);
        BoardState state = board(Player.PLAYER_2, placed);

        Position move = minimax.findBestMove(state, Player.PLAYER_2);

        assertNotNull(move);
        assertTrue(move.x() >= 0 && move.x() < BOARD_SIZE && move.y() >= 0 && move.y() < BOARD_SIZE, "Nước đi phải nằm trong bàn cờ");
        assertFalse(state.pieces().containsKey(move), "Nước đi phải vào ô trống");
        assertTrue(ruleEngine.isValidMove(state, new MoveStep(null, move), Player.PLAYER_2), "Nước đi phải hợp luật theo GameRuleEngine");
        boolean nearExisting = state.pieces().keySet().stream()
                .anyMatch(q -> Math.abs(q.x() - move.x()) <= 2 && Math.abs(q.y() - move.y()) <= 2);
        assertTrue(nearExisting, "Nước đi phải nằm trong bán kính 2 quanh quân đã có");
    }

    @Test
    @DisplayName("Sinh ứng viên: chỉ các ô trống trong bán kính 2 (1 quân ở giữa → 24 ô)")
    void generateCandidates_SinglePieceCenter_Returns24CellsWithinRadius2() {
        BoardState state = board(Player.PLAYER_2, Map.of(p(7, 7), Player.PLAYER_1));

        List<Position> candidates = minimax.generateCandidates(state);

        assertEquals(24, candidates.size(), "Hình vuông 5x5 trừ ô đã có quân = 24 ô");
        assertFalse(candidates.contains(p(7, 7)));
        assertTrue(candidates.stream().allMatch(c -> Math.abs(c.x() - 7) <= 2 && Math.abs(c.y() - 7) <= 2));
    }

    @Test
    @DisplayName("Sinh ứng viên: quân ở góc (0,0) → chỉ 8 ô hợp lệ trong bàn")
    void generateCandidates_CornerPiece_ClipsToBoard() {
        BoardState state = board(Player.PLAYER_2, Map.of(p(0, 0), Player.PLAYER_1));

        List<Position> candidates = minimax.generateCandidates(state);

        assertEquals(8, candidates.size(), "Hình vuông 3x3 trong bàn trừ ô (0,0) = 8 ô");
        assertTrue(candidates.stream().allMatch(c -> c.x() >= 0 && c.y() >= 0 && c.x() <= 2 && c.y() <= 2));
    }

    @Test
    @DisplayName("Minimax ủy quyền luật cho GameRuleEngine được inject (không tự tạo CaroRuleEngine)")
    void usesInjectedRuleEngine() {
        AtomicInteger executeCalls = new AtomicInteger();
        GameRuleEngine delegate = new CaroRuleEngine();
        GameRuleEngine spy = new GameRuleEngine() {
            @Override public BoardState initializeBoard() { return delegate.initializeBoard(); }
            @Override public boolean isValidMove(BoardState b, MoveStep m, Player pl) { return delegate.isValidMove(b, m, pl); }
            @Override public BoardState executeMove(BoardState b, MoveStep m, Player pl) {
                executeCalls.incrementAndGet();
                return delegate.executeMove(b, m, pl);
            }
            @Override public boolean isGameOver(BoardState b) { return delegate.isGameOver(b); }
            @Override public Optional<Player> checkWinner(BoardState b, Position last) { return delegate.checkWinner(b, last); }
        };
        CaroMinimax spyMinimax = new CaroMinimax(spy, new CaroHeuristicEvaluator(BOARD_SIZE), BOARD_SIZE, 2);

        spyMinimax.findBestMove(board(Player.PLAYER_2, Map.of(p(7, 7), Player.PLAYER_1)), Player.PLAYER_2);

        assertTrue(executeCalls.get() > 0, "CaroMinimax phải sinh trạng thái con qua GameRuleEngine được inject");
    }
}
